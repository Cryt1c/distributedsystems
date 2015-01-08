/**
 * 
 */
package controller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;

import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import channel.Base64Channel;
import channel.SecurityHelper;
import channel.TcpChannel;
import channel.iChannel;

/**
 * @author David
 * 
 */
public class CloudWorker implements Runnable {

	private CloudController cloudController;
	private User user;
	private NodeSet nodeset;
	private Socket socket;
	private PrintWriter calcWriter;
	private BufferedReader calcReader;
	private Socket calcSocket;
	private boolean closed = true;
	private iChannel clientChannel =null;
	private Config config;
	

	public CloudWorker(Socket socket, CloudController mainclass, Config config) {
		this.closed = false;
		this.cloudController = mainclass;
		this.config=config;
		
		try {
			if(SecurityHelper.useSecurity) {
				Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
				
				File privateKeyFile=new File(config.getString("key"));
				Base64Channel c=new Base64Channel(socket);
				c.setPrivateKey(util.Keys.readPrivatePEM(privateKeyFile));
				this.clientChannel=c;
			}
			else {
				this.clientChannel=new TcpChannel(socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		this.socket = socket;
		Thread.currentThread().setName("clientworker");
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted() && !socket.isClosed()) {
				String[] input = { "" };
				System.out.println("receive 2nd message");
				String msg = this.clientChannel.receive();

				System.out.println("cloudWorker:" + msg);
				input = msg.split(" ");
				if (this.user == null || user.isLoggedin() == false) {
					switch (input[0]) {
					case "login":
						if (this.cloudController.getUsers().containsKey(
								input[1])
								&& cloudController.getUsers().get(input[1])
										.getPassword().equals(input[2])) {
							this.user = cloudController.getUsers()
									.get(input[1]).setLoggedin(true);
							this.clientChannel.send("successfully logged in!");
						} else {
							this.clientChannel.send("wrong credentials!");
						}
						break;
					case "authenticate":
						try {
							this.authenticate(input);
							this.user = cloudController.getUsers()
									.get(input[1]).setLoggedin(true);
						}
						catch(IOException e) {
							throw new IOException(e.getMessage());
						}
						break;
					default:
						this.clientChannel.send("login or authenticate first!");
							break;
						
					}
				} else if (user.isLoggedin() == true) {
					switch (input[0]) {
					case "logout":
						this.user.setLoggedin(false);
						this.clientChannel.send("loggedout");
						break;

					case "credits":
						this.clientChannel.send("You have " + user.getCredits()
								+ " credits left.");
						break;
					case "buy":
						user.addCredits(Long.parseLong(input[1]));
						this.clientChannel.send("You now have " + user.getCredits()
								+ "credits.");
						break;
					case "list":
						this.clientChannel.send(cloudController.getNodeSet()
								.getOperators());
						break;
					case "compute":
						this.compute(input);
						break;
					case "login":
					case "authenticate":
						this.clientChannel.send("You are already authenticated or logged in!");
						break;
					}
				}
			}
		} catch (Exception e) {
			this.closeAll();
		}
	}

	/**
	 * 
	 * @param input "authenticate <username> <clientChallenge>"
	 * @throws IOException 
	 */
	private void authenticate(String[] input) throws IOException {		

		String receivedClientChallenge = input[2];
		
		String controllerChallenge=SecurityHelper.generateRandom64(new byte[32]).toString();
		
		// prepare and send 2nd message
		Base64Channel channel=(Base64Channel)this.clientChannel;
		SecretKey key=Base64Channel.generateSecretAESKey();		
		byte[] iv = SecurityHelper.generateRandom64(new byte[16]);
		String user = input[1];		
		String message = "!ok " + receivedClientChallenge + " "
				+ controllerChallenge + " " + Base64.encode(key.toString().getBytes())
				+ " " + Base64.encode(iv);
		PublicKey publicUserKey = util.Keys.readPublicPEM(new File(
				config.getString("keys.dir")+"/" + user + ".pub.pem"));
		channel.setPublicKey(publicUserKey);
		channel.send(message);
		
		// receive 3nd message
		String receivedCloudChallenge = this.clientChannel.receive();
		if (!receivedCloudChallenge.equals(controllerChallenge.toString())) {
			throw new IOException("authentication not successful.");
		}
	}

	private void close() {
		if (user != null)
			user.setLoggedin(false);
		try {
			this.clientChannel.close();
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

	private void compute(String input[]) throws IOException {
		int credits = 0;
		nodeset = this.cloudController.getNodeSet();

		// checks if user has enough credits left, returns error if not
		if (this.user.getCredits() < (input.length - 2) * 25) {
			this.clientChannel.send("Error: You won't have enough credits for this calculation!");
			return;
		}
		for(int i = 2; i < input.length; i = i + 2) {
			if(!nodeset.getOperators().contains(input[i])) {
				this.clientChannel.send("Error: no node for this calculation");
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
					this.clientChannel.send(temp);
					break;
				}
			} 
			if (temp.contains("Error")) {
				this.clientChannel.send(temp);
				break;
			}
			
			else if (i == input.length - 1) {
				this.clientChannel.send(input[i]);
			}
		}

		this.user.setCredits(this.user.getCredits() - credits);
	}

	private Node nodeLookUp(String operator) {
		int usage = Integer.MAX_VALUE;
		Node returnNode = null;
		for (Node element : nodeset.getSet()) {
			if (element.getOperators().contains(operator)
					&& element.getUsage() < usage && element.isOnline()) {
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
