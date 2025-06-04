package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.NotNull;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示导航网格（Navigation Mesh）的类，用于路径规划和障碍物处理。
 * 该类通过维护一个三角剖分网格（mesh）和多边形障碍列表来实现导航功能。
 * 提供生成多边形障碍、判断点是否在多边形内、计算三角形重心以及检测重心是否在障碍物中 等方法。
 *
 * 该类依赖于增量三角剖分（IncrementalTin）来管理地图网格，并通过多边形列表存储障碍物信息。
 *
 * @author 0109
 * @since 2025-05-15
 * @deprecated 当前版本暂不使用，保留供未来扩展
 */
@Deprecated
public class NevMesh {
    /**
     * 用于存储地图的mesh
     */
    private IncrementalTin mesh;
    /**
     * 存储多边形障碍的列表
     */
    private List<Polygon> polygons;
    /**
     * 存储三角形和是否在多边形内的映射关系
     * 这是缓存，避免每次计算重心时都重新计算
     *
     * 为什么要额外定义这个缓存：因为SimpleTriangle源码没有isObstacle字段；
     * 当试图下载Tinfour的源码时遇到下载失败的问题，所以也无法更改源码。
     * （而且为什么在apache maven central 中复制过来的 gradle依赖，构建时居然报错找不到artifact）
     */
    private Map<SimpleTriangle, Boolean> triangles;

    /**
     * 缓存每个三角形的邻居列表，以提高性能
     */
    private Map<SimpleTriangle, List<SimpleTriangle>> triangleNeighbors;


    public NevMesh() {
        this.mesh = new IncrementalTin(1.0);

        // 初始化mesh的边界
        mesh.add(new Vertex(0,0,0));
        mesh.add(new Vertex(0,600,0));
        mesh.add(new Vertex(800,0,0));
        mesh.add(new Vertex(800,600,0));

        polygons = new ArrayList<Polygon>();
        triangles = new HashMap<SimpleTriangle, Boolean>();
        triangleNeighbors = new HashMap<SimpleTriangle, List<SimpleTriangle>>();
    }
    /**
     * 在mesh中生成一个多边形障碍,
     * 顶点列表需要按逆时针方向给出
     *
     * @param vertices 多边形障碍的顶点列表
     *
     */
    public void generatePolygon(@NotNull List<Vertex> vertices) {
        polygons.add(new Polygon(vertices));
        vertices.forEach(mesh::add);
    }
    public void generatePolygon(@NotNull Polygon polygon) {
        polygons.add(polygon);
        polygon.getVertices().forEach(mesh::add);
    }
    public void generatePolygons(@NotNull List<Polygon> polygons) {
        polygons.forEach(this::generatePolygon);
        mesh.triangles().forEach(triangle -> {
            triangles.put(triangle, isCentroidInAnyPolygon(triangle));
            triangleNeighbors.put(triangle, findNeighbors(triangle));
        });
    }

    /**
     * 使用射线检测法判断点是否在多边形内
     *
     * @param point   待检测的点
     * @param polygon 多边形
     * @return 如果点在多边形内返回true，否则返回false
     */
    public boolean isPointInPolygon(Vertex point, @NotNull Polygon polygon) {
        List<Vertex> vertices = polygon.getVertices();
        int n = vertices.size();
        boolean result = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Vertex vi = vertices.get(i);
            Vertex vj = vertices.get(j);
            if ((vi.y > point.y) != (vj.y > point.y) &&
                    (point.x < (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x)) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * 计算三角形的重心
     *
     * @param triangle 三角形
     * @return 重心点
     */
    public Vertex calculateTriangleCentroid(@NotNull SimpleTriangle triangle) {
        Vertex v1 = triangle.getVertexA();
        Vertex v2 = triangle.getVertexB();
        Vertex v3 = triangle.getVertexC();
        double centroidX = (v1.x + v2.x + v3.x) / 3.0;
        double centroidY = (v1.y + v2.y + v3.y) / 3.0;
        return new Vertex(centroidX, centroidY, 0);
    }

    /**
     * 判断三角形是否在某一个障碍物中
     * 由于不会出现 某一个三角形只有一部分是障碍的情况，所以可根据重心判断
     *
     * @param triangle 三角剖分生成的三角形
     * @return 如果重心在某个多边形内返回true，否则返回false
     */
    public boolean isCentroidInAnyPolygon(SimpleTriangle triangle) {
        Vertex centroid = calculateTriangleCentroid(triangle);
        /*
            如果多边形的数量不多（<= 100）,则串行流的效率优于并行流
            这个100是由毫无经验的作者临时猜的（
         */
        if(polygons.size() <= 100) {
            return polygons.stream().anyMatch(polygon -> isPointInPolygon(centroid, polygon));
        } else {
            return polygons.parallelStream().anyMatch(polygon -> isPointInPolygon(centroid, polygon));
        }
    }

    /**
     * 获取某个三角形的邻居们
     * @param triangle 三角形
     * @return 邻居列表
     */
    public List<SimpleTriangle> getNeighbors(@NotNull SimpleTriangle triangle) {
        return triangleNeighbors.getOrDefault(triangle, new ArrayList<>());
    }

    /**
     * 通过判断两个三角形是否有公共边判断是否是邻居
     */
    private @NotNull List<SimpleTriangle> findNeighbors(SimpleTriangle triangle) {
        List<SimpleTriangle> neighbors = new ArrayList<>();
        mesh.triangles().forEach(candidate -> {
            if (triangle != candidate && sharesEdge(triangle, candidate)) {
                neighbors.add(candidate);
            }
        });
        return neighbors;
    }

    /**
     * 判断两个三角形是否有公共边
     * 两个三角形有公共边，当且仅当它们有两个公共顶点
     * @param t1 三角形1
     * @param t2 三角形2
     * @return 是否有公共边
     */
    private boolean sharesEdge(@NotNull SimpleTriangle t1, @NotNull SimpleTriangle t2) {
        int sharedVertices = 0;
        if (t1.getVertexA().equals(t2.getVertexA()) || t1.getVertexA().equals(t2.getVertexB()) || t1.getVertexA().equals(t2.getVertexC())) {
            sharedVertices++;
        }
        if (t1.getVertexB().equals(t2.getVertexA()) || t1.getVertexB().equals(t2.getVertexB()) || t1.getVertexB().equals(t2.getVertexC())) {
            sharedVertices++;
        }
        if (t1.getVertexC().equals(t2.getVertexA()) || t1.getVertexC().equals(t2.getVertexB()) || t1.getVertexC().equals(t2.getVertexC())) {
            sharedVertices++;
        }
        return sharedVertices >= 2;
    }

    /**
     * 预计算每个三角形的邻居三角形
     */
    public void precomputeNeighbors() {
        mesh.triangles().forEach(triangle -> {
            triangleNeighbors.put(triangle, findNeighbors(triangle));
        });
    }

    public Iterable<SimpleTriangle> getTriangles() {
        return mesh.triangles();
    }
}
