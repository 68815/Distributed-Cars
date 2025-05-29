package ncepusa.distributedcars.navigator.algorithm;/*
package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

*/
/**
 * <p>JPS(jump point search)算法</p>
 *
 * @author 0109
 * @since 2025-05-21
 *//*

public class JPS implements PathPlanningStrategy {
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        if (start.equals(end)) {
            return List.of(start);
        }
        PriorityQueue<GridNode> openList = new PriorityQueue<>(Comparator.comparingDouble(node -> node.getG() + node.getH()));
        Map<GridNode, GridNode> cameFrom = new HashMap<>();
        Map<GridNode, Double> costSoFar = new HashMap<>();
        start.setG(0);
        start.setH(map.ManhattanDistance(start, end));
        openList.add(start);
        costSoFar.put(start, 0.0);
        while (!openList.isEmpty()) {
            GridNode current = openList.poll();
            if (current.equals(end)) {
                return reconstructPath(cameFrom, start, end);
            }

            for (GridNode jumpPoint : identifyJumpPoints(map, current, end)) {
                double newCost = costSoFar.get(current) + map.ManhattanDistance(current, jumpPoint);
                if (!costSoFar.containsKey(jumpPoint) || newCost < costSoFar.get(jumpPoint)) {
                    costSoFar.put(jumpPoint, newCost);
                    jumpPoint.setG(newCost);
                    jumpPoint.setH(map.ManhattanDistance(jumpPoint, end));
                    cameFrom.put(jumpPoint, current);
                    openList.add(jumpPoint);
                }
            }
        }

        return Collections.emptyList();
    }

        private @NotNull List<GridNode> identifyJumpPoints (@NotNull GridMap map, GridNode current, GridNode goal){
            List<GridNode> jumpPoints = new ArrayList<>();
            for (GridNode neighbor : map.getNeighbors(current)) {
                GridNode jumpPoint = jump(map, current, neighbor, goal);
                if (jumpPoint != null) {
                    jumpPoints.add(jumpPoint);
                }
            }
            return jumpPoints;
        }

        private GridNode jump (GridMap map, GridNode current, GridNode direction, GridNode goal){
            if (direction == null || direction.isObstacle()) {
                return null;
            }

            if (direction.equals(goal)) {
                return direction;
            }

            if (map.hasForcedNeighbor(current, direction)) {
                return direction;
            }
        
            return jump(map, direction, map.getNextInDirection(direction, current), goal);
        }
        
        private List<GridNode> reconstructPath(Map<GridNode, GridNode> cameFrom, GridNode start, GridNode end) {
            List<GridNode> path = new ArrayList<>();
            GridNode current = end;
        
            while (!current.equals(start)) {
                path.add(current);
                current = cameFrom.get(current);
            }
        
            path.add(start);
            Collections.reverse(path);
            return path;
        }
    }
}*/
