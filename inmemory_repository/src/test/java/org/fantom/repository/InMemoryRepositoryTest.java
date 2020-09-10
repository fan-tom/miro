package org.fantom.repository;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.IdGenerator;
import org.fantom.repositories.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryRepositoryTest {
    private static class IntegerIdGenerator implements IdGenerator<Integer> {
        private int nextValue = Integer.MIN_VALUE;

        @Override
        public Integer generate() {
            return nextValue++;
        }
    }

    private InMemoryWidgetRepository<Integer> repository;

    @BeforeEach
    public void refreshRepo() {
        repository = new InMemoryWidgetRepository<>(new IntegerIdGenerator());
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
            var widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
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
    public void throwsZIndexConflictExceptionOnSave() {
        try {
            var widget = repository.add(new WidgetCreateDto(0, 0, 0, 0, 0, new Date()));
            var widget2 = repository.add(new WidgetCreateDto(0, 0, 1, 0, 0, new Date()));
            var newWidget = new Widget.Builder<>(widget2).withZIndex(0).build();
            repository.save(newWidget);
            fail("Widget saved without zIndex conflict exception");
        } catch (ZIndexConflictException ignored) {
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

        Widget<Integer> widget = null;
        try {
            widget = repository.add(new WidgetCreateDto(0, 0, Integer.MAX_VALUE, 0, 0, new Date()));
        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }

        Widget<Integer> finalWidget = widget;
        new Thread(() -> {
            modifyStartLock.lock();
            try {
                repository.save(new Widget.Builder<>(finalWidget).withX(5).build());
            } catch (ZIndexConflictException e) {
                fail("zIndex conflict exception was thrown", e);
            }
        }).start();
        var newWidget = repository.runAtomically(repo -> {
                modifyStartLock.unlock();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                var newW = repo.getById(finalWidget.id);
                assertTrue(newW.isPresent());
                return newW.get();
            });
        assertEquals(finalWidget, newWidget, "Object in repo modified while runAtomically");
    }

    @Test
    public void shiftUpwardsDoesNothingIfZIndexIsFree() {
        try {
            var widget = repository.add(new WidgetCreateDto(0,0,0,0,0, new Date()));
            repository.shiftUpwards(widget.zIndex-1);
            var widgets = repository.getAll().collect(Collectors.toList());
            assertEquals(widgets.size(), 1);
            assertEquals(widgets.get(0), widget, "widget is changed while shiftUpwards");
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

            assertEquals(widgets.get(0), widget0, "widget0 is changed while shiftUpwards");
            assertEquals(widgets.get(3), widget4, "widget4 is changed while shiftUpwards");

            var shiftedWidget1 = widgets.get(1);
            assertEquals(shiftedWidget1.id, widget1.id);
            assertEquals(shiftedWidget1.x, widget1.x);
            assertEquals(shiftedWidget1.y, widget1.y);
            assertEquals(shiftedWidget1.width, widget1.width);
            assertEquals(shiftedWidget1.height, widget1.height);
            assertEquals(shiftedWidget1.updatedAt, widget1.updatedAt);
            assertEquals(shiftedWidget1.zIndex, widget1.zIndex + 1);

            var shiftedWidget2 = widgets.get(2);
            assertEquals(shiftedWidget2.id, widget2.id);
            assertEquals(shiftedWidget2.x, widget2.x);
            assertEquals(shiftedWidget2.y, widget2.y);
            assertEquals(shiftedWidget2.width, widget2.width);
            assertEquals(shiftedWidget2.height, widget2.height);
            assertEquals(shiftedWidget2.updatedAt, widget2.updatedAt);
            assertEquals(shiftedWidget2.zIndex, widget2.zIndex + 1);

        } catch (ZIndexConflictException e) {
            fail("zIndex conflict exception was thrown", e);
        }
    }
}
