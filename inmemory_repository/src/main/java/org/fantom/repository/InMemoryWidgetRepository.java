package org.fantom.repository;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.AtomicAction;
import org.fantom.repositories.widget.AtomicFunction;
import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repositories.widget.dto.Area;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.repositories.widget.dto.WidgetCreateDto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class Pair<T1, T2> {
    public final T1 first;
    public final T2 second;

    Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }
}

public class InMemoryWidgetRepository<ID> implements WidgetRepository<ID> {
    // Thread-unsafe impl of widget repository to use in runAtomically without synchronization overhead
    protected static class InternalInMemoryWidgetRepository<ID> implements WidgetRepository<ID> {
        private final Map<ID, WidgetDao<ID>> widgetsById;
        // here we need to keep keys ordered
        private final TreeMap<Integer, WidgetDao<ID>> widgetsByZIndex;
        private final IdGenerator<ID> idGenerator;

        // Indexes over widgets coordinates, to easy filter by area borders
        private final TreeMap<Integer, HashSet<WidgetDao<ID>>> leftIndex;
        private final TreeMap<Integer, HashSet<WidgetDao<ID>>> rightIndex;
        private final TreeMap<Integer, HashSet<WidgetDao<ID>>> topIndex;
        private final TreeMap<Integer, HashSet<WidgetDao<ID>>> bottomIndex;

        protected InternalInMemoryWidgetRepository(Map<ID, WidgetDao<ID>> widgets, IdGenerator<ID> idGenerator) {
            this.widgetsById = widgets;
            this.idGenerator = idGenerator;
            this.widgetsByZIndex = new TreeMap<>();

            this.leftIndex = new TreeMap<>();
            // we always require widget right edge to have x-coordinate to be LESS than the search area right edge's one
            this.rightIndex = new TreeMap<>(Collections.reverseOrder());
            // the same for top: widget top edge y-coordinate must be LESS than the search area top edge's one
            // this reverse order allows to always take tailMap when searching for widgets
            this.topIndex = new TreeMap<>(Collections.reverseOrder());
            this.bottomIndex = new TreeMap<>();
        }

        public InternalInMemoryWidgetRepository(IdGenerator<ID> idGenerator) {
            this(new HashMap<>(), idGenerator);
        }

        public InternalInMemoryWidgetRepository(Iterable<Widget<ID>> widgets, IdGenerator<ID> idGenerator) throws ZIndexConflictException {
            this(StreamSupport
                            .stream(widgets.spliterator(), true)
                            .map(WidgetDao::new)
                            .collect(Collectors.toMap(w -> w.id, w -> w)),
                    idGenerator);
            for (var widget : widgetsById.values()) {
                var existing = widgetsByZIndex.putIfAbsent(widget.zIndex, widget);
                if (existing != null) {
                    throw new ZIndexConflictException(widget.zIndex);
                }

                addToIndexes(widget);
            }
        }

        protected void addToIndexes(WidgetDao<ID> widget) {
            leftIndex.computeIfAbsent(widget.x, k -> new HashSet<>()).add(widget);
            rightIndex.computeIfAbsent(widget.x + widget.width, k -> new HashSet<>()).add(widget);
            topIndex.computeIfAbsent(widget.y + widget.height, k -> new HashSet<>()).add(widget);
            bottomIndex.computeIfAbsent(widget.y, k -> new HashSet<>()).add(widget);
        }

        protected void removeFromIndexes(WidgetDao<ID> widget) {
            Stream.of(
                    new Pair<>(rightIndex, widget.x),
                    new Pair<>(leftIndex, widget.x + widget.width),
                    new Pair<>(topIndex, widget.y + widget.height),
                    new Pair<>(bottomIndex, widget.y)
            )
                    .map(pair -> pair.first.get(pair.second))
                    .filter(Objects::nonNull)
                    .forEach(set -> set.remove(widget));
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
            addToIndexes(widgetDao);

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
                var newWidget = new WidgetDao<>(widget);
                if (oldWidget.zIndex != newWidget.zIndex) {
                    if (!widgetsByZIndex.containsKey(newWidget.zIndex)) {
                        // zIndex is free
                        widgetsByZIndex.put(newWidget.zIndex, oldWidget);
                    } else {
                        // zIndex already occupied by other widget
                        throw new ZIndexConflictException(newWidget.zIndex);
                    }
                }

                widgetsById.put(newWidget.id, newWidget);
                removeFromIndexes(oldWidget);
                addToIndexes(newWidget);

                return Optional.of(widget);
            }
        }

        @Override
        public void save(Iterable<Widget<ID>> widgets) throws ZIndexConflictException {
            // TODO: what if exception thrown in the middle?
            for (var widget : widgets) {
                save(widget);
            }
        }

        @Override
        public Optional<Widget<ID>> getById(ID id) {
            return Optional.ofNullable(widgetsById.get(id)).map(WidgetDao::toWidget);
        }

        @Override
        public Stream<Widget<ID>> getAll() {
            return widgetsByZIndex.values().stream().map(WidgetDao::toWidget);
        }

        @Override
        public Stream<Widget<ID>> getInArea(Area area) {
            var result = leftIndex.tailMap(area.left).values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            return Stream.of(new Pair<>(rightIndex, area.right), new Pair<>(topIndex, area.top), new Pair<>(bottomIndex, area.bottom))
                    // avoid synchronization of resulting hashset
                    .sequential()
                    .reduce(result, (res, pair) -> {
                                res.retainAll(pair.first.tailMap(pair.second).values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
                                return res;
                            },
                            (set1, set2) -> {
                                // get exception on parallel execution
                                throw new RuntimeException("Sequential reduce executed in parallel");
                            })
                    .stream()
                    .map(WidgetDao::toWidget);
        }

        protected WidgetDao<ID> deleteAndReturnByIdInternal(ID id) {
            var widget = widgetsById.remove(id);
            if (widget != null) {
                widgetsByZIndex.remove(widget.zIndex);
                removeFromIndexes(widget);
            }
            return widget;
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

            leftIndex.clear();
            rightIndex.clear();
            topIndex.clear();
            bottomIndex.clear();
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
            if (!(widgetsByZIndex.containsKey(zIndex + 1))) {
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
                    } else {
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
    // rwlock to protect internal repo from concurrent writes or read/write
    // couldn't use thread-safe map impls as multiple maps must be changed concurrently
    private final ClosableReentrantReadWriteLock rwLock = new ClosableReentrantReadWriteLock(true);

    public InMemoryWidgetRepository(Iterable<Widget<ID>> widgets, IdGenerator<ID> idGenerator) throws ZIndexConflictException {
        internal = new InternalInMemoryWidgetRepository<>(widgets, idGenerator);
    }

    public InMemoryWidgetRepository(IdGenerator<ID> idGenerator) {
        internal = new InternalInMemoryWidgetRepository<>(idGenerator);
    }

    @Override
    public Widget<ID> add(WidgetCreateDto widgetDto) throws ZIndexConflictException {
        try (var ignored = rwLock.writeLock()) {
            return internal.add(widgetDto);
        }
    }

    @Override
    public Stream<Widget<ID>> add(Iterable<WidgetCreateDto> widgets) throws ZIndexConflictException {
        try (var ignored = rwLock.writeLock()) {
            return internal.add(widgets);
        }
    }

    @Override
    public Optional<Widget<ID>> save(Widget<ID> widget) throws ZIndexConflictException {
        try (var ignored = rwLock.writeLock()) {
            return internal.save(widget);
        }
    }

    @Override
    public void save(Iterable<Widget<ID>> widgets) throws ZIndexConflictException {
        try (var ignored = rwLock.writeLock()) {
            internal.save(widgets);
        }
    }

    @Override
    public Optional<Widget<ID>> getById(ID id) {
        try (var ignored = rwLock.readLock()) {
            return internal.getById(id);
        }
    }

    @Override
    public Stream<Widget<ID>> getAll() {
        try (var ignored = rwLock.readLock()) {
            return internal.getAll();
        }
    }

    @Override
    public Stream<Widget<ID>> getInArea(Area area) {
        try (var ignored = rwLock.readLock()) {
            return internal.getInArea(area);
        }
    }

    @Override
    public Optional<Widget<ID>> deleteAndReturnById(ID id) {
        try (var ignored = rwLock.writeLock()) {
            return internal.deleteAndReturnById(id);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        try (var ignored = rwLock.writeLock()) {
            return internal.deleteById(id);
        }
    }

    @Override
    public void deleteAll() {
        try (var ignored = rwLock.writeLock()) {
            internal.deleteAll();
        }
    }

    @Override
    public Optional<Integer> getMaxZIndex() {
        try (var ignored = rwLock.readLock()) {
            return internal.getMaxZIndex();
        }
    }

    @Override
    public void shiftUpwards(Integer zIndex) throws ArithmeticException {
        try (var ignored = rwLock.writeLock()) {
            internal.shiftUpwards(zIndex);
        }
    }

    @Override
    public <T, E extends Exception> T runAtomically(AtomicFunction<WidgetRepository<ID>, T, E> action) throws E {
        try (var ignored = rwLock.writeLock()) {
            return action.run(internal);
        }
    }

    @Override
    public <E extends Exception> void runAtomically(AtomicAction<WidgetRepository<ID>, E> action) throws E {
        try (var ignored = rwLock.writeLock()) {
            action.run(internal);
        }
    }
}
