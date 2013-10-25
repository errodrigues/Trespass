package trespass;

/**
 * Usually thrown when a proxy method signature is invalid.
 *
 * @author <a target="github" href="https://github.com/errodrigues">Eduardo Rodrigues</a>
 * @version $Revision$
 */
public final class InvalidSignatureException extends Exception
{
   public InvalidSignatureException(String message)
   {
      super(message);
   }
}
