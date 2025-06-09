package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.geo.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>算法规划类</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class PathPlanning {
    private final List<PathPlanningStrategy> pathPlannings;
    private int currentIndex = 0;

    public PathPlanning() {
        pathPlannings = new ArrayList<>();
        pathPlannings.add(new JPS());
        pathPlannings.add(new AStar());
        pathPlannings.add(new Dijkstra());
        pathPlannings.add(new BFS());
        pathPlannings.add(new Floyd());
    }

    public List<Point> planPath(@NotNull GridMap map, GridNode start, GridNode end) {
        return pathPlannings.get(currentIndex).planPath(map, start, end);
    }

    public void setStrategy(int index) {
        if (index >= 0 && index < pathPlannings.size()) {
            currentIndex = index;
        }
    }
}
