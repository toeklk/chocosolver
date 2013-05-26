package org.clafer.collection;

import java.util.Iterator;
import org.clafer.common.Check;

/**
 * In iterator for an array in order of increasing index.
 *
 * @param <T> the type of the elements
 * @author jimmy
 */
public class ArrayIterator<T> implements Iterator<T> {

    private final T[] array;
    private int index;
    private final int to;

    /**
     * Iterate an array in order from the first to last element of the array.
     *
     * @param array
     */
    public ArrayIterator(T[] array) {
        this(array, 0, array.length);
    }

    /**
     * Iterate an array in order starting in position from (inclusive) and
     * ending in position to (exclusive).
     *
     * @param array iterate this array
     * @param from start iterating from this index
     * @param to stop before this index
     */
    public ArrayIterator(T[] array, int from, int to) {
        if (to < from) {
            throw new IllegalArgumentException();
        }
        if (from < 0) {
            throw new IllegalArgumentException();
        }
        if (to > array.length) {
            throw new IllegalArgumentException();
        }
        this.array = Check.notNull(array);
        this.index = from;
        this.to = to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return index < to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T next() {
        return array[index++];
    }

    /**
     * Not supported.
     *
     * @throws UnsupportedOperationException if invoked
     */
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
