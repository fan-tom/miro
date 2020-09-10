package org.fantom.repositories.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Interface for widget repositories
 * Generates id on widget add.
 * Enforces id and zIndex uniqueness.
 * @param <ID> Type of widget id
 */
public interface WidgetRepository<ID> {

    /**
     * Add one widget to storage
     * @param widget widget dto to create widget from and save
     * @return new widget with generated id
     * @throws ZIndexConflictException when there already exists widget with the same zIndex
     */
    Widget<ID> add(WidgetCreateDto widget) throws ZIndexConflictException;

    /**
     * Add multiple widgets to storage
     * @param widgets iterable of widget dto to create widgets from and save
     * @return new widgets with generated ids
     * @throws ZIndexConflictException when there already exists widget with the same zIndex
     */
    Stream<Widget<ID>> add(Iterable<WidgetCreateDto> widgets) throws ZIndexConflictException;

    /**
     * Update given widget, is exists
     * @param widget to update
     * @return saved widget, or none if no such widget
     */
    Optional<Widget<ID>> save(Widget<ID> widget) throws ZIndexConflictException;

    /**
     * Update given widgets
     * @param widgets to update
     */
    void save(Iterable<Widget<ID>> widgets) throws ZIndexConflictException;

    /**
     * Get widget by it's id
     * @param id of widget to find
     * @return widget with given id or none. Note, that this method may return different java objects(Widget)
     * even when called with the same id, so, compare widgets only by their ids.
     */
    Optional<Widget<ID>> getById(ID id);

    /**
     * Delete widget by it's id
     * @param id of widget to delete
     * @return Optional with deleted widget or none if widget with given id doesn't exist
     */
    Optional<Widget<ID>> deleteAndReturnById(ID id);

    /**
     * Delete widget by it's id
     * @param id of widget to delete
     * @return flag whether widget with given id existed before delete or not.
     * You may use this method to avoid deserialization and network overhead when you don't need deleted object.
     * Also, it may help to avoid double lookup in some implementations, comparing to {@link #deleteAndReturnById }
     */
    boolean deleteById(ID id);

    /**
     * Get all widgets, sorted by zIndex asc
     * @return stream over all widgets
     */
    Stream<Widget<ID>> getAll();

    /**
     * @return max zIndex among all widgets or none if repository is empty
     */
    Optional<Integer> getMaxZIndex();

    /**
     * Shift widget with given zIndex upwards. If there is no room, shift overlying widgets too
     * @param zIndex to free
     * @exception ArithmeticException if there is no room to shift existing widgets into
     */
    void shiftUpwards(Integer zIndex) throws ArithmeticException;

    /**
     * Run function under lock of repository
     * Implementations may use different ways to provide atomicity, like locks or transactions
     * @param action function to perform atomically. Isolation level must be equal to Serializable, in other words,
     * the set of entities in repository, read at the beginning of transaction, must be the same when read at the end.
     * @param <T> type of action result
     */
    <T, E extends Exception> T runAtomically(AtomicFunction<WidgetRepository<ID>, T, E> action) throws E;

    /**
     * Run function under lock of repository only for side effects
     * Implementations may use different ways to provide atomicity, like locks or transactions
     * @param action function to perform atomically. Isolation level must be equal to Serializable, in other words,
     * the set of entities in repository, read at the beginning of transaction, must be the same when read at the end.
     */
    <E extends Exception> void runAtomically(AtomicAction<WidgetRepository<ID>, E> action) throws E;
}
