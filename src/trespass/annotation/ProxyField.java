package trespass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate methods in an interface extending {@link trespass.InternalProxy} in order
 * to give the method the semantics of a field accessor to the named field
 * declared in the target class
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyField {
   /**
    * Name of a field/constant (static or not) declared in a target class being
    * mapped by the proxy interface declaring the annotated method
    */
   String value() default "";
}
