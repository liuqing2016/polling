package com.example.redisweb.redisUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;

public class RedisLockFactory {

    private RedisTemplate redisTemplate;
    private final Logger logger = LoggerFactory.getLogger(RedisLockFactory.class);

    private ThreadLocal<String> lockFlag = new ThreadLocal<String>();
    private static final String LUA_SCRIPT_LOCK = "return redis.call('set',KEYS[1],ARGV[1],'NX','PX',ARGV[2])";
    private static final RedisScript<String> SCRIPT_LOCK = new DefaultRedisScript<String>(LUA_SCRIPT_LOCK, String.class);
    private static final String LUA_SCRIPT_UNLOCK = "if redis.call('get',KEYS[1]) == ARGV[1] then return tostring(redis.call('del', KEYS[1])) else return '0' end";
    private static final RedisScript<String> SCRIPT_UNLOCK = new DefaultRedisScript<String>(LUA_SCRIPT_UNLOCK, String.class);

    public RedisLockFactory(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 加锁
     *
     * @param redisKey   缓存KEY
     * @param expire     到期时间 毫秒
     * @param retryTimes 重试次数
     * @param sleepMillis 重试等待时间
     * @return
     */
    public boolean tryLock(String redisKey, long expire, int retryTimes,long sleepMillis) {
        boolean result = setRedis(redisKey, expire);
        // 如果获取锁失败，按照传入的重试次数进行重试
        while(!result){
            retryTimes--;
            if(retryTimes < 0 ){
                result = false;
                break;
            }
            try {
                logger.info("lock failed, retrying..." + retryTimes);
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            result = setRedis(redisKey, expire);
        }
        return result;
    }

    public boolean setRedis(String key,long expire){
        String lockId = UUID.randomUUID().toString();
        lockFlag.set(lockId);
        Object lockResult = redisTemplate.execute(SCRIPT_LOCK,
                redisTemplate.getStringSerializer(),
                redisTemplate.getStringSerializer(),
                Collections.singletonList(key),
                lockId, String.valueOf(expire));
        return null != lockResult && "OK".equals(lockResult);
    }



    /**
     * 解锁
     *
     * @return
     */
    public boolean releaseLock(String key) {
        Object releaseResult = null;
        try {
            releaseResult = redisTemplate.execute(SCRIPT_UNLOCK,
                    redisTemplate.getStringSerializer(),
                    redisTemplate.getStringSerializer(),
                    Collections.singletonList(key),
                    lockFlag.get());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null != releaseResult && releaseResult.equals(1);
    }
}
