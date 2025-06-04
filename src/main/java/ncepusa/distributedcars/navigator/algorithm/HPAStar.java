package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>HPA*(Hierarchical Pathfinding A*)算法</p>
 *
 * @author 0109
 * @since 2025-06-06
 * @deprecated  该算法目前存在问题，需要进一步改正
 */
@Deprecated
public class HPAStar implements PathPlanningStrategy{
    @Override
    public List<GridNode> planPath(GridMap map, GridNode start, GridNode end) {
        if (start.equals(end)) {
            return List.of();
        }

        // 第一步：将地图划分为多个簇
        Map<GridNode, Integer> clusterMap = divideIntoClusters(map);

        // 第二步：在簇之间寻找高层次路径
        List<Integer> highLevelPath = findHighLevelPath(clusterMap, start, end);

        // 第三步：在簇内重建详细路径
        List<GridNode> detailedPath = new ArrayList<>();
        for (int i = 0; i < highLevelPath.size() - 1; i++) {
            int currentCluster = highLevelPath.get(i);
            int nextCluster = highLevelPath.get(i + 1);
            detailedPath.addAll(findIntraClusterPath(map, clusterMap, currentCluster, nextCluster));
        }

        // 第四步：从最终路径中移除起点
        if (!detailedPath.isEmpty() && detailedPath.get(0).equals(start)) {
            detailedPath.remove(0);
        }

        return detailedPath;
    }

    private @NotNull Map<GridNode, Integer> divideIntoClusters(GridMap map) {
        // 逻辑：将地图划分为多个簇
        // 每个簇分配一个唯一的整数ID
        Map<GridNode, Integer> clusterMap = new HashMap<>();
        int clusterId = 0;
        for (GridNode node : map.getAllNodes()) {
            clusterMap.put(node, clusterId++);
        }
        return clusterMap;
    }

    @Contract("_, _, _ -> new")
    private @NotNull @Unmodifiable List<Integer> findHighLevelPath(Map<GridNode, Integer> clusterMap, GridNode start, GridNode end) {
        // 逻辑：在簇之间寻找高层次路径
        // 可以使用BFS或Dijkstra算法在簇图上完成
        return List.of(clusterMap.get(start), clusterMap.get(end));
    }

    private List<GridNode> findIntraClusterPath(GridMap map, Map<GridNode, Integer> clusterMap, int currentCluster, int nextCluster) {
        // 逻辑：在簇内寻找详细路径
        // 使用BFS或Dijkstra算法进行簇内路径搜索
        return new BFS().planPath(map, getClusterEntrance(clusterMap, currentCluster), getClusterEntrance(clusterMap, nextCluster));
    }

    private GridNode getClusterEntrance(@NotNull Map<GridNode, Integer> clusterMap, int clusterId) {
        // 逻辑：为给定的簇找到入口节点
        return clusterMap.entrySet().stream()
                .filter(entry -> entry.getValue() == clusterId)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到簇"));
    }
}
