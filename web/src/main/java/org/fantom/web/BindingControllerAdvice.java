package org.fantom.web;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
class BindingControllerAdvice {
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // let spring create immutable dtos
        binder.initDirectFieldAccess();
    }
}
