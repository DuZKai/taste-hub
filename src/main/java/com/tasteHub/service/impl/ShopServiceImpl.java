package com.tasteHub.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tasteHub.dto.Result;
import com.tasteHub.entity.Shop;
import com.tasteHub.mapper.ShopMapper;
import com.tasteHub.service.IDistributedLock;
import com.tasteHub.service.IShopService;
import com.tasteHub.utils.CacheClient;
import com.tasteHub.constant.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tasteHub.constant.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Autowired
    private IDistributedLock distributedLock;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //
        // // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.SECONDS);


        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        // Shop shop = queryWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }

        return Result.ok(shop);
    }


    // public Shop queryWithMutex(Long id) {
    //     String key = CACHE_SHOP_KEY + id;
    //     // 1.查询缓存
    //     String shopJson = stringRedisTemplate.opsForValue().get(key);
    //
    //     // 判断是否存在
    //     if(StrUtil.isNotBlank(shopJson)){
    //         // 存在，返回
    //         return JSONUtil.toBean(shopJson, Shop.class);
    //     }
    //     // 判断命中是否是空值
    //     if (shopJson != null) {
    //         return null;
    //     }
    //     // 缓存重建
    //     // 获取互斥锁
    //     String lockKey = "lock:shop" + id;
    //     Shop shop = null;
    //     try{
    //         boolean islock = tryLock(lockKey);
    //         // 判断是否获取成功
    //         if (!islock) {
    //             // 失败。休眠重试
    //             Thread.sleep(50);
    //             return queryWithMutex(id);
    //         }
    //
    //         // 成功，查询数据库
    //         // 不存在，查询数据库
    //         shop = getById(id);
    //         // 不存在返回错误
    //         if (shop == null) {
    //             // 将空值写入redis
    //             stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
    //             return null;
    //         }
    //         // 存在，写入缓存
    //         stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //
    //     }
    //     catch (InterruptedException e){
    //         throw new RuntimeException(e);
    //     }finally {
    //         // 释放互斥锁
    //         unlock(lockKey);
    //     }
    //     return shop;
    // }
    //
    // private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    //
    // public Shop queryWithLogicalExpire(Long id) {
    //     String key = CACHE_SHOP_KEY + id;
    //     // 1.查询缓存
    //     String shopJson = stringRedisTemplate.opsForValue().get(key);
    //
    //     // 判断是否存在
    //     if(StrUtil.isBlank(shopJson)){
    //         // 存在，返回
    //         return null;
    //     }
    //     // 4.命中，需要先把json反序列化为对象
    //     RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
    //     Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
    //     LocalDateTime expireTime = redisData.getExpireTime();
    //     // 5.判断是否过期
    //     if(expireTime.isAfter(LocalDateTime.now())) {
    //         // 5.1.未过期，直接返回店铺信息
    //         return shop;
    //     }
    //     // 5.2.已过期，需要缓存重建
    //     // 6.缓存重建
    //     // 6.1.获取互斥锁
    //     String lockKey = LOCK_SHOP_KEY + id;
    //     boolean isLock = tryLock(lockKey);
    //     // 6.2.判断是否获取锁成功
    //     if (isLock){
    //         CACHE_REBUILD_EXECUTOR.submit( ()->{
    //
    //             try{
    //                 //重建缓存
    //                 this.saveShop2Redis(id,20L);
    //             }catch (Exception e){
    //                 throw new RuntimeException(e);
    //             }finally {
    //                 unlock(lockKey);
    //             }
    //         });
    //     }
    //     return shop;
    // }
    //
    // public void saveShop2Redis(Long id, Long expireSeconds) {
    //     // 查询店铺数据
    //     Shop shop = getById(id);
    //     RedisData redisData = new RedisData();
    //     redisData.setData(shop);
    //     redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
    //     // 写入redis
    //     stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(redisData));
    // }
    //
    // public Shop queryWithPassThrough(Long id) {
    //     String key = CACHE_SHOP_KEY + id;
    //     // 1.查询缓存
    //     String shopJson = stringRedisTemplate.opsForValue().get(key);
    //
    //     // 判断是否存在
    //     if(StrUtil.isNotBlank(shopJson)){
    //         // 存在，返回
    //         return JSONUtil.toBean(shopJson, Shop.class);
    //     }
    //     // 判断命中是否是空值
    //     if (shopJson != null) {
    //         return null;
    //     }
    //     // 不存在，查询数据库
    //     Shop shop = getById(id);
    //     // 不存在返回错误
    //     if (shop == null) {
    //         // 将空值写入redis
    //         stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
    //         return null;
    //     }
    //     // 存在，写入缓存
    //     stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
    //
    //     return shop;
    // }
    //
    // private boolean tryLock(String key) {
    //     Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
    //     return BooleanUtil.isTrue(flag);
    // }
    //
    // private void unlock(String key) {
    //     stringRedisTemplate.delete(key);
    // }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        try {
            // 获取分布式锁并执行任务
            distributedLock.executeWithLock(() -> {
                // 1.更新数据库
                updateById(shop);
                // 2.删除缓存
                stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
            }, 3, TimeUnit.SECONDS); // 设置锁的超时时间为3秒

            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("更新失败，获取锁时发生异常");
        } finally {
            // 分布式锁的关闭操作可以放在外部进行，保证在所有业务操作后释放锁
            distributedLock.close();
        }
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}
