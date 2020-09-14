package org.fantom.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fantom.services.widget.WidgetService;
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
import org.springframework.util.LinkedMultiValueMap;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.util.AssertionErrors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ControllerTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WidgetService<Integer> widgetService;

    @AfterEach
    public void clearAll() {
        widgetService.clearAll();
    }

    @Test
    public void canCreateWidgetWithAllFieldsSpecified() throws Exception {
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
        assertThat(responseDto.id).isNotNull();
        assertEquals("x must be the same", responseDto.x, requestDto.x);
        assertEquals("y must be the same", responseDto.y, requestDto.y);
        assertEquals("zIndex must be the same", responseDto.zIndex, requestDto.zIndex);
        assertEquals("width must be the same", responseDto.width, requestDto.width);
        assertEquals("height must be the same", responseDto.height, requestDto.height);
        assertNotNull("updatedAt must be not null", responseDto.updatedAt);
    }

    @Test
    public void canCreateWidgetWithoutZIndex() throws Exception {
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
        assertThat(responseDto.id).isNotNull();
        assertEquals("x must be the same", requestDto.x, responseDto.x);
        assertEquals("y must be the same", requestDto.y, responseDto.y);
        assertThat(responseDto.zIndex).isNotNull();
        assertEquals("width must be the same", requestDto.width, responseDto.width);
        assertEquals("height must be the same", requestDto.height, responseDto.height);
        assertThat(responseDto.updatedAt).isNotNull();
    }

    @Test
    public void zIndexIsChangedOnConflicts() throws Exception {
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
    public void canGetListOrderedByZIndexAsc() throws Exception {
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
        var zIndices = responseDtos.stream().map(r -> r.zIndex).collect(Collectors.toList());
        assertThat(zIndices).isEqualTo(IntStream.range(0, requestNumber).boxed().collect(Collectors.toList()));
    }

    @Test
    public void canUpdate() throws Exception {
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

        var updateResponseBody = mvc.perform(put("/widgets/" + createResponseDto.id.toString())
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

        mvc.perform(delete("/widgets/" + createResponseDto.id.toString()))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
        mvc.perform(get("/widgets/" + createResponseDto.id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void canFindByArea() throws Exception {
        var createRequestDto = new WidgetCreateDto(1, 2, 3, 4, 5);
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

        var areaParams = new LinkedMultiValueMap<String, String>();
        areaParams.add("left", "1");
        areaParams.add("right", "6");
        areaParams.add("bottom", "1");
        areaParams.add("top", "7");
        var widgetsBody = mvc.perform(get("/widgets")
                .queryParams(areaParams))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var widgetsInArea = objectMapper.readValue(widgetsBody, WidgetResponseDto[].class);
        assertThat(widgetsInArea).hasSize(1);
        assertThat(widgetsInArea[0]).isEqualTo(createResponseDto);
    }
}
