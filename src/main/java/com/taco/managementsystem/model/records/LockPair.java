package com.taco.managementsystem.model.records;

public record LockPair(String firstLockKey,
                       String secondLockKey,
                       String lockId) {
}
