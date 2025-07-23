package com.sectrend.buildscan.annotation.detect.business.param;

import java.lang.annotation.*;

/**
 *  <p>构建器动态参数元信息,用来给参数进行信息补充归类</p>
 * @since 20250218
 * @author yue.geng
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamMeta {

    /**构建器动态参数名称*/
    String attrName() default "";

    /**构建器参数归属*/
    String category() ;

    /**构建器动态参数名称*/
    String attrDesc() default "";


}
