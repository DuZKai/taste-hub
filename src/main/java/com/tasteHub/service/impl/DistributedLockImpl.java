package com.tasteHub.service.impl;

import com.tasteHub.service.IDistributedLock;
import jakarta.annotation.PostConstruct;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DistributedLockImpl implements IDistributedLock {
    // connectString = "192.168.101.65:2181,192.168.101.65:2182,192.168.101.65:2183"
    private CuratorFramework client;
    private InterProcessMutex lock;

    @Value("${zookeeper.connectString}")
    private String connectString;

    // 使用@PostConstruct注解，确保connectString已经注入后再初始化client和lock
    @PostConstruct
    public void init() {
        // 初始化Curator客户端并创建分布式锁
        client = CuratorFrameworkFactory.builder()
                .connectString(connectString)  // 在此处connectString应该不为null
                .sessionTimeoutMs(60 * 1000) // 会话超时时间
                .connectionTimeoutMs(15 * 1000) // 连接超时时间
                .retryPolicy(new ExponentialBackoffRetry(1000, 3)) // 重试策略，重试3次，每次间隔1秒
                .namespace("tastHub") // 命名空间
                .build();
        client.start();
        boolean connected = false;
        try {
            connected = client.blockUntilConnected(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!connected) {
            throw new IllegalStateException("ZooKeeper 连接超时");
        }

        lock = new InterProcessMutex(client, "/lock");
        System.out.println("ZooKeeper 客户端初始化成功");
    }

    // 获取分布式锁并执行任务
    @Override
    public void executeWithLock(Runnable task, long timeout, TimeUnit unit) throws Exception {
        try {
            if (lock.acquire(timeout, unit)) {
                try {
                    task.run(); // 执行传入的任务
                } finally {
                    lock.release(); // 确保释放锁
                }
            } else {
                System.out.println("无法获取锁，任务未执行");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("获取锁时发生异常", e);
        }
    }

    // 关闭Curator客户端
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}