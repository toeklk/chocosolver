package org.clafer.ast;

import org.clafer.ir.IrDomain;

/**
 *
 * @author jimmy
 */
public class JoinSetWithStringException extends AstException {

    public JoinSetWithStringException(AstSetExpr expr, IrDomain card) {
        super("Cannot join a non-singleton set with strings. For example:\n"
                + "\n"
                + "    A -> string 0..1\n"
                + "    [ A.ref = \"abc\" ]\n"
                + "\n"
                + "The expression \"A\" evaluates to a set of size 0 or 1. The join is forbidden.\n"
                + "\n"
                + "    A -> string 1..1\n"
                + "    [ A.ref = \"abc\" ]\n"
                + "\n"
                + "The expression \"A\" evaluates to a singleton of size 1. The join is permitted."
                + "\n"
                + "Problem compiling the expression:\n"
                + "\n"
                + "    " + expr + "\n"
                + "\n"
                + "The compiler infered a size of " + card.getLowBound() + " to " + card.getHighBound() + ".");
    }
}
