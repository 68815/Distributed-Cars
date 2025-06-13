package ncepusa.distributedcars.config;

import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthChecker {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisInteraction redisInteraction;
    private static final Logger logger = LoggerFactory.getLogger(RedisHealthChecker.class);

    public RedisHealthChecker(RedisTemplate<String, String> redisTemplate, RedisInteraction redisInteraction) {
        this.redisTemplate = redisTemplate;
        this.redisInteraction = redisInteraction;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkAndHandleReconnect() {
        try {
            logger.info("🔄 检查 Redis 连接状态");
            String result = redisTemplate.opsForValue().get("health_check");
            if (result == null) {
                redisTemplate.opsForValue().set("health_check", "ok");
                onRedisAvailable();
            }
        } catch (Exception e) {
            // Redis 不可用
        }
    }

    private void onRedisAvailable() {
        logger.info("🔄 Redis重连成功，执行恢复操作");
        redisInteraction.setIsNaviOpen(1);
        redisInteraction.setNaViIdFinish();
    }
}