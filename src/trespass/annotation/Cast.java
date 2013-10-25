package trespass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation should be used when parameter types required
 * in the signature of a method defined in a proxy interface to make it match
 * a corresponding method declared in the target class are not accessible
 * to the proxy interface.</p>
 * 
 * <p>To workaround such an issue, when declaring method arguments in a proxy
 * interface, replace inaccessible types with Object or other types that
 * are accessible and can be cast to the target type. Then annotate each 
 * replaced argument type with {@literal @Cast("<fully qualified name of the target type>")}.
 * The named type defined by this annotation will then be used by the proxy when
 * matching the target method to be invoked.</p>
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cast
{
   String value();
}
