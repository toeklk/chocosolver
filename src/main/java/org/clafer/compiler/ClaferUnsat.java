package org.clafer.compiler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.clafer.ast.AstConstraint;
import org.clafer.collection.Either;
import org.clafer.collection.Pair;
import org.clafer.common.Check;
import org.clafer.instance.InstanceModel;
import org.clafer.ir.IrBoolVar;

/**
 * Either call {@link #minUnsat()} xor {@link #unsatCore()} at most once. If you
 * need to invoke both, you need to two ClaferUnsat objects.
 *
 * @author jimmy
 */
public class ClaferUnsat {

    private final Solver solver;
    private final ClaferSolutionMap solutionMap;
    private final Pair<AstConstraint, Either<Boolean, BoolVar>>[] softVars;
    private final Either<Integer, IntVar> score;

    ClaferUnsat(Solver solver, ClaferSolutionMap solutionMap) {
        this.solver = Check.notNull(solver);
        this.solutionMap = Check.notNull(solutionMap);
        Map<AstConstraint, IrBoolVar> softVarsMap = solutionMap.getAstSolution().getSoftVarsMap();
        @SuppressWarnings("unchecked")
        Pair<AstConstraint, Either<Boolean, BoolVar>>[] soft = new Pair[softVarsMap.size()];
        int i = 0;
        for (Entry<AstConstraint, IrBoolVar> entry : softVarsMap.entrySet()) {
            soft[i++] = new Pair<>(
                    entry.getKey(),
                    solutionMap.getIrSolution().getVar(entry.getValue()));
        }
        assert i == soft.length;
        this.softVars = soft;
        this.score = solutionMap.getIrSolution().getVar(solutionMap.getAstSolution().getSumSoftVar());
    }

    public ClaferUnsat limitTime(long ms) {
        getInternalSolver().limitTime(ms);
        return this;
    }

    public Solver getInternalSolver() {
        return solver;
    }

    /**
     * Compute the minimal set of constraints that need to be removed before the
     * model is satisfiable. If the model is already satisfiable, then the set
     * is empty. Guaranteed to be minimum.
     *
     * @return the Min-Unsat and the corresponding near-miss example or null if
     * unknown
     */
    public Pair<Set<AstConstraint>, InstanceModel> minUnsat() throws ReachedLimitException {
        Solution lastSolution;
        if ((lastSolution = maximize()) != null) {
            Set<AstConstraint> unsat = new HashSet<>();
            for (Pair<AstConstraint, Either<Boolean, BoolVar>> softVar : softVars) {
                Either<Boolean, BoolVar> var = softVar.getSnd();
                if (var.isLeft()
                        ? !var.getLeft()
                        : lastSolution.getIntVal(var.getRight()) == 0) {
                    unsat.add(softVar.getFst());
                }
            }
            if (solver.isStopCriterionMet()) {
                throw new ReachedLimitBestKnownUnsatException(unsat, solutionMap.getInstance(lastSolution));
            }
            return new Pair<>(unsat, solutionMap.getInstance(lastSolution));
        }
        if (solver.isStopCriterionMet()) {
            throw new ReachedLimitException();
        }
        return null;
    }

    /**
     * Compute a small set of constraints that are mutually unsatisfiable.
     * Undefined behaviour if the model is satisfiable. This method is always
     * slower to compute than {@link #minUnsat()}. Not guaranteed to be minimum.
     *
     * @return the Unsat-Core or null if unknown
     */
    public Set<AstConstraint> unsatCore() throws ReachedLimitException {
        Set<AstConstraint> unsat = new HashSet<>();
        Solution lastSolution;
        if ((lastSolution = maximize()) != null) {
            boolean changed;
            do {
                if (solver.isStopCriterionMet()) {
                    throw new ReachedLimitException();
                }
                changed = false;
                List<BoolVar> minUnsat = new ArrayList<>();
                for (Pair<AstConstraint, Either<Boolean, BoolVar>> softVar : softVars) {
                    Either<Boolean, BoolVar> var = softVar.getSnd();
                    if (var.isLeft()
                            ? !var.getLeft()
                            : lastSolution.getIntVal(var.getRight()) == 0) {
                        changed |= unsat.add(softVar.getFst());
                        if (var.isRight()) {
                            minUnsat.add(var.getRight());
                        }
                    }
                }
                solver.reset();
                for (BoolVar var : minUnsat) {
                    solver.getModel().arithm(var, "=", 1).post();
                }
            } while (changed && (lastSolution = maximize()) != null);
            return unsat;
        }
        if (solver.isStopCriterionMet()) {
            throw new ReachedLimitException();
        }
        return null;
    }

    private Solution maximize() {
        return score.isLeft()
                ? solver.findSolution()
                : solver.findOptimalSolution(score.getRight(), true);
    }
}
