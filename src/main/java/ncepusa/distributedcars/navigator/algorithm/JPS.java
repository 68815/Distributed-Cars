package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;

import java.util.*;

/**
 * <p>JPS(jump point search)算法</p>
 *
 * @author 0109
 * @since 2025-06-01
 */
public class JPS implements PathPlanningStrategy {
    private static final Logger logger = LoggerFactory.getLogger(JPS.class);
    private final int[][] DIRECTIONS = {
            {0, 1}, {1, 0}, {0, -1}, {-1, 0},
            {1, 1}, {1, -1}, {-1, -1}, {-1, 1}
    };
    @Override
    public List<GridNode> planPath(@NotNull GridMap map, @NotNull GridNode start, @NotNull GridNode end) {
        if (start.equals(end)) {
            return List.of(start);
        }
        PriorityQueue<GridNode> openList = new PriorityQueue<>();
        Set<GridNode> closedSet = new HashSet<>();

        start.setG(0);
        start.setH(map.ManhattanDistance(start, end));
        openList.add(start);

        while (!openList.isEmpty()) {
            GridNode current = openList.poll();
            if (current.equals(end)) {
                return reconstructFullPath(current,map);
            }
            closedSet.add(current);
            List<GridNode> jumpPoints = identifyJumpPoints(map, current, end);

            if(jumpPoints.isEmpty()) { // 没有跳点，退化为A*，直接遍历所有邻居
                jumpPoints = map.getNeighbors(current);
                for (GridNode neighbor : jumpPoints) {
                    if (neighbor.isObstacle() || closedSet.contains(neighbor)) continue;
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
            else { // 有跳点，直接遍历跳点
                for (GridNode jumpPoint : jumpPoints) {
                    if (closedSet.contains(jumpPoint)) continue;
                    double newCost = current.getG() +
                            Math.min(Math.abs(current.getX() - jumpPoint.getX()), Math.abs(current.getY() - jumpPoint.getY())) * Math.sqrt(2) +
                            Math.abs(Math.abs(current.getX() - jumpPoint.getX())) - Math.abs(current.getY() - jumpPoint.getY()) +
                            (jumpPoint.isVisited() ? 2 : 0);
                    if (newCost < jumpPoint.getG()) {
                        jumpPoint.setG(newCost);
                        jumpPoint.setH(map.ManhattanDistance(jumpPoint, end));
                        jumpPoint.setParent(current);
                        openList.add(jumpPoint);
                    }
                }
            }
        }
        return Collections.emptyList();
    }
    /**
     * 在网格地图中识别跳点。
     * 该方法遍历当前节点的邻居，并根据朝向目标的方向，
     * 判断它们中的任何一个是否符合跳点的条件。
     *
     * @param map   表示环境的网格地图，提供对邻居节点的访问
     * @param current 当前节点，从该节点识别跳点
     * @param goal  搜索目标节点
     * @return 作为JPS算法一部分识别出的跳点列表，如果没有找到跳点，则可能为空
     */
    private @NotNull List<GridNode> identifyJumpPoints(@NotNull GridMap map, GridNode current, GridNode goal) {
        List<GridNode> jumpPoints = new ArrayList<>();

        for (int[] dir : DIRECTIONS) {
            int dx = dir[0];
            int dy = dir[1];
            GridNode jumpPoint = jump(map, current, new Point(dx, dy), goal);
            if (null != jumpPoint && !jumpPoints.contains(jumpPoint)) {
                jumpPoints.add(jumpPoint);
            }
        }
        return jumpPoints;
    }

    /**
     * 在网格地图中识别跳点。
     * 该方法沿着给定方向遍历网格，寻找符合跳点条件的节点。
     * 如果某个节点是目标节点、具有强制邻居或满足递归条件，则它被识别为跳点。
     *
     * @param map 表示环境的网格地图，提供对邻居节点的访问
     * @param current 当前节点，从该节点评估跳点
     * @param direction 从当前节点出发的遍历方向；如果为null，方法返回null
     * @param goal 在网格中搜索的目标节点
     * @return 识别出的跳点作为GridNode返回，如果没有找到跳点或方向无效，则返回null
     */
    private @Nullable GridNode jump(@NotNull GridMap map, @NotNull GridNode current, @NotNull Point direction, GridNode goal) {
        int newX = current.getX() + (int) direction.getX();
        int newY = current.getY() + (int) direction.getY();
        GridNode newNode = map.getGridNode(newX, newY);
        if (newNode == null || newNode.isObstacle()) return null;
        if (newNode.equals(goal) || map.hasForcedNeighbor(current, direction)) return newNode;

        return jump(map, newNode, direction, goal);
    }

    /**
     * 从目标节点回溯到起点，构建完整的路径。
     *
     * @param endNode 目标节点
     * @param map 网格地图
     * @return 从起点到目标节点的完整路径
     */
    private @NotNull List<GridNode> reconstructFullPath(GridNode endNode, GridMap map) {
        List<GridNode> jumpPoints = new ArrayList<>();
        GridNode current = endNode;

        while (current != null) {
            jumpPoints.add(current);
            current = current.getParent();
        }
        Collections.reverse(jumpPoints);

        if (jumpPoints.size() <= 1) return jumpPoints;
        List<GridNode> fullPath = new ArrayList<>();

        for (int i = 0; i < jumpPoints.size() - 1; i++) {
            GridNode from = jumpPoints.get(i);
            GridNode to = jumpPoints.get(i + 1);
            List<GridNode> segment = generateFullPathBetween(from, to, map);
            if (segment.isEmpty()) return Collections.emptyList();
            fullPath.addAll(segment.subList(i == 0 ? 1 : 0, segment.size() - 1));
        }
        fullPath.add(jumpPoints.get(jumpPoints.size() - 1));

        return fullPath;
    }

    /**
     * 生成从起点到终点的完整路径。
     * 该方法使用Bresenham算法或简单线性插值来计算两点之间的直线路径。
     * 如果路径无效（例如遇到障碍或越界），则返回空列表。
     *
     * @param from 起点
     * @param to 终点
     * @param map 地图
     * @return 完整路径
     */
    private @NotNull List<GridNode> generateFullPathBetween(@NotNull GridNode from, @NotNull GridNode to, GridMap map) {
        List<GridNode> segment = new ArrayList<>();

        int x0 = from.getX(), y0 = from.getY();
        int x1 = to.getX(), y1 = to.getY();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x1 > x0) ? 1 : ((x1 < x0) ? -1 : 0);
        int sy = (y1 > y0) ? 1 : ((y1 < y0) ? -1 : 0);
        int steps = Math.max(dx, dy);
        for (int i = 0; i <= steps; i++) {
            int x = x0 + sx * i;
            int y = y0 + sy * i;
            GridNode node = map.getGridNode(x, y);
            if (node == null || node.isObstacle()) return Collections.emptyList();
            segment.add(node);
        }
        return segment;
    }
}

