package com.sectrend.buildscan.factory;


import com.sectrend.buildscan.model.DetectProperties;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * 根据构建器类型对参数分发
 *
 * @ClassName OptionFactory
 * @Author Jimmy
 * @Date 1/15/25 17:45
 */
public class OptionOperation {

    public static Properties getProperties(String type, DetectProperties detectProperties) {
        if ("goMod".equals(type)) {
            type = "go";
        }
        if ("mvn".equals(type)) {
            type = "maven";
        }
        if ("composer".equals(type)) {
            type = "packagist";
        }
        if ("rubygems".equals(type)) {
            type = "ruby";
        }

        Properties result = null;
        try {
            Class<?> clazz = detectProperties.getClass();

            Field declaredField = clazz.getDeclaredField(type + "Argument");

            declaredField.setAccessible(true);

            result = (Properties) declaredField.get(detectProperties);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            // TODO 异常处理
            e.printStackTrace();
        }
        return result;
    }


    public static DetectProperties setProperties(String type, Properties properties, DetectProperties target) {
        try {
            Class<?> clazz = target.getClass();

            Field declaredField = clazz.getDeclaredField(type + "Argument");

            declaredField.setAccessible(true);

            declaredField.set(target, properties);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            return target;
        }
    }


}
