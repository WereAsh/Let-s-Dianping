package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: WereAsh
 * @Date:2024-03-11 23:00
 **/

@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.153.129:6379").setPassword("redis");
        return Redisson.create(config);
    }
}
