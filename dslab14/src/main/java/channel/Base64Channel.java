package channel;

import java.io.IOException;
import java.net.Socket;

import com.sun.org.apache.xml.internal.security.utils.Base64;

public class Base64Channel implements iChannel {

	private TcpChannel channel;

	public Base64Channel(Socket socket) throws IOException {
		this.channel = new TcpChannel(socket); 
	}

	@Override
	public void send(String message) throws IOException {
		this.send(message.getBytes());
			
	}

	@Override
	public String receive() throws IOException {
		return channel.receive();
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	@Override
	public void send(byte[] message) throws IOException {
		channel.send(Base64.encode(message));
		
	}
}
