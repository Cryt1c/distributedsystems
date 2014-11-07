/**
 * 
 */
package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * @author David
 * 
 */
public class ClientWorker implements Runnable {

	private CloudController cloudController = null;
	private PrintWriter writer;
	private BufferedReader reader;
	private User user;
	private Socket socket;

	public ClientWorker(Socket socket, CloudController mainclass) {
		this.cloudController = mainclass;
		this.socket = socket;
		Thread.currentThread().setName("clientworker");
		try {
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// prepare the writer for responding to clients requests
		try {
			writer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (!socket.isClosed()) {
			String[] input = { "" };

			try {
				input = reader.readLine().split(" ");

				System.out.println(input[0]);

				if (this.user == null || user.isLoggedin() == false) {
					if (input[0].equals("login")) {
						if (cloudController.users.containsKey(input[1])
								&& cloudController.users.get(input[1])
										.getPassword().equals(input[2])) {
							this.user = cloudController.users.get(input[1])
									.setLoggedin(true);
							writer.println("loggedin");
							System.out.println(input[1] + " logged in");
						} else {
							writer.println("wrong credentials!");
						}
					} else {
						writer.println("login first!");
					}

				} else if (user.isLoggedin() == true) {
					switch (input[0]) {
					case "logout":
						this.user.setLoggedin(false);
						writer.println("loggedout");
						break;

					case "credits":
					case "buy":
					case "list":
					case "compute":
					case "exit":
					case "login":
						writer.println("already logged in!");
						break;
					}

				}
			} catch (IOException e) {
				System.err
						.println("Error occurred while waiting for/communicating with client: "
								+ e.getMessage());
				break;
			}
		}
	}
}
