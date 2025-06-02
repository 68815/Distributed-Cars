
package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Floyd implements PathPlanningStrategy {
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        if (start.equals(end)) {
            return List.of(start);
        }

        // 初始化距离矩阵和路径矩阵
        Map<GridNode, Map<GridNode, Double>> dist = new HashMap<>();
        Map<GridNode, Map<GridNode, GridNode>> next = new HashMap<>();
        
        // 获取所有节点
        List<GridNode> allNodes = map.getAllNodes();
        
        // 初始化距离和路径
        for (GridNode u : allNodes) {
            dist.put(u, new HashMap<>());
            next.put(u, new HashMap<>());
            
            for (GridNode v : allNodes) {
                if (u.equals(v)) {
                    dist.get(u).put(v, 0.0);
                } else if (map.isNeighbor(u, v) && !v.isObstacle()) {
                    double cost = (u.getX() != v.getX() && u.getY() != v.getY()) ? Math.sqrt(2) : 1;
                    dist.get(u).put(v, cost);
                    next.get(u).put(v, v);
                } else {
                    dist.get(u).put(v, Double.POSITIVE_INFINITY);
                }
            }
        }
        
        // Floyd算法核心
        for (GridNode k : allNodes) {
            for (GridNode i : allNodes) {
                for (GridNode j : allNodes) {
                    if (dist.get(i).get(k) + dist.get(k).get(j) < dist.get(i).get(j)) {
                        dist.get(i).put(j, dist.get(i).get(k) + dist.get(k).get(j));
                        next.get(i).put(j, next.get(i).get(k));
                    }
                }
            }
        }
        
        // 如果无法到达终点
        if (next.get(start).get(end) == null) {
            return Collections.emptyList();
        }
        
        // 重建路径
        List<GridNode> path = new ArrayList<>();
        GridNode current = start;
        path.add(current);

        int maxSteps = allNodes.size() * 2;
        int steps = 0;

        while (!current.equals(end)) {
            GridNode nextNode = next.get(current).get(end);
            if (nextNode == null || path.contains(nextNode)) {
                return Collections.emptyList(); // 防止死循环或无效路径
            }
            path.add(nextNode);
            current = nextNode;

            if (++steps > maxSteps) {
                return Collections.emptyList(); // 超过最大步数，可能是环路
            }
        }
        return path;
    }
}