package org.clafer.ir;

import org.clafer.domain.BoolDomain;
import org.clafer.domain.Domains;

/**
 *
 * @author jimmy
 */
public class IrBoolVar extends IrIntVar implements IrBoolExpr {

    IrBoolVar(String name, BoolDomain domain) {
        super(name, domain);
    }

    IrBoolVar(boolean constant) {
        this(Boolean.toString(constant), constant ? Domains.TrueDomain : Domains.FalseDomain);
    }

    @Override
    public BoolDomain getDomain() {
        return (BoolDomain) super.getDomain();
    }

    @Override
    public IrBoolExpr negate() {
        return new IrNot(this, getDomain().invert());
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public <A, B> B accept(IrBoolExprVisitor<A, B> visitor, A a) {
        return visitor.visit(this, a);
    }

    @Override
    public <A, B> B accept(IrIntExprVisitor<A, B> visitor, A a) {
        return visitor.visit(this, a);
    }
}
