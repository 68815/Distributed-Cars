package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * QuadTree 类用于表示四叉树数据结构。
 * 四叉树是一种用于高效组织和查询空间数据的树状数据结构。
 * 它将二维空间划分为四个象限，每个象限可以进一步划分为四个象限，以此类推，形成一个层次结构。
 * 每个节点代表一个矩形区域，该区域可以包含子节点和碰撞对象。
 * 该四叉树属于静态四叉树，不支持动态分裂和合并。
 *
 * @author 0109
 * @since 2025-05-10
 * @deprecated 当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public class QuadTree {
    /**
     * 表示四叉树结构的根节点。
     * 该字段保存定义四叉树层次结构的顶层节点。
     * 根节点是执行空间查询、插入和删除等操作的关键。
     * 它作为遍历整个四叉树结构的入口点。
     * 所有这些都通过关联的 QuadTreeNode 类进行管理。
     */
    private final QuadTreeNode root;
    /**
     * 表示四叉树的最大深度。
     * 该字段定义了四叉树结构的最大层次深度。
     * 深度决定了四叉树的分割级别，即每个节点可以包含的子节点数量。
     * 最大深度限制了树的生长，防止无限分割和内存消耗过大。
     */
    private final int maxDepth;
    @Contract(pure = true)
    public QuadTree(QuadTreeNode root, int maxDepth) {
        this.root = root;
        this.maxDepth = maxDepth;
    }
    /**
     * 根据碰撞对象的坐标判断其所在区域
     * @param x 碰撞对象x坐标
     * @param y 碰撞对象y坐标
     * @return 碰撞对象所在的区域节点
     */
    public QuadTreeNode findRegion(float x, float y) {
        return findRegionRecursive(root, x, y);
    }
    private @NotNull QuadTreeNode findRegionRecursive(@NotNull QuadTreeNode node, float x, float y) {
        if (node.isLeafNode()) {
            return node;
        }
        for (QuadTreeNode child : node.getChildren()) {
            if (child.getBound().contains(x, y)) {
                return findRegionRecursive(child, x, y);
            }
        }
        return node;
    }
}