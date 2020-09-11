package org.fantom.web.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fantom.services.widget.WidgetService;
import org.fantom.web.controllers.widget.WidgetsController;
import org.fantom.web.controllers.widget.dto.WidgetCreateDto;
import org.fantom.web.controllers.widget.dto.WidgetResponseDto;
import org.fantom.web.controllers.widget.dto.WidgetUpdateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

//@WebMvcTest(WidgetsController.class)
@SpringBootTest
@AutoConfigureMockMvc
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ActiveProfiles("test")
//@ContextConfiguration(classes=TestApp.class, initializers=ConfigFileApplicationContextInitializer.class)
public class ControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WidgetsController<Integer> widgetsController;

    @Autowired
    WidgetService<Integer> widgetService;

    @AfterEach
    public void clearAll() {
        widgetService.clearAll();
    }

    @Test
    public void canCreateWidgetWithAllFieldsSpecified() throws Exception {
        assertThat(widgetsController).isNotNull();
        var requestDto = new WidgetCreateDto(0,0,0,1,1);
        var request = objectMapper.writeValueAsString(requestDto);
        var response = mvc.perform(post("/widgets")
                .content(request)
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        var responseBody = response.getResponse().getContentAsString();
        var responseDto = objectMapper.readValue(responseBody, WidgetResponseDto.class);
        assertEquals("Id must be string", responseDto.id.getClass(), String.class);
        assertEquals("x must be the same", responseDto.x, requestDto.x);
        assertEquals("y must be the same", responseDto.y, requestDto.y);
        assertEquals("zIndex must be the same", responseDto.zIndex, requestDto.zIndex);
        assertEquals("width must be the same", responseDto.width, requestDto.width);
        assertEquals("height must be the same", responseDto.height, requestDto.height);
        assertNotNull("updatedAt must be not null", responseDto.updatedAt);
    }

    @Test
    public void canCreateWidgetWithoutZIndex() throws Exception {
        assertThat(widgetsController).isNotNull();
        var requestDto = new WidgetCreateDto(0,0,null,1,1);
        var request = objectMapper.writeValueAsString(requestDto);
        var response = mvc.perform(post("/widgets")
                .content(request)
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();
        var responseBody = response.getResponse().getContentAsString();
        var responseDto = objectMapper.readValue(responseBody, WidgetResponseDto.class);
        assertEquals("Id must be string", responseDto.id.getClass(), String.class);
        assertEquals("x must be the same", responseDto.x, requestDto.x);
        assertEquals("y must be the same", responseDto.y, requestDto.y);
        assertThat(responseDto.zIndex).isNotNull();
        assertEquals("width must be the same", responseDto.width, requestDto.width);
        assertEquals("height must be the same", responseDto.height, requestDto.height);
        assertThat(responseDto.updatedAt).isNotNull();
    }

    @Test
    public void zIndexIsChangedOnConflicts() throws Exception {
        assertThat(widgetsController).isNotNull();
        var requestDto = new WidgetCreateDto(0,0,0,1,1);
        var request = objectMapper.writeValueAsString(requestDto);
        final var requestNumber = 10;
        IntStream.generate(() -> 0).limit(requestNumber).forEach((_i) -> {
            try {
                mvc.perform(post("/widgets")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                        .andExpect(MockMvcResultMatchers.status().isCreated());
            } catch (Exception e) {
                fail("Unexpected exception while creating widget request", e);
            }
        });
        var responseBody = mvc
                .perform(get("/widgets")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var responseDtos = Arrays.asList(objectMapper.readValue(responseBody, WidgetResponseDto[].class));
        assertThat(responseDtos).hasSize(requestNumber);
        var zIndices = responseDtos.stream().map(r -> r.zIndex).collect(Collectors.toSet());
        assertThat(responseDtos.stream().map(r -> r.zIndex)).containsExactlyElementsOf(zIndices);
    }

    @Test
    public void canUpdate() throws Exception {
        assertThat(widgetsController).isNotNull();
        var createRequestDto = new WidgetCreateDto(0,0,0,1,1);
        var createRequest = objectMapper.writeValueAsString(createRequestDto);
        var createResponseBody = mvc.perform(post("/widgets")
                .content(createRequest)
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createResponseDto = objectMapper.readValue(createResponseBody, WidgetResponseDto.class);

        var updateRequestDto = new WidgetUpdateDto(1,1,1,2,2);
        var updateRequest = objectMapper.writeValueAsString(updateRequestDto);

        var updateResponseBody = mvc.perform(put("/widgets/"+createResponseDto.id)
                .content(updateRequest)
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var updateResponseDto = objectMapper.readValue(updateResponseBody, WidgetResponseDto.class);

        var updatedWidgetBody = mvc.perform(get("/widgets/"+createResponseDto.id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var updatedWidgetDto = objectMapper.readValue(updatedWidgetBody, WidgetResponseDto.class);

        assertThat(updateResponseDto).isEqualToComparingFieldByField(updatedWidgetDto);

        assertEquals("x must be the same", updateRequestDto.x, updatedWidgetDto.x);
        assertEquals("y must be the same", updateRequestDto.y, updatedWidgetDto.y);
        assertEquals("zIndex must be the same", updateRequestDto.zIndex, updatedWidgetDto.zIndex);
        assertEquals("width must be the same", updateRequestDto.width, updatedWidgetDto.width);
        assertEquals("height must be the same", updateRequestDto.height, updatedWidgetDto.height);
        assertNotEquals("updatedAt must be updated", createResponseDto.updatedAt, updatedWidgetDto.updatedAt);
    }

    @Test
    public void canDelete() throws Exception {
        assertThat(widgetsController).isNotNull();
        var createRequestDto = new WidgetCreateDto(0, 0, 0, 1, 1);
        var createRequest = objectMapper.writeValueAsString(createRequestDto);
        var createResponseBody = mvc.perform(post("/widgets")
                .content(createRequest)
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createResponseDto = objectMapper.readValue(createResponseBody, WidgetResponseDto.class);

        mvc.perform(delete("/widgets/" + createResponseDto.id))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        mvc.perform(get("/widgets/" + createResponseDto.id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }
}
