package org.fantom.services.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repositories.widget.dto.Area;
import org.fantom.services.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.services.widget.dto.WidgetUpdateDto;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class WidgetService<ID> {
    private final WidgetRepository<ID> widgetRepository;

    public WidgetService(WidgetRepository<ID> widgetRepository) {
        System.out.println("Creating service with repo" + widgetRepository.getClass().getName());
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
            return this.widgetRepository.runAtomically(repo -> {
                if (widget.zIndex == null) {
                    var maxZIndex = repo.getMaxZIndex().orElse(Integer.MIN_VALUE);
                    // move new widget to foreground, throws ArithmeticException on overflow
                    // TODO: think about moving preceding widgets down to fit into room
                    int newZIndex = Math.addExact(maxZIndex, 1);
                    // cannot get zIndex conflict here
                    return repo.add(new org.fantom.repositories.widget.dto.WidgetCreateDto(widget.x, widget.y, newZIndex, widget.width, widget.height, updatedAt));
                } else {
                    repo.shiftUpwards(widget.zIndex);
                    // cannot get zIndex conflict here
                    return repo.add(widget.toRepoDto(updatedAt));
                }
            });
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

    public List<Widget<ID>> getAll() {
        return widgetRepository.getAll();
    }

    public List<Widget<ID>> getInArea(int left, int right, int bottom, int top) {
        return widgetRepository.getInArea(new Area(left, right, bottom, top));
    }

    public void clearAll() {
        widgetRepository.deleteAll();
    }
}
