package trespass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Provides static utility methods to gain easy access to classes, methods or fields
 * that would normally be inaccessible due to their defined visibility, via Java's Reflection API.
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
public final class ReflectionUtils {

   // disallow construction
   private ReflectionUtils() {}

   public static Field getInternalField(final Class clazz, final String fieldName) {
      try {
         final Field f = clazz.getDeclaredField(fieldName);
         // override its access permission so we can get its value here even if its private
         f.setAccessible(true);
         return f;
      } catch (Exception ex) {
         ex.printStackTrace();
         throw new RuntimeException(ex);
      }
   }

   public static Method getInternalMethod(final Class<?> clazz, final String methodName, final Class... args) {
      try {
         final Method m = clazz.getDeclaredMethod(methodName, args);
         // override its access permission so we can invoke it here even if its private
         m.setAccessible(true);
         return m;
      } catch (NoSuchMethodException nsm) {
         if (clazz == Object.class) {
            throw new RuntimeException(nsm);
         }
         return getInternalMethod(clazz.getSuperclass(), methodName, args);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static <T> Constructor<T> getInternalConstructor(final Class<T> clazz,
                                                           final Class... args) {
      try {
         final Constructor<T> c = clazz.getDeclaredConstructor(args);
         // override its access permission so we can invoke it here even if its private
         c.setAccessible(true);
         return c;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static Class getClass(final String fullname) {
      try {
         return Class.forName(fullname, false, Thread.currentThread().getContextClassLoader());
      } catch (final ClassNotFoundException ex) {
         throw new RuntimeException(ex);
      }
   }

   public static Object get(final Field staticField) {
      return get(staticField, null);
   }

   public static Object get(final Field field, final Object obj) {
      try {
         return field.get(obj);
      } catch (final Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static void set(final Field staticField, final Object value) {
      set(staticField, null, value);
   }

   public static void set(final Field field, final Object obj, final Object value) {
      try {
         field.set(obj, value);
      } catch (final Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static Class[] getTypes(final Object[] args) {
      final Class[] types = args != null ? new Class[args.length] : null;
      if (args != null) {
         for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
         }
      }
      return types;
   }
}