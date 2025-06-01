package ncepusa.distributedcars.navigator.algorithm;


import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;

import java.util.List;

/**
 * <p>D*Lite算法</p>
 * <p>原本用于处理动态环境或未知环境下的路径规划，当小车无法预先知道地图障碍物情况时使用。<\p>
 * <p>该算法是A*算法的改进版本，能够在环境变化时高效地重新规划路径。</p>
 *
 * @author 0109
 * @since 2025-05-21
 * @deprecated 当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public class DStarLite implements PathPlanningStrategy {

    @Override
    public List<GridNode> planPath(GridMap map, GridNode start, GridNode end){
        return null;
    }
}
