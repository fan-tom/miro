package org.fantom.repositories.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.dto.Area;
import org.fantom.repositories.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RepositoryTest<ID> {
    protected WidgetRepository<ID> repository;

    public RepositoryTest(WidgetRepository<ID> repository) {
        this.repository = repository;
    }

    abstract protected void resetRepo();

    @BeforeEach
    public void refreshRepo() {
        resetRepo();
    }

    @Test
    public void fulfillsId() {
        try {
            var widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            assertNotNull(widget.id);
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict", e);
        }
    }

    @Test
    public void throwsZIndexConflictExceptionOnAdd() {
        try {
            repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            fail("zIndex conflict exception was not thrown");
        } catch (ZIndexConflictException ignored) {
        }
    }

    @Test
    public void saves() {
        try {
            var widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            var newWidget = new Widget.Builder<>(widget)
                    .withX(1)
                    .withY(2)
                    .withWidth(3)
                    .withHeight(4)
                    .withZIndex(5)
                    .withUpdatedAt(Date.from(new Date().toInstant().plusSeconds(1000)))
                    .build();
            var saved = repository.save(newWidget);
            assertTrue(saved.isPresent(), "widget was not saved");
            assertEquals(saved.get(), newWidget, "saved widget differs");
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }
    }

    @Test
    public void saveOfDeletedWidgetReturnsNone() {
        try {
            var widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            var newWidget = new Widget.Builder<>(widget)
                    .withX(1)
                    .withY(2)
                    .withWidth(3)
                    .withHeight(4)
                    .withZIndex(5)
                    .withUpdatedAt(Date.from(new Date().toInstant().plusSeconds(1000)))
                    .build();
            var deleted = repository.deleteById(widget.id);
            assertTrue(deleted);
            var saved = repository.save(newWidget);
            assertTrue(saved.isEmpty(), "widget was saved");
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }
    }

    @Test
    public void throwsZIndexConflictExceptionOnSave() {
        Widget<ID> widget = null;
        Widget<ID> widget2 = null;
        try {
            widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            widget2 = repository.add(new WidgetCreateDto(0, 0, 1, 0, 0, new Date()));
            var newWidget = new Widget.Builder<>(widget2).withZIndex(0).build();
            repository.save(newWidget);
            fail("Widget saved without zIndex conflict exception");
        } catch (ZIndexConflictException ignored) {
            var widgets = repository.getAll().collect(Collectors.toList());
            assertEquals(2, widgets.size());
            assertEquals(widget, widgets.get(0));
            assertEquals(widget2, widgets.get(1));
        }
    }

    @RepeatedTest(10)
    public void runAtomicallyPreservesRepo() {
        var modifyStartLock = new ReentrantLock();

        modifyStartLock.lock();

        new Thread(() -> {
            modifyStartLock.lock();
            try {
                repository.add(new WidgetCreateDto(0, 0, Integer.MAX_VALUE, 0, 0, new Date()));
            } catch (ZIndexConflictException e) {
                fail("zIndex conflict exception was thrown", e);
            }
        }).start();
        repository.runAtomically(repo -> {
            var maxZIndex = repo.getMaxZIndex();
            modifyStartLock.unlock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            var newMaxZIndex = repo.getMaxZIndex();
            assertEquals(maxZIndex, newMaxZIndex, "Repo modified while runAtomically");
        });
    }

    @RepeatedTest(10)
    public void runAtomicallyPreservesObjects() {
        var modifyStartLock = new ReentrantLock();

        modifyStartLock.lock();

        Widget<ID> widget = null;
        try {
            widget = repository.add(new WidgetCreateDto(0, 0, Integer.MAX_VALUE, 0, 0, new Date()));
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }

        Widget<ID> finalWidget = widget;
        new Thread(() -> {
            modifyStartLock.lock();
            try {
                repository.save(new Widget.Builder<>(finalWidget).withX(5).build());
            } catch (ZIndexConflictException e) {
                fail("zIndex conflict exception was thrown", e);
            }
        }).start();
        repository.runAtomically(repo -> {
            var oldWidget = repo.getById(finalWidget.id);
            assertTrue(oldWidget.isPresent());
            modifyStartLock.unlock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            var newWidget = repo.getById(finalWidget.id);
            assertTrue(newWidget.isPresent());
            assertEquals(oldWidget.get(), newWidget.get(), "Object in repo modified while runAtomically");
        });
    }

    @Test
    public void shiftUpwardsDoesNothingIfZIndexIsFree() {
        try {
            var widget = repository.add(new WidgetCreateDto(0,0,0,0,0, new Date()));
            repository.shiftUpwards(widget.zIndex-1);
            var widgets = repository.getAll().collect(Collectors.toList());
            assertEquals(widgets.size(), 1);
            assertEquals(widget, widgets.get(0), "widget is changed while shiftUpwards");
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }
    }

    @Test
    public void shiftUpwardsMovesOnlyNecessaryWidgets() {
        try {
            var widget0 = repository.add(new WidgetCreateDto(0,0,0,0,0, new Date()));
            var widget1 = repository.add(new WidgetCreateDto(0,0,1,0,0, new Date()));
            var widget2 = repository.add(new WidgetCreateDto(0,0,2,0,0, new Date()));
            var widget4 = repository.add(new WidgetCreateDto(0,0,4,0,0, new Date()));

            repository.shiftUpwards(widget1.zIndex);
            var widgets = repository.getAll().collect(Collectors.toList());

            assertEquals(widgets.size(), 4);

            assertEquals(widget0, widgets.get(0), "widget0 is changed while shiftUpwards");
            assertEquals(widget4, widgets.get(3), "widget4 is changed while shiftUpwards");

            var shiftedWidget1 = widgets.get(1);
            assertEquals(widget1.id, shiftedWidget1.id);
            assertEquals(widget1.x, shiftedWidget1.x);
            assertEquals(widget1.y, shiftedWidget1.y);
            assertEquals(widget1.width, shiftedWidget1.width);
            assertEquals(widget1.height, shiftedWidget1.height);
            assertEquals(widget1.updatedAt, shiftedWidget1.updatedAt);
            assertEquals(widget1.zIndex + 1, shiftedWidget1.zIndex);

            var shiftedWidget2 = widgets.get(2);
            assertEquals(widget2.id, shiftedWidget2.id);
            assertEquals(widget2.x, shiftedWidget2.x);
            assertEquals(widget2.y, shiftedWidget2.y);
            assertEquals(widget2.width, shiftedWidget2.width);
            assertEquals(widget2.height, shiftedWidget2.height);
            assertEquals(widget2.updatedAt, shiftedWidget2.updatedAt);
            assertEquals(widget2.zIndex + 1, shiftedWidget2.zIndex);

        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }
    }

    @Test
    public void canFindByArea() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        var widgetsInArea = repository.getInArea(new Area(-10, 10, 20, 60)).collect(Collectors.toList());
        assertEquals(1, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));
    }

    @Test
    public void canFindByExactTheSameArea() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        var widgetsInArea = repository.getInArea(new Area(widget.x, widget.x+widget.width, widget.y, widget.y+widget.height)).collect(Collectors.toList());
        assertEquals(1, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));
    }

    @Test
    public void partiallyFallingIntoAreaIsSkipped() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        repository.add(new WidgetCreateDto(-4, 20, 31, 10, 20, new Date()));
        var widgetsInArea = repository.getInArea(new Area(widget.x, widget.x+widget.width, widget.y, widget.y+widget.height)).collect(Collectors.toList());
        assertEquals(1, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));
    }

    @Test
    public void oneWidgetInOtherIsFoundToo() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        var widget2 = repository.add(new WidgetCreateDto(-4, 40, 31, 5, 10, new Date()));
        var widgetsInArea = repository.getInArea(new Area(widget.x, widget.x+widget.width, widget.y, widget.y+widget.height))
                .sorted(Comparator.comparingInt(w -> w.zIndex))
                .collect(Collectors.toList());
        assertEquals(2, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));
        assertEquals(widget2, widgetsInArea.get(1));
    }

    @Test
    public void cannotFindAfterUpdate() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        var area = new Area(widget.x, widget.x + widget.width, widget.y, widget.y + widget.height);
        var widgetsInArea = repository.getInArea(area).collect(Collectors.toList());

        assertEquals(1, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));

        var widget2 = repository.save(new Widget<>(widget.id, -4, 20, 30, 5, 10, new Date()));
        assertTrue(widget2.isPresent());

        var widgetsInAreaAfterUpdate = repository.getInArea(area).collect(Collectors.toList());

        assertTrue(widgetsInAreaAfterUpdate.isEmpty());
    }

    @Test
    public void cannotFindAfterDelete() throws ZIndexConflictException {
        var widget = repository.add(new WidgetCreateDto(-5, 30, 30, 10, 20, new Date()));
        var area = new Area(widget.x, widget.x + widget.width, widget.y, widget.y + widget.height);
        var widgetsInArea = repository.getInArea(area).collect(Collectors.toList());

        assertEquals(1, widgetsInArea.size());
        assertEquals(widget, widgetsInArea.get(0));

        var deleted = repository.deleteById(widget.id);
        assertTrue(deleted);

        var widgetsInAreaAfterDelete = repository.getInArea(area).collect(Collectors.toList());
        assertTrue(widgetsInAreaAfterDelete.isEmpty());
    }
}
