package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author: WereAsh
 * @Date:2024-03-09 19:43
 **/
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP=1696118400L;

    private static final int COUNT_BITS=32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        LocalDateTime now=LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp=nowSecond-BEGIN_TIMESTAMP;
        //生成序列号
            //获取当前日期。精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
            //自增长
        long count=stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);


        //拼接并返回
        return timeStamp<<COUNT_BITS|count;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023, 10, 1, 0,0,0 );
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println("second="+second);

    }
}
