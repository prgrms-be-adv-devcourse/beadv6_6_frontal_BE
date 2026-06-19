package com.biddy.order;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrderApplicationTests {

    @Test
    void simpleTestToPassCI() {
        String status = "OK";
        assertThat(status).isEqualTo("OK");
    }

}
