package ncepusa.distributedcars.navigator.data_structures;

import ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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
    public void electEndpoint(int carNumbers, int carid) {
        Point candidatePoint = null;
        GridNode tmpNode;
        int heightCount = (carNumbers + 1) / 2;
        int heightLength = height / heightCount;
        if((carNumbers & 1) == 1) heightLength = (int) (height / (0.5 + (double)heightCount));
        int startx = (((carid & 1) == 0) ? width / 2 : 0);
        int endx = (startx == 0 ? width / 2 : width);
        int starty = ((carid - 1) / 2) * heightLength;
        int endy = starty + heightLength;
        if(carNumbers == 1)
        {
            startx = 0;
            endx = width;
            starty = 0;
            endy = height;
        }
        else if((carNumbers & 1) == 1 && carid == carNumbers) {
            startx = 0;
            endx = width;
            starty = height - heightLength * heightCount;
            endy = height;
        }
        st : for(;starty < endy; starty++) {
            for (int i = (start.getX() < (endx + startx) / 2 ? (endx - 1) : startx); i < endx && i >= startx; i += (start.getX() < (endx + startx) / 2 ? -1 : 1)) {
                if (i == start.getX() && starty == start.getY()) continue;
                tmpNode = grid.get(starty).get(i);
                if(!tmpNode.isObstacle()) {
                    if (countUnexploredNeighbors(tmpNode) > 0) {
                        candidatePoint = new Point(starty, i);
                        break st;
                    }
                }
            }
        }
        if (candidatePoint != null) {
            this.end = candidatePoint;
        }
        else
        {
            int maxUnexploredCount = -1;
            st : for(int j = 0; j < height; j++){
                for(int i = 0; i < width; i++){
                    if (i == start.getX() && j == start.getY()) continue;
                    tmpNode = grid.get(j).get(i);
                    if(!tmpNode.isObstacle() && tmpNode.isArrived()) {
                        int exploredCount = countUnexploredNeighbors(tmpNode);
                        if (exploredCount > maxUnexploredCount) {
                            maxUnexploredCount = exploredCount;
                            candidatePoint = new Point(j, i);
                            if (maxUnexploredCount == 9) break st;
                        }
                    }
                }
            }
            logger.info("maxUnexploredCount: {}", maxUnexploredCount);
            if (candidatePoint != null) {
                this.end = candidatePoint;
            }
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
            if (!neighbor.isVisited()) {
                count++;
            }
        }
        return count;
    }
}