package controller;

import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudController implements ICloudControllerCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private List<User> users = new ArrayList<User>();
	private ExecutorService executorService = Executors.newFixedThreadPool(10);

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// TODO
	}

	@Override
	public void run() {
		// adds all users from the config-file to the users-list
		Config userconfig = new Config("user");
		Iterator iter = userconfig.listKeys().iterator();
		while (iter.hasNext()) {
			String temp = iter.next().toString();
		 	if(temp.contains("password")) {
		 		String name;
		 		name = temp.substring(0, temp.length() - 9);
		 		users.add(new User(name, userconfig.getString(temp)));
		 	}
		}
		// adds the credit to the user list
		for(User user : users) {
			user.setCredits(userconfig.getString(user.getName() + ".credits"));
		}
		
		// lists the users in the list
		try {
			this.users();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// creates a new TCP socket and waits for new incoming connections
		try {
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			System.out.println("Socket created.");
			
			
		}	catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
		executorService.execute(new Runnable() {
			public void run() {
				while(true) {
					try {
						System.out.println("serversocket accepting");
						serverSocket.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		
		// creates a new UDP socket and waits for new incoming connections
		try {
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
			System.out.println("Socket created.");
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		executorService.execute(new Runnable() {
			public void run() {
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				while(true) {
					try {
						System.out.println("datagramsocket receiving");
						datagramSocket.receive(packet);
					} catch (IOException e) {
						throw new RuntimeException("Cannot listen on UDP port.", e);
					}
				}
			}
		});
		
	}

	@Override
	public String nodes() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String users() throws IOException {
		// TODO Auto-generated method stub
		System.out.println(users);
		return null;
	}

	@Override
	public String exit() throws IOException {
		serverSocket.close();
		datagramSocket.close();
		executorService.shutdown();
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {
		CloudController cloudcontroller = new CloudController(args[0],
				new Config("controller"), System.in, System.out);
		// TODO: start the cloud controller
		cloudcontroller.run();
	}

}
