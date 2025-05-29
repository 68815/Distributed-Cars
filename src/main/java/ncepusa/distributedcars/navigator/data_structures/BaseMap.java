package ncepusa.distributedcars.navigator.data_structures;

import java.util.List;

/**
 * <p>地图基类</p>
 * <p>应该让地图随时可以更换，而不是永远都是单一的网格地图</p>
 * <p>比如对于不规则形状的多边形障碍物，如果坚持使用网格，那么网格需划分得非常细致才不会出问题，但这样会导致寻路算法的效率非常低，这种情况下需要构造不同的数据结构</p>
 *
 * @author 0109
 * @since 2025-05-21
 * @deprecated 让地图数据结构可以随时更换的想法并不可靠，难以实现。当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public abstract class BaseMap {
    /**
     *
     * @param gridNode 当前节点
     * @return 当前节点的邻居节点
     */
    public abstract List<GridNode> getNeighbors(GridNode gridNode);

    /**
     *
     * @param a 当前节点
     * @param b 目标节点
     * @return 当前节点到目标节点的距离
     */
    public abstract double distanceBetween(GridNode a, GridNode b);
}
