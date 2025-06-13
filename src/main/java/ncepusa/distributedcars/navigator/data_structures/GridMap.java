package ncepusa.distributedcars.navigator.data_structures;

import ncepusa.distributedcars.navigator.algorithm.PrimesUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


/**
 * <p>表示用于路径规划和导航的基于网格的地图结构。</p>
 * <p>获取邻居等任务应该由地图本身完成，而不是由路径规划算法完成，要考虑 更换数据结构时算法代码不用更改</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class GridMap {
    private List<List<GridNode>> grid;
    private int width;
    private int height;
    private Point start;
    private Point end;
    /**
     * 簇的长宽
     */
    private int clusterWidth,clusterHeight;
    private static final Logger logger = LoggerFactory.getLogger(GridMap.class);


    @Contract(pure = true)
    public GridMap(@NotNull List<List<GridNode>> grid) {
        this.grid = grid;
        this.width = grid.get(0).size();
        this.height = grid.size();
        this.clusterWidth = 1;
        this.clusterHeight = 1;
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
        /*
          cluster的长宽影响了A*的效率，如果是普通的A*,不分层的情况下，cluster的长宽均为1
          在HPA*中，需要设置cluster的长宽为合适的值
        */
        this.clusterWidth = 1;
        this.clusterHeight = 1;
    }

    /**
     * 计算两个节点之间的曼哈顿距离
     *
     * @param a 起始节点
     * @param b 目标节点
     * @return 曼哈顿距离
     * @since 2025-05-21
     */
    public double ManhattanDistance(@NotNull GridNode a, @NotNull GridNode b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }
    public double ManhattanDistance(@NotNull Point a, @NotNull Point b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    /**
     * 计算两个节点之间的欧几里得距离
     *
     * @param a 起始节点
     * @param b 目标节点
     * @return 欧几里得距离
     */
    public double diagonalDistance(GridNode a, GridNode b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dy = Math.abs(a.getY() - b.getY());
        return dx + dy + (Math.sqrt(2) - 2) * Math.min(dx, dy);
    }
    public double diagonalDistance(Point a, Point b) {
        int dx = Math.abs((int) a.getX() - (int) b.getX());
        int dy = Math.abs((int) a.getY() - (int) b.getY());
        return dx + dy + (Math.sqrt(2) - 2) * Math.min(dx, dy);
    }


    /**
     * <p>寻找簇的宽度和高度</p>
     * <p>尽量保证高层长宽和低层长宽满足<b>分形关系</b></p>
     * <p>尽量保证所有簇的容量相同，如果不能保证，尽量保证容量差值不大</p>
     */
    public void setClusterSize() {
        int tmpWidth = width;
        while(PrimesUtil.isPrime(tmpWidth)) tmpWidth++; //最多循环2次，99.99999%情况下只循环1次
        int st = (int) Math.sqrt(tmpWidth);
        for(int i = st; i > 0; i--) {
            if(tmpWidth % i == 0) {
                clusterWidth = i;
                break;
            }
        }
        int tmphHeight = height;
        while(PrimesUtil.isPrime(tmphHeight)) tmphHeight++;
        st = (int) Math.sqrt(tmphHeight);
        for(int i = st; i > 0; i--) {
            if(tmphHeight % i == 0) {
                clusterHeight = i;
                break;
            }
        }
    }

    /**
     * <p>获取给定节点所在簇与其所有邻居簇的 入口和出口<b>点对</b></p>
     * @param nodeInCluster 指定的簇中的任意一个结点
     * @return 入口和出口点对。
     */
    public @NotNull List<Pair<Point,Point>> getClusterNeighbors(@NotNull GridNode nodeInCluster) {
        List<Pair<Point,Point>> neighborPointsPairs = new ArrayList<>();
        //簇的长宽均为1时，直接返回该节点的所有邻居
        if(clusterWidth == 1 && clusterHeight == 1) return getNodeNeighbors(nodeInCluster);
        int clusterX = nodeInCluster.getX() / clusterWidth;
        int clusterY = nodeInCluster.getY() / clusterHeight;

        int startX = clusterX * clusterWidth;
        int startY = clusterY * clusterHeight;
        int endX = Math.min(startX + clusterWidth, width);
        int endY = Math.min(startY + clusterHeight, height);
        int count = clusterWidth * 2 + clusterHeight * 2 - 4;

        //从左上角开始遍历边界，查找入口和出口点对
        int sti = startX;
        int stj = startY;
        int dx = 1;
        int dy = 0;
        for(int k = 0; k < count; k++){
            GridNode currentNode = getGridNode(sti, stj);
            if(null != currentNode && !currentNode.isObstacle()) {
                //当前节点为东/西/南/北边界上的点时，
                //判断其东/西/南/北方向的邻居节点
                GridNode neighborNode = getGridNode(sti + (dx == 0 ? dy : 0), stj + (dy == 0 ? -dx : 0));
                if(null != neighborNode && !neighborNode.isObstacle()) {
                    neighborPointsPairs.add(Pair.of(new Point(sti, stj), new Point(sti, stj + (dy == 0 ? -dx : 0))));
                }
                //判断其东北/西北/西南/西北方向的邻居节点
                neighborNode = getGridNode(sti + (dx == 0 ? dy : -1), stj + (dy == 0 ? -dx : -1));
                if(null != neighborNode && !neighborNode.isObstacle()) {
                    neighborPointsPairs.add(Pair.of(new Point(sti, stj), new Point(sti + (dx == 0 ? dy : -1), stj + (dy == 0 ? -dx : -1))));
                }
                //判断其东南/西南/东南/东北方向的邻居节点
                neighborNode = getGridNode(sti + (dx == 0 ? dy : 1), stj + (dy == 0 ? -dx : 1));
                if(null != neighborNode && !neighborNode.isObstacle()) {
                    neighborPointsPairs.add(Pair.of(new Point(sti, stj), new Point(sti + (dx == 0 ? dy : 1), stj + (dy == 0 ? -dx : 1))));
                }
            }
            //顺时针
            if(sti + dx >= endX){
                dx = 0;
                dy = 1;
            }
            else if(sti + dx < startX){
                dx = 0;
                dy = -1;
            }
            else if(stj + dy >= endY){
                dx = -1;
                dy = 0;
            }
            sti += dx;
            stj += dy;
        }

        return neighborPointsPairs;
    }

    /**
     * <p>预计算生成所有簇的入口和出口点对，保证性能</p>
     * @return 所有簇的入口和出口点对。
     */
    public List<List<Pair<Point, Point>>> generateClustersPointsPairs() {
        List<List<Pair<Point, Point>>> clustersPointsPairs = new ArrayList<>();
        for(int i = 0; i < width; i += clusterWidth) {
            for(int j = 0; j < height; j += clusterHeight) {
                clustersPointsPairs.add(getClusterNeighbors(Objects.requireNonNull(getGridNode(i, j))));
            }
        }
        return clustersPointsPairs;
    }

    /**
     * 获取给定节点的邻居节点
     *
     * @param gridNode 目标节点
     * @return 邻居节点列表：.first:入口；.second:出口
     */
    public List<Pair<Point,Point>> getNodeNeighbors(@NotNull GridNode gridNode) {
        List<Pair<Point,Point>> neighbors = new ArrayList<>();
        int x = gridNode.getX();
        int y = gridNode.getY();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (dx != 0 && dy != 0) {
                    GridNode horizontalNeighbor = getGridNode(nx, y);
                    GridNode verticalNeighbor = getGridNode(x, ny);
                    if (null != horizontalNeighbor && !horizontalNeighbor.isObstacle() &&
                            null != verticalNeighbor && !verticalNeighbor.isObstacle()) {
                        neighbors.add(Pair.of(new Point(x, y), new Point(nx, ny)));
                    }
                } else if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    GridNode neighbor = getGridNode(nx, ny);
                    if (null!= neighbor &&!neighbor.isObstacle()) {
                        neighbors.add(Pair.of(new Point(x, y), new Point(nx, ny)));
                    }
                }
            }
        }
        return neighbors;
    }

    public void setG(){
        for(int j = 0; j < height; j++) {
            for(int i = 0; i < width; i++) {
                grid.get(j).get(i).setG(2147000000);
            }
        }
    }
    /**
     * <p>根据小车数量划分区域：</p>1021121
     * <ul>
     *   <li>奇数小车数量时，最后一个区域覆盖剩余空间，<b>尽量</b>保证所有小车探索区域的大小是一致的</li>
     *   <li>偶数小车数量时，均匀分配区域</li>
     * </ul>
     * <p>根据当前的carid分配探索区域：</p>
     * <ul>
     *   <li>ID为奇数的小车负责左侧区域</li>
     * <p>分配区域后，每个小车在自己的区域内选择具有较多未探索点的点作为终点。</p>
     * <p>终点的选择基于每个节点自身及其周围未探索邻居的数量。</p>
     * <p>小车探索完自己的区域后，会协助其他小车探索区域</p>
     *
     * @param carNumbers 小车数量
     * @param carid 当前小车的id
     */
    public void electEndpoint(int carNumbers, int carid) {
        GridNode tmpNode;
        Point tmpEnd = null;
        int maxUnexploredCount = 0;
        Random random = new Random();

        int heightCount = carNumbers / 2;
        int heightLength = 0;
        if ((carNumbers & 1) == 1) heightLength = (int) ((double) height / (0.5 + (double) heightCount));
        else heightLength = height / heightCount;

        int startx = (((carid & 1) == 0) ? width / 2 : 0);
        int endx = (startx == 0 ? width / 2 : width);
        int starty = ((carid - 1) / 2) * heightLength;
        int endy = starty + heightLength;

        if (carNumbers == 1) {
            startx = 0;
            endx = width;
            starty = 0;
            endy = height;
        } else if ((carNumbers & 1) == 1 && carid == carNumbers) {
            startx = 0;
            endx = width;
            starty = heightLength * heightCount - 1;
            endy = height;
        }

        boolean isUp = PrimesUtil.isPrime(carid + random.nextInt(4));
        st:
        for (int j = isUp ? starty : (endy - 1);
             j >= starty && j < endy;
             j += isUp ? 1 : -1) {
            for (int i = (start.getX() < (endx + startx) / 2 ? startx : (endx - 1));
                 i < endx && i >= startx;
                 i += (start.getX() < (endx + startx) / 2 ? 1 : -1)) {
                if (i == start.getX() && j == start.getY()) continue;
                tmpNode = grid.get(j).get(i);
                if (!tmpNode.isObstacle() && tmpNode.isArrived()) {
                    int exploredCount = countUnexploredNeighbors(tmpNode);
                    if (exploredCount > maxUnexploredCount) {
                        maxUnexploredCount = exploredCount;
                        tmpEnd = new Point(i, j);
                        if(maxUnexploredCount == 9 || random.nextDouble() <= 0.7) break st;
                    }
                }
            }
        }
        if (null != tmpEnd) {
            this.end = tmpEnd;
            return;
        }

        //当前小车已探索完自己的区域,开始协助其他小车探索其他区域
        maxUnexploredCount = 0;
        st:
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (i == start.getX() && j == start.getY()) continue;
                if (i >= startx && i < endx && j >= starty && j < endy) continue;
                tmpNode = grid.get(j).get(i);
                if (!tmpNode.isObstacle() && tmpNode.isArrived()) {
                    int exploredCount = countUnexploredNeighbors(tmpNode);
                    if (exploredCount > maxUnexploredCount) {
                        maxUnexploredCount = exploredCount;
                        tmpEnd = new Point(i, j);
                        if (maxUnexploredCount == 9 || random.nextDouble() <= 0.8) break st;
                    }
                }
            }
        }
        if (null != tmpEnd) this.end = tmpEnd;
    }

    /**
     * 计算给定节点的未探索邻居数量。
     *
     * @param node 要评估的节点
     * @return 未探索邻居的数量
     */
    private int countUnexploredNeighbors(@NotNull GridNode node) {
        int count = node.isVisited() ? 0 : 1;
        for(int dx = -1; dx <= 1; dx++) {
            for(int dy = -1; dy <= 1; dy++) {
                if(dx == 0 && dy == 0) continue;
                int nx = node.getX() + dx;
                int ny = node.getY() + dy;
                if(nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                GridNode neighbor = getGridNode(nx, ny);
                if (neighbor != null && !neighbor.isVisited()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * <p>检查在给定方向上是否存在“强制邻居”。</p>
     * <p>不需要考虑父节点和当前节点的相对方向，JPS算法的实现保证了父节点和当前节点的相对方向和direction指示的方向一定是一致的</p>
     *
     * @param node   当前节点
     * @param direction 方向节点（表示方向）
     * @return 如果存在强制邻居则返回 true，否则返回 false
     */
    public boolean hasForcedNeighbor(@NotNull GridNode node, @NotNull Point direction) {
        int dx = (int) direction.getX();
        int dy = (int) direction.getY();

        if (dx != 0 && dy != 0) { // 斜向移动
            GridNode horizontal = getGridNode(node.getX() + dx, node.getY());
            GridNode vertical = getGridNode(node.getX(), node.getY() + dy);
            return (horizontal != null && horizontal.isObstacle()) ||
                    (vertical != null && vertical.isObstacle());
        } else if (dx != 0) { // 水平移动
            GridNode upper = getGridNode(node.getX() + dx, node.getY() + 1);
            GridNode lower = getGridNode(node.getX() + dx, node.getY() - 1);
            return (upper != null && upper.isObstacle()) ||
                    (lower != null && lower.isObstacle());
        } else if (dy != 0) { // 垂直移动
            GridNode left = getGridNode(node.getX() - 1, node.getY() + dy);
            GridNode right = getGridNode(node.getX() + 1, node.getY() + dy);
            return (left != null && left.isObstacle()) ||
                    (right != null && right.isObstacle());
        }
        return false;
    }

    /**
     * 获取当前节点在指定方向上的强制邻居方向。
     * @param current 当前节点
     * @param direction 方向节点（表示方向）
     * @return 强制邻居方向列表，如果没有强制邻居则返回空列表
     */
    @NotNull
    public List<Point> getForcedDirs(GridNode current, @NotNull Point direction) {
        int dx = (int) direction.getX();
        int dy = (int) direction.getY();
        List<Point> forcedDirs = new ArrayList<>();

        if (dx != 0 && dy != 0) { // 斜向移动
            // 检查横向和纵向两个方向是否有障碍
            if (isObstacleAt(current.getX() + dx, current.getY())) {
                forcedDirs.add(new Point(dx, 0));
            }
            if (isObstacleAt(current.getX(), current.getY() + dy)) {
                forcedDirs.add(new Point(0, dy));
            }
        } else if (dx != 0) { // 水平移动
            if (isObstacleAt(current.getX() + dx, current.getY() + 1)) {
                forcedDirs.add(new Point(0, 1));
            }
            if (isObstacleAt(current.getX() + dx, current.getY() - 1)) {
                forcedDirs.add(new Point(0, -1));
            }
        } else if (dy != 0) { // 垂直移动
            if (isObstacleAt(current.getX() + 1, current.getY() + dy)) {
                forcedDirs.add(new Point(1, 0));
            }
            if (isObstacleAt(current.getX() - 1, current.getY() + dy)) {
                forcedDirs.add(new Point(-1, 0));
            }
        }

        return forcedDirs;
    }

    public boolean isObstacleAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return true;
        }
        return grid.get(y).get(x).isObstacle();
    }
    /**
     * 获取当前节点在指定方向上的下一个节点。
     *
     * @param direction 方向节点（表示方向）
     * @param current   当前节点
     * @return 下一个节点，如果超出地图范围则返回 null
     */
    public GridNode getNextInDirection(@NotNull Point direction, @NotNull GridNode current) {
        int nextX = (int) (current.getX() + direction.getX());
        int nextY = (int) (current.getY() + direction.getY());

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
    public GridNode getGridNode(@NotNull Point point) {
        return getGridNode((int) point.getX(), (int) point.getY());
    }

    /**
     * 判断两个节点是否为邻居,同时考虑空间和可达性
     *
     * @param u 第一个节点
     * @param v 第二个节点
     * @return 如果是邻居返回true，否则返回false
     */
    public boolean isNodeNeighbor(@NotNull GridNode u, @NotNull GridNode v) {
        int dx = Math.abs(u.getX() - v.getX());
        int dy = Math.abs(u.getY() - v.getY());
        return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0) && !u.isObstacle() &&!v.isObstacle();
    }

    /**
     * 判断两个簇是否是邻居，同时考虑空间和可达性
     * @param cluster1 第一个簇的高层坐标
     * @param cluster2 第二个簇的高层坐标
     * @return 如果是邻居返回true，否则返回false
     * @deprecated 未完成，不使用
     */
    @Deprecated
    public boolean isClusterNeighbor(@NotNull Point cluster1, @NotNull Point cluster2) {
        int dx = (int)cluster2.getX() - (int)cluster1.getX();
        int dy = (int)cluster2.getY() - (int)cluster1.getY();
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || (dx == 0 && dy == 0)) return false;
        if (dx != 0 && dy != 0) {

        }
        else {

        }
        return false;
    }

    /**
     * 获取地图中所有节点
     *
     * @return 所有节点的一维列表
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
        return grid.get((int) start.getY()).get((int) start.getX());
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public GridNode getEnd() {
        return grid.get((int) end.getY()).get((int) end.getX());
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public int getClusterWidth() {
        return clusterWidth;
    }

    public void setClusterWidth(int clusterWidth) {
        this.clusterWidth = clusterWidth;
    }

    public int getClusterHeight() {
        return clusterHeight;
    }

    public void setClusterHeight(int clusterHeight) {
        this.clusterHeight = clusterHeight;
    }
}