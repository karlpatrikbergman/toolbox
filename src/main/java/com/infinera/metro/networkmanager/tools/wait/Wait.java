package com.infinera.metro.networkmanager.tools.wait;

import com.github.rholder.retry.*;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Wait {
    int multiplier, maximumTime, attemptNumber;

    public Wait(int multiplier, int maximumTime, int attemptNumber) {
        this.multiplier = multiplier;
        this.maximumTime = maximumTime;
        this.attemptNumber = attemptNumber;
    }

    public Wait() {
        this.multiplier = 1000;
        this.maximumTime = 5;
        this.attemptNumber = 300;
    }

    public <T> T perform(Callable<T> callable) {
        Retryer<T> retryer = RetryerBuilder.<T>newBuilder()
            .retryIfResult(Objects::isNull)
            .retryIfException()
            .retryIfRuntimeException()
            .withWaitStrategy(WaitStrategies.exponentialWait(multiplier, maximumTime, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(attemptNumber))
            .build();
        try {
            return retryer.call(callable);
        } catch (RetryException | ExecutionException e) {
            throw new RuntimeException("Failed to invoke " + callable.toString() + ": " + e.getMessage());
        }
    }
}
