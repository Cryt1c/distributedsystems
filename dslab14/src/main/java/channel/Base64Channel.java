package channel;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class Base64Channel implements iChannel {
	protected iChannel channel;
	private File keyfile;

	public Base64Channel(Socket socket, String keyfile) throws IOException {
		this.channel = new TcpChannel(socket);
		this.keyfile = new File(keyfile);
	}

	@Override
	public void send(String message) throws IOException {

		PublicKey publicKey = util.Keys.readPublicPEM(this.keyfile);

		try {
			Cipher cipher = Cipher
					.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] base64bytes = message.getBytes();
			byte[] cipherData = cipher.doFinal(base64bytes);
			channel.send(Base64.encode(cipherData));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException("Error when initializing Cipher: "
					+ e.getMessage());
		}		
	}

	@Override
	public String receive() throws IOException {
		return channel.receive();
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}
}
