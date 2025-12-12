package svl.kadatha.filex.usb;

import java.util.concurrent.atomic.AtomicLong;

public class MyStampedLock {

    // --------------------------------------------------
    // State Layout
    // --------------------------------------------------
    //
    // We'll store our state in one 64-bit long (AtomicLong).
    // Let's define:
    //
    //   lower 15 bits: number of active readers (0..32767)
    //   bit 15: 1 if a writer is active, 0 if not
    //   bits [16..63]: a "version" or "stamp" that increments on each write
    //
    // So the structure is:
    //   [ version (48 bits) ] [ writerActive (1 bit) ] [ readerCount (15 bits) ]
    //
    // We'll define helper methods to extract/modify these fields.

    private static final int READER_MASK = 0x7FFF;       // lower 15 bits
    private static final int WRITER_SHIFT = 15;           // next bit is writer bit
    private static final long WRITER_BIT = 1L << WRITER_SHIFT;
    private static final int VERSION_SHIFT = 16;          // next 48 bits for version

    private final AtomicLong state = new AtomicLong(0L);

    // --------------------------------------------------
    // Helper methods to decode/encode fields in "state"
    // --------------------------------------------------

    private int readerCount(long s) {
        return (int) (s & READER_MASK);
    }

    private boolean writerActive(long s) {
        return ((s >>> WRITER_SHIFT) & 1L) == 1L;
    }

    private long version(long s) {
        return s >>> VERSION_SHIFT;
    }

    /**
     * Increments version by 1, resets writer bit to 0 and optionally sets new reader count.
     */
    private long makeState(long version, boolean writer, int readers) {
        long v = (version << VERSION_SHIFT);
        long w = writer ? WRITER_BIT : 0L;
        long r = (readers & READER_MASK);
        return (v | w | r);
    }

    // --------------------------------------------------
    // Write Lock
    // --------------------------------------------------

    /**
     * Blocks until no writer is active and no readers are present, then sets writer active.
     * Returns a stamp that includes the (incremented) version.
     */
    public long writeLock() {
        while (true) {
            long s = state.get();
            // If no writer active and no readers, try to set writer bit
            if (!writerActive(s) && readerCount(s) == 0) {
                long ver = version(s);
                // We'll increment version right when we acquire the write lock.
                long newState = makeState(ver + 1, true, 0);
                if (state.compareAndSet(s, newState)) {
                    // Return the new full stamp
                    return newState;
                }
            }
            // else spin/yield
            Thread.yield();
        }
    }

    /**
     * Unlock the write lock using the stamp returned from writeLock().
     */
    public void unlockWrite(long stamp) {
        // Must match exactly: the current state must have the same version + writer bit
        // We'll clear the writer bit but keep the version the same.
        while (true) {
            long s = state.get();
            if (s == stamp) {
                long ver = version(s);
                // writer => false, readers => 0
                long newState = makeState(ver, false, 0);
                if (state.compareAndSet(s, newState)) {
                    return;
                }
            } else {
                // If the state changed, we might have an error or concurrency bug
                // or someone else changed the lock behind our back
                throw new IllegalMonitorStateException("Invalid stamp for unlockWrite: "
                        + Long.toHexString(stamp)
                        + ", current: " + Long.toHexString(s));
            }
        }
    }

    // --------------------------------------------------
    // Read Lock
    // --------------------------------------------------
    // This approach allows multiple readers if no writer is active.

    /**
     * Acquires a read lock, blocking if a writer is active.
     * Version is not incremented on read lock, only on writes.
     */
    public long readLock() {
        while (true) {
            long s = state.get();
            if (!writerActive(s)) {
                int readers = readerCount(s);
                if (readers < READER_MASK) {
                    long ver = version(s);
                    long newState = makeState(ver, false, readers + 1);
                    if (state.compareAndSet(s, newState)) {
                        // Return the full stamp representing read lock
                        return newState;
                    }
                } else {
                    // Too many readers, unusual scenario, fallback or spin
                    throw new RuntimeException("Too many readers!");
                }
            }
            // If writer is active, we must wait
            Thread.yield();
        }
    }

    /**
     * Unlock the read lock using the stamp returned from readLock().
     */
    public void unlockRead(long stamp) {
        while (true) {
            long s = state.get();
            // A valid read stamp must share the same version, must not have writer bit
            if (version(s) == version(stamp) && !writerActive(s)) {
                int readers = readerCount(s);
                if (readers > 0) {
                    int oldReaders = readerCount(stamp);
                    // We expect the old stamp also had readers>0
                    if (oldReaders == 0) {
                        throw new IllegalMonitorStateException("Stamp not from a read lock");
                    }
                    long ver = version(s);
                    long newState = makeState(ver, false, readers - 1);
                    if (state.compareAndSet(s, newState)) {
                        return;
                    }
                } else {
                    throw new IllegalMonitorStateException("No readers to unlock");
                }
            } else {
                // Possibly stale or invalid stamp
                throw new IllegalMonitorStateException("Invalid stamp for unlockRead: "
                        + Long.toHexString(stamp)
                        + ", current: " + Long.toHexString(s));
            }
        }
    }

    // --------------------------------------------------
    // Optimistic Read
    // --------------------------------------------------
    // We'll return a "stamp" that is just the *current state*, but if
    // a write occurs, the version will differ from the stored stamp.

    /**
     * Returns a stamp representing an optimistic read.
     * This does not increment version, does not acquire a lock.
     */
    public long tryOptimisticRead() {
        // Just return the current state. The user can read data,
        // then call validate(stamp) to see if a writer occurred.
        return state.get();
    }

    /**
     * Validate if the given stamp is still valid (no conflicting write).
     * Returns true if:
     * 1) The version part of "stamp" matches the version part of current state,
     * 2) And writerActive() is false if it wasn't active in "stamp".
     * <p>
     * Implementation detail: In a real StampedLock, "writerActive" might instantly invalidate
     * the read, but let's keep it simple: we say it's valid as long as the version is the same
     * and there's no new writer that changed the version in between.
     */
    public boolean validate(long stamp) {
        long s = state.get();
        // Compare versions
        return version(s) == version(stamp);
    }

    // --------------------------------------------------
    // Lock Conversion
    // --------------------------------------------------

    /**
     * Attempt to convert a read lock stamp to a write lock (without fully unlocking).
     * Returns 0 if conversion fails, or the new write stamp if successful.
     */
    public long tryConvertToWriteLock(long readStamp) {
        while (true) {
            long s = state.get();
            // Must match same version, no writer active, and we must have at least 1 reader
            if (version(s) == version(readStamp) && !writerActive(s) && readerCount(s) > 0) {
                int readers = readerCount(s);
                // We want to go from "readers" to "writer=1, readers=0" while incrementing version
                long newStamp = makeState(version(s) + 1, true, 0);
                if (state.compareAndSet(s, newStamp)) {
                    return newStamp;
                }
            } else {
                // Can't convert (someone else changed the lock)
                return 0L;
            }
        }
    }

    /**
     * Attempt to convert a write lock stamp to a read lock stamp.
     * Return 0 if fails, or new read stamp if success.
     */
    public long tryConvertToReadLock(long writeStamp) {
        while (true) {
            long s = state.get();
            // Must match exactly, writer bit set, no readers
            if (s == writeStamp && writerActive(s)) {
                // We'll keep same version, set writer=false, readers=1
                long ver = version(s);
                long newState = makeState(ver, false, 1);
                if (state.compareAndSet(s, newState)) {
                    return newState;
                }
            } else {
                // fail
                return 0L;
            }
        }
    }
}
