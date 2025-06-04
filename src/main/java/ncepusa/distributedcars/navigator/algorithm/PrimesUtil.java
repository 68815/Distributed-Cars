package ncepusa.distributedcars.navigator.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>素数工具类</p>
 *
 * @author 0109
 * @since 2025-06-01
 */
public class PrimesUtil {
    private static List<Boolean> isPrime;
    private static final int MAX_CAR_ID = 10000;
    static {
        initializePrimes();
    }

    /**
     * <p>欧拉筛</p>
     */
    private static void initializePrimes() {
        isPrime = new ArrayList<Boolean>(Collections.nCopies(MAX_CAR_ID, true));
        List<Integer> primes = new ArrayList<Integer>();
        isPrime.set(0, false);
        isPrime.set(1, false);
        for (int i = 2; i < MAX_CAR_ID; i++) {
            if(isPrime.get(i)) primes.add(i);
            for (int j = 0; j < primes.size() && primes.get(j) * i < MAX_CAR_ID; j++) {
                isPrime.set(primes.get(j) * i, false);
                if (i % primes.get(j) == 0) break;
            }
        }
    }
    public static boolean isPrime(int x) {
        if (x < MAX_CAR_ID) return isPrime.get(x);
        return false;
    }
}
