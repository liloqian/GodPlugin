package com.leo.buildsrc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by qian on 2020-04-04
 * Describe:
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Time {
}
