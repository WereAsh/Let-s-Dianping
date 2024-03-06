package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author WereAsh
 
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryShopList() {

        List<String> shopTypesJson = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);

        if (CollectionUtil.isNotEmpty(shopTypesJson)) {
            List<ShopType> shopTypes = shopTypesJson.stream()
                    .map(item -> JSONUtil.toBean(item, ShopType.class))
                    .sorted(Comparator.comparingInt(ShopType::getSort))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        if(CollectionUtil.isEmpty(shopTypes)){
            //不存在则返回一个空集合，解决缓存穿透

            stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY,Collections.emptyList());
            stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY,CACHE_SHOP_TTL,TimeUnit.MINUTES);

            return Result.fail("商铺数据为空！");
        }


        shopTypesJson = shopTypes.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());

        //此处使用右插入保存顺序性
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY,shopTypesJson);
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        return Result.ok(shopTypes);
    }
}
