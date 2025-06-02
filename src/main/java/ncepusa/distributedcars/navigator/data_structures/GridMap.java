package ncepusa.distributedcars.navigator.data_structures;

import ncepusa.distributedcars.navigator.algorithm.PrimesUtil;
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

    public GridMap(byte[] visitedMap, byte[] obstaclesMap, @NotNull Point mapSize, @NotNull Point start) {
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
                if (index / 8 < visitedMap.length && ((visitedMap[index / 8] >> (7 - index % 8)) & 1) != 0) {
                    gridNode.setVisited(true);
                }
                if (index / 8 < obstaclesMap.length && ((obstaclesMap[index / 8] >> (7 - index % 8)) & 1) != 0) {
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
                if(dx != 0 && dy!= 0) {
                    GridNode horizontalNeighbor = getGridNode(nx, y);
                    GridNode verticalNeighbor = getGridNode(x, ny);
                    if(null != horizontalNeighbor && !horizontalNeighbor.isObstacle() &&
                            null!= verticalNeighbor && !verticalNeighbor.isObstacle()){
                        neighbors.add(getGridNode(nx,ny));
                    }
                }
                else if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    neighbors.add(getGridNode(nx,ny));
                }
            }
        }
        return neighbors;
    }

    /**
     * 选择具有较多未探索点的区域作为终点。
     * 终点的选择基于每个节点自身及其周围未探索邻居的数量。
     */
    public void electEndpoint(int carNumbers, int carid) {
        GridNode tmpNode;
        int maxUnexploredCount = -1;
        Random random = new Random();
        int heightCount = carNumbers / 2;
        int heightLength = 0;
        if((carNumbers & 1) == 1) heightLength = (int) ((double)height / (0.5 + (double)heightCount));
        else heightLength = height / heightCount;
        int startx = (((carid & 1) == 0) ? width / 2 : 0);
        int endx = (startx == 0 ? width / 2 : width);
        int starty = ((carid - 1) / 2) * heightLength;
        int endy = starty + heightLength;
        if(carNumbers == 1) {
            startx = 0;
            endx = width;
            starty = 0;
            endy = height;
        }
        else if((carNumbers & 1) == 1 && carid == carNumbers) {
            startx = 0;
            endx = width;
            starty = heightLength * heightCount - 1;
            endy = height;
        }
        st : for(int j = (PrimesUtil.isPrime(carid) ? starty : (endy - 1));
                 j >= starty && j < endy;
                 j += (PrimesUtil.isPrime(carid) ? 1 : -1)) {
            for (int i = (start.getX() < (endx + startx) / 2 ? (endx - 1) : startx);
                 i < endx && i >= startx;
                 i += (start.getX() < (endx + startx) / 2 ? -1 : 1)) {
                if (i == start.getX() && j == start.getY()) continue;
                tmpNode = grid.get(j).get(i);
                if (!tmpNode.isObstacle() && tmpNode.isArrived()) {
                    int exploredCount = countUnexploredNeighbors(tmpNode);
                    if (exploredCount > maxUnexploredCount) {
                        maxUnexploredCount = exploredCount;
                        this.end = new Point(j, i);
                        if (maxUnexploredCount == 9) break st;
                    }
                }
            }
        }
        logger.info("maxUnexploredCount: {}", maxUnexploredCount);
        //当前小车已探索完自己的区域,开始协助其他小车探索其他区域
        if (null == this.end) {
            maxUnexploredCount = -1;
            st : for(int j = 0; j < height; j++){
                for(int i = 0; i < width; i++){
                    if (i == start.getX() && j == start.getY()) continue;
                    tmpNode = grid.get(j).get(i);
                    if(!tmpNode.isObstacle() && tmpNode.isArrived()) {
                        int exploredCount = countUnexploredNeighbors(tmpNode);
                        if (exploredCount > maxUnexploredCount) {
                            maxUnexploredCount = exploredCount;
                            this.end = new Point(j, i);
                            if (maxUnexploredCount == 9 && random.nextDouble() <= 0.9) break st;
                        }
                    }
                }
            }
            logger.info("maxUnexploredCount: {}", maxUnexploredCount);
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
    
    /**
     * 判断两个节点是否为邻居
     * @param u 第一个节点
     * @param v 第二个节点
     * @return 如果是邻居返回true，否则返回false
     */
    public boolean isNeighbor(@NotNull GridNode u, @NotNull GridNode v) {
        int dx = Math.abs(u.getX() - v.getX());
        int dy = Math.abs(u.getY() - v.getY());
        return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
    }
    
    /**
     * 获取地图中所有节点的列表
     * @return 所有节点的列表
     */
    public List<GridNode> getAllNodes() {
        List<GridNode> allNodes = new ArrayList<>();
        for (List<GridNode> row : grid) {
            allNodes.addAll(row);
        }
        return allNodes;
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
}