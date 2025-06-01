package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 四叉树结点，每个结点可以是叶结点，也可以继续分裂
 *
 * @author 0109
 * @deprecated 当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public class QuadTreeNode {
    /**
     * 表示直接位于此 QuadTreeNode 下的子节点列表。
     * 子节点存储在列表结构中，定义了四叉树中的层次关系。每个子节点本身也是 QuadTreeNode 的实例，
     * 从而允许递归遍历和操作树结构。该列表在初始化时为空，或者在构造时填充。
     * 对该列表的修改通过 addChild、deleteChild 和 deleteAllChild 等方法进行控制，
     * 以维护结构的完整性。此字段在引用上是不可变的，但在内容上是可变的，
     * 确保列表本身不能被替换，但其元素可以被修改。
     */
    private final List<QuadTreeNode> children;

    /**
     * 表示该节点区域内存储的碰撞体对象列表。
     * 每个碰撞体对象代表该区域内的一个实体，用于碰撞检测或其他空间查询操作。
     */
    private final List<Object> collisionObjects;

    /**
     * 表示当前结点是否是障碍
     * 该变量属于缓存变量，在计算isObstacle时会被赋值
     */
    private Boolean isObstacle;
    /**
     * 表示与此 QuadTreeNode 关联的矩形边界。
     * 该边界定义了节点在二维空间中覆盖的区域。
     * 它用于确定节点在四叉树结构中的责任区域。
     * 该字段的值通常在节点初始化时设置，并在此后保持不变。
     * 该字段对空间查询和操作（如确定相交或包含关系）至关重要。
     */
    private Rectangle bound;
    
    @Contract(pure = true)
    public QuadTreeNode(List<QuadTreeNode> children) {
        this.children = children;
        this.collisionObjects = new ArrayList<>();
    }
    @Contract(pure = true)
    public QuadTreeNode() {
        children = new ArrayList<>();
        collisionObjects = new ArrayList<>();
    }
    public void addChild(@NotNull QuadTreeNode child) {
        children.add(Objects.requireNonNull(child));
    }

    /**
     * 向当前节点添加一个碰撞体对象。
     * 碰撞体对象将被存储在 `collisionObjects` 列表中。
     *
     * @param collisionObject 要添加的碰撞体对象；不能为空
     */
    public void addCollisionObject(@NotNull Object collisionObject) {
        collisionObjects.add(Objects.requireNonNull(collisionObject));
    }

    /**
     * 获取当前节点存储的所有碰撞体对象。
     *
     * @return 包含所有碰撞体对象的不可修改列表
     */
    public List<Object> getCollisionObjects() {
        return List.copyOf(collisionObjects);
    }

    /**
     * 从当前节点的子节点列表中移除指定的子节点。
     * 在移除子节点之前，该方法会递归删除指定子节点的所有后代节点，
     * 方法是调用其 {@code deleteAllChild} 方法。
     * 然后将该子节点从当前节点维护的子节点列表中移除。
     * 如果指定的子节点不在列表中，则不执行任何操作。
     *
     * @param child 要移除的子节点；不能为空
     */
    public void deleteChild(@NotNull QuadTreeNode child) {
        child.deleteAllChild();
        if(null != children){
            children.remove(child);
        }
    }
    /**
     * 递归删除当前节点的所有子节点。
     * 该方法会遍历当前节点的子节点列表，并对每个子节点调用其 {@code deleteAllChild} 方法，
     * 从而递归地删除所有后代节点。然后，该方法会清空当前节点的子节点列表，
     * 确保所有子节点都被删除。
     * 如果当前节点没有子节点，则不执行任何操作。
     */
    public void deleteAllChild() {
        if(null != children) {
            for (QuadTreeNode child : children) {
                child.deleteAllChild();
            }
            children.clear();
        }
    }
    public List<QuadTreeNode> getChildren() {
        return children;
    }
    @Contract(pure = true)
    public boolean isLeafNode() {
        return null == children || children.isEmpty();
    }

    /**
     * 确定当前节点是否表示一个障碍物。
     * 如果该节点是叶子节点且是障碍物，或者其所有子节点都是障碍物，则该节点被识别为障碍物。
     * 结果会被缓存在 {@code isObstacle} 字段中，以避免重复计算。
     *
     * @return 如果当前节点是障碍物则返回 true，否则返回 false
     */
    @Contract(pure = true)
    public boolean isObstacle() {
        if(null != isObstacle) {
            return isObstacle;
        }
        if (isLeafNode()) {
            return isObstacle = false;
        }
        return isObstacle = children.stream().allMatch(QuadTreeNode::isObstacle);
    }
    @Contract(pure = true)
    public Rectangle getBound() {
        return bound;
    }
    public void setBound(Rectangle bound) {
        this.bound = bound;
    }
}
