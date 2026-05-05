package io.cockroachdb.demo.task;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.cockroachdb.demo.task.support.Name;
import io.cockroachdb.demo.task.support.RecoverableException;
import io.cockroachdb.demo.task.support.Task;

/**
 * A fake task that sleeps to simulate I/O waits and optionally
 * throws exceptions based on probability.
 *
 * @author Kai Niemi
 */
@Name(value = "fake", alias = "f",
        options = {
                "--param minWait=5",
                "--param maxWait=15",
                "--param errorProbability=.001",
        })
public class Fake implements Task {
    private long minWaitMillis;

    private long maxWaitMillis;

    private double errorProbability;

    @Override
    public void prepare(Map<String, String> params) {
        this.minWaitMillis = Integer.parseInt(params.getOrDefault("minWait", "5"));
        this.maxWaitMillis = Integer.parseInt(params.getOrDefault("maxWait", "15"));
        this.errorProbability = Double.parseDouble(
                params.getOrDefault("errorProbability", ".001"));
    }

    @Override
    public void run() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (r.nextDouble(0, 1.0) < errorProbability) {
            throw new RecoverableException("Moon beam missed the mark!");
        }

        try {
            if (minWaitMillis > 0 && maxWaitMillis > 0) {
                TimeUnit.MILLISECONDS.sleep(r.nextLong(minWaitMillis, maxWaitMillis));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

    }
}
