package com.tasteHub.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tasteHub.dto.Result;
import com.tasteHub.entity.SeckillVoucher;
import com.tasteHub.entity.VoucherOrder;
import com.tasteHub.mapper.VoucherOrderMapper;
import com.tasteHub.service.ISeckillVoucherService;
import com.tasteHub.service.IVoucherOrderService;
import com.tasteHub.utils.IdWorkerUtils;
import com.tasteHub.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // ------------------------------------------------------------------------
    // 使用Lua脚本在Redis中执行秒杀操作
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // Long orderId = redisIdWorker.nextId("order");
        Long orderId = IdWorkerUtils.getInstance().nextId();
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), orderId.toString()
        );
        int r = 0;
        if (result != null) {
            r = result.intValue();
        }
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 3.返回订单id
        return Result.ok(orderId);
    }
    // ------------------------------------------------------------------------
    // 使用Redis分布式锁初级版本，在极端情况下，即线程1因为业务阻塞长时间占用锁，线程1中Redis超时释放后，线程2获取锁执行业务
    // 线程1业务执行完毕后，线程1直接释放线程2的锁，再次新来线程3获取锁，导致线程2的业务未执行完毕，线程3已经在执行锁，两者则同时执行业务
    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 1.查询优惠券
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     // 2.判断秒杀是否开始
    //     if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
    //         // 尚未开始
    //         return Result.fail("秒杀尚未开始！");
    //     }
    //     // 3.判断秒杀是否已经结束
    //     if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
    //         // 尚未开始
    //         return Result.fail("秒杀已经结束！");
    //     }
    //     // 4.判断库存是否充足
    //     if (voucher.getStock() < 1) {
    //         // 库存不足
    //         return Result.fail("库存不足！");
    //     }
    //     Long userId = UserHolder.getUser().getId();
    //     SimpleRedisLockImpl lock = new SimpleRedisLockImpl("lock:order:" + userId, stringRedisTemplate);
    //     boolean isLock = lock.tryLock(10);
    //     // 判断是否获取到锁
    //     if (!isLock) {
    //         return Result.fail("请勿重复提交订单！");
    //     }
    //     // 获取代理对象，使得事务生效
    //     try{
    //         IVoucherOrderService currentProxy = (IVoucherOrderService) AopContext.currentProxy();
    //         return currentProxy.createVoucherOrder(voucherId);
    //     }
    //     finally {
    //         lock.unlock();
    //     }
    // }
    //
    // @Transactional
    // public Result createVoucherOrder(Long voucherId) {
    //     // 5.一人一单
    //     Long userId = UserHolder.getUser().getId();
    //     // 5.1.查询订单
    //     long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    //     // 5.2.判断是否存在
    //     if (count > 0) {
    //         // 用户已经购买过了
    //         return Result.fail("用户已经购买过一次！");
    //     }
    //
    //     // 6.扣减库存
    //     boolean success = seckillVoucherService.update()
    //             .setSql("stock = stock - 1") // set stock = stock - 1
    //             .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
    //             .update();
    //     if (!success) {
    //         // 扣减失败
    //         return Result.fail("库存不足！");
    //     }
    //
    //     // 7.创建订单
    //     VoucherOrder voucherOrder = new VoucherOrder();
    //     // 7.1.订单id
    //     long orderId = IdWorkerUtils.getInstance().nextId();
    //     voucherOrder.setId(orderId);
    //     // 7.2.用户id
    //     voucherOrder.setUserId(userId);
    //     // 7.3.代金券id
    //     voucherOrder.setVoucherId(voucherId);
    //     save(voucherOrder);
    //
    //     // 7.返回订单id
    //     return Result.ok(orderId);
    // }
    // ------------------------------------------------------------------------
    // 单机部署使用悲观锁和乐观锁解决超卖问题
    // @Override
    // public Result seckillVoucher(Long voucherId) {
    //     // 1.查询优惠券
    //     SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
    //     // 2.判断秒杀是否开始
    //     if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
    //         // 尚未开始
    //         return Result.fail("秒杀尚未开始！");
    //     }
    //     // 3.判断秒杀是否已经结束
    //     if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
    //         // 尚未开始
    //         return Result.fail("秒杀已经结束！");
    //     }
    //     // 4.判断库存是否充足
    //     if (voucher.getStock() < 1) {
    //         // 库存不足
    //         return Result.fail("库存不足！");
    //     }
    //     Long userId = UserHolder.getUser().getId();
    //     synchronized (userId.toString().intern()) {
    //         // 获取代理对象，使得事务生效
    //         IVoucherOrderService currentProxy = (IVoucherOrderService) AopContext.currentProxy();
    //         return currentProxy.createVoucherOrder(voucherId);
    //     }
    // }
    //
    // // 悲观锁解决超卖问题
    // @Transactional
    // public Result createVoucherOrder(Long voucherId) {
    //     // 5.一人一单
    //     Long userId = UserHolder.getUser().getId();
    //     // 5.1.查询订单
    //     long count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
    //     // 5.2.判断是否存在
    //     if (count > 0) {
    //         // 用户已经购买过了
    //         return Result.fail("用户已经购买过一次！");
    //     }
    //
    //     // 6.扣减库存
    //     boolean success = seckillVoucherService.update()
    //             .setSql("stock = stock - 1") // set stock = stock - 1
    //             .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
    //             .update();
    //     if (!success) {
    //         // 扣减失败
    //         return Result.fail("库存不足！");
    //     }
    //
    //     // 7.创建订单
    //     VoucherOrder voucherOrder = new VoucherOrder();
    //     // 7.1.订单id
    //     long orderId = IdWorkerUtils.getInstance().nextId();
    //     voucherOrder.setId(orderId);
    //     // 7.2.用户id
    //     voucherOrder.setUserId(userId);
    //     // 7.3.代金券id
    //     voucherOrder.setVoucherId(voucherId);
    //     save(voucherOrder);
    //
    //     // 7.返回订单id
    //     return Result.ok(orderId);
    // }
}
