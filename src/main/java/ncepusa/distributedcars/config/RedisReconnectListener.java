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
                Flux.from(redisClient.getResources().eventBus().get())
                        .filter(event -> event instanceof ConnectedEvent || event instanceof DisconnectedEvent)
                        .subscribe(event -> {
                            if (event instanceof ConnectedEvent) {
                                onRedisReconnected();
                            }
                        });
            }
        } catch (Exception e) {
        }
    }
    private void onRedisReconnected() {
        redisInteraction.setNaViIdFinish();
    }
}