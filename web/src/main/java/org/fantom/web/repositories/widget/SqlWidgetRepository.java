package org.fantom.web.repositories.widget;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.AtomicAction;
import org.fantom.repositories.widget.AtomicFunction;
import org.fantom.repositories.widget.WidgetRepository;
import org.fantom.repositories.widget.dto.Area;
import org.fantom.repositories.widget.dto.WidgetCreateDto;
import org.fantom.repositories.widget.exceptions.ZIndexConflictException;
import org.fantom.web.repositories.widget.dao.WidgetEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Lazy
@Repository
interface InternalSqlWidgetRepository extends PagingAndSortingRepository<WidgetEntity, Long> {
    @Query("select w from WidgetEntity w where w.lbx >= :x1 and w.lby >= :y1 and w.rtx <= :x2 and w.rty <= :y2")
    List<WidgetEntity> getInArea(@Param("x1") int x1, @Param("y1") int y1, @Param("x2") int x2, @Param("y2") int y2);

    @Query("select max(w.zIndex) from WidgetEntity w")
    Optional<Integer> getMaxZIndex();

    @Modifying
    @Query("delete from WidgetEntity w where w.id = :id")
    WidgetEntity removeByIdReturning(@Param("id") Long id);

    int removeById(Long id);

    @Modifying
    @Query(value = "update widgets w set w.z_index = w.z_index + 1 where w.id in (select id from (" +
            " select id, max(coalesce(lag, 100500)) over(order by z_index) as maxlag " +
            " from (" +
            "  select id," +
            "    z_index," +
            "    z_index - coalesce(lag(z_index) over (order by z_index), case when z_index = :zIndex then 0 else null end) as lag " +
            "  from widgets " +
            "  where z_index >= :zIndex" +
            "  ) as lags " +
            " ) as maxlags " +
            " where maxlag <= 1" +
            ")",
            nativeQuery = true
    )
    void shiftUpwards(@Param("zIndex") Integer zIndex);
}

@Lazy
@Repository
public class SqlWidgetRepository implements WidgetRepository<Long> {

    @Lazy
    @Autowired
    InternalSqlWidgetRepository internal;

    @Autowired
    TransactionTemplate transactionTemplate;

    protected <T> T convertToZIndexConflict(DataIntegrityViolationException e, Integer zIndex) throws DataIntegrityViolationException, ZIndexConflictException {
        var cause = e.getCause();
        if (cause instanceof ConstraintViolationException) {
            if (((ConstraintViolationException) cause).getConstraintName().toLowerCase().contains("z_index_unique")) {
                throw new ZIndexConflictException(zIndex);
            }
        }
        throw e;
    }

    @Override
    public Widget<Long> add(WidgetCreateDto widget) throws ZIndexConflictException {
        try {
            return transactionTemplate.execute(status -> {
                var saved = internal.save(new WidgetEntity(widget));
                return saved.toWidget();
            });
        } catch (DataIntegrityViolationException e) {
            return convertToZIndexConflict(e, widget.zIndex);
        }
    }

    @Override
    public List<Widget<Long>> add(Iterable<WidgetCreateDto> widgets) throws ZIndexConflictException {
        try {
            return transactionTemplate.execute(status -> {
                var saved = internal.saveAll(StreamSupport
                        .stream(widgets.spliterator(), false)
                        .map(WidgetEntity::new)::iterator
                );
                return StreamSupport
                        .stream(saved.spliterator(), false)
                        .map(WidgetEntity::toWidget)
                        .collect(Collectors.toList());
            });
        } catch (DataIntegrityViolationException e) {
            return convertToZIndexConflict(e, null);
        }
    }

    @Override
    public Optional<Widget<Long>> save(Widget<Long> widget) throws ZIndexConflictException {
        try {
            return transactionTemplate.execute(status -> {
                if (internal.existsById(widget.id)) {
                    return Optional.of(internal.save(new WidgetEntity(widget)).toWidget());
                } else {
                    return Optional.empty();
                }
            });
        } catch (DataIntegrityViolationException e) {
            return convertToZIndexConflict(e, null);
        }
    }

    @Override
    public void save(Iterable<Widget<Long>> widgets) throws ZIndexConflictException {
        try {
            transactionTemplate.executeWithoutResult(status -> StreamSupport
                    .stream(widgets.spliterator(), false)
                    .map(WidgetEntity::new)
                    .forEach(w -> {
                        if (internal.existsById(w.id)) {
                            internal.save(w);
                        }
                    })
            );
        } catch (DataIntegrityViolationException e) {
            convertToZIndexConflict(e, null);
        }
    }

    @Override
    @Transactional
    public Optional<Widget<Long>> getById(Long id) {
        return internal.findById(id).map(WidgetEntity::toWidget);
    }

    @Override
    @Transactional
    public List<Widget<Long>> getAll() {
        return StreamSupport
                .stream(internal.findAll(Sort.by("zIndex")).spliterator(), false)
                .map(WidgetEntity::toWidget)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Widget<Long>> getInArea(Area area) {
        return internal
                .getInArea(area.left, area.bottom, area.right, area.top)
                .stream()
                .map(WidgetEntity::toWidget)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Optional<Widget<Long>> deleteAndReturnById(Long id) {
        return Optional.ofNullable(internal.removeByIdReturning(id)).map(WidgetEntity::toWidget);
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return internal.removeById(id) > 0;
    }

    @Override
    @Transactional
    public void deleteAll() {
        internal.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> getMaxZIndex() {
        return internal.getMaxZIndex();
    }

    @Override
    @Transactional
    public void shiftUpwards(Integer zIndex) throws ArithmeticException {
        internal.shiftUpwards(zIndex);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public <T, E extends Exception> T runAtomically(AtomicFunction<WidgetRepository<Long>, T, E> action) throws E {
        return action.run(this);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public <E extends Exception> void runAtomically(AtomicAction<WidgetRepository<Long>, E> action) throws E {
            action.run(this);
    }
}
