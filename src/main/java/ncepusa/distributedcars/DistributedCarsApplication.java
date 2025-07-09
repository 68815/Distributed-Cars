package ncepusa.distributedcars;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import ncepusa.distributedcars.config.RedisReconnectListener;
import ncepusa.distributedcars.navigator.Navigator;
import ncepusa.distributedcars.navigator.algorithm.PrimesUtil;
import ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener;
import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
//@EnableScheduling
public class DistributedCarsApplication{
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        for (DotenvEntry entry : dotenv.entries()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        ConfigurableApplicationContext context = SpringApplication.run(DistributedCarsApplication.class, args);
        RedisInteraction redisInteraction = context.getBean(RedisInteraction.class);
        ActiveMQListener activeMQListener = context.getBean(ActiveMQListener.class);
        RedisReconnectListener redisReconnectListener = context.getBean(RedisReconnectListener.class);
        new PrimesUtil();
        // 启动导航器
        Navigator navigator = new Navigator(redisInteraction, activeMQListener);


        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> redisInteraction.setIsNaviOpen(-1)));
    }

}