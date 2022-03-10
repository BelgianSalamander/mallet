package me.salamander.mallet.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Layout {
    int location() default -1;
    int index() default -1;
    int component() default -1;
    int binding() default -1;

    int local_size_x() default -1;
    int local_size_y() default -1;
    int local_size_z() default -1;
}
