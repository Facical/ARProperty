package com.arproperty.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void successResponseSerializesOkStatusAndMeta() throws Exception {
        ApiResponse<List<String>> response = ApiResponse.success(
                List.of("item"),
                Map.of("page", 1, "page_size", 20, "total_count", 1)
        );

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":\"ok\"");
        assertThat(json).contains("\"data\":[\"item\"]");
        assertThat(json).contains("\"page\":1");
        assertThat(json).contains("\"page_size\":20");
        assertThat(json).contains("\"total_count\":1");
        assertThat(json).doesNotContain("\"error\"");
    }

    @Test
    void errorResponseSerializesErrorBodyAndOmitsData() throws Exception {
        ApiResponse<Object> response = ApiResponse.error("INVALID_PARAMETER", "lat is required");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":\"error\"");
        assertThat(json).contains("\"code\":\"INVALID_PARAMETER\"");
        assertThat(json).contains("\"message\":\"lat is required\"");
        assertThat(json).doesNotContain("\"data\"");
        assertThat(json).doesNotContain("\"meta\"");
    }
}
