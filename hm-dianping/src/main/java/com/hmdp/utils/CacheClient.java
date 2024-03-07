package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @Author: WereAsh
 * @Date:2024-03-07 14:00
 **/
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    public void setWithLogicExpire(String key,Object value,Long time,TimeUnit unit){
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    public  <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type,
                                          Function<ID,R> dbFallBack,
                                          Long time,TimeUnit unit){
        String shopKey= keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(shopKey);
        if(StrUtil.isNotBlank(json)){
            return JSONUtil.toBean(json, type);
        }
        if(json!=null){
            return null;
        }
        R r=dbFallBack.apply(id);
        if(r==null){
            //将空值写入redis 避免缓存穿透
            this.set(shopKey,"",CACHE_NULL_TTL, unit);
            return null;
        }

        this.set(shopKey,r,time, unit);
        return r;
    }
}
