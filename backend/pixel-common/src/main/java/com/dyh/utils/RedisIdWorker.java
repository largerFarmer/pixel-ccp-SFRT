package com.dyh.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.dyh.constant.RedisConstants.REDIS_ID_KEY;

/**
 * 通过Redis生成全局唯一ID
 *
 * @author pixel-revolve
 * @date 2022/11/20
 */
@Component
public class RedisIdWorker {
    /**
     * 开始时间戳：2022/1/1 08:00:00
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 16;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
    //    this.stringRedisTemplate = stringRedisTemplate;
    //}

    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2.生成序列号
        // 2.1.获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长
        long count = stringRedisTemplate.opsForValue().increment(REDIS_ID_KEY + keyPrefix + ":" + date);
        // 3.拼接并返回
        return timestamp << COUNT_BITS | count;
    }
}
