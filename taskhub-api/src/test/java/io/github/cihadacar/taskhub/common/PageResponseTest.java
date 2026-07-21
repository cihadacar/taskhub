package io.github.cihadacar.taskhub.common;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void veryLargePageNumbersReturnAnEmptyPageWithoutOverflowing() {
        PageResponse<String> page = PageResponse.from(List.of("one"), Integer.MAX_VALUE, 100);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(Integer.MAX_VALUE);
        assertThat(page.size()).isEqualTo(100);
    }
}
