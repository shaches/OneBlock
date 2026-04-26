package oneblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeightedPoolTest {

    @Test
    @DisplayName("empty pool: pick returns null, totalWeight is 0")
    void emptyPool() {
        WeightedPool<String> pool = new WeightedPool<>();
        assertThat(pool.size()).isZero();
        assertThat(pool.totalWeight()).isZero();
        assertThat(pool.pick(new Random(0))).isNull();
    }

    @Test
    @DisplayName("non-positive weights are ignored")
    void nonPositiveWeightsIgnored() {
        WeightedPool<String> pool = new WeightedPool<>();
        pool.add("a", 0);
        pool.add("b", -5);
        pool.add("c", 1);
        assertThat(pool.size()).isEqualTo(1);
        assertThat(pool.totalWeight()).isEqualTo(1);
        assertThat(pool.pick(new Random(0))).isEqualTo("c");
    }

    @Test
    @DisplayName("single entry always picked")
    void singleEntryAlwaysPicked() {
        WeightedPool<String> pool = new WeightedPool<>();
        pool.add("only", 7);
        Random rnd = new Random(42);
        for (int i = 0; i < 200; i++)
            assertThat(pool.pick(rnd)).isEqualTo("only");
    }

    @Test
    @DisplayName("distribution: frequencies approximate configured weights")
    void distributionMatchesWeights() {
        WeightedPool<String> pool = new WeightedPool<>();
        pool.add("a", 1);
        pool.add("b", 4);
        pool.add("c", 5);
        // 10% / 40% / 50%
        int n = 100_000;
        Map<String, Integer> counts = new HashMap<>();
        Random rnd = new Random(123456789L);
        for (int i = 0; i < n; i++)
            counts.merge(pool.pick(rnd), 1, (a, b) -> a.intValue() + b.intValue());

        // allow +/- 1.5 % absolute tolerance
        double tol = 0.015 * n;
        assertThat(counts.get("a")).isCloseTo(10_000, org.assertj.core.data.Offset.offset((int) tol));
        assertThat(counts.get("b")).isCloseTo(40_000, org.assertj.core.data.Offset.offset((int) tol));
        assertThat(counts.get("c")).isCloseTo(50_000, org.assertj.core.data.Offset.offset((int) tol));
    }

    @Test
    @DisplayName("build() is idempotent and lazy rebuild happens after add()")
    void lazyRebuild() {
        WeightedPool<String> pool = new WeightedPool<>();
        pool.add("a", 1);
        assertThat(pool.totalWeight()).isEqualTo(1);  // triggers first build()
        pool.add("b", 9);
        assertThat(pool.totalWeight()).isEqualTo(10); // must rebuild after add
        pool.build();
        pool.build(); // idempotent
        assertThat(pool.totalWeight()).isEqualTo(10);
    }

    @Test
    @DisplayName("entries() returns an unmodifiable snapshot")
    void entriesSnapshot() {
        WeightedPool<String> pool = new WeightedPool<>();
        pool.add("a", 1);
        pool.add("b", 2);
        assertThatThrownBy(() -> pool.entries().clear())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(pool.entries()).hasSize(2);
        assertThat(pool.entries().get(0).value).isEqualTo("a");
        assertThat(pool.entries().get(0).weight).isEqualTo(1);
        assertThat(pool.entries().get(1).weight).isEqualTo(2);
    }
}
