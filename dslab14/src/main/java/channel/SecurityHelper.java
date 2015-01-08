package channel;

import java.security.SecureRandom;

import org.bouncycastle.util.encoders.Base64;

abstract public class SecurityHelper {
	public static boolean useSecurity=true;
	/**
	 * generate a random number for given parameter.
	 * @param number number that will be overwritten with random bytes
	 * @return
	 */
	public static byte[] generateRandom64(final byte[] number) {
		SecureRandom secureRandom = new SecureRandom();
		 secureRandom.nextBytes(number);
		 return Base64.encode(number);
	}
		
}
