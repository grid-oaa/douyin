package com.douyin.liverecorder.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 抖音号验证注解
 */
@Documented
@Constraint(validatedBy = DouyinIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDouyinId {
    
    String message() default "抖音号格式无效，请输入有效的抖音号（字母和数字组合）";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
