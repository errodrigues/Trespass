package trespass;

/**
 * Usually thrown when one of the annotations from package {@link trespass.annotation} was expected but missing.
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
public final class MissingAnnotationException extends Exception
{
   public MissingAnnotationException(String message)
   {
      super(message);
   }
}
