package ncepusa.distributedcars.navigator.data_structures;

import ncepusa.distributedcars.navigator.algorithm.PrimesUtil;
import ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * <p>表示用于路径规划和导航的基于网格的地图结构。</p>
 * <p>获取邻居等任务应该由网格本身完成，而不是由路径规划算法完成，要考虑 更换数据结构时算法代码不用更改</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class GridMap{
    private List<List<GridNode>> grid;
    private int width;
    private int height;
    private Point start;
    private Point end;

    private static final Logger logger = LoggerFactory.getLogger(GridMap.class);

    @Contract(pure = true)
    public GridMap(@NotNull List<List<GridNode>> grid) {
        this.grid = grid;
        this.width = grid.get(0).size();
        this.height = grid.size();
    }

    public GridMap(String visitedMap, String obstaclesMap, @NotNull Point mapSize, @NotNull Point start) {
        this.width = (int) mapSize.getX();
        this.height = (int) mapSize.getY();
        this.start = start;
        this.grid = new ArrayList<>();
        for (int j = 0; j < height; j++) {
            ArrayList<GridNode> arrayList = new ArrayList<>();
            for (int i = 0; i < width; i++) {
                GridNode gridNode = new GridNode(i, j);
                int index = j * width + i;
                gridNode.setG(2147000000);
                gridNode.setParent(null);
                if (index < visitedMap.length() && visitedMap.charAt(index) == '1') {
                    gridNode.setVisited(true);
                }
                if (index < obstaclesMap.length() && obstaclesMap.charAt(index) == '1') {
                    gridNode.setObstacle(true);
                }
                gridNode.setArrived(true);
                arrayList.add(gridNode);
            }
            grid.add(arrayList);
        }
    }

    /**
     * 计算两个节点之间的曼哈顿距离
     * @param a 起始节点
     * @param b 目标节点
     * @return 曼哈顿距离
     * @since 2025-05-21
     */
    public double ManhattanDistance(@NotNull GridNode a, @NotNull GridNode b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    /**
     * 获取给定节点的邻居节点
     * @param gridNode 目标节点
     * @return 邻居节点列表
     */
    public List<GridNode> getNeighbors(@NotNull GridNode gridNode) {
        List<GridNode> neighbors = new ArrayList<>();
        int x = gridNode.getX();
        int y = gridNode.getY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    neighbors.add(grid.get(ny).get(nx));
                }
            }
        }
        return neighbors;
    }

    public List<List<GridNode>> getGrid() {
        return grid;
    }

    public void setGrid(List<List<GridNode>> grid) {
        this.grid = grid;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public GridNode getStart() {
        return grid.get((int)start.getX()).get((int)start.getY());
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public GridNode getEnd() {
        return grid.get((int)end.getX()).get((int)end.getY());
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    /**
     * 选择具有较多未探索点的区域作为终点。
     * 终点的选择基于每个节点周围未探索邻居的数量。
     */
    public void electEndpoint(int carNumber, int carid) {
        int maxUnexploredCount = -1;
        Point candidatePoint = null;
        Random rand = new Random();
        GridNode tmpNode;
        //第一层随机：倾向于探索远处的点还是近处的点？
        boolean isFar = rand.nextBoolean();
        //第二层随机：优先选择左右还是上下?
        boolean isLeftRight = rand.nextBoolean();
        //第三层随机：随机一个起始点,优先外围点
        int i = isLeftRight ?
                (isFar ?
                        ((int)start.getX() < width / 2 ?
                                width - 1 : 0) :
                        ((int)start.getX() < width / 2 ?
                                0 : width - 1))
                : rand.nextInt(width);
        int j = isLeftRight ?
                rand.nextInt(height)
                : (isFar?
                ((int)start.getY() < height / 2 ?
                        height - 1 : 0) :
                ((int)start.getY() < height / 2 ?
                        0 : height - 1));
        int count = width * 2 + height * 2 - 4;
        int times = 0;
        //从外围开始，逐渐向中心靠近
        st:while(count > 0)
        {
            int ii = i;
            int jj = j;
            //顺时针
            int directions = 0;//方向
            if(j == times && i != width - 1 - times) directions = 1;//向右
            else if(i == width - 1 - times && j != height - 1 - times) directions = -2;//向下
            else if(j == height - 1 - times && i != times) directions = -1;//向左
            else if(i == times && j != times) directions = 2;//向上
            for(int k = 0; k < count; k++)
            {
                if((directions & 1) == 0) {
                    if(ii + directions < times){
                        directions = 2;
                        k--;
                        continue;
                    }
                    else if(ii + directions > width - 1 - times) {
                        directions = -2;
                        k--;
                        continue;
                    }
                    else ii += directions;
                }
                else {
                    if(jj + directions < times){
                        directions = 1;
                        k--;
                        continue;
                    }
                    else if(jj + directions > height - 1 - times) {
                        directions = -1;
                        k--;
                        continue;
                    }
                    else jj += directions / 2;
                }
                if(ii == start.getX() && jj == start.getY()) continue;
                tmpNode = grid.get(jj).get(ii);
                int unexploredCount = countUnexploredNeighbors(tmpNode);
                if (unexploredCount > maxUnexploredCount ||
                        (unexploredCount == maxUnexploredCount && maxUnexploredCount == 9 && rand.nextDouble() < 0.9) ||
                        (unexploredCount < maxUnexploredCount && rand.nextDouble() < 0.1)) {
                    maxUnexploredCount = unexploredCount;
                    candidatePoint = new Point(jj, ii);
                    if (maxUnexploredCount == 9 && rand.nextDouble() >= 0.9) break st;
                }
            }
            if(i == times) i++;
            else if(i == width - 1 - times) i--;
            if(j == times) j++;
            else if(j == height - 1 - times) j--;
            count -= 8;
            times++;
        }
        logger.info("maxUnexploredCount:{}", maxUnexploredCount);
        if (candidatePoint != null) {
            this.end = candidatePoint;
        }
    }
    /**
     * 计算给定节点的未探索邻居数量。
     *
     * @param node 要评估的节点
     * @return 未探索邻居的数量
     */
    private int countUnexploredNeighbors(@NotNull GridNode node) {
        int count = node.isVisited() ? 0 : 1;
        List<GridNode> neighbors = getNeighbors(node);
        for (GridNode neighbor : neighbors) {
            if (!neighbor.isVisited()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 检查在给定方向上是否存在“强制邻居”。
     *
     * @param current   当前节点
     * @param direction 方向节点（表示方向）
     * @return 如果存在强制邻居则返回 true，否则返回 false
     */
        public boolean hasForcedNeighbor(@NotNull GridNode current, @NotNull Point direction) {
        int dx = (int)direction.getX();
        int dy = (int)direction.getY();

        if (dx != 0 && dy != 0) {
            GridNode horizontalNeighbor = getGridNode(current.getX() + dx, current.getY());
            GridNode verticalNeighbor = getGridNode(current.getX(), current.getY() + dy);

            return (horizontalNeighbor != null && horizontalNeighbor.isObstacle()) ||
                    (verticalNeighbor != null && verticalNeighbor.isObstacle());
        }

        else if (dx != 0) {
            GridNode upperNeighbor = getGridNode(current.getX() + dx, current.getY() + 1);
            GridNode lowerNeighbor = getGridNode(current.getX() + dx, current.getY() - 1);

            return (upperNeighbor != null && upperNeighbor.isObstacle()) ||
                    (lowerNeighbor != null && lowerNeighbor.isObstacle());
        }

        else if (dy != 0) {
            GridNode leftNeighbor = getGridNode(current.getX() - 1, current.getY() + dy);
            GridNode rightNeighbor = getGridNode(current.getX() + 1, current.getY() + dy);

            return (leftNeighbor != null && leftNeighbor.isObstacle()) ||
                    (rightNeighbor != null && rightNeighbor.isObstacle());
        }

        return false;
    }

    /**
     * 获取当前节点在指定方向上的下一个节点。
     *
     * @param direction 方向节点（表示方向）
     * @param current   当前节点
     * @return 下一个节点，如果超出地图范围则返回 null
     */
    public GridNode getNextInDirection(@NotNull Point direction, @NotNull GridNode current) {
        int nextX = (int)(current.getX() + direction.getX());
        int nextY = (int)(current.getY() + direction.getY());

        return getGridNode(nextX, nextY);
    }

    /**
     * 获取网格中指定坐标的节点。
     *
     * @param x X 坐标
     * @param y Y 坐标
     * @return 节点对象，如果超出地图范围则返回 null
     */
    public @Nullable GridNode getGridNode(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid.get(y).get(x);
    }

}