package trespass.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation will optionally mark the one method in an interface extending
 * {@link trespass.Trespasser} that will be used to obtain the default instance
 * of a target class.</p>
 * 
 * <p>The marked method in the proxy interface must match either a regular
 * method, declared in the target class, that will return a valid instance that
 * class (such as a getInstance() for example) or a valid constructor, also
 * declared in the target class.</p>
 * 
 * <p>There shouldn't be more than one method marked with this annotation in the proxy
 * interface. If more than one is found, only the 1st one found will be used but
 * there will be no guarantees on the order in which method are processed.</p>
 *
 * @see trespass.Factory#createProxy
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultInstanceProvider
{
}
