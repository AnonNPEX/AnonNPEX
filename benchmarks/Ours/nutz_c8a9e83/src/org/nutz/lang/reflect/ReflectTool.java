package org.nutz.lang.reflect;

import org.nutz.lang.Lang;

import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ReflectTool {

    private static Method DEFINE_CLASS;
    private static final ProtectionDomain PROTECTION_DOMAIN;

    static {
        PROTECTION_DOMAIN = getProtectionDomain(ReflectTool.class);

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    Class loader = Class.forName("java.lang.ClassLoader"); // JVM
                                                                           // crash
                                                                           // w/o
                                                                           // this
                    DEFINE_CLASS = loader.getDeclaredMethod("defineClass",
                                                            new Class[]{String.class,
                                                                        byte[].class,
                                                                        Integer.TYPE,
                                                                        Integer.TYPE,
                                                                        ProtectionDomain.class});
                    DEFINE_CLASS.setAccessible(true);
                }
                catch (ClassNotFoundException e) {
                    // Lang.impossible();
                }
                catch (NoSuchMethodException e) {
                    // Lang.impossible();
                }
                return null;
            }
        });
    }

    public static ProtectionDomain getProtectionDomain(final Class source) {
        if (source == null) {
            return null;
        }
        return (ProtectionDomain) AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                return source.getProtectionDomain();
            }
        });
    }

    public static Class defineClass(String className, byte[] b, ClassLoader loader)
            throws Exception {
        return defineClass(className, b, loader, PROTECTION_DOMAIN);
    }

    public static Class defineClass(String className,
                                    byte[] b,
                                    ClassLoader loader,
                                    ProtectionDomain protectionDomain) throws Exception {
        Object[] args = new Object[]{className,
                                     b,
                                     new Integer(0),
                                     new Integer(b.length),
                                     protectionDomain};
        if (loader == null)
            loader = ReflectTool.class.getClassLoader();
        Class c = (Class) DEFINE_CLASS.invoke(loader, args);
        // Force static initializers to run.
        Class.forName(className, true, loader);
        return c;
    }

    /**
     * ?????????clazz,field???????????????class??????.
     * @param clazz ??????class
     * @param field ??????
     * @return
     */
    public static Class<?> getGenericFieldType(Class<?> clazz, Field field) {
        Type fieldType = field.getGenericType();
        return getRealGenericClass(clazz, fieldType);
    }


    /**
     * ????????????????????????????????????.?????? Map<String, E>?????????E???????????????.
     * @param clazz ????????????.
     * @param type ????????????????????????type
     * @return
     */
    public static Class<?> getParameterRealGenericClass(Class<?> clazz, Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            return getRealGenericClass(clazz, actualTypeArguments[index]);
        }
        return Object.class;
    }



    /**
     * ??????????????????type?????????????????????class
     * @param clazz ????????????.
     * @param type ????????????????????????type
     * @return
     */
    public static Class<?> getRealGenericClass(Class<?> clazz, Type type) {
        if(type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            Type genericFieldType = getInheritGenericType(clazz, tv);
            if (genericFieldType != null) {
                return Lang.getTypeClass(genericFieldType);
            }
        }
        return Lang.getTypeClass(type);
    }

    /**
     * ??????????????????type???????????????Type
     * @param clazz
     * @param type
     * @return
     */
    public static Type getInheritGenericType(Class<?> clazz, Type type) {
        if(type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            return getInheritGenericType(clazz, tv);
        }else {
            return type;
        }
    }

    /**
     * ???????????????clazz?????????TypeVariable???????????????Type.
     * @param clazz
     * @param tv
     * @return
     */
    public static Type getInheritGenericType(Class<?> clazz, TypeVariable<?> tv) {
        Type type = null;
        GenericDeclaration gd = tv.getGenericDeclaration();

        do {
            type = clazz.getGenericSuperclass();
            if (type == null) {
                return null;
            }
            if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) type;

                Type rawType = ptype.getRawType();
                boolean eq = gd.equals(rawType) || (gd instanceof Class && rawType instanceof Class && ((Class) gd).isAssignableFrom((Class) rawType));
                if (eq) {
                    TypeVariable<?>[] tvs = gd.getTypeParameters();
                    Type[] types = ptype.getActualTypeArguments();
                    for (int i = 0; i < tvs.length; i++) {
                        if (tv.equals(tvs[i])) {
                            return types[i];
                        }
                    }
                    return null;
                }
            }
            clazz = Lang.getTypeClass(type);
        } while (type != null);
        return null;
    }
}
