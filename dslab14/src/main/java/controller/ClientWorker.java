/**
 * 
 */
package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @author David
 * 
 */
public class ClientWorker implements Runnable {

	private CloudController cloudController;
	private PrintWriter writer;
	private BufferedReader reader;
	private User user;
	private NodeSet nodeset;

	public ClientWorker(Socket socket, CloudController mainclass) {
		this.cloudController = mainclass;
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
		System.out.println("new client connected.");
	}

	@Override
	public void run() {

		// TODO: delete: just for testing
		//this.user = cloudController.getUsers().get("alice").setLoggedin(true);

		while (true) {
			String[] input = { "" };

			try {
				input = reader.readLine().split(" ");

				if (this.user == null || user.isLoggedin() == false) {
					if (input[0].equals("login")) {
						if (this.cloudController.getUsers().containsKey(
								input[1])
								&& cloudController.getUsers().get(input[1])
										.getPassword().equals(input[2])) {
							this.user = cloudController.getUsers()
									.get(input[1]).setLoggedin(true);
							writer.println("success");
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
						writer.println("You have " + user.getCredits()
								+ " credits left.");
						break;
					case "buy":
						user.addCredits(Long.parseLong(input[1]));
						writer.println("You now have " + user.getCredits()
								+ "credits.");
						break;
					case "list":
						writer.println(cloudController.getNodeSet()
								.getOperators());
						break;
					case "compute":
						this.compute(input);
						break;
					case "login":
						writer.println("You are already logged in!");
						break;
					}

				}
			} catch (IOException e) {
				user.setLoggedin(false);
				System.out
						.println("Error occurred while waiting for/communicating with client: "
								+ user.getName());
				break;
			}
		}
	}

	private void compute(String input[]) {
		int credits = 0;
		nodeset = this.cloudController.getNodeSet();

		for (String element : input) {
			if (element.equals("+") || element.equals("-")
					|| element.equals("*") || element.equals("="))
				credits += 50;
		}

		if (this.user.getCredits() < credits) {
			this.writer.println("Error: You don't have enough Credits!");
			return;
		}

		for (int i = 0; i < input.length; i++) {
			if (input[i].equals("+")) {
				input[i + 1] = sendCalculation(nodeLookUp("+"),
						Arrays.copyOfRange(input, i - 1, i + 2));
			} else if (input[i].equals("-")) {
				input[i + 1] = sendCalculation(nodeLookUp("-"),
						Arrays.copyOfRange(input, i - 1, i + 2));
			} else if (input[i].equals("*")) {
				input[i + 1] = sendCalculation(nodeLookUp("*"),
						Arrays.copyOfRange(input, i - 1, i + 2));
			} else if (input[i].equals("/")) {
				input[i + 1] = sendCalculation(nodeLookUp("/"),
						Arrays.copyOfRange(input, i - 1, i + 2));
			} else if (i == input.length - 1) {
				this.writer.println(input[i]);
			}
		}

		this.user.setCredits(this.user.getCredits() - credits);
	}

	private Node nodeLookUp(String operator) {
		int usage = Integer.MAX_VALUE;
		Node returnNode = null;
		for (Node element : nodeset.getSet()) {
			if (element.getOperators().contains(operator)
					&& element.getUsage() < usage) {
				usage = element.getUsage();
				returnNode = element;
			}
		}
		return returnNode;
	}

	private String sendCalculation(Node node, String[] calc) {
		Socket calcSocket = null;
		PrintWriter calcWriter = null;
		BufferedReader calcReader = null;
		String result = "Error: No result";
		
		if(node == null) {
			return "Error: No node for this calculation";
		}
		
		try {
			System.out.println(node.getIP() + " " + node.getPort());
			calcSocket = new Socket(node.getIP(), node.getPort());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (calcSocket != null) {

			try {
				calcWriter = new PrintWriter(calcSocket.getOutputStream(),
						true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				calcReader = new BufferedReader(new InputStreamReader(
						calcSocket.getInputStream()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			calcWriter.println(calc[0] + " " + calc[1] + " "+ calc[2]);
			try {
				result = calcReader.readLine();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				
				calcSocket.close();
			} catch (IOException e) {
				System.out.println("calcSocket: connection closed");
				e.printStackTrace();
			}
		}
		
		return result;
	}
}
