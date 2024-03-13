# 校园娱乐共享平台

技术栈：SpringBoot+MyBatis-Plus+MySQL+Redis

- 采用 Redis 实现共享 session 登入，解决原生 session 共享问题；
- 封装封装 Redis 工具类工具类，解决可能出现的缓存穿透、缓存击穿等问题；
- 使用 Redis Lua 脚本和阻塞队列，完成店铺优惠券秒杀和异步下单优惠券秒杀和异步下单功能；
- 采用 Zset 实现博客点赞、点赞排行榜以及关注等相关功能；
- 分别采用 GEO 和和 BitMap 实现附近相关店铺搜索和签到功能
