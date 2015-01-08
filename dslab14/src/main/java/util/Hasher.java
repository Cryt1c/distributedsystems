package util;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

public class Hasher {
	
	/*
	 * class is used to hash messages with a secret key
	 * and test if the hashed messages are valid
	 * 
	 */
	
	public Hasher() {
		// TODO Auto-generated constructor stub
	}

	public static String hash(String message, String keydir) {
		Key secretKey;
		try {
			secretKey = Keys.readSecretKey(new File(keydir));
			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretKey);
			hMac.update(message.getBytes());
			byte[] hash = hMac.doFinal();
			message = new String(Base64.encode(hash)) + " " + message;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return message;
	}

	public static boolean testHash(String message, String keydir) {
		boolean validHash = false;
		String hash = message.substring(0, message.indexOf(' '));
//		System.out.println(hash);
		String data = message.substring(message.indexOf(' ') + 1);
//		System.out.println(data);
		
		try {
			Key secretKey = Keys.readSecretKey(new File(keydir));
			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretKey);
			hMac.update(data.getBytes());
			byte[] computedHash = hMac.doFinal();
			byte[] receivedHash = Base64.decode(hash.getBytes());
			validHash = MessageDigest.isEqual(computedHash, receivedHash);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return validHash;
	}
	
	public static String getMessage(String message) {
		String data = message.substring(message.indexOf(' ') + 1);
		return data;
	}
}
