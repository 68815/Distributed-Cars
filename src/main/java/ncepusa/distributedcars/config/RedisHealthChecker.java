package ncepusa.distributedcars.config;

import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthChecker {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisInteraction redisInteraction;

    public RedisHealthChecker(RedisTemplate<String, String> redisTemplate, RedisInteraction redisInteraction) {
        this.redisTemplate = redisTemplate;
        this.redisInteraction = redisInteraction;
    }

    @Scheduled(fixedDelay = 5000)
    public void checkAndHandleReconnect() {
        try {
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
        redisInteraction.setIsNaviOpen(1);
        redisInteraction.setNaViIdFinish();
    }
}