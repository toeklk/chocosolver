package org.clafer.ir;

import org.clafer.Check;

/**
 *
 * @author jimmy
 */
public class IrCompare implements IrDualExpr {

    private final IrIntExpr left;
    private final Op op;
    private final IrIntExpr right;

    IrCompare(IrIntExpr left, Op op, IrIntExpr right) {
        this.left = Check.notNull(left);
        this.op = Check.notNull(op);
        this.right = Check.notNull(right);
    }

    public IrIntExpr getLeft() {
        return left;
    }

    public Op getOp() {
        return op;
    }

    public IrIntExpr getRight() {
        return right;
    }

    @Override
    public <A, B> B accept(IrBoolExprVisitor<A, B> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public IrDualExpr opposite() {
        return Irs.compare(left, op.getOpposite(), right);
    }

    @Override
    public String toString() {
        return left + " " + op.getSyntax() + " " + right;
    }

    public static enum Op {

        Equal("="),
        NotEqual("!="),
        LessThan("<"),
        LessThanEqual("<="),
        GreaterThan(">"),
        GreaterThanEqual(">=");
        private final String syntax;

        Op(String syntax) {
            this.syntax = syntax;
        }

        public String getSyntax() {
            return syntax;
        }

        public Op getOpposite() {
            switch (this) {
                case Equal:
                    return NotEqual;
                case NotEqual:
                    return Equal;
                case LessThan:
                    return GreaterThanEqual;
                case LessThanEqual:
                    return GreaterThan;
                case GreaterThan:
                    return LessThanEqual;
                case GreaterThanEqual:
                    return LessThan;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}
