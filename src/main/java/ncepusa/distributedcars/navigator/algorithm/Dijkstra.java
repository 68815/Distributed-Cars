package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.geo.Point;
import org.springframework.data.util.Pair;

import java.util.*;

/**
 * <p>Dijkstra算法</p>
 *
 * @author 0109
 * @since 2025-06-02
 */
public class Dijkstra implements PathPlanningStrategy {
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        if (start.equals(end)) {
            return List.of();
        }

        map.setClusterWidth(1);
        map.setClusterHeight(1);

        PriorityQueue<GridNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(GridNode::getG));
        Set<GridNode> closedSet = new HashSet<>();

        start.setG(0);
        openSet.add(start);

        while (!openSet.isEmpty()) {
            GridNode current = openSet.poll();
            if (current.equals(end)) {
                return reconstructPath(current);
            }
            closedSet.add(current);
            List<Pair<Point, Point>> neighbors = map.getClusterNeighbors(current);
            for (int i = 0; i < neighbors.size(); i++) {
                GridNode neighbor = map.getGridNode(neighbors.get(i).getSecond());
                if (neighbor.isObstacle() || closedSet.contains(neighbor)) {
                    continue;
                }
                double tentativeGScore = current.getG() +
                    (neighbor.getX() != current.getX() && neighbor.getY() != current.getY() ? Math.sqrt(2) : 1);
                if (tentativeGScore < neighbor.getG()) {
                    neighbor.setParent(current);
                    neighbor.setG(tentativeGScore);
                    openSet.add(neighbor);
                }
            }
        }

        return Collections.emptyList();
    }

    private @NotNull List<GridNode> reconstructPath(GridNode current) {
        LinkedList<GridNode> path = new LinkedList<>();
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        path.removeLast();
        Collections.reverse(path);
        return path;
    }
}