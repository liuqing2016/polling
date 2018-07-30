package com.example.redisweb.service;

import com.example.redisweb.redisUtil.RedisLockFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 轮牌 实现
 */
@Service
public class PollingService {

    public static String KEYLOCK = "ys:web:group:lockkey:";
    public static String KEYINDEX = "ys:web:group:index:";
    public static String KEYGROUPS= "ys:web:group:";

    @Autowired
    RedisTemplate redisTemplate;

    private final Logger logger = LoggerFactory.getLogger(PollingService.class);

    /**
     * 初始化数据
     * @param keyIndex
     * @param keyGroups
     * @param groups
     */
    void init(String keyIndex,String keyGroups, List<String> groups){
        boolean flag = redisTemplate.hasKey(keyIndex);
        if(!flag){
            logger.info("init keyIndex:"+keyIndex);
            redisTemplate.opsForValue().set(keyIndex,"-1");
        }
        boolean flag2 = redisTemplate.hasKey(keyGroups);
        if(!flag2){
            logger.info("init keyGroups"+keyGroups);
            String[] groupArr = new String[groups.size()];
            groups.toArray(groupArr);
            String groupArrStr = String.join(",",groupArr);
            redisTemplate.opsForValue().set(keyGroups,groupArrStr);
        }
    }

    /**
     * 获取指定操作的轮牌值
     * @param option
     * @param groups
     * @return
     */
    public String getGroup(String option,List<String> groups)throws Exception{
        String keyLock = KEYLOCK+option;
        String keyIndex = KEYINDEX+option;
        String keyGroups = KEYGROUPS + option;
        init(keyIndex,keyGroups,groups);
        String group ="";
        // 获取锁
        RedisLockFactory lock= new RedisLockFactory(redisTemplate);
        boolean flagLock= lock.tryLock(keyLock,3000,3,1000); // 重试三次 每次等待 1s
        if(flagLock){
            group = polling(keyIndex,keyGroups,groups); //轮牌操作
            // 释放锁
            lock.releaseLock(keyLock);
        }else{
            throw new Exception("无法获取锁");
        }
        return group;
    }

    /**
     * 轮牌
     * @param keyIndex
     * @param keyGroups
     * @param groups
     * @return
     */
    String polling(String keyIndex,String keyGroups, List<String> groups){
        String ret ="";
        int index = Integer.parseInt(redisTemplate.opsForValue().get(keyIndex).toString());
        if(index == -1){ // 第一次轮牌
            ret = groups.get(0);
            updateData(keyIndex,0+"",keyGroups,groups);
            return ret;
        }
        // 获取历史记录
        List<String> recordArr = getList(redisTemplate.opsForValue().get(keyGroups).toString());

        boolean flagFinded= false;
        int findIndex = 0;
        // 定位遍历循环
        for(int i = index+1,j=0;j<recordArr.size();i++,j++){
            if(i==recordArr.size()){
                i = 0;
            }
            for(int k=0; k<groups.size(); k++){
                if(groups.get(k).equals(recordArr.get(i))){
                    flagFinded = true;
                    ret =groups.get(k);
                    findIndex = k;
                    break;
                }
            }
            if(flagFinded){
                break;
            }
        }
        if(!flagFinded){ // 不存在 直接取第一个
            ret = groups.get(0);
        }
        updateData(keyIndex,findIndex+"",keyGroups,groups);
        return ret;
    }

    /**
     * 跟新redis里面数据
     * @param keyIndex
     * @param keyVlaue
     * @param keyGroups
     * @param groups
     */
    void updateData(String keyIndex,String keyVlaue,String keyGroups, List<String> groups){
        redisTemplate.opsForValue().set(keyIndex,keyVlaue);
        String[] groupArr = new String[groups.size()];
        groups.toArray(groupArr);
        String groupArrStr = String.join(",",groupArr);
        redisTemplate.opsForValue().set(keyGroups,groupArrStr);
    }

    /**
     * 字符转集合
     * @param str
     * @return
     */
    List<String> getList(String str){
        String[] groupArr= str.split(",");
        return new ArrayList(Arrays.asList(groupArr));
    }

    /**
     * 查看数据
     * @param option
     * @return
     */
    public boolean showRedis(String option){
        String keyLock = KEYLOCK+option;
        String keyIndex = KEYINDEX+option;
        String keyGroups = KEYGROUPS + option;

        logger.info(keyLock+":\t"+redisTemplate.opsForValue().get(keyLock));
        logger.info(keyIndex+":\t"+redisTemplate.opsForValue().get(keyIndex));
        logger.info(keyGroups+":\t"+redisTemplate.opsForValue().get(keyGroups));
        return true;
    }

    /**
     * 清除数据
     * @param option
     * @return
     */
    public boolean clear(String option){

        String keyIndex = KEYINDEX+option;
        String keyGroups = KEYGROUPS + option;
        String keyLock = KEYLOCK+option;

        boolean flag = redisTemplate.hasKey(keyIndex);
        if(flag){
            logger.info("clear keyIndex:"+keyIndex);
            redisTemplate.delete(keyIndex);
        }
        boolean flag2 = redisTemplate.hasKey(keyGroups);
        if(flag2){
            logger.info("clear keyGroups:"+keyGroups);
            redisTemplate.delete(keyGroups);
        }
        boolean flag3 = redisTemplate.hasKey(keyLock);
        if(flag2){
            logger.info("clear keyLock:"+keyLock);
            redisTemplate.delete(keyLock);
        }
        return true;
    }
}
