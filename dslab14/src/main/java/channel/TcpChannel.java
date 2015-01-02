package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpChannel implements iChannel {
	Socket socket;
	private PrintWriter serverWriter;
	private BufferedReader serverReader;

	public TcpChannel(Socket socket) throws IOException {
		this.socket=socket;
		this.serverWriter = new PrintWriter(socket.getOutputStream(), true);
		this.serverReader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
	}

	@Override
	public void send(String string) {
		serverWriter.println(string);
	}

	@Override
	public String receive() throws IOException  {
		return serverReader.readLine();
	}

	@Override
	public void close() throws IOException {
		if (serverReader != null)
			serverReader.close();
		if (serverWriter != null)
			serverWriter.close();
		if (socket != null && !socket.isClosed())
			socket.close();
	}

}
