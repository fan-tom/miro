package org.fantom.web;

import org.fantom.web.controllers.widget.WidgetsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ApplicationTest {

    @Autowired
    WidgetsController<Integer> widgetsController;

    @Test
    public void contextLoads() {
        assertThat(widgetsController).isNotNull();
    }
}
