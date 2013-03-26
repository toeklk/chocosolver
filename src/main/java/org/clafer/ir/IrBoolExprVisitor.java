package org.clafer.ir;

/**
 *
 * @author jimmy
 */
public interface IrBoolExprVisitor<A, B> {

    public B visit(IrBoolVar ir, A a);

    public B visit(IrNot ir, A a);

    public B visit(IrAnd ir, A a);

    public B visit(IrImplies ir, A a);

    public B visit(IrIntCompare ir, A a);

    public B visit(IrSetCompare ir, A a);
}