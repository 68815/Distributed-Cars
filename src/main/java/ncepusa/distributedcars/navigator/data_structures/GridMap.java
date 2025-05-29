package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
        for (int i = 0; i < width; i++) {
            ArrayList<GridNode> arrayList = new ArrayList<>();
            for (int j = 0; j < height; j++) {
                GridNode gridNode = new GridNode(i, j);
                int index = i * height + j;
                gridNode.setG(2147000000);
                gridNode.setParent(null);
                if (index < visitedMap.length() && visitedMap.charAt(index) == '1') {
                    gridNode.setVisited(true);
                }
                if (index < obstaclesMap.length() && obstaclesMap.charAt(index) == '1') {
                    gridNode.setObstacle(true);
                }
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
                    neighbors.add(grid.get(nx).get(ny));
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
    public void electEndpoint() {
        int maxUnexploredCount = -1;
        Point candidatePoint = null;
        Random rand = new Random();
        GridNode tmpNode;
        st:for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if(i == start.getX() && j == start.getY()) continue;
                tmpNode = grid.get(i).get(j);
                if (!tmpNode.isObstacle() && tmpNode.isArrived()) {
                    int unexploredCount = countUnexploredNeighbors(tmpNode);
                    if (unexploredCount > maxUnexploredCount ||
                            (unexploredCount == maxUnexploredCount && maxUnexploredCount == 9 && rand.nextDouble() < 0.9)) {
                        maxUnexploredCount = unexploredCount;
                        candidatePoint = new Point(i, j);
                        if (maxUnexploredCount == 9 && rand.nextDouble() >= 0.9) break st;
                    }
                }
            }
        }

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
    private int countUnexploredNeighbors(GridNode node) {
        int count = node.isVisited() ? 0 : 1;
        List<GridNode> neighbors = getNeighbors(node);
        for (GridNode neighbor : neighbors) {
            if (!neighbor.isVisited() && !neighbor.isObstacle()) {
                count++;
            }
        }
        return count;
    }
}
