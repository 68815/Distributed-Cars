package ncepusa.distributedcars.navigator.algorithm;

import ncepusa.distributedcars.navigator.data_structures.GridMap;
import ncepusa.distributedcars.navigator.data_structures.GridNode;
import org.springframework.data.geo.Point;

import java.util.List;

/**
 * <p>PSO(Particle Swarm Optimization)算法</p>
 *
 * @author 0109
 * @since 2025-05-28
 * @deprecated 作者暂时还没学PSO(
 */
@Deprecated
public class PSO implements PathPlanningStrategy{

    @Override
    public List<Point> planPath(GridMap map, GridNode start, GridNode end) {
        return List.of();
    }
}
