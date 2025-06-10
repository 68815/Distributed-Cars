package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.geo.Point;

import java.util.Objects;

/**
 * <p>网格地图节点类</p>
 *
 * @author 0109
 * @since 2025-05-21
 */
public class GridNode implements Comparable<GridNode> {
    private Point position;
    private double g,h;
    private boolean isObstacle = false;
    private boolean isVisited = false;
    private GridNode parent;
    private boolean isArrived = true;
    public GridNode(int x, int y){
        this.position = new Point(x, y);
    }
    public GridNode(Point xy){
        this.position = xy;
    }

    public Point getXY() {
        return position;
    }

    public void setXY(Point xy) {
        this.position = xy;
    }
    public int getX(){
        return (int) position.getX();
    }
    public int getY(){
        return (int) position.getY();
    }
    public double getG() {
        return g;
    }

    public void setG(double g) {
        this.g = g;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }

    public boolean isObstacle() {
        return isObstacle;
    }

    public void setObstacle(boolean obstacle) {
        isObstacle = obstacle;
    }

    public boolean isVisited() {
        return isVisited;
    }

    public void setVisited(boolean visited) {
        isVisited = visited;
    }

    public GridNode getParent() {
        return parent;
    }

    public void setParent(GridNode parent) {
        this.parent = parent;
    }

    public boolean isArrived() {
        return isArrived;
    }

    public void setArrived(boolean arrived) {
        isArrived = arrived;
    }

    @Override
    public int compareTo(@NotNull GridNode other) {
        double thisCost = this.g + this.h;
        double otherCost = other.g + other.h;
        return Integer.compare((int)thisCost, (int)otherCost);
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GridNode gridNode = (GridNode) o;
        return (int)position.getX() == (int)gridNode.getXY().getX() && (int)position.getY() == (int)gridNode.getXY().getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash((int)position.getX(), (int)position.getY());
    }
}
