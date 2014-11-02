/**
 * 
 */
package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author David
 *
 */
public class ClientWorker implements Runnable {

	private Socket clientSocket;

	public ClientWorker(Socket socket) {
		this.clientSocket = socket;
	}


	@Override
	public void run() {
		try {
			InputStream input = clientSocket.getInputStream();
			OutputStream output = clientSocket.getOutputStream();
			
			input.close();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

