package ncepusa.distributedcars.config;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.event.Event;
import io.lettuce.core.RedisClient;
import org.springframework.data.redis.core.RedisCallback;
import reactor.core.publisher.Flux;
import jakarta.annotation.PostConstruct;
import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;
import rx.Observable;

import java.lang.reflect.Field;

@Component
public class RedisReconnectListener {

    private RedisInteraction redisInteraction;
    private final LettuceConnectionFactory connectionFactory;
    private static final Logger logger = LoggerFactory.getLogger(RedisReconnectListener.class.getName());

    @Autowired
    public RedisReconnectListener(LettuceConnectionFactory connectionFactory, RedisInteraction redisInteraction) {
        this.connectionFactory = connectionFactory;
        this.redisInteraction = redisInteraction;
    }

    @PostConstruct
    public void init() {
        try {

            RedisClient redisClient = (RedisClient) connectionFactory.getNativeClient();


            if (redisClient != null) {
                logger.info("✅ 成功获取到 Spring 管理的 RedisClient");

                Flux.from(redisClient.getResources().eventBus().get())
                        .filter(event -> event instanceof ConnectedEvent || event instanceof DisconnectedEvent)
                        .subscribe(event -> {
                            if (event instanceof ConnectedEvent) {
                                logger.info("🔗 Redis 连接已建立");
                                onRedisReconnected();
                            } else if (event instanceof DisconnectedEvent) {
                                logger.info("🔌 Redis 连接已断开");
                            }
                        });
            } else {
                logger.info("❌ RedisClient 为 null，请检查 Lettuce 配置");
            }

        } catch (Exception e) {
            logger.info("❌ 获取 RedisClient 失败{}", e.getMessage());
        }
    }


    private void onRedisReconnected() {
        System.out.println("🔄 Redis重连成功，执行恢复操作");
        redisInteraction.setNaViIdFinish();
    }
}