package org.clafer.choco.constraint.propagator;

import java.util.Arrays;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.SetEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;

/**
 *
 * @author jimmy
 */
public class PropJoinInjectiveRelationCard extends Propagator<Variable> {

    private static final long serialVersionUID = 1L;

    private final SetVar take;
    private final IntVar takeCard;
    private final IntVar[] childrenCards;
    private final IntVar toCard;

    public PropJoinInjectiveRelationCard(SetVar take, IntVar takeCard, IntVar[] childrenCards, IntVar toCard) {
        super(buildArray(take, takeCard, toCard, childrenCards), PropagatorPriority.LINEAR, false);
        this.take = take;
        this.takeCard = takeCard;
        this.childrenCards = childrenCards;
        this.toCard = toCard;
    }

    private static Variable[] buildArray(SetVar take, IntVar takeCard, IntVar toCard, IntVar[] childrenCards) {
        Variable[] array = new Variable[childrenCards.length + 3];
        array[0] = take;
        array[1] = takeCard;
        array[2] = toCard;
        System.arraycopy(childrenCards, 0, array, 3, childrenCards.length);
        return array;
    }

    private boolean isTakeVar(int idx) {
        return idx == 0;
    }

    private boolean isTakeCardVar(int idx) {
        return idx == 1;
    }

    private boolean isToCardVar(int idx) {
        return idx == 2;
    }

    private boolean isChildCardVar(int idx) {
        return idx >= 3;
    }

    private int getChildCardVarIndex(int idx) {
        assert isChildCardVar(idx);
        return idx - 3;
    }

//    @Override
//    public boolean advise(int idxVarInProp, int mask) {
//        if (isChildCardVar(idxVarInProp)) {
//            return take.getUB().contains(getChildCardVarIndex(idxVarInProp));
//        }
//        return super.advise(idxVarInProp, mask);
//    }
    @Override
    public int getPropagationConditions(int vIdx) {
        if (isTakeVar(vIdx)) {
            return SetEventType.all();
        }
        assert isTakeCardVar(vIdx) || isToCardVar(vIdx) || isChildCardVar(vIdx);
        return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISetIterator takeEnv = take.getUB().iterator();
        while (takeEnv.hasNext()) {
            int i = takeEnv.nextInt();
            if (i < 0 || i >= childrenCards.length) {
                take.remove(i, this);
            }
        }

        boolean changed;
        do {
            int minCard = 0;
            int maxCard = 0;
            takeEnv.reset();
            while (takeEnv.hasNext()) {
                int i = takeEnv.nextInt();
                IntVar childCard = childrenCards[i];
                if (take.getLB().contains(i)) {
                    minCard += childCard.getLB();
                }
                maxCard += childCard.getUB();
            }

            changed = false;
            toCard.updateLowerBound(minCard, this);
            toCard.updateUpperBound(maxCard, this);

            int lb = toCard.getLB();
            int ub = toCard.getUB();

            int minCardInc = 0;
            int maxCardDec = 0;

            takeEnv.reset();
            while (takeEnv.hasNext()) {
                int i = takeEnv.nextInt();
                if (!take.getLB().contains(i)) {
                    IntVar childCard = childrenCards[i];
                    if (maxCard - childCard.getUB() < lb) {
                        take.force(i, this);
                        minCardInc += childCard.getLB();
                        changed = true;
                    } else if (minCard + childCard.getLB() > ub) {
                        take.remove(i, this);
                        maxCardDec += childCard.getUB();
                        changed = true;
                    }
                }
            }
            minCard += minCardInc;
            maxCard -= maxCardDec;

            takeEnv.reset();
            while (takeEnv.hasNext()) {
                int i = takeEnv.nextInt();
                if (take.getLB().contains(i)) {
                    changed |= childrenCards[i].updateLowerBound(lb - maxCard + childrenCards[i].getUB(), this);
                    changed |= childrenCards[i].updateUpperBound(ub - minCard + childrenCards[i].getLB(), this);
                } else {
                    if (maxCard - childrenCards[i].getLB() < lb) {
                        take.force(i, this);
                    }
                    if (minCard - childrenCards[i].getUB() > ub) {
                        take.remove(i, this);
                    }
                }
            }
        } while (changed);

        int lb = toCard.getLB();
        int ub = toCard.getUB();
        int[] envLbs = new int[take.getUB().size() - take.getLB().size()];
        int[] envUbs = new int[envLbs.length];
        int kerMinCard = 0;
        int kerMaxCard = 0;
        int env = 0;
        takeEnv.reset();
        while (takeEnv.hasNext()) {
            int i = takeEnv.nextInt();
            if (take.getLB().contains(i)) {
                kerMinCard += childrenCards[i].getLB();
                kerMaxCard += childrenCards[i].getUB();
            } else {
                envLbs[env] = childrenCards[i].getLB();
                envUbs[env] = childrenCards[i].getUB();
                env++;
            }
        }
        Arrays.sort(envLbs);
        Arrays.sort(envUbs);
        int i;
        for (i = 0; i < envLbs.length && (kerMinCard < ub || envLbs[i] == 0); i++) {
            kerMinCard += envLbs[i];
        }
        takeCard.updateUpperBound(i + take.getLB().size(), this);
        for (i = envUbs.length - 1; i >= 0 && kerMaxCard < lb; i--) {
            kerMaxCard += envUbs[i];
        }
        takeCard.updateLowerBound(envUbs.length - 1 - i + take.getLB().size(), this);
    }

    @Override
    public ESat isEntailed() {
        boolean completelyInstantiated = take.isInstantiated() && takeCard.isInstantiated() && toCard.isInstantiated();
        int minCard = 0;
        int maxCard = 0;
        ISetIterator takeEnv = take.getUB().iterator();
        while (takeEnv.hasNext()) {
            int i = takeEnv.nextInt();
            if (i >= 0 && i < childrenCards.length) {
                IntVar childCard = childrenCards[i];
                completelyInstantiated = completelyInstantiated && childCard.isInstantiated();
                if (take.getLB().contains(i)) {
                    minCard += childCard.getLB();
                }
                maxCard += childCard.getUB();
            }
        }

        if (toCard.getUB() < minCard) {
            return ESat.FALSE;
        }
        if (toCard.getLB() > maxCard) {
            return ESat.FALSE;
        }

        return completelyInstantiated ? ESat.TRUE : ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return "joinInjectiveRelationCard(" + take + ", " + takeCard + ", " + Arrays.toString(childrenCards) + ", " + toCard + ")";
    }
}
