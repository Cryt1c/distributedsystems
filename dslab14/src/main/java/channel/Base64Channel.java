package channel;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class Base64Channel implements iChannel {
	protected iChannel channel;
	public Base64Channel (Socket socket) throws IOException {
		this.channel=new TcpChannel(socket);
	}
	@Override
	public void send(String string) throws IOException {
		Cipher c=null;
		 try {
			c=Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			//c.init(Cipher.PUBLIC_KEY, arg1);
		} catch (NoSuchAlgorithmException|NoSuchPaddingException e) {
			throw new IOException("Error when initializing Cipher");
		}
		channel.send(string);
		
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
