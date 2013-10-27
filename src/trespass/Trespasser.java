package trespass;

/**
 * This marker interface must be extended by any interface used to define
 * a dynamic proxy that can be created using the {@link Factory}.
 *
 * <p>Interfaces extending InternalProxy will declare method signatures that should map existing methods and/or fields
 * that are declared in a target class. Each InternalProxy interface must specify <b>one single target class</b> by annotating
 * the interface declaration with {@link trespass.annotation.ProxyClass}.</p>
 *
 * <p>To map a method in the interface with the actual method in the target class, just make sure to declare a signature
 * that is identical to the target. For example:</p>
 *
 * <code>
 *    {@literal @}ProxyClass(System.class)
 *    interface SystemProxy extends InternalProxy
 *    {
 *       <blockquote>
 *          long nanoTime(); // will be implicitly mapped to target {@link System#nanoTime()}
 *       </blockquote>
 *    }
 * </code>
 *
 *
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
public interface Trespasser<T>
{
   static final String PROXY_INST_GETTER = "getProxyInstance";
   
   T getProxyInstance();
}
