package comp370.srms;

import java.util.ArrayList;
import java.util.List;

public final class MessageSerializerTest {
    public static void main(String[] args) {
        exampleObjectComparisonTest();
        exampleValueTest();

        System.out.println("PASS: ExampleTests");
    }

    private static void exampleObjectComparisonTest() {
        List<Integer> expected = List.of(0, 1, 2);
        List<Integer> actual = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            actual.add(i);
        }

        // Will pass, [0,1,2] == [0,1,2]
        TestAssertions.assertEquals(
                expected,
                actual,
                "Example object test");
    }

    private static void exampleValueTest() {
        TestAssertions.assertTrue(4 * 4 == 16, "Example boolean test");
    }
}
