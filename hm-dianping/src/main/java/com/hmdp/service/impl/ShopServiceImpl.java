package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author WereAsh
 
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
//        Shop shop = queryWithPassThrough(id);
        Shop shop = queryWithMutex(id);
        if(shop==null){
            return Result.fail("店铺id不存在!");
        }
        return Result.ok(shop);
    }

    private Shop queryWithPassThrough(Long id){
        String shopKey= CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        Shop shop=getById(id);
        if(shop==null){
            //将空值写入redis 避免缓存穿透
            stringRedisTemplate.opsForValue().set(shopKey,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        stringRedisTemplate.opsForValue().set(shopKey,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return shop;
    }
    private Shop queryWithMutex(Long id){
        String shopKey=CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if(StrUtil.isNotBlank(shopJson)){
            return JSONUtil.toBean(shopJson,Shop.class);
        }
        if(shopJson!=null){
            return null;
        }
        String lockKey=LOCK_SHOP_KEY+id;
        Shop shop = null;
        boolean isLock = tryLock(lockKey);
        try {

            if(!isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //双重检查锁
            shopJson = stringRedisTemplate.opsForValue().get(shopKey);
            if(StrUtil.isNotBlank(shopJson)){
                return JSONUtil.toBean(shopJson,Shop.class);
            }
            if(shopJson!=null){
                return null;
            }
            shop = getById(id);
            if(shop==null){
                stringRedisTemplate.opsForValue().set(shopKey,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            stringRedisTemplate.opsForValue().set(shopKey,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unLock(lockKey);
        }

        return shop;
    }

    private static final ExecutorService CACHE_BUILD_EXECUTOR=Executors.newFixedThreadPool(10);
    private Shop queryWithLogicExpire(Long id){
        String shopKey=CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        if(StrUtil.isBlank(shopJson)){
            return null;
        }
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        if(expireTime.isAfter(LocalDateTime.now())){
            return shop;
        }
        String lockKey=LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        if(isLock){
            CACHE_BUILD_EXECUTOR.submit(()->{
                try{
                    this.saveShop2Redis(id,20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    unLock(lockKey);
                }
            });
        }
        return shop;
    }

    private void saveShop2Redis(Long id,Long expireTime){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY,JSONUtil.toJsonStr(redisData));
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }

        String shopKey=CACHE_SHOP_KEY+id;
        updateById(shop);

        stringRedisTemplate.delete(shopKey);


        return Result.ok();
    }
}
