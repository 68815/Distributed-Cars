package ncepusa.distributedcars.navigator.data_structures;

import org.jetbrains.annotations.NotNull;

/**
 * <p>并查集</p>
 *
 * @author TraeAI
 * @since 2025-06-04
 * @version 1.0
 */
public class UnionFind {
    private final int[] parent;
    private final int[] rank;
    private int count;

    public UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        count = size;
        for (int i = 0; i < size; i++) {
            parent[i] = i;
            rank[i] = 0;
        }
    }

    public int find(int p) {
        while (p != parent[p]) {
            parent[p] = parent[parent[p]]; // 路径压缩
            p = parent[p];
        }
        return p;
    }

    public void union(int p, int q) {
        int rootP = find(p);
        int rootQ = find(q);
        if (rootP == rootQ) return;

        // 按秩合并
        if (rank[rootP] > rank[rootQ]) {
            parent[rootQ] = rootP;
        } else if (rank[rootP] < rank[rootQ]) {
            parent[rootP] = rootQ;
        } else {
            parent[rootQ] = rootP;
            rank[rootP]++;
        }
        count--;
    }

    public int getCount() {
        return count;
    }

    /**
     * 统计gridMap中的连通块数量
     * @param grid 二维网格数组
     * @return 连通块数量
     */
    public static int countConnectedBlocks(byte[][] grid) {
        if (grid == null || grid.length == 0) return 0;
        
        int m = grid.length;
        int n = grid[0].length;
        final UnionFind uf = getUnionFind(grid, m, n);

        // 统计实际连通块数量（排除障碍物）
        int connected = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] != 0 && uf.find(i * n + j) == i * n + j) {
                    connected++;
                }
            }
        }
        
        return connected;
    }

    private static @NotNull UnionFind getUnionFind(byte[][] grid, int m, int n) {
        UnionFind uf = new UnionFind(m * n);

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 0) continue; // 跳过障碍物

                // 检查右侧和下侧相邻单元格
                if (j + 1 < n && grid[i][j + 1] != 0) {
                    uf.union(i * n + j, i * n + j + 1);
                }
                if (i + 1 < m && grid[i + 1][j] != 0) {
                    uf.union(i * n + j, (i + 1) * n + j);
                }
            }
        }
        return uf;
    }
}