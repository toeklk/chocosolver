package org.clafer.ast.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.clafer.assertion.Assertion;
import org.clafer.ast.AstAbstractClafer;
import org.clafer.ast.AstBoolExpr;
import org.clafer.ast.AstClafer;
import org.clafer.ast.AstConcreteClafer;
import org.clafer.ast.AstConstraint;
import org.clafer.ast.AstException;
import org.clafer.ast.AstExpr;
import org.clafer.ast.AstModel;
import org.clafer.ast.AstRef;
import org.clafer.ast.AstSetExpr;
import org.clafer.ast.AstUtil;
import org.clafer.ast.Card;
import org.clafer.ast.ProductType;
import org.clafer.collection.Pair;
import org.clafer.common.Util;
import org.clafer.domain.Domain;
import org.clafer.objective.Objective;
import org.clafer.scope.Scopable;
import org.clafer.scope.Scope;

/**
 *
 * @author jimmy
 */
public class Analysis {

    private final AstModel model;
    private Scope scope;
    private Objective[] objectives;
    private Map<Objective, AstSetExpr> objectiveExprs;
    private Assertion[] assertions;
    private Map<Assertion, AstBoolExpr> assertionExprs;
    private final List<AstClafer> clafers;
    private final List<AstAbstractClafer> abstractClafers;
    private final List<AstConcreteClafer> concreteClafers;
    private final List<Set<AstClafer>> clafersInParentAndSubOrder;
    private List<AstConstraint> constraints;
    private Set<AstConstraint> hardConstraints;
    private Map<AstConstraint, AstBoolExpr> constraintExprs;
    private Map<AstConcreteClafer, Card> cardMap;
    private Map<AstClafer, Card> globalCardMap;
    private Map<AstClafer, Format> formatMap;
    private Map<AstAbstractClafer, Offsets> offsetMap;
    private Map<AstClafer, PartialSolution> partialSolutionMap;
    private Map<AstConcreteClafer, Domain[]> partialIntsMap;
    private Map<AstClafer, AstConcreteClafer[]> breakableChildrenMap;
    private Map<AstRef, int[]> breakableRefsMap;
    private Map<AstClafer, AstRef[]> breakableTargetsMap;
    private Map<AstExpr, Type> typeMap;
    private Map<AstClafer, AstClafer> inverseMap;

    Analysis(AstModel model, Scope scope) {
        this(model, scope, new Objective[0], new Assertion[0]);
    }

    Analysis(AstModel model, Scope scope, Objective[] objectives, Assertion[] assertions) {
        this(model, scope, objectives, assertions,
                AstUtil.getAbstractClafersInSubOrder(model),
                AstUtil.getConcreteClafers(model),
                AstUtil.getClafersInParentAndSubOrder(model));
    }

    Analysis(AstModel model, Scope scope,
            Objective[] objectives,
            Assertion[] assertions,
            List<AstAbstractClafer> abstractClafers,
            List<AstConcreteClafer> concreteClafers,
            List<Set<AstClafer>> clafersInParentAndSubOrder) {
        this.model = model;
        this.scope = scope;
        this.objectives = objectives;
        this.objectiveExprs = new HashMap<>(objectives.length);
        for (Objective objective : objectives) {
            this.objectiveExprs.put(objective, objective.getExpr());
        }
        this.assertions = assertions;
        this.assertionExprs = new HashMap<>(assertions.length);
        for (Assertion assertion : assertions) {
            this.assertionExprs.put(assertion, assertion.getExpr());
        }
        this.clafers = append(abstractClafers, concreteClafers);
        this.abstractClafers = abstractClafers;
        this.concreteClafers = concreteClafers;
        this.clafersInParentAndSubOrder = clafersInParentAndSubOrder;
        this.constraints = AstUtil.getNestedConstraints(model);
        this.hardConstraints = new HashSet<>(constraints.size());
        this.constraintExprs = new HashMap<>(constraints.size());
        for (AstConstraint constraint : constraints) {
            if (constraint.isHard()) {
                hardConstraints.add(constraint);
            }
            constraintExprs.put(constraint, constraint.getExpr());
        }
        this.cardMap = buildCardMap(clafers);
        this.inverseMap = new HashMap<>(0);
    }

    private static Map<AstConcreteClafer, Card> buildCardMap(List<AstClafer> clafers) {
        Map<AstConcreteClafer, Card> cardMap = new HashMap<>();
        for (AstClafer clafer : clafers) {
            if (clafer instanceof AstConcreteClafer) {
                AstConcreteClafer concreteClafer = (AstConcreteClafer) clafer;
                cardMap.put(concreteClafer, concreteClafer.getCard());
            }
        }
        return cardMap;
    }

    private static List<AstClafer> append(List<? extends AstClafer> abstractClafers, List<? extends AstConcreteClafer> concreteClafers) {
        List<AstClafer> clafers = new ArrayList<>(abstractClafers.size() + concreteClafers.size());
        clafers.addAll(abstractClafers);
        clafers.addAll(concreteClafers);
        return clafers;
    }

    public static Analysis analyze(AstModel model, Scopable scope, Analyzer... analyzers) {
        return analyze(model, scope, new Objective[0], analyzers);
    }

    public static Analysis analyze(AstModel model, Scopable scope, Objective[] objectives, Analyzer... analyzers) {
        Analysis analysis = new Analysis(model, scope.toScope(), objectives, new Assertion[0]);
        for (Analyzer analyzer : analyzers) {
            analysis = analyzer.analyze(analysis);
        }
        return analysis;
    }

    public static Analysis analyze(AstModel model, Scopable scope, Assertion[] assertions, Analyzer... analyzers) {
        Analysis analysis = new Analysis(model, scope.toScope(), new Objective[0], assertions);
        for (Analyzer analyzer : analyzers) {
            analysis = analyzer.analyze(analysis);
        }
        return analysis;
    }

    private <T> T notNull(String analysisName, T t) {
        if (t == null) {
            throw new AnalysisException(analysisName + " not yet analyzed.");
        }
        return t;
    }

    private <T> T notNull(String key, String analysisName, T t) {
        if (t == null) {
            throw new AnalysisException(analysisName + " for " + key + " not yet analyzed.");
        }
        return t;
    }

    private <T> T notNull(AstClafer key, String analysisName, T t) {
        return notNull(key.getName(), analysisName, t);
    }

    /**
     * Returns the original model. Analyzers are forbidden to alter the original
     * model.
     *
     * @return the original model
     */
    public AstModel getModel() {
        return model;
    }

    public int getScope(AstClafer clafer) {
        return getScope().getScope(clafer);
    }

    public int getScope(ProductType clafer) {
        int product = 1;
        for (AstClafer type : clafer.getProduct()) {
            product *= getScope(type);
        }
        return product;
    }

    public Scope getScope() {
        return notNull("Scope", scope);
    }

    public Analysis setScope(Scope scope) {
        this.scope = scope;
        return this;
    }

    public AstSetExpr getExpr(Objective objective) {
        return objectiveExprs.get(objective);
    }

    public Objective[] getObjectives() {
        return objectives;
    }

    public Map<Objective, AstSetExpr> getObjectiveExprs() {
        return Collections.unmodifiableMap(objectiveExprs);
    }

    public Analysis setObjectiveExprs(Map<Objective, AstSetExpr> objectiveExprs) {
        this.objectiveExprs = objectiveExprs;
        return this;
    }

    public AstBoolExpr getExpr(Assertion assertion) {
        return assertionExprs.get(assertion);
    }

    public Assertion[] getAssertions() {
        return assertions;
    }

    public Map<Assertion, AstBoolExpr> getAssertionExprs() {
        return assertionExprs;
    }

    public Analysis setAssertionExprs(Map<Assertion, AstBoolExpr> assertionExprs) {
        this.assertionExprs = assertionExprs;
        return this;
    }

    /**
     * @return the Clafers in no specific order
     */
    public List<AstClafer> getClafers() {
        return Collections.unmodifiableList(clafers);
    }

    /**
     * @return the abstract Clafers in sub-order
     */
    public List<AstAbstractClafer> getAbstractClafers() {
        return Collections.unmodifiableList(abstractClafers);
    }

    /**
     * @return the concrete Clafers in parent-order
     */
    public List<AstConcreteClafer> getConcreteClafers() {
        return Collections.unmodifiableList(concreteClafers);
    }

    /**
     * @return the Clafers in parent-order and sub-order
     */
    public List<Set<AstClafer>> getClafersInParentAndSubOrder() {
        return clafersInParentAndSubOrder;
    }

    public List<AstConstraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    public Analysis setConstraints(List<AstConstraint> constraints) {
        this.constraints = constraints;
        return this;
    }

    public boolean isHard(AstConstraint constraint) {
        return hardConstraints.contains(constraint);
    }

    public boolean isSoft(AstConstraint constraint) {
        return !isHard(constraint);
    }

    public Set<AstConstraint> getHardConstraints() {
        return hardConstraints;
    }

    public Analysis setHardConstraints(Set<AstConstraint> hardConstraints) {
        this.hardConstraints = hardConstraints;
        return this;
    }

    public AstBoolExpr getExpr(AstConstraint constraint) {
        return constraintExprs.get(constraint);
    }

    public Map<AstConstraint, AstBoolExpr> getConstraintExprs() {
        return constraintExprs;
    }

    public Analysis setConstraintExprs(Map<AstConstraint, AstBoolExpr> constraintExprs) {
        this.constraintExprs = constraintExprs;
        return this;
    }

    public Card getCard(AstConcreteClafer clafer) {
        return notNull(clafer, "Card", getCardMap().get(clafer));
    }

    public Map<AstConcreteClafer, Card> getCardMap() {
        return notNull("Card", cardMap);
    }

    public Analysis setCardMap(Map<AstConcreteClafer, Card> cardMap) {
        this.cardMap = cardMap;
        return this;
    }

    public Card getGlobalCard(AstClafer clafer) {
        return notNull(clafer, "GlobalCard", getGlobalCardMap().get(clafer));
    }

    public Map<AstClafer, Card> getGlobalCardMap() {
        return notNull("Global card", globalCardMap);
    }

    public Analysis setGlobalCardMap(Map<AstClafer, Card> globalCardMap) {
        this.globalCardMap = globalCardMap;
        return this;
    }

    public Format getFormat(AstClafer clafer) {
        return notNull(clafer, "Format", getFormatMap().get(clafer));
    }

    public Map<AstClafer, Format> getFormatMap() {
        return notNull("Format", formatMap);
    }

    public Analysis setFormatMap(Map<AstClafer, Format> formatMap) {
        this.formatMap = formatMap;
        return this;
    }

    public Pair<AstConcreteClafer, Integer> getConcreteId(AstClafer clafer, int id) {
        AstClafer sub = clafer;
        int curId = id;
        while (sub instanceof AstAbstractClafer) {
            Offsets offset = getOffsets((AstAbstractClafer) sub);
            sub = offset.getClafer(curId);
            curId -= offset.getOffset(sub);
        }
        return new Pair<>((AstConcreteClafer) sub, curId);
    }

    public List<Pair<AstClafer, Integer>> getHierarcyOffsets(AstClafer clafer) {
        return getHierarcyIds(clafer, 0);
    }

    public List<Pair<AstClafer, Integer>> getHierarcyIds(AstClafer clafer, int id) {
        List<Pair<AstClafer, Integer>> superIds = new ArrayList<>();
        superIds.add(new Pair<>(clafer, id));
        AstClafer sup = clafer;
        int curId = id;
        while (sup.hasSuperClafer()) {
            curId += getOffset(sup.getSuperClafer(), sup);
            sup = sup.getSuperClafer();
            superIds.add(new Pair<>(sup, curId));
        }
        return superIds;
    }

    public int getOffset(AstClafer sup, AstClafer sub) {
        if (sup instanceof AstConcreteClafer) {
            assert sup.equals(sub);
            return 0;
        }
        return getOffset((AstAbstractClafer) sup, sub);
    }

    public int getOffset(AstAbstractClafer sup, AstClafer sub) {
        int offset = 0;
        for (AstClafer cur = sub; !sup.equals(cur); cur = cur.getSuperClafer()) {
            if (!cur.hasSuperClafer()) {
                throw new AstException(sub + " is not a sub clafer of " + sup);
            }
            offset += getOffsets(cur.getSuperClafer()).getOffset(cur);
        }
        return offset;
    }

    public Offsets getOffsets(AstAbstractClafer clafer) {
        return notNull(clafer, "Offset", getOffsetMap().get(clafer));
    }

    public Map<AstAbstractClafer, Offsets> getOffsetMap() {
        return notNull("Offset", offsetMap);
    }

    public Analysis setOffsetMap(Map<AstAbstractClafer, Offsets> offsetMap) {
        this.offsetMap = offsetMap;
        return this;
    }

    public PartialSolution getPartialSolution(AstClafer clafer) {
        return notNull(clafer, "Partial solution", getPartialSolutionMap().get(clafer));
    }

    public Map<AstClafer, PartialSolution> getPartialSolutionMap() {
        return notNull("Partial solution", partialSolutionMap);
    }

    public Analysis setPartialSolutionMap(Map<AstClafer, PartialSolution> partialSolutionMap) {
        this.partialSolutionMap = partialSolutionMap;
        return this;
    }

    public Domain[] getPartialInts(AstConcreteClafer clafer) {
        return notNull(clafer, "Partial integer", getPartialIntsMap().get(clafer));
    }

    public Map<AstConcreteClafer, Domain[]> getPartialIntsMap() {
        return notNull("Partial integer", partialIntsMap);
    }

    public Analysis setPartialIntsMap(Map<AstConcreteClafer, Domain[]> partialIntsMap) {
        this.partialIntsMap = partialIntsMap;
        return this;
    }

    public boolean hasInteritedBreakableChildren(AstClafer clafer) {
        AstClafer sup = clafer;
        do {
            if (hasBreakableChildren(sup)) {
                return true;
            }
            sup = sup.getSuperClafer();
        } while (sup != null);
        return false;
    }

    public boolean hasBreakableChildren(AstClafer clafer) {
        return getBreakableChildren(clafer).length > 0;
    }

    public AstConcreteClafer[] getBreakableChildren(AstClafer clafer) {
        return notNull("Breakable children", getBreakableChildrenMap().get(clafer));
    }

    public Map<AstClafer, AstConcreteClafer[]> getBreakableChildrenMap() {
        return notNull("Breakable children", breakableChildrenMap);
    }

    public Analysis setBreakableChildrenMap(Map<AstClafer, AstConcreteClafer[]> breakableChildren) {
        this.breakableChildrenMap = breakableChildren;
        return this;
    }

    public boolean isBreakableRef(AstRef ref) {
        return getBreakableRefsMap().containsKey(ref);
    }

    public boolean isBreakableRefId(AstRef ref, int id) {
        int[] breakbleIDs = getBreakableRefsMap().get(ref);
        if (breakbleIDs == null) {
            return false;
        }
        return Util.in(id, breakbleIDs);
    }

    public Map<AstRef, int[]> getBreakableRefsMap() {
        return notNull("Breakable ref", breakableRefsMap);
    }

    public Analysis setBreakableRefsMap(Map<AstRef, int[]> breakableRefs) {
        this.breakableRefsMap = breakableRefs;
        return this;
    }

    public boolean isInheritedBreakableTarget(AstClafer clafer) {
        AstClafer sup = clafer;
        do {
            if (isBreakableTarget(sup)) {
                return true;
            }
            sup = sup.getSuperClafer();
        } while (sup != null);
        return false;
    }

    public boolean isBreakableTarget(AstClafer clafer) {
        return getBreakableTargetsMap().containsKey(clafer);
    }

    public AstRef[] getBreakableTarget(AstClafer clafer) {
        AstRef[] ref = getBreakableTargetsMap().get(clafer);
        return ref == null ? new AstRef[0] : ref;
    }

    public Map<AstClafer, AstRef[]> getBreakableTargetsMap() {
        return notNull("Breakable target", breakableTargetsMap);
    }

    public Analysis setBreakableTargetsMap(Map<AstClafer, AstRef[]> breakableTargetsMap) {
        this.breakableTargetsMap = breakableTargetsMap;
        return this;
    }

    public Type getType(AstExpr expr) {
        return notNull(expr.toString(), "Type", getTypeMap().get(expr));
    }

    public ProductType getCommonSupertype(AstExpr expr) {
        return notNull(expr.toString(), "Type", getTypeMap().get(expr)).getCommonSupertype();
    }

    public Map<AstExpr, Type> getTypeMap() {
        return notNull("Type", typeMap);
    }

    public Analysis setTypeMap(Map<AstExpr, Type> typeMap) {
        this.typeMap = typeMap;
        return this;
    }

    public AstClafer getInverse(AstClafer clafer) {
        return getInverseMap().get(clafer);
    }

    public Map<AstClafer, AstClafer> getInverseMap() {
        return notNull("Inverses", inverseMap);
    }

    public Analysis setInverseMap(Map<AstClafer, AstClafer> inverseMap) {
        this.inverseMap = inverseMap;
        return this;
    }
}
