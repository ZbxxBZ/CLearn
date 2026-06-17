package com.clearn.worker.sandbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputComparatorTest {
    private final OutputComparator comparator = new OutputComparator();

    @Test
    void ignoresTrailingWhitespace() {
        assertThat(comparator.matches("3\n", "3\n\n")).isTrue();
    }

    @Test
    void preservesInnerWhitespaceDifference() {
        assertThat(comparator.matches("hello world\n", "hello  world\n")).isFalse();
    }
}
