package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * <p>算法规划类</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class PathPlanning {
    private PathPlanningStrategy pathPlanning;
    @Contract(pure = true)
    public PathPlanning(PathPlanningStrategy pathPlanning) {
        this.pathPlanning = pathPlanning;
    }
    public List<GridNode> planPath(@NotNull GridMap map, GridNode start, GridNode end) {
        return pathPlanning.planPath(map, start, end);
    }
    public void setPathPlanning(@NotNull PathPlanningStrategy pathPlanning) {
        this.pathPlanning = pathPlanning;
    }
}
