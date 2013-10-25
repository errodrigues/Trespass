package trespass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is mandatory for proxy interfaces extending
 * {@link trespass.InternalProxy} and defines the target class for the proxy interface.</p>
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyClass
{
   /**
    * The annotated interface's target Class definition. When not available or accessible at compile time,
    * then the class' fully qualified name can be specified via {@link #targetClassName}
    */
   Class value() default ProxyClass.class;
   /**
    * Fully qualified name of the target class for the annotated interface
    */
   String targetClassName() default "";
}
