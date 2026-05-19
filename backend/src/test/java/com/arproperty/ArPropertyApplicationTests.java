package com.arproperty;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Spring Boot 애플리케이션 진입점 smoke test */
class ArPropertyApplicationTests {

    @Test
    void applicationClassExists() {
        assertThat(ArPropertyApplication.class).isNotNull();
    }
}
