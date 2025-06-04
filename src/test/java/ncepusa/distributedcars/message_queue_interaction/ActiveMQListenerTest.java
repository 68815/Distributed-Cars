package ncepusa.distributedcars.message_queue_interaction;

import ncepusa.distributedcars.navigator.algorithm.PrimesUtil;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener;
import ncepusa.distributedcars.navigator.redis_interaction.RedisInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ActiveMQListener.class)
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class ActiveMQListenerTest {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQListenerTest.class);

    @InjectMocks
    private ActiveMQListener activeMQListener;

    @Mock
    private RedisInteraction redisInteraction;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        activeMQListener = new ActiveMQListener(redisInteraction);
    }

    @Test
    public void testPrimaryOnMessage_ValidMessage_SuccessfulProcessing() throws InterruptedException {
        // Arrange
        new PrimesUtil();
        String carId = "001";
        String message = "\"Car" + carId + "\"";
        String carPositionCoordinate = "1,150";
        Point mapSize = new Point(1000,1000);
        byte[] mapArray = new byte[(int) ((mapSize.getX() * mapSize.getY() + 7)/ 8)];
        for(int j = 0; j < mapSize.getY(); j++){
            for(int i = 0; i < mapSize.getX(); i++){
                int index = j * (int)mapSize.getX() + i;
                if(index % 8 == 0) mapArray[index / 8] = 0;
                if(i % (3 + (i & 1)) == 0 || j % (7 + ((i * j) & 1)) == 0) mapArray[index / 8] |= (byte) (1 << (7 - index % 8));
            }
        }
        byte[] obstacleMapArray = new byte[(int) ((mapSize.getX() * mapSize.getY() + 7)/ 8)];
        for(int j = 0; j < mapSize.getY(); j++){
            for(int i = 0; i < mapSize.getX(); i++){
                int index = j * (int)mapSize.getX() + i;
                if(index % 8 == 0) obstacleMapArray[index / 8] = 0;
                if(i % (3 + (i & 1)) == 0 && j % (7 + ((i * j) & 1)) <= 2) obstacleMapArray[index / 8] |= (byte) (1 << (7 - index % 8));
            }
        }

        when(redisInteraction.getCarNumbers()).thenReturn(3);
        when(redisInteraction.getCarPositionCoordinate(carId)).thenReturn(carPositionCoordinate);
        when(redisInteraction.getMap()).thenReturn(mapArray);
        when(redisInteraction.getObstacleMap()).thenReturn(obstacleMapArray);
        when(redisInteraction.getMapSize()).thenReturn(mapSize);

        ArgumentCaptor<List<GridNode>> pathCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        activeMQListener.primaryOnMessage(message);

        Thread.sleep(10000000);

        // Assert
        verify(redisInteraction, times(1)).getCarPositionCoordinate(eq(carId));
        verify(redisInteraction, times(1)).getMap();
        verify(redisInteraction, times(1)).getObstacleMap();

        verify(redisInteraction, times(1)).getMapSize();
        verify(redisInteraction, times(1)).setTaskQueue(eq(carId), pathCaptor.capture());

        List<GridNode> capturedPath = pathCaptor.getValue();
        assertNotNull(capturedPath);
        assertFalse(capturedPath.isEmpty());
    }
}