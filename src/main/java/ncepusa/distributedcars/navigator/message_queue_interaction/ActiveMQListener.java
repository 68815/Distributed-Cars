package ncepusa.distributedcars.navigator.message_queue_interaction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ncepusa.distributedcars.navigator.algorithm.AStar;
import ncepusa.distributedcars.navigator.algorithm.JPS;
import ncepusa.distributedcars.navigator.algorithm.PathPlanning;
import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.geo.Point;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>ActiveMQ监听器类，用于监听并处理来自ActiveMQ的消息</p>
 * <p>只监听，不发送</p>
 *
 * @author 0109
 * @since 2025-05-22
 */
@Lazy
@Component
public class ActiveMQListener {

    private final RedisInteraction redisInteraction;
    private final MeterRegistry registry = new SimpleMeterRegistry();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private final PathPlanning pathPlanning = new PathPlanning(null);
    private int carNumbers = 0;
    List<String> visitedMap;
    List<String> obstacleMap;
    private Point mapSize;
    List<Point> carPosition;
    List<GridMap> gridMap;
    List<List<GridNode>> path;

    @Autowired
    @Contract(pure = true)
    public ActiveMQListener(RedisInteraction redisInteraction) {
        this.redisInteraction = redisInteraction;
        redisInteraction.setNaViIdFinish();
        visitedMap = new ArrayList<String>();
        obstacleMap = new ArrayList<String>();
        carPosition = new ArrayList<Point>();
        gridMap = new ArrayList<GridMap>();
        path = new ArrayList<List<GridNode>>();
    }
    private static final Logger logger = LoggerFactory.getLogger(ActiveMQListener.class);

    /**
     * <p>使用独占消费者模式确保只有一个消费者进程在监听该消息，否则会出现一条消息被多个消费者同时消费的问题</p>
     * <p>这样当最开始获得连接的消费者进程关闭时（手动关闭或异常退出）可以有其他的消费者进程接手，保证导航器的正常运行</p>
     * <p>但独占消费者模式目前还存在一个待优化的问题：永远都只有一个消费者进程在处理消息，其他消费者只能一直尝试连接activemq队列，从而不能贡献自己的算力</p>
     * <p>应该在主消费者进程来不及处理其他消息时（即使是多线程在处理消息也有可能处理不过来），有其他消费者处理这些积压的消息</p>
     *
     * @param message 消息
     */
    @JmsListener(destination = "UpdateNavigate?consumer.exclusive=true")
    public void primaryOnMessage(@NotNull String message) {
        carNumbers = redisInteraction.getCarNumbers();
        while(carPosition.size() - 1 < carNumbers) {
            visitedMap.add("");
            obstacleMap.add("");
            carPosition.add(new Point(0, 0));
            gridMap.add(null);
            path.add(null);
        }
        executor.submit(() -> processMessage(message));
    }

    public void processMessage(String message) {
        try {

            String carId = message.substring(4);
            carId = carId.substring(0, carId.length() - 1);
            readDataFromRedis(carId);
            generatePath(carId);
            writePathToRedis(carId);
            path.set(Integer.parseInt(carId), null);
        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            registry.counter("messages.failed").increment();
        }
    }

    /**
     *  <p>从Redis中读取数据</p>
     *  <p>如果读取失败，记录失败消息并返回</p>
     */
    private void readDataFromRedis(String carId) {
        long redisReadStart = System.nanoTime();
        logger.info(carId);
        int carid = Integer.parseInt(carId);
        String carPositionCoordinate = redisInteraction.getCarPositionCoordinate(carId);

        visitedMap.set(carid, redisInteraction.getMap());
        obstacleMap.set(carid, redisInteraction.getObstacleMap());
        mapSize = redisInteraction.getMapSize();

        if(null == carPositionCoordinate || null == visitedMap || null == obstacleMap || null == mapSize){
            registry.counter("messages.failed").increment();
            logger.error("redis中没有足够的数据，无法进行路径规划");
            return;
        }
        logger.info(obstacleMap.get(carid));
        logger.info(visitedMap.get(carid));
        if(mapSize.getX() * mapSize.getY() != visitedMap.get(carid).length() ||
                mapSize.getX() * mapSize.getY()!= obstacleMap.get(carid).length()){
            registry.counter("messages.failed").increment();
            logger.error("redis中地图数据长度不一致，无法进行路径规划");
            return;
        }
        String[] parts = carPositionCoordinate.split(",");
        carPosition.set(carid,new Point(Integer.parseInt(parts[1]), Integer.parseInt(parts[0])));

        long redisReadEnd = System.nanoTime();

        logger.info("读redis花费时间： {} ms", (redisReadEnd - redisReadStart) / 1e6);
    }

    /**
     * <p>生成路径</p>
     * <p>如果生成失败，说明终点对于起点来说是不可达的，则更换终点重新规划</p>
     */
    private void generatePath(String carId) {
        long pathPlanningStart = System.nanoTime();
        int carid = Integer.parseInt(carId);
        gridMap.set(carid,
                new GridMap(visitedMap.get(carid),
                        obstacleMap.get(carid),
                        mapSize,
                        carPosition.get(carid)));
        pathPlanning.setPathPlanning(new JPS());
        while(null == path.get(carid) || path.get(carid).isEmpty()) {
            gridMap.get(carid).electEndpoint(carNumbers, carid);
            path.set(carid, pathPlanning.planPath(gridMap.get(carid), gridMap.get(carid).getStart(), gridMap.get(carid).getEnd()));
            gridMap.get(carid).getEnd().setArrived(false);
        }
        long pathPlanningEnd = System.nanoTime();

        //监视器记录时间
        registry.timer("pathPlanning.time").record(pathPlanningEnd - pathPlanningStart, TimeUnit.NANOSECONDS);

        //写日志
        logger.info("路径规划(({},{}) -> ({},{}))花费时间: {} ms ",
                gridMap.get(carid).getStart().getX(), gridMap.get(carid).getStart().getY(),
                gridMap.get(carid).getEnd().getX(), gridMap.get(carid).getEnd().getY(),
                (pathPlanningEnd - pathPlanningStart) / 1e6);
        // 修改日志输出方式
        logger.info("路径结果: {}",
                path.get(carid).stream()
                        .map(node -> String.format("(%d,%d)", node.getX(), node.getY()))
                        .collect(Collectors.joining(" -> ")));
    }

    /**
     * <p>将路径写入Redis</p>
     * <p>如果写入失败，记录失败消息并返回</p>
     */
    private void writePathToRedis(String carId) {
        long redisWriteStart = System.nanoTime();
        int carid = Integer.parseInt(carId);
        redisInteraction.setTaskQueue(carId, path.get(carid));
        redisInteraction.setNaViIdFinish();

        long redisWriteEnd = System.nanoTime();

        logger.info("写redis花费时间: {} ms", (redisWriteEnd - redisWriteStart) / 1e6);
    }
}