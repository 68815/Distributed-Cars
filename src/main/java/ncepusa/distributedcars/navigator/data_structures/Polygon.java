package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.tinfour.common.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * 多边形类
 *
 * @deprecated 当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public class Polygon {
    /**
     * 顶点列表
     */
    private List<Vertex> vertices;

    @Contract(pure = true)
    public Polygon() {
        this.vertices = new ArrayList<Vertex>();
    }

    @Contract(pure = true)
    public Polygon(@NotNull List<Vertex> vertices) {
        this.vertices = vertices;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public void setVertices(List<Vertex> vertices) {
        this.vertices = vertices;
    }
}
