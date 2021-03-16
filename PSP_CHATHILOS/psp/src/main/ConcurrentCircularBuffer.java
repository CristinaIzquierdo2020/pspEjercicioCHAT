package chat;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link ArrayBlockingQueue}
 */
public class ConcurrentCircularBuffer<T> extends AbstractQueue<T> implements BlockingQueue<T> {

	protected final Lock LOCK;
	protected final Condition NOT_EMPTY;
	protected final Condition NOT_FULL;

	protected final int MAX_SIZE;

	protected int currentSize = 0;
	protected int firstOccupied = 0;
	protected int lastFree = 0;

	protected final Object[] Q;

	@SuppressWarnings("unused")
	private ConcurrentCircularBuffer() {
		LOCK = null;
		NOT_EMPTY = NOT_FULL = null;
		MAX_SIZE = 0;
		Q = null;
	}

	public ConcurrentCircularBuffer(final int maxSize) {
		LOCK = new ReentrantLock();
		NOT_EMPTY = LOCK.newCondition();
		NOT_FULL = LOCK.newCondition();

		MAX_SIZE = maxSize;
		Q = new Object[MAX_SIZE];
	}

	/**
	 * Whether the queue is full
	 */
	public boolean isFull() {
		return currentSize == MAX_SIZE;
	}

	/**
	 * enqueue -> añade un elemento a la cola.
	 * 
	 * @pre LOCK adquirido && currentSize < MAX_SIZE
	 */
	protected void nq(final T e) {
		Q[lastFree++] = e;
		if (lastFree == MAX_SIZE) {
			lastFree = 0;
		}
		currentSize++;
		NOT_EMPTY.signal();
	}

	/**
	 * dequeue -> retira un elemento de la cola.
	 * 
	 * @pre LOCK adquirido && currentSize > 0
	 */
	@SuppressWarnings("unchecked")
	protected T dq() {
		try {
			currentSize--;
			return (T) Q[firstOccupied++];
		} finally {
			if (firstOccupied == MAX_SIZE) {
				firstOccupied = 0;
			}
			NOT_FULL.signal();
		}
	}

	@Override
	public void put(final T e) throws InterruptedException {
		Objects.requireNonNull(e);
		try {
			LOCK.lockInterruptibly();
			while (isFull()) {
				NOT_FULL.await();
			}
			nq(e);
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	public T take() throws InterruptedException {
		try {
			LOCK.lockInterruptibly();
			while (isEmpty()) {
				NOT_EMPTY.await();
			}
			return dq();
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	public int size() {
		return currentSize;
	}

	@Override
	public T poll() {
		try {
			LOCK.lock();
			if (isEmpty()) {
				return null;
			} else {
				return dq();
			}
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public T peek() {
		try {
			LOCK.lock();
			if (isEmpty()) {
				return null;
			} else {
				return (T) Q[firstOccupied];
			}
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	public boolean offer(final T e) {
		if (LOCK.tryLock()) {
			try {
				if (isFull()) {
					return false;
				} else {
					nq(e);
					return true;
				}
			} finally {
				LOCK.unlock();
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean offer(final T e, final long timeout, final TimeUnit unit) throws InterruptedException {
		try {
			LOCK.lockInterruptibly();
			long ns = Objects.requireNonNull(unit).toNanos(timeout);
			while (isFull()) {
				if (ns < 0) {
					return false;
				}
				ns = NOT_FULL.awaitNanos(ns);
			}
			nq(e);
			return true;
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	public T poll(final long timeout, final TimeUnit unit) throws InterruptedException {
		try {
			LOCK.lockInterruptibly();
			long ns = Objects.requireNonNull(unit).toNanos(timeout);
			while (isEmpty()) {
				if (ns < 0) {
					return null;
				}
				ns = NOT_EMPTY.awaitNanos(ns);
			}
			return dq();
		} finally {
			LOCK.unlock();
		}
	}

	@Override
	public int remainingCapacity() {
		return MAX_SIZE - currentSize;
	}

	@Override
	public int drainTo(final Collection<? super T> c) {
		return drainTo(c, Integer.MAX_VALUE);
	}

	@Override
	@SuppressWarnings("unchecked")
	public int drainTo(final Collection<? super T> c, final int maxElements) {
		if (Objects.requireNonNull(c) == this) {
			throw new IllegalArgumentException("Cannot drain onto itself");
		} else if (maxElements < 0) {
			throw new IllegalArgumentException("maxElements cannot be less than 0");
		}
		try {
			LOCK.lock();
			final int drainLimit = maxElements < currentSize ? maxElements : currentSize;
			int actuallyAdded = 0;
			for (int i = 0; i < drainLimit; ++i) {
				if (c.add((T) Q[(firstOccupied + i) % MAX_SIZE])) {
					actuallyAdded++;
				}
			}
			return actuallyAdded;
		} finally {
			currentSize = firstOccupied = lastFree = 0;
			LOCK.unlock();
		}
	}

	@Override
	public java.util.Iterator<T> iterator() {
		return new Iterator();
	}

	class Iterator implements java.util.Iterator<T> {

		private final Object[] copy;
		private int itPos = 0;

		private Iterator() {
			try {
				LOCK.lock();
				if (isEmpty()) {
					copy = new Object[0];
				} else {
					copy = new Object[currentSize];
					int copyPos = 0;
					for (int i = firstOccupied; i < firstOccupied + currentSize; ++i) {
						copy[copyPos++] = Q[i % MAX_SIZE];
					}
				}
			} finally {
				LOCK.unlock();
			}
		}

		@Override
		public boolean hasNext() {
			return itPos < copy.length;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T next() {
			try {
				return (T) copy[itPos++];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new NoSuchElementException();
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("[");
		if (currentSize > 0) {
			b.append(Q[firstOccupied]);
		}
		for (int i = firstOccupied + 1; i < firstOccupied + currentSize; ++i) {
			b.append(", ").append(Q[i % MAX_SIZE]);
		}
		return b.append("]").toString();
	}
}
