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
public class RedisInteraction{
    /**
     * 地图
     */
    private final RedisTemplate<String, String> map;

    private final String IS_NAVI_OPEN_LOCK_KEY = "IsNaviOpenLock";
    private final String IS_NAVI_FINISH_LOCK_KEY = "IsNaviFinishLock";



    @Autowired
    public RedisInteraction(RedisTemplate<String, String> redisTemplate) {
        this.map = redisTemplate;
    }

    public String getMap() {
        assert map != null;
        return map.execute((RedisCallback<String>) connection -> {
            byte[] bytes = connection.stringCommands().get("map".getBytes());
            if (bytes == null) return null;
            Point mapSize = getMapSize();
            StringBuilder result = new StringBuilder();
            for (int k = 0; k < bytes.length; k++) {
                for (int i = 7; i >= 0; i--) {
                    if(k * 8 + (8 - i) > (int)(mapSize.getX() * mapSize.getY())) break;
                    result.append((bytes[k] >> i) & 1);
                }
            }
            return result.toString();
        });
    }

    public String getObstacleMap() {
        assert map != null;
        return map.execute((RedisCallback<String>) connection -> {
            byte[] bytes = connection.stringCommands().get("obstacle_map".getBytes());
            if (bytes == null) return null;
            Point mapSize = getMapSize();
            StringBuilder result = new StringBuilder();
            for (int k = 0; k < bytes.length; k++) {
                for (int i = 7; i >= 0; i--) {
                    if(k * 8 + (8 - i) > (int)(mapSize.getX() * mapSize.getY())) break;
                    result.append((bytes[k] >> i) & 1);
                }
            }
            return result.toString();
        });
    }

    public String getCarPositionCoordinate(String carId) {
        assert map != null;
        return map.opsForValue().get("Car" + carId);
    }
    public int getCarNumbers()
    {
        assert map!= null;
        return Integer.parseInt(Objects.requireNonNull(map.opsForValue().get("CarNumber")));
    }
    public Point getMapSize() {
        assert map != null;
        return new Point(
                Integer.parseInt(Objects.requireNonNull(map.opsForValue().get("mapLength"))),
                Integer.parseInt(Objects.requireNonNull(map.opsForValue().get("mapWidth"))));
    }

    public void setTaskQueue(String carId, @NotNull List<GridNode> gridNodes) {
        assert map != null;
        map.opsForList().rightPushAll("Car" + carId + "TaskList", gridNodes.stream()
                .map(node -> node.getX() + "," + node.getY())
                .toArray(String[]::new));
    }

    public void setNaViIdFinish()
    {
       /* String sp = "rw";
        char spp = sp.charAt(1);
              spp |= 22;*/
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
            String currentValue = map.opsForValue().get("IsNaviFinish");
            int count = currentValue == null ? 0 : (Integer.parseInt(currentValue) + 1);
            map.opsForValue().set("IsNaviFinish", String.valueOf(count));
        } finally {
            if (lockValue.equals(map.opsForValue().get(IS_NAVI_FINISH_LOCK_KEY))) {
                releaseLock(IS_NAVI_FINISH_LOCK_KEY);
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
        Boolean result = map.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 释放参数key的锁
     * @param key 锁的键
     */
    public void releaseLock(String key) {
        map.delete(key);
    }

    /**
     * 设置导航器数量加1
     * 加锁后，获取当前导航器数量，加1，然后释放锁
     * 加锁是为了避免多个进程同时获取了导航器的数量然后加1，导致导航器数量不正确
     */
    public void setOpen()
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
            String currentValue = map.opsForValue().get("IsNaviOpen");
            int count = currentValue == null ? 0 : Integer.parseInt(currentValue);
            map.opsForValue().set("IsNaviOpen", String.valueOf(count + 1));
        } finally {
            if (lockValue.equals(map.opsForValue().get(IS_NAVI_OPEN_LOCK_KEY))) {
                releaseLock(IS_NAVI_OPEN_LOCK_KEY);
            }
        }
    }

    public void setClose() {
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
            String currentValue = map.opsForValue().get("IsNaviOpen");
            int count = 1;
            if (currentValue != null) {
                count = Integer.parseInt(currentValue);
            }
            map.opsForValue().set("IsNaviOpen", String.valueOf(count - 1));
        } finally {
            if (lockValue.equals(map.opsForValue().get(IS_NAVI_OPEN_LOCK_KEY))) {
                releaseLock(IS_NAVI_OPEN_LOCK_KEY);
            }
        }
    }
}