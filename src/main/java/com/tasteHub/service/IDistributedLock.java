package com.tasteHub.service;

import java.util.concurrent.TimeUnit;

public interface IDistributedLock {
    public void executeWithLock(Runnable task, long timeout, TimeUnit unit) throws Exception;

    public void close();
}
