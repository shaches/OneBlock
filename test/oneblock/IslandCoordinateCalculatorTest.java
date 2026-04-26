package oneblock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IslandCoordinateCalculatorTest {

    @Test
    @DisplayName("non-circle mode: linear X stride with fixed Z")
    void linearMode() {
        int[] id0 = IslandCoordinateCalculator.getById(0, 50, 10, 100, false);
        int[] id1 = IslandCoordinateCalculator.getById(1, 50, 10, 100, false);
        int[] id7 = IslandCoordinateCalculator.getById(7, 50, 10, 100, false);
        assertThat(id0).containsExactly(50,  10, 0);
        assertThat(id1).containsExactly(150, 10, 1);
        assertThat(id7).containsExactly(750, 10, 7);
    }

    @Test
    @DisplayName("circle mode: id 0 is at (baseX, baseZ)")
    void circleModeZero() {
        int[] c = IslandCoordinateCalculator.getById(0, 0, 0, 100, true);
        assertThat(c).containsExactly(0, 0, 0);
    }

    @Test
    @DisplayName("circle mode: iterative and hybrid branches agree on the boundary")
    void iterAndHybridAgreeAtBoundary() {
        // For id > 30 the implementation switches from the iterative walker to
        // closed-form ring math. Sample both sides of the boundary and every
        // ring boundary up to id 500 to ensure the two implementations agree.
        int baseX = 12, baseZ = -34, diameter = 97;
        for (int id = 0; id < 500; id++) {
            int[] a = IslandCoordinateCalculator.getById(id, baseX, baseZ, diameter, true);
            // iterative version is O(id) — OK for a test
            int[] b = iterBrute(id, baseX, baseZ, diameter);
            assertThat(a).as("id=" + id).containsExactly(b);
        }
    }

    @Test
    @DisplayName("circle mode: first 500 ids map to 500 distinct coordinates")
    void circleModeUniqueCoords() {
        Set<Long> seen = new HashSet<>();
        for (int id = 0; id < 500; id++) {
            int[] c = IslandCoordinateCalculator.getById(id, 0, 0, 100, true);
            long key = ((long) c[0] << 32) | (c[1] & 0xFFFFFFFFL);
            assertThat(seen.add(key))
                .as("duplicate coordinate at id=" + id + " (" + c[0] + "," + c[1] + ")")
                .isTrue();
        }
        assertThat(seen).hasSize(500);
    }

    @Test
    @DisplayName("circle mode: every island sits on the grid (multiples of diameter)")
    void circleModeGridAligned() {
        int diameter = 128;
        for (int id = 0; id < 200; id++) {
            int[] c = IslandCoordinateCalculator.getById(id, 0, 0, diameter, true);
            assertThat(c[0] % diameter).as("id=" + id).isZero();
            assertThat(c[1] % diameter).as("id=" + id).isZero();
        }
    }

    /** O(id) reference walker matching the production iterative spiral. */
    private static int[] iterBrute(int id, int x, int z, int diameter) {
        int X = 0, Z = 0;
        for (int i = 0; i < id; i++) {
            if (X > Z) {
                if (X > -Z) Z--;
                else        X--;
            } else if (-X > Z || (X == Z && Z < 0)) {
                Z++;
            } else {
                X++;
            }
        }
        return new int[] { X * diameter + x, Z * diameter + z, id };
    }
}
