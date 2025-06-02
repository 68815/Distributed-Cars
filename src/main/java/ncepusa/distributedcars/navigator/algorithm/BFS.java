package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * <p>BFS(Breadth-First Search)算法</p>
 *
 * @author 0109
 * @since 2025-06-02
 */
public class BFS implements PathPlanningStrategy {
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        if (start.equals(end)) {
            return List.of();
        }

        Queue<GridNode> queue = new LinkedList<>();
        Set<GridNode> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            GridNode current = queue.poll();

            if (current.equals(end)) {
                return reconstructPath(current);
            }

            for (GridNode neighbor : map.getNeighbors(current)) {
                if (!neighbor.isObstacle() && !visited.contains(neighbor)) {
                    neighbor.setParent(current);
                    visited.add(neighbor);
                    queue.add(neighbor);
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