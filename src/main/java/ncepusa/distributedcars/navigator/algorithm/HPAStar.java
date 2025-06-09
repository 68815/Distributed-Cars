package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.springframework.data.geo.Point;

import java.util.*;

/**
 * <p>HPA*(Hierarchical Pathfinding A*)算法</p>
 *
 * @author 0109
 * @since 2025-06-04
 * @deprecated 未完成，不使用
 */
@Deprecated
public class HPAStar implements PathPlanningStrategy {
    private int clusterWidth;
    private int clusterHeight;
    @Override
    public List<Point> planPath(GridMap map, @NotNull GridNode start, GridNode end) {
        if (start.equals(end)) {
            return List.of();
        }
        List<Point> highLevelPath = findHighLevelPath(map, start, end);

        List<Point> detailedPath = new ArrayList<>();
        for (int i = 0; i < highLevelPath.size() - 1; i++) {
            Point currentCluster = highLevelPath.get(i);
            Point nextCluster = highLevelPath.get(i + 1);
            detailedPath.addAll(findIntraClusterPath(map, currentCluster, nextCluster));
        }

        if(!detailedPath.isEmpty()) detailedPath.remove(0);

        return detailedPath;
    }

    @Contract("_, _, _ -> new")
    private @NotNull @Unmodifiable List<Point> findHighLevelPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        map.setClusterSize();
        clusterWidth = map.getClusterWidth();
        clusterHeight = map.getClusterHeight();
        return new AStar().planPath(map, start, end);
    }

    @Contract(pure = true)
    private @NotNull @Unmodifiable List<Point> findIntraClusterPath(@NotNull GridMap map, Point currentCluster, Point nextCluster) {
        currentCluster = new Point(currentCluster.getX() * clusterWidth, currentCluster.getY() * clusterHeight);
        nextCluster = new Point(nextCluster.getX() * clusterWidth, nextCluster.getY() * clusterHeight);
        map.setClusterWidth(1);
        map.setClusterHeight(1);
        return new JPS().planPath(map, map.getGridNode(currentCluster), map.getGridNode(nextCluster));
    }

    @Contract(pure = true)
    private @Nullable GridNode getClusterEntrance(@NotNull GridMap map, int clusterId) {
        return null;
    }
}
