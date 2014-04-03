package org.clafer.ir;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jimmy
 */
public class IrUtil {

    private IrUtil() {
    }

    public static boolean isTrue(IrBoolExpr b) {
        return IrBoolDomain.TrueDomain.equals(b.getDomain());
    }

    public static boolean isFalse(IrBoolExpr b) {
        return IrBoolDomain.FalseDomain.equals(b.getDomain());
    }

    public static boolean isConstant(IrBoolExpr b) {
        return !IrBoolDomain.BoolDomain.equals(b.getDomain());
    }

    public static Boolean getConstant(IrBoolExpr b) {
        switch (b.getDomain()) {
            case TrueDomain:
                return Boolean.TRUE;
            case FalseDomain:
                return Boolean.FALSE;
            case BoolDomain:
                return null;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static IrBoolVar asConstant(IrBoolVar b) {
        switch (b.getDomain()) {
            case TrueDomain:
                return Irs.True;
            case FalseDomain:
                return Irs.False;
            case BoolDomain:
                return b;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static boolean isConstant(IrIntExpr i) {
        IrDomain domain = i.getDomain();
        return domain.size() == 1;
    }

    public static Integer getConstant(IrIntExpr i) {
        IrDomain domain = i.getDomain();
        return domain.size() == 1 ? domain.getLowBound() : null;
    }

    public static int[] getConstant(IrIntExpr[] is) {
        if (is.length == 0) {
            return new int[0];
        }
        if (is[0].getDomain().size() == 1) {
            int[] constant = new int[is.length];
            constant[0] = is[0].getDomain().getLowBound();
            for (int i = 1; i < is.length; i++) {
                if (is[i].getDomain().size() == 1) {
                    constant[i] = is[i].getDomain().getLowBound();
                } else {
                    return null;
                }
            }
            return constant;
        }
        return null;
    }

    public static IrIntVar asConstant(IrIntVar i) {
        IrDomain domain = i.getDomain();
        return domain.size() == 1 ? Irs.constant(domain.getLowBound()) : i;
    }

    public static boolean isConstant(IrSetExpr s) {
        IrDomain env = s.getEnv();
        IrDomain ker = s.getKer();
        assert IrUtil.isSubsetOf(ker, env);
        return env.size() == ker.size();
    }

    public static int[] getConstant(IrSetExpr s) {
        IrDomain env = s.getEnv();
        IrDomain ker = s.getKer();
        assert IrUtil.isSubsetOf(ker, env);
        return env.size() == ker.size() ? ker.getValues() : null;
    }

    public static IrSetVar asConstant(IrSetVar s) {
        IrDomain env = s.getEnv();
        IrDomain ker = s.getKer();
        assert IrUtil.isSubsetOf(ker, env);
        if (env.size() == ker.size()) {
            return Irs.constant(ker);
        }
        IrDomain card = s.getCard();
        if (card.size() == 1) {
            int constantCard = card.getLowBound();
            if (constantCard == ker.size()) {
                return Irs.constant(ker);
            }
            if (constantCard == env.size()) {
                return Irs.constant(env);
            }
        }
        return s;
    }

    public static IrSetExpr asConstant(IrSetExpr s) {
        IrDomain env = s.getEnv();
        IrDomain ker = s.getKer();
        assert IrUtil.isSubsetOf(ker, env);
        if (env.size() == ker.size()) {
            return Irs.constant(env);
        }
        IrDomain card = s.getCard();
        if (card.size() == 1) {
            int constantCard = card.getLowBound();
            if (constantCard == ker.size()) {
                return Irs.constant(ker);
            }
        }
        return s;
    }

    public static IrIntExpr asInt(IrSetExpr set) {
        if (set instanceof IrSingleton) {
            return ((IrSingleton) set).getValue();
        }
        int[] constant = getConstant(set);
        if (constant != null && constant.length == 1) {
            return Irs.constant(constant[0]);
        }
        return null;
    }

    public static IrIntExpr[] asInts(IrSetExpr[] sets) {
        if (sets.length == 0) {
            return new IrIntExpr[0];
        }
        IrIntExpr asInt = asInt(sets[0]);
        if (asInt == null) {
            return null;
        }
        IrIntExpr[] ints = new IrIntExpr[sets.length];
        ints[0] = asInt;
        for (int i = 1; i < sets.length; i++) {
            asInt = asInt(sets[i]);
            if (asInt == null) {
                return null;
            }
            ints[i] = asInt;
        }
        return ints;
    }

    public static IrStringExpr asConstant(IrStringExpr s) {
        if (s.getLength().size() != 1) {
            return s;
        }
        int length = s.getLength().getLowBound();
        char[] string = new char[length];
        for (int i = 0; i < length; i++) {
            if (s.getChars()[i].size() != 1) {
                return s;
            }
            int chari = s.getChars()[i].getLowBound();
            if (chari < Character.MIN_VALUE
                    || chari > Character.MAX_VALUE
                    || chari == 0) {
                return s;
            }
            string[i] = (char) chari;
        }
        for (int i = length; i < s.getChars().length; i++) {
            if (s.getChars()[i].size() != 1
                    || s.getChars()[i].getLowBound() != 0) {
                return s;
            }
        }
        return Irs.constant(new String(string));
    }

    public static int maxLength(IrStringExpr... strings) {
        int maxLength = 0;
        for (IrStringExpr string : strings) {
            maxLength = Math.max(maxLength, string.getChars().length);
        }
        return maxLength;
    }

    public static boolean containsAll(int[] values, IrDomain domain) {
        for (int value : values) {
            if (!domain.contains(value)) {
                return false;
            }
        }
        return true;
    }

    public static boolean intersects(IrDomain d1, IrDomain d2) {
        if (d1.isEmpty() || d2.isEmpty()) {
            return false;
        }
        if (d1 == d2) {
            return true;
        }
        if (d1.getLowBound() > d2.getHighBound()) {
            return false;
        }
        if (d1.getHighBound() < d2.getLowBound()) {
            return false;
        }
        if (d1.isBounded()
                && (d2.isBounded()
                || d2.getLowBound() >= d1.getLowBound()
                || (d2.getHighBound() <= d1.getHighBound()))) {
            // Bounds are already checked.
            return true;
        }
        if (d2.isBounded()
                && (d1.getLowBound() >= d2.getLowBound()
                || (d1.getHighBound() <= d2.getHighBound()))) {
            // Bounds are already checked.
            return true;
        }
        if (d1.size() <= d2.size()) {
            TIntIterator iter = d1.iterator();
            while (iter.hasNext()) {
                if (d2.contains(iter.next())) {
                    return true;
                }
            }
        } else {
            TIntIterator iter = d2.iterator();
            while (iter.hasNext()) {
                if (d1.contains(iter.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSubsetOf(IrDomain sub, IrDomain sup) {
        if (sub == sup) {
            return true;
        }
        if (sub.isEmpty()) {
            return true;
        }
        if (sub.size() > sup.size()) {
            return false;
        }
        if (sub.getLowBound() < sup.getLowBound()) {
            return false;
        }
        if (sub.getHighBound() > sup.getHighBound()) {
            return false;
        }
        if (sup.isBounded()) {
            // Bounds are already checked.
            return true;
        }
        TIntIterator iter = sub.iterator();
        while (iter.hasNext()) {
            if (!sup.contains(iter.next())) {
                return false;
            }
        }
        return true;
    }

    public static IrDomain add(IrDomain domain, int value) {
        if (domain.isEmpty()) {
            return Irs.constantDomain(value);
        }
        if (domain.contains(value)) {
            return domain;
        }
        if (domain.isBounded()) {
            if (value == domain.getLowBound() - 1) {
                return Irs.boundDomain(domain.getLowBound() - 1, domain.getHighBound());
            }
            if (value == domain.getHighBound() + 1) {
                return Irs.boundDomain(domain.getLowBound(), domain.getHighBound() + 1);
            }
        }
        TIntHashSet values = new TIntHashSet(domain.size() + 1);
        domain.transferTo(values);
        values.add(value);
        return Irs.enumDomain(values);
    }

    public static IrDomain add(IrDomain addend1, IrDomain addend2) {
        if (addend1.isEmpty()) {
            return addend2;
        }
        if (addend2.isEmpty()) {
            return addend1;
        }
        if (addend1.size() == 1) {
            return offset(addend2, addend1.getLowBound());
        }
        if (addend2.size() == 1) {
            return offset(addend1, addend2.getLowBound());
        }
        return Irs.boundDomain(
                addend1.getLowBound() + addend2.getLowBound(),
                addend1.getHighBound() + addend2.getHighBound());
    }

    public static IrDomain remove(IrDomain domain, int value) {
        if (!domain.contains(value)) {
            return domain;
        }
        if (domain.isBounded()) {
            if (value == domain.getLowBound()) {
                return domain.size() == 1
                        ? Irs.EmptyDomain
                        : Irs.boundDomain(domain.getLowBound() + 1, domain.getHighBound());
            }
            if (value == domain.getHighBound()) {
                return domain.size() == 1
                        ? Irs.EmptyDomain
                        : Irs.boundDomain(domain.getLowBound(), domain.getHighBound() - 1);
            }
        }
        TIntHashSet values = new TIntHashSet(domain.size());
        domain.transferTo(values);
        values.remove(value);
        return Irs.enumDomain(values);
    }

    public static IrDomain boundLow(IrDomain domain, int lb) {
        if (domain.isEmpty()) {
            return domain;
        }
        if (lb > domain.getHighBound()) {
            return Irs.EmptyDomain;
        }
        if (lb == domain.getHighBound()) {
            return Irs.constantDomain(lb);
        }
        if (lb <= domain.getLowBound()) {
            return domain;
        }
        if (domain.isBounded()) {
            return Irs.boundDomain(lb, domain.getHighBound());
        }
        TIntHashSet values = new TIntHashSet(Math.min(domain.size(), domain.getHighBound() - lb + 1));
        TIntIterator iter = domain.iterator();
        while (iter.hasNext()) {
            int value = iter.next();
            if (value >= lb) {
                values.add(value);
            }
        }
        return Irs.enumDomain(values);
    }

    public static IrDomain boundHigh(IrDomain domain, int hb) {
        if (domain.isEmpty()) {
            return domain;
        }
        if (hb < domain.getLowBound()) {
            return Irs.EmptyDomain;
        }
        if (hb == domain.getLowBound()) {
            return Irs.constantDomain(hb);
        }
        if (hb >= domain.getHighBound()) {
            return domain;
        }
        if (domain.isBounded()) {
            return Irs.boundDomain(domain.getLowBound(), hb);
        }
        TIntHashSet values = new TIntHashSet(Math.min(domain.size(), hb - domain.getLowBound() + 1));
        TIntIterator iter = domain.iterator();
        while (iter.hasNext()) {
            int value = iter.next();
            if (value <= hb) {
                values.add(value);
            }
        }
        return Irs.enumDomain(values);
    }

    public static IrDomain boundBetween(IrDomain domain, int lb, int hb) {
        if (lb > hb) {
            throw new IllegalArgumentException();
        }
        return boundHigh(boundLow(domain, lb), hb);
    }

    public static IrDomain difference(IrDomain minuend, IrDomain subtrahend) {
        if (!intersects(minuend, subtrahend)) {
            return minuend;
        }
        if (minuend.isBounded() && subtrahend.isBounded()) {
            if (minuend.getLowBound() < subtrahend.getLowBound()
                    && minuend.getHighBound() <= subtrahend.getHighBound()) {
                return Irs.boundDomain(minuend.getLowBound(), subtrahend.getLowBound() - 1);
            }
            if (minuend.getHighBound() > subtrahend.getHighBound()
                    && minuend.getLowBound() >= subtrahend.getLowBound()) {
                return Irs.boundDomain(subtrahend.getHighBound() + 1, minuend.getHighBound());
            }
        }
        TIntHashSet values = new TIntHashSet(minuend.size());
        minuend.transferTo(values);
        values.removeAll(subtrahend.getValues());
        return Irs.enumDomain(values);
    }

    public static IrDomain intersection(IrDomain d1, IrDomain d2) {
        if (isSubsetOf(d1, d2)) {
            return d1;
        }
        if (isSubsetOf(d2, d1)) {
            return d2;
        }
        if (d1.isBounded() && d2.isBounded()) {
            if (d1.getLowBound() <= d2.getLowBound()
                    && d1.getHighBound() >= d2.getLowBound()) {
                return Irs.boundDomain(d2.getLowBound(), d1.getHighBound());
            }
            if (d2.getLowBound() <= d1.getLowBound()
                    && d2.getHighBound() >= d1.getLowBound()) {
                return Irs.boundDomain(d1.getLowBound(), d2.getHighBound());
            }
        }
        TIntHashSet values = new TIntHashSet(d1.size());
        d1.transferTo(values);
        values.retainAll(d2.getValues());
        return Irs.enumDomain(values);
    }

    public static IrDomain intersectionEnvs(IrSetExpr... sets) {
        if (sets.length == 0) {
            return Irs.EmptyDomain;
        }
        IrDomain domain = sets[0].getEnv();
        for (int i = 1; i < sets.length; i++) {
            domain = intersection(domain, sets[i].getEnv());
        }
        return domain;
    }

    public static IrDomain intersectionKers(IrSetExpr... sets) {
        if (sets.length == 0) {
            return Irs.EmptyDomain;
        }
        IrDomain domain = sets[0].getKer();
        for (int i = 1; i < sets.length; i++) {
            domain = intersection(domain, sets[i].getKer());
        }
        return domain;
    }

    public static IrDomain union(IrDomain d1, IrDomain d2) {
        if (isSubsetOf(d1, d2)) {
            return d2;
        }
        if (isSubsetOf(d2, d1)) {
            return d1;
        }
        if (d1.isBounded() && d2.isBounded()) {
            if (d1.getLowBound() <= d2.getLowBound()
                    && d1.getHighBound() >= d2.getLowBound()) {
                return Irs.boundDomain(d1.getLowBound(), d2.getHighBound());
            }
            if (d2.getLowBound() <= d1.getLowBound()
                    && d2.getHighBound() >= d1.getLowBound()) {
                return Irs.boundDomain(d2.getLowBound(), d1.getHighBound());
            }
        }
        TIntHashSet values = new TIntHashSet(d1.size() + d2.size());
        d1.transferTo(values);
        d2.transferTo(values);
        return Irs.enumDomain(values);
    }

    public static IrDomain unionEnvs(IrSetExpr... sets) {
        if (sets.length == 0) {
            return Irs.EmptyDomain;
        }
        IrDomain domain = sets[0].getEnv();
        for (int i = 1; i < sets.length; i++) {
            domain = union(domain, sets[i].getEnv());
        }
        return domain;
    }

    public static IrDomain unionKers(IrSetExpr... sets) {
        if (sets.length == 0) {
            return Irs.EmptyDomain;
        }
        IrDomain domain = sets[0].getKer();
        for (int i = 1; i < sets.length; i++) {
            domain = union(domain, sets[i].getKer());
        }
        return domain;
    }

    public static IrDomain minus(IrDomain domain) {
        if (domain.isEmpty()) {
            return Irs.EmptyDomain;
        }
        if (domain.isBounded()) {
            return Irs.boundDomain(-domain.getHighBound(), -domain.getLowBound());
        }
        int[] values = domain.getValues();
        int[] minusValues = new int[values.length];
        for (int i = 0; i < minusValues.length; i++) {
            minusValues[i] = -values[i];
        }
        return Irs.enumDomain(minusValues);
    }

    public static IrDomain offset(IrDomain domain, int offset) {
        if (domain.isEmpty()) {
            return Irs.EmptyDomain;
        }
        if (domain.isBounded()) {
            return Irs.boundDomain(domain.getLowBound() + offset, domain.getHighBound() + offset);
        }
        int[] values = domain.getValues();
        int[] offsetValues = new int[values.length];
        for (int i = 0; i < offsetValues.length; i++) {
            offsetValues[i] = values[i] + offset;
        }
        return Irs.enumDomain(offsetValues);
    }

    public static IrDomain mask(IrDomain domain, int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException();
        }
        if (from == to || domain.isEmpty()) {
            return Irs.EmptyDomain;
        }
        if (from > domain.getHighBound() || to <= domain.getLowBound()) {
            return Irs.EmptyDomain;
        }
        if (domain.isBounded()) {
            return Irs.boundDomain(
                    Math.max(0, domain.getLowBound() - from),
                    Math.min(to - 1, domain.getHighBound()) - from);
        }
        TIntList mask = new TIntArrayList();
        for (int value : domain.getValues()) {
            if (value >= from && value < to) {
                mask.add(value - from);
            }
        }
        return Irs.enumDomain(mask);
    }

    public static Ordering compare(IrIntExpr a, IrIntExpr b) {
        if (a.equals(b)) {
            return Ordering.EQ;
        }
        IrDomain da = a.getDomain();
        IrDomain db = b.getDomain();
        if (da.size() == 1 && db.size() == 1 && da.getLowBound() == db.getLowBound()) {
            return Ordering.EQ;
        }
        int aLb = da.getLowBound();
        int aUb = da.getHighBound();
        int bLb = db.getLowBound();
        int bUb = db.getHighBound();
        if (aLb > bUb) {
            return Ordering.GT;
        }
        if (aLb >= bUb) {
            return Ordering.GE;
        }
        if (aUb < bLb) {
            return Ordering.LT;
        }
        if (aUb <= bLb) {
            return Ordering.LE;
        }
        return Ordering.UNKNOWN;
    }

    public static Ordering compareString(IrIntExpr[] a, IrIntExpr[] b) {
        return compareString(a, b, 0);
    }

    public static Ordering compareString(IrIntExpr[] a, IrIntExpr[] b, int index) {
        if (index == a.length) {
            return a.length == b.length ? Ordering.EQ : Ordering.LT;
        }
        if (index == b.length) {
            assert a.length != b.length;
            return Ordering.GT;
        }
        Ordering ord = compare(a[index], b[index]);
        switch (ord) {
            case EQ:
                return compareString(a, b, index + 1);
            case LE:
                switch (compareString(a, b, index + 1)) {
                    case LT:
                        return Ordering.LT;
                    case LE:
                    case EQ:
                        return Ordering.LE;
                    default:
                        return Ordering.UNKNOWN;
                }
            case GE:
                switch (compareString(a, b, index + 1)) {
                    case GT:
                        return Ordering.GT;
                    case GE:
                    case EQ:
                        return Ordering.GE;
                    default:
                        return Ordering.UNKNOWN;
                }
            default:
                return ord;
        }
    }

    public static IrIntExpr[] pad(IrIntExpr[] chars, int length) {
        if (length < chars.length) {
            throw new IllegalArgumentException();
        }
        if (length == chars.length) {
            return chars;
        }
        IrIntExpr[] pad = Arrays.copyOf(chars, length);
        Arrays.fill(pad, chars.length, pad.length, Irs.Zero);
        return pad;
    }

    public static Set<IrVar> getVariables(IrBoolExpr constraint) {
        Set<IrVar> variables = new HashSet<>();
        constraint.accept(VariableFinder, variables);
        return variables;
    }

    public static IrModule renameVariables(
            IrModule module,
            Map<IrIntVar, IrIntVar> intRename,
            Map<IrSetVar, IrSetVar> setRename,
            Map<IrStringVar, IrStringVar> stringRename) {
        return new VariableRenamer(intRename, setRename, stringRename).rewrite(module, null);
    }

    public static enum Ordering {

        LT,
        LE,
        GT,
        GE,
        EQ,
        UNKNOWN;
    }

    private static final IrTraverser<Set<IrVar>> VariableFinder
            = new IrTraverser<Set<IrVar>>() {

                @Override
                public Void visit(IrBoolVar ir, Set<IrVar> a) {
                    a.add(ir);
                    return super.visit(ir, a);
                }

                @Override
                public Void visit(IrIntVar ir, Set<IrVar> a) {
                    a.add(ir);
                    return super.visit(ir, a);
                }

                @Override
                public Void visit(IrSetVar ir, Set<IrVar> a) {
                    a.add(ir);
                    return super.visit(ir, a);
                }

                @Override
                public Void visit(IrStringVar ir, Set<IrVar> a) {
                    a.add(ir);
                    return super.visit(ir, a);
                }
            };

    private static class VariableRenamer extends IrRewriter<Void> {

        private final Map<IrIntVar, IrIntVar> intRename;
        private final Map<IrSetVar, IrSetVar> setRename;
        private final Map<IrStringVar, IrStringVar> stringRename;

        VariableRenamer(
                Map<IrIntVar, IrIntVar> intRename,
                Map<IrSetVar, IrSetVar> setRename,
                Map<IrStringVar, IrStringVar> stringRename) {
            this.intRename = intRename;
            this.setRename = setRename;
            this.stringRename = stringRename;
        }

        @Override
        public IrBoolVar visit(IrBoolVar ir, Void a) {
            IrBoolVar var = (IrBoolVar) intRename.get(ir);
            return var == null ? ir : var;
        }

        @Override
        public IrIntVar visit(IrIntVar ir, Void a) {
            IrIntVar var = intRename.get(ir);
            return var == null ? ir : var;
        }

        @Override
        public IrSetVar visit(IrSetVar ir, Void a) {
            IrSetVar var = setRename.get(ir);
            return var == null ? ir : var;
        }

        @Override
        public IrStringVar visit(IrStringVar ir, Void a) {
            IrStringVar var = stringRename.get(ir);
            return var == null ? ir : var;
        }
    };
}
