package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @Author: WereAsh
 * @Date:2024-03-10 22:31
 **/
public class SimpleRedisLock implements ILock{

    private static final String PRE_FIX="lock:";

    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeOutSec) {

        String threadId =ID_PREFIX+ Thread.currentThread().getId();

        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(PRE_FIX + name, threadId, timeOutSec, TimeUnit.SECONDS);


        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unLock() {
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(PRE_FIX+name),
                ID_PREFIX+Thread.currentThread().getId()
        );
    }


/*    public void unLock() {
        String threadID=ID_PREFIX+Thread.currentThread().getId();
        String id=stringRedisTemplate.opsForValue().get(PRE_FIX+name);
        if(threadID.equals(id)) {
            stringRedisTemplate.delete(PRE_FIX + name);
        }
    }*/
}
