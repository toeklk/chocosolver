package org.clafer.ir.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.clafer.collection.Pair;
import org.clafer.ir.IrJoinFunction;
import org.clafer.ir.IrJoinRelation;
import org.clafer.ir.IrModule;
import org.clafer.ir.IrRewriter;
import org.clafer.ir.IrSetExpr;

/**
 *
 * @author jimmy
 */
public class CommonSubexpression {

    private CommonSubexpression() {
    }

    public static Pair<List<IrJoinFunction>, List<IrJoinRelation>> findCommonSubexpressions(IrModule module) {
        CommonSubexpressionFinder finder = new CommonSubexpressionFinder();
        finder.rewrite(module, null);
        return new Pair<List<IrJoinFunction>, List<IrJoinRelation>>(finder.duplicateFunctions, finder.duplicateRelations);
    }

    private static class CommonSubexpressionFinder extends IrRewriter<Void> {

        private final Set<IrJoinFunction> seenFunctions = new HashSet<IrJoinFunction>();
        private final List<IrJoinFunction> duplicateFunctions = new ArrayList<IrJoinFunction>();
        private final Set<IrJoinRelation> seenRelations = new HashSet<IrJoinRelation>();
        private final List<IrJoinRelation> duplicateRelations = new ArrayList<IrJoinRelation>();

        @Override
        public IrSetExpr visit(IrJoinFunction ir, Void a) {
            // Check children before checking this ir. That way the smaller
            // subexpression duplicates will be cached first, which the cache
            // can then be used for the larger subexpressions.
            rewrite(ir.getTake(), a);
            rewrite(ir.getRefs(), a);
            if (!seenFunctions.add(ir)) {
                duplicateFunctions.add(ir);
            }
            return ir;
        }

        @Override
        public IrSetExpr visit(IrJoinRelation ir, Void a) {
            rewrite(ir.getTake(), a);
            rewrite(ir.getChildren(), a);
            // Check children before checking this ir. That way the smaller
            // subexpression duplicates will be cached first, which the cache
            // can then be used for the larger subexpressions.
            if (!seenRelations.add(ir)) {
                duplicateRelations.add(ir);
            }
            return ir;
        }
    }
}