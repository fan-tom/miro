package org.fantom.web.controllers.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.services.widget.WidgetService;
import org.fantom.web.controllers.widget.dto.WidgetCreateDto;
import org.fantom.web.controllers.widget.dto.WidgetResponseDto;
import org.fantom.web.controllers.widget.dto.WidgetUpdateDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.stream.Stream;

@RestController
@RequestMapping("/widgets")
@Validated
public class WidgetsController<ID> {
    private final WidgetService<ID> widgetService;

    @Autowired
    public WidgetsController(WidgetService<ID> widgetService) {
        this.widgetService = widgetService;
    }

    protected ResponseStatusException wrapZIndexException(ZIndexConflictException e) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Widget with zIndex "+e.zIndex+" already exists");
    }

    @GetMapping
    Stream<Widget<ID>> getAll() {
        return widgetService.getAll();
    }

    @PostMapping()
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
    ResponseEntity<WidgetResponseDto<ID>> update(@PathVariable("id") ID id, @Valid @RequestBody WidgetUpdateDto widget) {
        try {
            return ResponseEntity.of(widgetService.update(widget.toServiceDto(id)).map(WidgetResponseDto::fromWidget));
        } catch (ZIndexConflictException e) {
            throw wrapZIndexException(e);
        }
    }

    @GetMapping("/{id}")
    ResponseEntity<WidgetResponseDto<ID>> getById(@PathVariable("id") ID id) {
        return ResponseEntity.of(widgetService.getById(id).map(WidgetResponseDto::fromWidget));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable ID id) {
        var deleted = widgetService.delete(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
