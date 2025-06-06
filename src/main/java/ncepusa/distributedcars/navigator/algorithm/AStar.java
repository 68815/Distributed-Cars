package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.geo.Point;
import org.springframework.data.util.Pair;

import java.util.*;

/**
 * <p>A*算法</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class AStar implements PathPlanningStrategy {
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {

        PriorityQueue<GridNode> openList = new PriorityQueue<GridNode>();
        Set<GridNode> closedList = new HashSet<GridNode>();
        start.setG(0);
        start.setH(map.ManhattanDistance(start, end));
        start.setParent(null);
        openList.add(start);
        while (!openList.isEmpty()) {
            GridNode current = openList.poll();
            if (current.equals(end)) {
                return reconstructPath(current);
            }
            closedList.add(current);
            List<Pair<Point, Point>> neighbors = map.getClusterNeighbors(current);
            for (int i = 0; i < neighbors.size(); i++) {
                GridNode neighbor = map.getGridNode(neighbors.get(i).getSecond());
                if (neighbor.isObstacle() || closedList.contains(neighbor)) continue;
                double tentativeGCost = current.getG()
                        + (neighbor.getX() != current.getX() && neighbor.getY() != current.getY() ? Math.sqrt(2) : 1)
                        + (neighbor.isVisited()? 2 : 0);
                if (tentativeGCost < neighbor.getG()) {
                    neighbor.setG(tentativeGCost);
                    neighbor.setParent(current);
                    neighbor.setH(map.ManhattanDistance(neighbor, end));
                    openList.add(neighbor);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 从目标节点开始，沿着父节点指针回溯，直到到达起点，形成路径。
     * 最后，将路径反转，使其从起点到终点。
     * @param gridNode 目标节点
     * @return 从起点到终点的路径
     */
    private @NotNull List<GridNode> reconstructPath(GridNode gridNode) {
        List<GridNode> path = new ArrayList<>();
        while (gridNode != null) {
            path.add(gridNode);
            gridNode = gridNode.getParent();
        }
        path.remove(path.size() - 1);
        Collections.reverse(path);
        return path;
    }

    public void setClusterSize(@NotNull GridMap map, int width, int height) {
        map.setClusterWidth(width);
        map.setClusterHeight(height);
    }
}