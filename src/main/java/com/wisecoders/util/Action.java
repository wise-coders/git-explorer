package com.wisecoders.util;



import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Action {
    String name() default "";

    String enabledProperty() default "";

    String disabledProperty() default "";

    String selectedProperty() default "";

    String taskService() default "default";

    public @interface Parameter {
        String value() default "";
    }
}