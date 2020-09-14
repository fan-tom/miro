package org.fantom.web.repositories.widget.dao;

import org.fantom.domain.Widget;
import org.fantom.repositories.widget.dto.WidgetCreateDto;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="widgets")
public class WidgetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(insertable = false, updatable = false)
    public Long id;

    /**
     * left-bottom vertex x-coordinate
     */
    @Column(nullable=false)
    public int lbx;

    /**
     * left-bottom vertex y-coordinate
     */
    @Column(nullable=false)
    public int lby;

    @Column(nullable=false)
    public int zIndex;

    /**
     * right-top vertex x-coordinate
     */
    @Column(nullable=false)
    public int rtx;

    /**
     * right-top vertex y-coordinate
     */
    @Column(nullable=false)
    public int rty;

    @LastModifiedDate
    public Date updatedAt;

    public WidgetEntity() {

    }

    public WidgetEntity(Long id, int lbx, int lby, Widget<Long> widget, int rtx, int rty) {
        this.id = id;
        this.lbx = lbx;
        this.lby = lby;
        this.rtx = rtx;
        this.rty = rty;
        this.zIndex = widget.zIndex;
        this.updatedAt = widget.updatedAt;
    }

    public WidgetEntity(Widget<Long> widget) {
        this.id = widget.id;
        this.lbx = widget.x;
        this.lby = widget.y;
        this.rtx = widget.x + widget.width;
        this.rty = widget.y + widget.height;
        this.zIndex = widget.zIndex;
        this.updatedAt = widget.updatedAt;
    }

    public WidgetEntity(WidgetCreateDto widget) {
        this.lbx = widget.x;
        this.lby = widget.y;
        this.rtx = widget.x + widget.width;
        this.rty = widget.y + widget.height;
        this.zIndex = widget.zIndex;
        this.updatedAt = widget.updatedAt;
    }

    public Widget<Long> toWidget() {
        return new Widget<>(id, lbx, lby, zIndex, rtx - lbx, rty - lby, updatedAt);
    }
}
