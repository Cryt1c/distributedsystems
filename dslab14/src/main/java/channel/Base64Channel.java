package channel;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
/**
 * channel for communication via tcp-socket,
 * encrypted with RSA and Base64.
 * @author romantanzer
 *
 */
public class Base64Channel implements iChannel {

	private TcpChannel channel;
	// used for encryption
	private PublicKey publicKey=null;
	// used for decryption
	private PrivateKey privateKey=null;

	public Base64Channel(Socket socket)
			throws IOException {
		this.channel = new TcpChannel(socket);
		
	}
	
	public void setPrivateKey(PrivateKey key) throws IOException {
		privateKey=key;
	}
	public void setPublicKey(PublicKey key) {
		publicKey=key;
	}
	
	@Override
	/* publicKey must be set before calling this method. */
	
	public void send(String message) throws IOException {
		channel.send(Base64.encode(encryptMessage(message)).toString());
	}

	@Override
	/* privateKey must be set before calling this method. */
	public String receive() throws IOException {
		try {
			if(privateKey==null) {
				throw new IOException("private key not set!");
			}
			String msg=channel.receive();
			System.out.println("Base64Channel.receive:"+msg);
			return decryptMessage(Base64.decode(msg.getBytes()));
		} catch (Base64DecodingException e) {
			throw new IOException(e);
		}
	}

	/**
	 * cipher message with public key and send it
	 * 
	 * @param message
	 * @param publicKeyfile
	 * @throws IOException
	 */
	private byte[] encryptMessage(String message) throws IOException {

		Cipher cipher;
			try {
				cipher = Cipher
						.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				return (cipher.doFinal(message.getBytes()));
			} catch (NoSuchAlgorithmException e) {
				throw new IOException(e);
			} catch (NoSuchPaddingException e) {
				throw new IOException(e);
			} catch (InvalidKeyException e) {
				throw new IOException(e);
			} catch (IllegalBlockSizeException e) {
				throw new IOException(e);
			} catch (BadPaddingException e) {
				throw new IOException(e);
			}
			
		
	}

	private String decryptMessage(byte[] message) throws IOException {
			Cipher cipher;
			try {
				cipher = Cipher
						.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
				cipher.init(Cipher.DECRYPT_MODE, privateKey);
				return (cipher.doFinal(message).toString());
			} catch (NoSuchAlgorithmException e) {
				throw new IOException(e);
			} catch (NoSuchPaddingException e) {
				throw new IOException(e);
			} catch (InvalidKeyException e) {
				throw new IOException(e);
			} catch (IllegalBlockSizeException e) {
				throw new IOException(e);
			} catch (BadPaddingException e) {
				throw new IOException(e);
			}
			
		
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	public static SecretKey generateSecretAESKey() throws IOException {
		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e1) {
			throw new IOException(e1.getMessage());
		}
		// KEYSIZE is in bits
		generator.init(256);
		return generator.generateKey();
	}

}
