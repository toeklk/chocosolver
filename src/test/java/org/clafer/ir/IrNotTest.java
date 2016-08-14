package org.clafer.ir;

import org.clafer.ir.IrQuickTest.Solution;
import static org.clafer.ir.Irs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;

/**
 *
 * @author jimmy
 */
@RunWith(IrQuickTest.class)
public class IrNotTest {

    @Test(timeout = 60000)
    public IrBoolExpr setup(IrBoolVar var) {
        return not(var);
    }

    @Solution
    public Constraint setup(BoolVar var) {
        return var.getModel().arithm(var, "=", 0);
    }
}
