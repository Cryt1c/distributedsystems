/**
 * 
 */
package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

/**
 * @author David
 * 
 */
public class CloudWorker implements Runnable {

	private CloudController cloudController;
	private PrintWriter writer;
	private BufferedReader reader;
	private User user;
	private NodeSet nodeset;
	private Socket socket;
	private PrintWriter calcWriter;
	private BufferedReader calcReader;
	private Socket calcSocket;
	private boolean closed = true;

	public CloudWorker(Socket socket, CloudController mainclass) {
		this.closed = false;
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
		try {
			while (!Thread.interrupted() && !socket.isClosed()) {
				String[] input = { "" };
				input = reader.readLine().split(" ");

				if (this.user == null || user.isLoggedin() == false) {
					if (input[0].equals("login")) {
						if (this.cloudController.getUsers().containsKey(
								input[1])
								&& cloudController.getUsers().get(input[1])
										.getPassword().equals(input[2])) {
							this.user = cloudController.getUsers()
									.get(input[1]).setLoggedin(true);
							writer.println("successfully logged in!");
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
			}
		} catch (Exception e) {
			this.closeAll();
		}
	}

	private void close() {
		if (user != null)
			user.setLoggedin(false);
		try {
			if (writer != null)
				writer.close();
			if (reader != null)
				reader.close();
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
			System.out.println("Error closing Cloudworker");
		}
	}

	public void closeAll() {
		if (!this.closed) {
			this.closed = true;
			close();
			closeCalc();
		}
	}

	private void compute(String input[]) {
		int credits = 0;
		nodeset = this.cloudController.getNodeSet();

		// checks if user has enough credits left, returns error if not
		if (this.user.getCredits() < (input.length - 2) * 25) {
			this.writer
					.println("Error: You won't have enough credits for this calculation!");
			return;
		}
		for(int i = 2; i < input.length; i = i + 2) {
			if(!nodeset.getOperators().contains(input[i])) {
				this.writer.println("Error: no node for this calculation");
				return;
			}
		}
		
		for (int i = 0; i < input.length; i++) {
			String temp = "";
			if (input[i].equals("+")) {
				temp = sendCalculation(nodeLookUp("+"),
						Arrays.copyOfRange(input, i - 1, i + 2));
				input[i + 1] = temp;
				credits += 50;
			} else if (input[i].equals("-")) {
				temp = sendCalculation(nodeLookUp("-"),
						Arrays.copyOfRange(input, i - 1, i + 2));
				input[i + 1] = temp;
				credits += 50;
			} else if (input[i].equals("*")) {
				temp = sendCalculation(nodeLookUp("*"),
						Arrays.copyOfRange(input, i - 1, i + 2));
				input[i + 1] = temp;
				credits += 50;
			} else if (input[i].equals("/")) {
				temp = sendCalculation(nodeLookUp("/"),
						Arrays.copyOfRange(input, i - 1, i + 2));
				input[i + 1] = temp;
				credits += 50;
				if (temp.contains("Error: division by 0")) {
					this.writer.println(temp);
					break;
				}
			} 
			if (temp.contains("Error")) {
				this.writer.println(temp);
				break;
			}
			
			else if (i == input.length - 1) {
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
		String result = "Error: no result";

		if (node == null) {
			return "Error: no node for this calculation";
		}
		
		try {
			calcSocket = new Socket(node.getIP(), node.getPort());
		}
		catch (Exception e) {
			this.closeCalc();
			return "Error: technical problems";
		}
		
		if (calcSocket != null) {
			try {
				calcWriter = new PrintWriter(calcSocket.getOutputStream(), true);
				calcReader = new BufferedReader(new InputStreamReader(
						calcSocket.getInputStream()));

				calcWriter.println(calc[0] + " " + calc[1] + " " + calc[2]);

				result = calcReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				this.closeCalc();
				return "Error: technical problems";
			}
		}
		this.closeCalc();
		if (!result.contains("Error")) {
			if (result.contains("-")) {
				node.setUsage(node.getUsage() + 50 * (result.length() - 1));
			} else
				node.setUsage(node.getUsage() + 50 * result.length());
		}
		return result;
	}

	// closes streams and sockets to node
	private void closeCalc() {
		try {
			if (calcWriter != null)
				calcWriter.close();
			if (calcReader != null)
				calcReader.close();
			if (calcSocket != null && !socket.isClosed())
				calcSocket.close();
		} catch (IOException e) {
			System.out.println("Error closing Cloudworker");
		}
	}
}
