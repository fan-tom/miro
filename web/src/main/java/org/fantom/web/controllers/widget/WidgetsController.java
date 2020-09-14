package org.fantom.web.controllers.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.services.widget.WidgetService;
import org.fantom.web.config.WidgetIdType;
import org.fantom.web.controllers.widget.dto.WidgetCreateDto;
import org.fantom.web.controllers.widget.dto.WidgetFindByArea;
import org.fantom.web.controllers.widget.dto.WidgetResponseDto;
import org.fantom.web.controllers.widget.dto.WidgetUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@RestController
@RequestMapping("/widgets")
@Validated
public class WidgetsController<ID> {
    private final WidgetService<ID> widgetService;
    protected final Function<String, ID> idConverter;

    @Autowired
    @SuppressWarnings("unchecked")
    public WidgetsController(WidgetService<ID> widgetService, WidgetIdType idClass) {
        this.widgetService = widgetService;
        switch (idClass) {
            case integer:
                this.idConverter = s -> (ID) Long.valueOf(s);
                break;
            case string:
                this.idConverter = s -> (ID) s;
                break;
            default:
                throw new IllegalArgumentException("idClass must be one of integer or string, got " + idClass.name());
        }
    }

    protected ID convertId(String id) {
        return idConverter.apply(id);
    }

    protected ResponseStatusException wrapZIndexException(ZIndexConflictException e) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Widget with zIndex "+e.zIndexAsString()+" already exists");
    }

    @GetMapping
    List<Widget<ID>> getAll() {
        return widgetService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WidgetResponseDto<ID> create(@Valid @RequestBody WidgetCreateDto widget) {
        try {
            return WidgetResponseDto.fromWidget(widgetService.create(widget.toServiceDto()));
        } catch (ArithmeticException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot move widgets upwards starting from zIndex " + widget.zIndex
            );
        }
    }

    @PutMapping("/{id}")
    ResponseEntity<WidgetResponseDto<ID>> update(@PathVariable("id") String id, @Valid @RequestBody WidgetUpdateDto widget) {
        try {
            return ResponseEntity.of(widgetService.update(widget.toServiceDto(convertId(id))).map(WidgetResponseDto::fromWidget));
        } catch (ZIndexConflictException e) {
            throw wrapZIndexException(e);
        }
    }

    @GetMapping("/{id}")
    ResponseEntity<WidgetResponseDto<ID>> getById(@PathVariable("id") String id) {
        return ResponseEntity.of(widgetService.getById(convertId(id)).map(WidgetResponseDto::fromWidget));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") String id) {
        var deleted = widgetService.delete(convertId(id));
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAll() {
        widgetService.clearAll();
    }

    @GetMapping(params = {"left", "right", "bottom", "top"})
    Stream<WidgetResponseDto<ID>> findInArea(@Valid WidgetFindByArea findCriteria) {
        return widgetService
                .getInArea(findCriteria.left, findCriteria.right, findCriteria.bottom, findCriteria.top)
                .stream()
                .map(WidgetResponseDto::fromWidget);
    }
}
