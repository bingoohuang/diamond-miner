package org.n3r.diamond.server.utils;

import java.util.concurrent.atomic.AtomicLong;

public class GlobalCounter {
    private static GlobalCounter instance = new GlobalCounter();

    private AtomicLong count = new AtomicLong(0);

    public static GlobalCounter getCounter() {
        return instance;
    }

    public long decrementAndGet() {
        if (count.get() == Long.MIN_VALUE) {
            count.set(0);
            return 0;
        }

        return count.decrementAndGet();
    }

    public long get() {
        return count.get();
    }

    public void set(long value) {
        count.set(value);
    }

}
