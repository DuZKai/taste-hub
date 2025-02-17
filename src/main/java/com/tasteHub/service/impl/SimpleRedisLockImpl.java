package com.tasteHub.service.impl;

import cn.hutool.core.lang.UUID;
import com.tasteHub.service.ILock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLockImpl implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLockImpl(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }
    // 使用锁标识解决锁极端情况下释放问题，但是还是存在问题，当判断标示一致时删除锁，但是在删除锁之前（比如因为垃圾回收阻塞了），锁已经被其他线程获取，就又会导致两个线程同时执行业务
    // @Override
    // public void unlock() {
    //     // 获取线程标示
    //     String threadId = ID_PREFIX + Thread.currentThread().getId();
    //     // 获取锁中的标示
    //     String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
    //     // 判断标示是否一致
    //     if(threadId.equals(id)) {
    //         // 释放锁
    //         stringRedisTemplate.delete(KEY_PREFIX + name);
    //     }
    // }
}
