package dslab14;

import java.io.IOException;
import java.net.Socket;

public class Base64Channel implements iChannel {
	protected iChannel channel;
	public Base64Channel (Socket socket) throws IOException {
		this.channel=new TcpChannel(socket);
	}
	@Override
	public void send(String string) {
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
