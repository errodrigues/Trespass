package trespass;

import trespass.annotation.DefaultInstanceProvider;
import trespass.annotation.ProxyClass;
import trespass.annotation.ProxyField;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides static factory methods to instantiate proxy objects according with properly annotated InternalProxy interfaces.
 *
 * @see InternalProxy
 * @see trespass.annotation
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
public final class ProxyFactory
{
   private static final String REGEX_CLEAN_ARRAY_TOSTRING = "[\\Q[\\E\\Q]\\E]|class |interface ";
   private static final Map<Class<?>, ProxyWrapper<?,? extends InternalProxy>> validProxies =
      new HashMap<Class<?>, ProxyWrapper<?,? extends InternalProxy>>();

   // disallow construction
   private ProxyFactory() {}

   private static <T, P extends InternalProxy<T>> Class<T> validateTargetClass(final Class<P> proxy, final ClassLoader loader)
      throws MissingAnnotationException, ClassNotFoundException
   {
      final ProxyClass proxyClass = proxy.getAnnotation(ProxyClass.class);
      if (proxyClass == null)
      {
         throw new MissingAnnotationException("Mandatory annotation " +
                                              ProxyClass.class.getSimpleName() +
                                              " is missing on " + proxy.getName());
      }
      final Class<T> c = (Class<T>)proxyClass.value();
      if (!c.equals(ProxyClass.class))
      {
         return c;
      }
      final String className = proxyClass.targetClassName();
      if (!className.trim().isEmpty())
      {
         return (Class<T>)Class.forName(proxyClass.targetClassName(), false, loader);
      }
      throw new IllegalArgumentException("Invalid target class");
   }

   private static <T, P extends InternalProxy<T>> ProxyWrapper<T,P> validateProxyInterface(
      final Class<P> proxy, final ClassLoader loader)
      throws NoSuchMethodException, NoSuchFieldException, InvalidSignatureException,
             ClassNotFoundException, MissingAnnotationException
   {
      final Class<P> proxyClass = (Class<P>)Proxy.getProxyClass(loader, proxy);
      ProxyWrapper<T,P> info = (ProxyWrapper<T,P>)validProxies.get(proxyClass);
      if (info == null)
      {
         Method instanceProvider = null;
         final Class<T> target = validateTargetClass(proxy, loader);
         final Method[] proxyMethods = proxy.getMethods();

         for (Method m : proxyMethods)
         {
            final Class[] proxyParams = m.getParameterTypes();
            if (InternalProxy.PROXY_INST_GETTER.equals(m.getName()) && proxyParams.length == 0 && m.getReturnType().isAssignableFrom(target))
            {
               continue;
            }
            if (instanceProvider == null &&
                m.isAnnotationPresent(DefaultInstanceProvider.class))
            {
               instanceProvider = m;
               validateProxyInstanceProvider(proxy, m, proxyParams, target, loader);
            }
            else if (m.isAnnotationPresent(ProxyField.class))
            {
               final String fieldName = getProxyFieldName(m);
               validateFieldProxy(proxy, m, proxyParams, target, fieldName, loader);
            }
            else
            {
               validateProxyMethod(proxy, m, proxyParams, target, loader);
            }
         }
         info = new ProxyWrapper<T,P>(proxyClass, target, instanceProvider);
         validProxies.put(proxyClass, info);
      }
      return info;
   }
   
   private static void validateProxyMethod(final Class<? extends InternalProxy> proxy,
                                           final Method proxyMethod,
                                           final Class[] proxyParams,
                                           final Class<?> target,
                                           final ClassLoader loader)
      throws NoSuchMethodException, NoSuchFieldException, ClassNotFoundException
   {
      try
      {
         final Class[] paramTypes = GenericProxyHandler.getTargetParamTypes(
            proxyMethod, loader);
         final Method m = target.getDeclaredMethod(proxyMethod.getName(), paramTypes);
         proxyMethod.getReturnType().isAssignableFrom(m.getReturnType());
      }
      catch(final NoSuchMethodException ex)
      {
         final String msg = String.format(
            "Method %s.%s(%s) returning %s doesn't match any method declared in %s",
            proxy.getName(),
            proxyMethod.getName(),
            Arrays.toString(proxyParams).replaceAll(REGEX_CLEAN_ARRAY_TOSTRING, ""),
            proxyMethod.getReturnType().getName(),
            target.getName()
         );
         final NoSuchMethodException newEx = new NoSuchMethodException(msg);
         newEx.initCause(ex);
         throw newEx;
      }
   }

   private static void validateFieldProxy(final Class<? extends InternalProxy> proxy,
                                          final Method proxyMethod,
                                          final Class[] proxyParams,
                                          final Class target,
                                          final String targetField,
                                          final ClassLoader loader)
      throws NoSuchFieldException, InvalidSignatureException, ClassNotFoundException
   {
      try
      {
         final Field field = target.getDeclaredField(targetField);
         final Class[] types = GenericProxyHandler.getTargetParamTypes(proxyMethod, loader);
         final Class<?> result = proxyMethod.getReturnType();
         boolean valid = types.length == 1 && result != null && result.isPrimitive() &&
                         "void".equals(result.getName()) && field.getType().isAssignableFrom(types[0]);
         if (!valid)
         {
            valid = types.length == 0 && result != null &&
                    result.isAssignableFrom(field.getType());
            if (!valid)
            {
               final String msg = String.format(
                  "Method %s.%s(%s) returning %s has a signature that is incompatible with annotation %s",
                  proxy.getName(),
                  proxyMethod.getName(),
                  Arrays.toString(proxyParams).replaceAll(REGEX_CLEAN_ARRAY_TOSTRING, ""),
                  result != null ? result.getName() : "void",
                  ProxyField.class.getName()
               );
               throw new InvalidSignatureException(msg);
            }
         }
      }
      catch(final NoSuchFieldException ex)
      {
         final String msg = String.format(
            "Field %s annotated on %s.%s(%s) is not declared in %s",
            targetField,
            proxy.getName(),
            proxyMethod.getName(),
            Arrays.toString(proxyParams).replaceAll(REGEX_CLEAN_ARRAY_TOSTRING, ""),
            target.getName()
         );
         final NoSuchFieldException newEx = new NoSuchFieldException(msg);
         newEx.initCause(ex);
         throw newEx;
      }
   }

   private static void validateProxyInstanceProvider(final Class<? extends InternalProxy> proxy,
                                                     final Method proxyMethod,
                                                     final Class[] proxyParams,
                                                     final Class<?> target,
                                                     final ClassLoader loader)
      throws NoSuchMethodException
   {
      try
      {
         try
         {
            validateProxyMethod(proxy, proxyMethod, proxyParams, target, loader);
         }
         catch(final NoSuchMethodException ex)
         {
            target.getDeclaredConstructor(proxyParams);
         }
      }
      catch(final Exception ex)
      {
         if (proxyParams.length > 0)
         {
            final String msg = String.format(
               "Method %s.%s(%s) annotated with @%s " +
               "doesn't match any method or constructor declared in %s",
               proxy.getName(),
               proxyMethod,
               Arrays.toString(proxyParams).replaceAll(REGEX_CLEAN_ARRAY_TOSTRING, ""),
               DefaultInstanceProvider.class.getSimpleName(),
               target.getName()
            );
            final NoSuchMethodException newEx = new NoSuchMethodException(msg);
            newEx.initCause(ex);
            throw newEx;
         }
      }
   }

   /**
    * <p>Equivalent to calling {@link #createProxy(Class,ClassLoader,Object...)
    * createProxy(targetClass, proxyIfc, Thread.currentThread().getContextClassLoader(), args)}</p>
    * @param <T> The target type of the proxy interface extending InternalProxy
    * @param <P> Type representing the proxy interface extending InternalProxy
    * which will be implemented by the dynamic proxy to be created
    * @param proxyIfc The interface extending InternalProxy and defining the
    * methods that will match methods or fields declared in the target class
    * @param args Optional arbitrary arguments that will be passed to the
    * method/constructor that is annotated with @DefaultInstanceProvider in the
    * provided proxy interface
    * @return A dynamic proxy that will implement the given proxy interface
    * and automatically forward calls to the target class. If a @DefaultInstanceProvider
    * is defined in the interface, it will be used to obtain an instance of
    * the target class that will be encapsulated by the proxy and then used
    * as the target instance for all forwarded calls.
    */
   public static <T, P extends InternalProxy<T>> P createProxy(
      final Class<P> proxyIfc, final Object... args)
   {
      return createProxy(proxyIfc, Thread.currentThread().getContextClassLoader(),
                         args);
   }

   /**
    * <p>This method is equivalent to {@link #createProxyToObject(Object,Class,ClassLoader)
    * createProxyToObject(target, proxyIfc, Thread.currentThread().getContextClassLoader())}</p>
    *
    * @param <T> The target type of the proxy interface extending InternalProxy
    * @param <P> Type representing the proxy interface extending InternalProxy
    * which will be implemented by the dynamic proxy to be created
    * @param target The instance to use as the proxy's target instance. It will
    * also implicitly define the target class to be the result of target.getClass()
    * @param proxyIfc The interface extending InternalProxy and defining the
    * methods that will match methods or fields declared in the target class
    * @return A dynamic proxy that will implement the given proxy interface
    * and automatically forward calls to the target object, which will be encapsulated by the proxy.
    */
   public static <T, P extends InternalProxy<T>> P createProxyToObject(
      final T target, final Class<P> proxyIfc)
   {
      return createProxyToObject(target, proxyIfc, Thread.currentThread().getContextClassLoader());
   }

   /**
    * <p>Create a dynamic proxy to a target class.</p>
    *
    * <p>The returned proxy will implement the given proxy interface and will
    * automatically forward method calls to corresponding methods in the target class.</p>
    *
    * <p>This is particularly useful for test cases that need/want to gain
    * easy access to methods and/or fields and/or constants in the target class that would not normally be accessible
    * from the caller (private, package protected or protected declarations).</p>
    *
    * <p>To achieve that, the only requirement is to define an interface
    * extending {@link InternalProxy} and declaring method signatures that
    * will match those in the target class.</p>
    *
    * <p>When the goal is to gain access to a field or constant (static or not) declared
    * in the target class, the interface just needs to define an accessor-like
    * method matching the target field's type. If the name of the declared accessor method follows the Java Bean standards
    * ( such as FieldType getFieldNameInCamelCase() or setFieldNameInCamelCase(FieldType value) ) then the mapping will
    * be done automatically. Otherwise, fields and/or constants can still be mapped just by annotating accessor methods
    * declared in the proxy interface with {@link trespass.annotation.ProxyField}.</p>
    *
    * <p>Optionally, the provided proxy interface may also define a method
    * that will either match a regular declared method or constructor in the
    * target class and then annotate it with {@link DefaultInstanceProvider}.
    * By doing so, the returned proxy will encapsulate the target class' instance created
    * forward calls on the proxy interface to this encapsulated instance.</p>
    *
    * <p>This method will do its best to validate the given interface's structure
    * in order to make sure it properly matches its target and fail as early as possible.</p>
    *
    * @param <T> The target type of the proxy interface extending InternalProxy
    * @param <P> Type representing the proxy interface extending InternalProxy
    * which will be implemented by the dynamic proxy to be created
    * @param proxyIfc The interface extending InternalProxy and defining the
    * methods that will match methods or fields declared in the target class
    * @param loader The class loader used to create the dynamic proxy
    * @param args Optional arbitrary arguments that will be passed to the
    * method/constructor that is annotated with @DefaultInstanceProvider in the
    * provided proxy interface
    * @return A dynamic proxy that will implement the given proxy interface
    * and automatically forward calls to the target class. If a @DefaultInstanceProvider
    * is defined in the interface, it will be used to obtain an instance of
    * the target class that will be encapsulated by the proxy and then used
    * as the target instance for all forwarded calls.
    * @see InternalProxy
    * @see DefaultInstanceProvider
    * @see trespass.annotation.ProxyField
    * @see trespass.annotation.Cast
    */
   public static <T, P extends InternalProxy<T>> P createProxy(
      final Class<P> proxyIfc, final ClassLoader loader, final Object... args)
   {
      try
      {
         final ProxyWrapper<T,P> wrapper = validateProxyInterface(proxyIfc, loader);
         final GenericProxyHandler<T> handler = new GenericProxyHandler<T>(
            wrapper.targetClass, wrapper.targetInstanceProvider, args);
         return wrapper.newProxyInstance(handler);
      }
      catch(final RuntimeException ex)
      {
         throw ex;
      }
      catch(final Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   /**
    * <p>This method is equivalent to {@link #createProxy(Class,ClassLoader,Object...)}
    * except that the returned proxy will explicitly use the given target instance
    * instead of trying to obtain one from a method annotated with @DefaultInstanceProvider.
    * The proxy's target class will implicitly be set to target.getClass()</p>
    *
    * @param <T> The target type of the proxy interface extending InternalProxy
    * @param <P> Type representing the proxy interface extending InternalProxy
    * which will be implemented by the dynamic proxy to be created
    * @param target The instance to use as the proxy's target instance. It will
    * also implicitly define the target class to be the result of target.getClass()
    * @param proxyIfc The interface extending InternalProxy and defining the
    * methods that will match methods or fields declared in the target class
    * @param loader The class loader used to create the dynamic proxy
    * @return A dynamic proxy that will implement the given proxy interface
    * and automatically forward calls to the target object, which will be encapsulated by the proxy.
    */
   public static <T, P extends InternalProxy<T>> P createProxyToObject(
      final T target, final Class<P> proxyIfc, final ClassLoader loader)
   {
      try
      {
         final ProxyWrapper<T,P> wrapper = validateProxyInterface(proxyIfc, loader);
         if (!wrapper.targetClass.isInstance(target))
         {
            throw new IllegalArgumentException(target + " is not a valid instance of "+
                                               wrapper.targetClass.getName());
         }
         final GenericProxyHandler<T> handler = new GenericProxyHandler<T>(target);
         return wrapper.newProxyInstance(handler);
      }
      catch(final RuntimeException ex)
      {
         throw ex;
      }
      catch(final Exception ex)
      {
         throw new RuntimeException(ex);
      }
   }

   static String getProxyFieldName(final Method m) throws InvalidSignatureException
   {
      final ProxyField proxyField = m.getAnnotation(ProxyField.class);
      String fieldName = proxyField.value().trim();
      if (!fieldName.isEmpty())
      {
         return fieldName;
      }
      return extractFieldNameFromAccessorMethodName(m.getName());
   }

   private static String extractFieldNameFromAccessorMethodName(final String methodName)
      throws InvalidSignatureException
   {
      final int accessorPrefixLength = 3;
      if (methodName.length() > accessorPrefixLength &&
          (methodName.startsWith("set") || methodName.startsWith("get")))
      {
         final String fieldName = methodName.substring(accessorPrefixLength);
         return fieldName.substring(0, 1).toLowerCase() +
                (fieldName.length() > 1 ? fieldName.substring(1) : "");
      }
      final String msg = String.format(
         "%s is not a standard Java Bean accessor method name, which is incompatible with annotation %s",
         methodName,
         ProxyField.class.getName()
      );
      throw new InvalidSignatureException(msg);
   }

   private static final class ProxyWrapper<T, P extends InternalProxy<T>>
   {
      private final Class<P> proxyClass;
      private final Constructor<P> proxyConstructor;
      private final Class<T> targetClass;
      private final Method targetInstanceProvider;

      private ProxyWrapper(final Class<P> proxy,
                           final Class<T> targetClass,
                           final Method instanceProvider) throws NoSuchMethodException
      {
         this.proxyClass = proxy;
         this.proxyConstructor = proxy.getConstructor(InvocationHandler.class);
         this.targetClass = targetClass;
         this.targetInstanceProvider = instanceProvider;
      }

      private P newProxyInstance(final GenericProxyHandler<T> handler)
         throws InstantiationException, IllegalAccessException, InvocationTargetException
      {
         return proxyConstructor.newInstance(handler);
      }
   }

}
