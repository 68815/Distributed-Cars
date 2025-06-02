package ncepusa.distributedcars.navigator;

import ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener;
import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>导航器类，整合读写redis、监听activemq等</p>
 *
 * @author 0109
 * @since 2025-05-22
 */
public class Navigator {
    private final RedisInteraction redisInteraction;
    private final ActiveMQListener activeMQListener;
    private static final Logger logger = LoggerFactory.getLogger(Navigator.class);
    @Contract(pure = true)
    public Navigator(@NotNull RedisInteraction redisInteraction, @NotNull ActiveMQListener activeMQListener) {
        this.redisInteraction = redisInteraction;
        this.activeMQListener = activeMQListener;
        redisInteraction.setIsNaviOpen(1);
    }
}
