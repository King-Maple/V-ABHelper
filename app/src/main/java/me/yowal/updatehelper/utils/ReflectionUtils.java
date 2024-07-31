package me.yowal.updatehelper.utils;

import android.annotation.SuppressLint;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import androidx.core.util.Preconditions;

@SuppressLint("RestrictedApi")
public class ReflectionUtils<T> {
    private Class<T> clazz;

    private ReflectionUtils(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static ReflectionUtils<?> on(String name) {
        return new ReflectionUtils(findClass(name));
    }

    public static ReflectionUtils<?> on(String name, ClassLoader loader) {
        return new ReflectionUtils(findClass(name, loader));
    }

    @SuppressLint("RestrictedApi")
    public static <T> ReflectionUtils<T> on(Class<T> clazz) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        return new ReflectionUtils(clazz);
    }


    public Class<T> unwrap() {
        return clazz;
    }

    public static MethodWrapper wrap(Method method) {
        Preconditions.checkNotNull(method, "method == null");
        return new MethodWrapper(method);
    }

    public MethodWrapper method(String name, Class<?>... parameterTypes) {
        return method(clazz, name, parameterTypes);
    }

    public static MethodWrapper method(String className, String name, Class<?>... parameterTypes) {
        return method(findClass(className), name, parameterTypes);
    }

    public static MethodWrapper method(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return wrap(getMethod(clazz, name, parameterTypes));
    }


    public static FieldWrapper wrap(Field field) {
        Preconditions.checkNotNull(field, "field == null");
        return new FieldWrapper(field);
    }

    public FieldWrapper field(String name) {
        return field(clazz, name);
    }

    public static FieldWrapper field(String className, String name) {
        return field(findClass(className), name);
    }

    public static FieldWrapper field(Class<?> clazz, String name) {
        return wrap(getField(clazz, name));
    }

    public static <T> ConstructorWrapper<T> wrap(Constructor<T> constructor) {
        Preconditions.checkNotNull(constructor, "constructor == null");
        return new ConstructorWrapper<>(constructor);
    }

    public ConstructorWrapper<T> constructor(Class<?>... parameterTypes) {
        return wrap(getConstructor(clazz, parameterTypes));
    }

    public T[] array(int length) {
        return (T[]) Array.newInstance(clazz, length);
    }


    public static Class<?> findClassOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }


    public static Class<?> findClassOrNull(String name, ClassLoader loader) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }


    public static Class<?> findClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + name + " not found", e);
        }
    }


    public static Class<?> findClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("No class " + name + " found in classloader " + loader, e);
        }
    }


    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Method method = findMethod(clazz, name, parameterTypes);
        if (method == null) {
            throw new RuntimeException("No method '" + name + getParameterTypesMessage(parameterTypes) + "' found in class " + clazz.getName());
        }
        return method;
    }

    private static String getParameterTypesMessage(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        boolean isFirst = true;
        for (Class<?> type : parameterTypes) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(type.getName());
        }
        return sb.append(')').toString();
    }


    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        checkForFindMethod(clazz, name, parameterTypes);
        return findMethodNoChecks(clazz, name, parameterTypes);
    }


    public static Method findMethodNoChecks(Class<?> clazz, String name, Class<?>... parameterTypes) {
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void checkForFindMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        Preconditions.checkStringNotEmpty(name, "name is null or empty");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == null) {
                    throw new NullPointerException("parameterTypes[" + i + "] == null");
                }
            }
        }

    }


    public static Field getField(Class<?> clazz, String name) {
        Field field = findField(clazz, name);
        if (field == null) {
            throw new RuntimeException("No field '" + name + "' found in class " + clazz.getName());
        }
        return field;
    }


    public static Field findField(Class<?> clazz, String name) {
        checkForFindField(clazz, name);
        return findFieldNoChecks(clazz, name);
    }


    public static Field findFieldNoChecks(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void checkForFindField(Class<?> clazz, String name) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        Preconditions.checkStringNotEmpty(name, "name is null or empty");
    }


    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        Constructor<T> c = findConstructor(clazz, parameterTypes);
        if (c == null) {
            throw new RuntimeException("No constructor '" + clazz.getName() + getParameterTypesMessage(parameterTypes) + "' found in class " + clazz.getName());
        }
        return c;
    }


    public static <T> Constructor<T> findConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        checkForFindConstructor(clazz, parameterTypes);
        return findConstructorNoChecks(clazz, parameterTypes);
    }


    public static <T> Constructor<T> findConstructorNoChecks(Class<T> clazz, Class<?>... parameterTypes) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    private static void checkForFindConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Preconditions.checkNotNull(clazz, "clazz == null");
        if (parameterTypes != null) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == null) {
                    throw new NullPointerException("parameterTypes[" + i + "] == null");
                }
            }
        }
    }

    public boolean isInstance(Object instance) {
        return clazz.isInstance(instance);
    }

    public int getModifiers() {
        return clazz.getModifiers();
    }

    public boolean isLambdaClass() {
        return isLambdaClass(clazz);
    }

    public boolean isProxyClass() {
        return isProxyClass(clazz);
    }

    public T proxy(InvocationHandler h) {
        return (T) Proxy.newProxyInstance(h.getClass().getClassLoader(), new Class[]{clazz}, h);
    }

    public static boolean isLambdaClass(Class<?> clazz) {
        return clazz.getName().contains("$$Lambda$");
    }

    public static boolean isProxyClass(Class<?> clazz) {
        return Proxy.isProxyClass(clazz);
    }

    public static <T extends Throwable> void throwUnchecked(Throwable e) throws T {
        throw (T) e;
    }

    public static class MemberWrapper<M extends AccessibleObject & Member> {
        M member;
        MemberWrapper(M member) {
            member.setAccessible(true);
            this.member = member;
        }

        public final M unwrap() {
            return member;
        }

        public final int getModifiers() {
            return member.getModifiers();
        }

        public final Class<?> getDeclaringClass() {
            return member.getDeclaringClass();
        }
    }

    public static class MethodWrapper extends MemberWrapper<Method> {
        MethodWrapper(Method method) {
            super(method);
        }

        public <T> T call(Object instance, Object... args) {
            try {
                return (T) member.invoke(instance, args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T callStatic(Object... args) {
            return call(null, args);
        }
    }

    public static class FieldWrapper extends MemberWrapper<Field> {
        FieldWrapper(Field field) {
            super(field);
        }

        public <T> T getValue(Object instance) {
            try {
                return (T) member.get(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public <T> T getStaticValue() {
            return getValue(null);
        }

        public void setValue(Object instance, Object value) {
            try {
                member.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public void setStaticValue(Object value) {
            setValue(null, value);
        }

        public Class<?> getType() {
            return member.getType();
        }
    }

    public static class ConstructorWrapper<T> extends MemberWrapper<Constructor<T>> {
        ConstructorWrapper(Constructor<T> constructor) {
            super(constructor);
        }

        public T newInstance(Object... args) {
            try {
                return member.newInstance(args);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
