package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;

import java.util.List;

/**
 * <p>策略模式算法接口</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public interface PathPlanningStrategy {
    List<GridNode> planPath(GridMap map, GridNode start, GridNode end);
}