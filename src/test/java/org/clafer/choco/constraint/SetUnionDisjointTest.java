package org.clafer.choco.constraint;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.chocosolver.solver.Model;
import static org.clafer.choco.constraint.ConstraintQuickTest.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.SetVar;
import static org.chocosolver.solver.variables.Var.*;
import org.clafer.test.NoCard;

/**
 *
 * @author jimmy
 */
@RunWith(ConstraintQuickTest.class)
public class SetUnionDisjointTest {

    @Input(solutions = 27)
    public Object testSetUnionDisjoint(Model model) {
        /*
         * import Control.Monad
         * import Data.List
         *
         * powerset = filterM (const [True, False])
         *
         * solutions = do
         *     s1 <- powerset [-1..2]
         *     s2 <- powerset [-2..1]
         *     s3 <- powerset [-1..1]
         *     guard $ (sort (nub $ s1 ++ s2) == sort s3) && (all (`notElem` s1) s2)
         *     return (s1, s2, s3)
         */
        return $(
                new SetVar[]{
                    model.setVar("s1", ker(), env(-1, 0, 1, 2)),
                    model.setVar("s2", ker(), env(-2, -1, 0, 1))},
                model.setVar("s3", ker(), env(-1, 0, 1)));
    }

    @Check
    public void check(TIntSet[] sets, TIntSet union) {
        TIntSet answer = new TIntHashSet();
        for (TIntSet set : sets) {
            for (int c : set.toArray()) {
                assertTrue(answer.add(c));
            }
        }
        assertEquals(union, answer);
    }

    @Test(timeout = 60000)
    public Constraint setup(@NoCard SetVar[] sets, @NoCard SetVar union) {
        return Constraints.union(
                sets, mapCard(sets),
                union, union.getCard(),
                true);
    }
}
