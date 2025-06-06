package ncepusa.distributedcars.navigator.redis_interaction;

import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>Redis交互类</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
@Lazy
@Component
public class RedisInteraction {
    /**
     * RedisTemplate对象，用于与Redis进行交互
     */
    private final RedisTemplate<String, String> redisTemplate;

    private static final String MAP_KEY = "map";
    private static final String OBSTACLE_MAP_KEY = "obstacle_map";
    private static final String CAR_NUMBER_KEY = "CarNumber";
    private static final String MAP_LENGTH_KEY = "mapLength";
    private static final String MAP_WIDTH_KEY = "mapWidth";
    private static final String IS_NAVI_FINISH_KEY = "IsNaviFinish";
    private static final String IS_NAVI_OPEN_KEY = "IsNaviOpen";
    private final String IS_NAVI_OPEN_LOCK_KEY = "IsNaviOpenLock";
    private final String IS_NAVI_FINISH_LOCK_KEY = "IsNaviFinishLock";
    @Autowired
    public RedisInteraction(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public byte[] getMap() {
        assert redisTemplate != null;
        return redisTemplate.execute((RedisCallback<byte[]>) connection -> {
            return connection.stringCommands().get(MAP_KEY.getBytes());
        });
    }

    public byte[] getObstacleMap() {
        assert redisTemplate != null;
        return redisTemplate.execute((RedisCallback<byte[]>) connection -> {
            return connection.stringCommands().get(OBSTACLE_MAP_KEY.getBytes());
        });
    }

    public String getCarPositionCoordinate(String carId) {
        assert redisTemplate != null;
        return redisTemplate.opsForValue().get("Car" + carId);
    }
    public int getCarNumbers()
    {
        assert redisTemplate != null;
        return Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(CAR_NUMBER_KEY)));
    }
    public Point getMapSize() {
        assert redisTemplate != null;
        return new Point(
                Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(MAP_LENGTH_KEY))),
                Integer.parseInt(Objects.requireNonNull(redisTemplate.opsForValue().get(MAP_WIDTH_KEY))));
    }

    public void setTaskQueue(String carId, @NotNull List<Point> gridNodes) {
        assert redisTemplate != null;
        redisTemplate.opsForList().rightPushAll("Car" + carId + "TaskList", gridNodes.stream()
                .map(node -> node.getX() + "," + node.getY())
                .toArray(String[]::new));
    }

    public void setNaViIdFinish()
    {
        String lockValue = UUID.randomUUID().toString();
        long expireTime = 10000;

        while (!acquireLock(IS_NAVI_FINISH_LOCK_KEY, lockValue, expireTime)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            String currentValue = redisTemplate.opsForValue().get(IS_NAVI_FINISH_KEY);
            int count = currentValue == null ? 0 : (Integer.parseInt(currentValue) + 1);
            redisTemplate.opsForValue().set(IS_NAVI_FINISH_KEY, String.valueOf(count));
        } finally {
            if (lockValue.equals(redisTemplate.opsForValue().get(IS_NAVI_FINISH_LOCK_KEY))) {
                releaseLock(IS_NAVI_FINISH_LOCK_KEY);
            }
        }
    }
    /**
     * 设置导航器数量
     * 加锁后，获取当前导航器数量，修改值，然后释放锁
     * 加锁是为了避免多个进程或线程同时获取了导航器的数量然后修改，导致导航器数量不正确
     */
    public void setIsNaviOpen(int fix)
    {
        String lockValue = UUID.randomUUID().toString();
        long expireTime = 10000;
        while (!acquireLock(IS_NAVI_OPEN_LOCK_KEY, lockValue, expireTime)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            String currentValue = redisTemplate.opsForValue().get(IS_NAVI_OPEN_KEY);
            int count = currentValue == null ? 0 : Integer.parseInt(currentValue);
            redisTemplate.opsForValue().set(IS_NAVI_OPEN_KEY, String.valueOf(count + fix));
        } finally {
            if (lockValue.equals(redisTemplate.opsForValue().get(IS_NAVI_OPEN_LOCK_KEY))) {
                releaseLock(IS_NAVI_OPEN_LOCK_KEY);
            }
        }
    }
    /**
     * 获取参数key的锁
     * 该锁属于redis上的分布式锁
     * 避免多进程的竞争资源问题
     *
     * @param key 锁的键
     * @param value 锁的随机值
     * @param expireTime 锁的过期时间
     * @return 是否获取到锁
     */
    public boolean acquireLock(String key, String value, long expireTime) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放参数key的锁
     * @param key 锁的键
     */
    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }
}