package org.clafer.ir;

/**
 *
 * @author jimmy
 */
public interface IrIntExprVisitor<A, B> {

    public B visit(IrIntVar ir, A a);

    public B visit(IrSetCard ir, A a);
}