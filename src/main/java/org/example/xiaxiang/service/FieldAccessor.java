package org.example.xiaxiang.service;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用字段路径读写器
 * 支持路径格式：   buildings[0].coverImage
 *                locations[3].modelKey
 *                photoCompares[1].oldImageKey
 *
 * 说明：
 * - rootObj 必须有对应属性的 getter/setter（由 Lombok 生成即可）
 * - 列表属性按 index 取对象，然后按字段名取 setXxx/getXxx
 * - 首字母大小写在 setter/getter 中被处理（coverImage → getCoverImage）
 */
@Slf4j
public class FieldAccessor {

    private static final Pattern PATH = Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\[(\\d+)\\]\\.([a-zA-Z_][a-zA-Z0-9_]*)$");

    public static String readField(Object root, String yamlPath) throws Exception {
        Path p = parse(yamlPath);
        List<?> list = (List<?>) getter(root, p.listField);
        if (list == null || p.index >= list.size()) return null;
        Object item = list.get(p.index);
        if (item == null) return null;
        Object v = getter(item, p.scalarField);
        return v == null ? null : v.toString();
    }

    public static void writeField(Object root, String yamlPath, String value) throws Exception {
        Path p = parse(yamlPath);
        List<Object> list = (List<Object>) getter(root, p.listField);
        if (list == null) {
            throw new IllegalStateException("列表属性不存在: " + p.listField);
        }
        if (p.index >= list.size()) {
            throw new IllegalStateException(String.format(
                    "索引越界: %s[%d] 实际大小=%d", p.listField, p.index, list.size()));
        }
        Object item = list.get(p.index);
        setter(item, p.scalarField, value);
    }

    private static Path parse(String yamlPath) {
        Matcher m = PATH.matcher(yamlPath);
        if (!m.matches()) {
            throw new IllegalArgumentException("路径格式错误，应为 foo[123].bar 实际: " + yamlPath);
        }
        Path p = new Path();
        p.listField = m.group(1);
        p.index = Integer.parseInt(m.group(2));
        p.scalarField = m.group(3);
        return p;
    }

    private static Object getter(Object target, String fieldName) throws Exception {
        Class<?> c = target.getClass();
        String getter = "get" + capitalize(fieldName);
        Method m;
        try {
            m = c.getMethod(getter);
        } catch (NoSuchMethodException e) {
            // 试试 boolean 风格的 isXxx
            String isGetter = "is" + capitalize(fieldName);
            try {
                m = c.getMethod(isGetter);
            } catch (NoSuchMethodException e2) {
                // 最后 fallback 反射字段
                Field f = findField(c, fieldName);
                f.setAccessible(true);
                return f.get(target);
            }
        }
        return m.invoke(target);
    }

    private static void setter(Object target, String fieldName, Object value) throws Exception {
        Class<?> c = target.getClass();
        String setter = "set" + capitalize(fieldName);
        // 找参数类型匹配的方法：优先 String，再 Object
        Method m = null;
        for (Method method : c.getMethods()) {
            if (method.getName().equals(setter) && method.getParameterCount() == 1) {
                m = method;
                break;
            }
        }
        if (m == null) {
            Field f = findField(c, fieldName);
            f.setAccessible(true);
            f.set(target, value);
            return;
        }
        Class<?> pt = m.getParameterTypes()[0];
        Object casted = castTo(value, pt);
        m.invoke(target, casted);
    }

    private static Object castTo(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isAssignableFrom(value.getClass())) return value;
        if (targetType == String.class) return value.toString();
        return value;
    }

    private static Field findField(Class<?> c, String name) throws NoSuchFieldException {
        while (c != null && c != Object.class) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static class Path {
        String listField;
        int index;
        String scalarField;
    }
}
