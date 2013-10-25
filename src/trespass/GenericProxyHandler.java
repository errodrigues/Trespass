package trespass;

import trespass.annotation.Cast;
import trespass.annotation.DefaultInstanceProvider;
import trespass.annotation.ProxyField;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * This class implements the {@link InvocationHandler} that will be used by any dynamic
 * proxy created by {@link ProxyFactory}.
 *
 * @param <T> The target type encapsulated by the proxy
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 * @see ProxyFactory
 */
final class GenericProxyHandler<T> implements InvocationHandler
{
   private final Class<T> targetClass;
   private final T targetObject;

   GenericProxyHandler(final Class<T> targetClass, final Method targetInstanceProvider,
                       final Object... instanceProviderParams) throws Exception
   {
      this.targetClass = targetClass;
      if (targetInstanceProvider != null)
      {
         this.targetObject = invokeInstanceProvider(targetInstanceProvider,
                                                    instanceProviderParams);
      }
      else
      {
         this.targetObject = null;
      }
   }

   GenericProxyHandler(final T targetObject)
   {
      this.targetObject = targetObject;
      this.targetClass = (Class<T>)targetObject.getClass();
   }

   public Object invoke(final Object proxy, final Method method, final Object[] args)
   {
      try
      {
         if (InternalProxy.PROXY_INST_GETTER.equals(method.getName()) && args == null)
         {
            return targetObject;
         }
         if (method.isAnnotationPresent(DefaultInstanceProvider.class))
         {
            return invokeInstanceProvider(method, args);
         }
         if (method.isAnnotationPresent(ProxyField.class))
         {
            return accessTargetField(method, args);
         }
         else
         {
            return invokeTargetMethod(method, args);
         }
      }
      catch(final RuntimeException rte)
      {
         throw rte;
      }
      catch(final Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   private T invokeInstanceProvider(final Method method, final Object[] args)
      throws Exception
   {
      final Class[] paramTypes = getTargetParamTypes(method, this.getClass().getClassLoader());
      try
      {
         final String methodName = method.getName();
         final Method m = targetClass.getDeclaredMethod(methodName, paramTypes);
         m.setAccessible(true);
         return (T)m.invoke(null, args);
      }
      catch(final NoSuchMethodException ex)
      {
         try
         {
            final Constructor<T> c = targetClass.getDeclaredConstructor(paramTypes);
            c.setAccessible(true);
            return c.newInstance(args);
         }
         catch(final NoSuchMethodException constructorEx)
         {
            return targetClass.newInstance();
         }
      }
   }

   private Object invokeTargetMethod(final Method method, final Object[] args)
      throws Exception
   {
      final String methodName = method.getName();
      final Class[] paramTypes = getTargetParamTypes(method, this.getClass().getClassLoader());
      final Method m = targetClass.getDeclaredMethod(methodName, paramTypes);
      m.setAccessible(true);
      return m.invoke(targetObject, args);
   }

   private Object accessTargetField(final Method method, final Object[] args)
      throws Exception
   {
      String fieldName = ProxyFactory.getProxyFieldName(method);
      Field field = null;
      try
      {
         field = targetClass.getDeclaredField(fieldName);
      }
      catch(final NoSuchFieldException ex)
      {
         fieldName = fieldName.substring(0, 1).toUpperCase() +
                     (fieldName.length() > 1 ? fieldName.substring(1) : "");
         field = targetClass.getDeclaredField(fieldName);
      }
      field.setAccessible(true);
      if (args == null)
      {
         return field.get(targetObject);
      }
      else
      {
         field.set(targetObject, args[0]);
         return null;
      }
   }

   static Class[] getTargetParamTypes(final Method method, final ClassLoader loader)
      throws ClassNotFoundException
   {
      final Class[] params = method.getParameterTypes();
      if (params.length > 0)
      {
         final Annotation[][] casts = method.getParameterAnnotations();
         for (int i = 0; i < params.length; i++)
         {
            if (casts[i].length == 1 && casts[i][0].annotationType() == Cast.class)
            {
               final Cast cast = (Cast)casts[i][0];
               params[i] = Class.forName(cast.value(), false, loader);
            }
         }
      }
      return params;
   }

}
   
