package org.fantom.repository;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.AtomicAction;
import org.fantom.repositories.widget.AtomicFunction;
import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.repositories.widget.dto.WidgetCreateDto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InMemoryWidgetRepository<ID> implements WidgetRepository<ID> {
    // Thread-unsafe impl of widget repository to use in runAtomically without synchronization overhead
    protected static class InternalInMemoryWidgetRepository<ID> implements WidgetRepository<ID> {
        private final Map<ID, WidgetDao<ID>> widgetsById;
        // here we need to keep keys ordered
        private final TreeMap<Integer, WidgetDao<ID>> widgetsByZIndex;
        private final IdGenerator<ID> idGenerator;

        public InternalInMemoryWidgetRepository(IdGenerator<ID> idGenerator) {
            this.idGenerator = idGenerator;
            this.widgetsById = new HashMap<>();
            this.widgetsByZIndex = new TreeMap<>();
        }

        public InternalInMemoryWidgetRepository(Iterable<Widget<ID>> widgets, IdGenerator<ID> idGenerator) throws ZIndexConflictException {
            this.widgetsById = StreamSupport
                    .stream(widgets.spliterator(), true)
                    .map(WidgetDao::new)
                    .collect(Collectors.toMap(w -> w.id, w -> w));
            this.idGenerator = idGenerator;
            this.widgetsByZIndex = new TreeMap<>();
            for (var widget: widgetsById.values()) {
                var existing = widgetsByZIndex.putIfAbsent(widget.zIndex, widget);
                if (existing != null) {
                    throw new ZIndexConflictException(widget.zIndex);
                }
            }
        }

        @Override
        public Widget<ID> add(WidgetCreateDto widgetDto) throws ZIndexConflictException {
            var widget = widgetDto.toWidget(idGenerator.generate());
            var widgetDao = new WidgetDao<>(widget);
            var oldWidget = widgetsByZIndex.putIfAbsent(widgetDao.zIndex, widgetDao);
            if (oldWidget != null) {
                throw new ZIndexConflictException(widgetDao.zIndex);
            }
            widgetsById.put(widgetDao.id, widgetDao);
            return widget;
        }

        @Override
        public Stream<Widget<ID>> add(Iterable<WidgetCreateDto> widgets) throws ZIndexConflictException {
            try {
                return StreamSupport.stream(widgets.spliterator(), false).map(widget -> {
                    try {
                        return add(widget);
                    } catch (ZIndexConflictException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException e) {
                var cause = e.getCause();
                if (cause instanceof ZIndexConflictException) {
                    throw (ZIndexConflictException) cause;
                } else {
                    throw e;
                }
            }
        }

        @Override
        public Optional<Widget<ID>> save(Widget<ID> widget) throws ZIndexConflictException {
            var oldWidget = widgetsById.get(widget.id);
            if (oldWidget == null) {
                return Optional.empty();
            } else {
                if (oldWidget.zIndex != widget.zIndex) {
                    var widgetByZIndex = widgetsByZIndex.get(widget.zIndex);
                    if (widgetByZIndex == null) {
                        // zIndex is free, remove old mapping and update widget
                        widgetsByZIndex.remove(oldWidget.zIndex);
                        // other fields are updated later
                        oldWidget.zIndex = widget.zIndex;
                        widgetsByZIndex.put(oldWidget.zIndex, oldWidget);
                    } else {
                        // zIndex already occupied by other widget
                        throw new ZIndexConflictException(widget.zIndex);
                    }
                }

                oldWidget.x = widget.x;
                oldWidget.y = widget.y;
                oldWidget.width = widget.width;
                oldWidget.height = widget.height;
                oldWidget.updatedAt = widget.updatedAt;

                return Optional.of(widget);
            }
        }

        @Override
        public void save(Iterable<Widget<ID>> widgets) throws ZIndexConflictException {
            // TODO: what if exception thrown in the middle?
            for (var widget: widgets) {
                save(widget);
            }
        }

        @Override
        public Optional<Widget<ID>> getById(ID id) {
            return Optional.ofNullable(widgetsById.get(id)).map(WidgetDao::toWidget);
        }

        protected WidgetDao<ID> deleteAndReturnByIdInternal(ID id) {
            var widget = widgetsById.remove(id);
            if (widget != null) {
                widgetsByZIndex.remove(widget.zIndex);
                return widget;
            }
            return null;
        }

        @Override
        public Optional<Widget<ID>> deleteAndReturnById(ID id) {
            return Optional.ofNullable(deleteAndReturnByIdInternal(id)).map(WidgetDao::toWidget);
        }

        @Override
        public boolean deleteById(ID id) {
            var dao = deleteAndReturnByIdInternal(id);
            return dao != null;
        }

        @Override
        public void deleteAll() {
            widgetsByZIndex.clear();
            widgetsById.clear();
        }

        @Override
        public Stream<Widget<ID>> getAll() {
            return widgetsByZIndex.values().stream().map(WidgetDao::toWidget);
        }

        @Override
        public Optional<Integer> getMaxZIndex() {
            try {
                return Optional.of(widgetsByZIndex.lastKey());
            } catch (NoSuchElementException e) {
                return Optional.empty();
            }
        }

        @Override
        public void shiftUpwards(Integer zIndex) throws ArithmeticException {
            var equalOrUpperKey = widgetsByZIndex.ceilingEntry(zIndex);
            if (equalOrUpperKey == null || !equalOrUpperKey.getKey().equals(zIndex)) {
                // no widgets above given zIndex or zIndex is free
                return;
            }
            if (!(widgetsByZIndex.containsKey(zIndex+1))){
                // fast version, zIndex is used, but next value is not
                var widget = equalOrUpperKey.getValue();
                var newZIndex = zIndex + 1;
                widget.zIndex = newZIndex;
                widgetsByZIndex.remove(zIndex);
                widgetsByZIndex.put(newZIndex, widget);
            } else {
                var overlyingWidgets = widgetsByZIndex.tailMap(zIndex);
                int topUsedZIndex = zIndex;
                // find max of continuous zIndex values sequence
                for (var z: overlyingWidgets.keySet()) {
                    if (z - topUsedZIndex > 1) {
                        break;
                    } else{
                        topUsedZIndex = z;
                    }
                }
                if (topUsedZIndex == Integer.MAX_VALUE) {
                    throw new ArithmeticException("No room to shift widgets upwards");
                }
                // actually shift widgets
                for (var z = topUsedZIndex; z >= zIndex; z--) {
                    var widget = overlyingWidgets.get(z);
                    var newZIndex = z + 1;
                    widget.zIndex = newZIndex;
                    overlyingWidgets.put(newZIndex, widget);
                }
                overlyingWidgets.remove(zIndex);
            }
        }

        @Override
        public <T, E extends Exception> T runAtomically(AtomicFunction<WidgetRepository<ID>, T, E> action) throws E {
            return action.run(this);
        }

        @Override
        public <E extends Exception> void runAtomically(AtomicAction<WidgetRepository<ID>, E> action) throws E {
            action.run(this);
        }
    }
    private final InternalInMemoryWidgetRepository<ID> internal;
    // acquire read lock if you don't need to keep maps unchanged for long time, like when you add/remove elements and
    // don't care if concurrent adds/removes are performed, or you simply read
    // otherwise, acquire write lock (like when you need to be sure map is not changed between different reads/writes)
    private final ClosableReentrantReadWriteLock rwLock = new ClosableReentrantReadWriteLock(true);

    public InMemoryWidgetRepository(Iterable<Widget<ID>> widgets, IdGenerator<ID> idGenerator) throws ZIndexConflictException {
        internal = new InternalInMemoryWidgetRepository<>(widgets, idGenerator);
    }

    public InMemoryWidgetRepository(IdGenerator<ID> idGenerator) {
        internal = new InternalInMemoryWidgetRepository<>(idGenerator);
    }

    @Override
    public Widget<ID> add(WidgetCreateDto widgetDto) throws ZIndexConflictException {
        try (var locked = rwLock.writeLock()) {
            return internal.add(widgetDto);
        }
    }

    @Override
    public Stream<Widget<ID>> add(Iterable<WidgetCreateDto> widgets) throws ZIndexConflictException {
        try(var locked = rwLock.writeLock()) {
            return internal.add(widgets);
        }
    }

    @Override
    public Optional<Widget<ID>> save(Widget<ID> widget) throws ZIndexConflictException {
        try(var locked = rwLock.writeLock()) {
            return internal.save(widget);
        }
    }

    @Override
    public void save(Iterable<Widget<ID>> widgets) throws ZIndexConflictException {
        try(var locked = rwLock.writeLock()) {
            internal.save(widgets);
        }
    }

    @Override
    public Optional<Widget<ID>> getById(ID id) {
        try (var locked = rwLock.readLock()) {
            return internal.getById(id);
        }
    }

    @Override
    public Optional<Widget<ID>> deleteAndReturnById(ID id) {
        try(var locked = rwLock.writeLock()) {
            return internal.deleteAndReturnById(id);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        try(var locked = rwLock.writeLock()) {
            return internal.deleteById(id);
        }
    }

    @Override
    public void deleteAll() {
        try (var locked = rwLock.writeLock()) {
            internal.deleteAll();
        }
    }

    @Override
    public Stream<Widget<ID>> getAll() {
        try(var locked = rwLock.readLock()) {
            return internal.getAll();
        }
    }

    @Override
    public Optional<Integer> getMaxZIndex() {
        try(var locked = rwLock.readLock()) {
            return internal.getMaxZIndex();
        }
    }

    @Override
    public void shiftUpwards(Integer zIndex) throws ArithmeticException {
        try(var locked = rwLock.writeLock()) {
            internal.shiftUpwards(zIndex);
        }
    }

    @Override
    public <T, E extends Exception> T runAtomically(AtomicFunction<WidgetRepository<ID>, T, E> action) throws E {
        try(var locked = rwLock.writeLock()) {
            return action.run(internal);
        }
    }

    @Override
    public <E extends Exception> void runAtomically(AtomicAction<WidgetRepository<ID>, E> action) throws E {
        try(var locked = rwLock.writeLock()) {
            action.run(internal);
        }
    }
}
