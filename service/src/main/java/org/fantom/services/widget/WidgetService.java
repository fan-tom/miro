package org.fantom.services.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.services.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.services.widget.dto.WidgetUpdateDto;

import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

public class WidgetService<ID> {
    private final WidgetRepository<ID> widgetRepository;

    public WidgetService(WidgetRepository<ID> widgetRepository) {
        this.widgetRepository = widgetRepository;
    }

    /**
     * Create Widget from given dto, filling up id, last modification date and zIndex, if not present
     * @param widget dto to create widget from
     * @return newly created widget
     * @throws ArithmeticException if zIndex of new
     */
    public Widget<ID> create(WidgetCreateDto widget) throws ArithmeticException {
        Date updatedAt = new Date();
        try {
            if (widget.zIndex == null) {
                return this.widgetRepository.runAtomically(repo -> {
                    var maxZIndex = repo.getMaxZIndex().orElse(Integer.MIN_VALUE);
                    // move new widget to foreground, throws ArithmeticException on overflow
                    // TODO: think about moving preceding widgets down to fit into room
                    int newZIndex = Math.addExact(maxZIndex, 1);
                    // cannot get zIndex conflict here
                    return repo.add(new org.fantom.repositories.widget.dto.WidgetCreateDto(widget.x, widget.y, newZIndex, widget.width, widget.height, updatedAt));
                });
            } else {
                return widgetRepository.runAtomically(repo -> {
                    repo.shiftUpwards(widget.zIndex);
                    // cannot get zIndex conflict here
                    return repo.add(widget.toRepoDto(updatedAt));
                });
            }
        } catch (ZIndexConflictException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Widget<ID>> update(WidgetUpdateDto<ID> update) throws ZIndexConflictException {
            var oldWidget = widgetRepository.getById(update.id);
            if (oldWidget.isPresent()) {
                var newWidget = update.apply(oldWidget.get(), new Date());
                return widgetRepository.save(newWidget);
            }
            return Optional.empty();
    }

    public boolean delete(ID id) {
        return widgetRepository.deleteById(id);
    }

    public Optional<Widget<ID>> getById(ID id) {
        return widgetRepository.getById(id);
    }

    public Stream<Widget<ID>> getAll() {
        return widgetRepository.getAll();
    }
}
