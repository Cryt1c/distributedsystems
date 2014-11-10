package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class CloudController implements ICloudControllerCli, Runnable {

	static CloudController cloudcontroller;
	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private HashMap<String, User> users = new HashMap<String, User>();
	private NodeSet nodeSet = new NodeSet();
	private Map<String, Long> lastpacket = new HashMap<String, Long>();
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private CloudShell shell;
	private Timer timer = new Timer();

	// Timer for the node.isAlive check
	TimerTask task = new TimerTask() {

		@Override
		public void run() {
			// check if Nodes are still alive
			nodeSet.checkStatus(lastpacket);
			// System.out.println("checkalive");
		}

	};

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
		cloudcontroller = this;
		extractConfig();
		startShell();
		createTCPSocket();
		startTCPThread();
		createUDPSocket();
		checkIsAlive();

	}

	// adds all users from the config-file to the users-list and sets the
	// credits
	private void extractConfig() {
		Config userconfig = new Config("user");
		Iterator<String> iter = userconfig.listKeys().iterator();
		while (iter.hasNext()) {
			String temp = iter.next().toString();
			if (temp.contains("password")) {
				String name;
				name = temp.substring(0, temp.length() - 9);
				users.put(name, new User(name, userconfig.getString(temp)));
				users.get(name).setCredits(
						Integer.parseInt(userconfig
								.getString(name + ".credits")));
			}

		}
	}

	// register this object at the shell and run the shell
	private void startShell() {
		this.shell = new CloudShell(componentName, users, userRequestStream,
				userResponseStream);
		shell.register(cloudcontroller);
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("shellservice");
				shell.run();
			}
		});
	}

	// starts UDP-Thread
	private void checkIsAlive() {
		timer.schedule(task, config.getInt("node.checkPeriod"),
				config.getInt("node.checkPeriod"));
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("udpservice");

				while (true) {
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					
					// receive the packet, extracts the data and creates new
					// nodes if needed
					try {
						datagramSocket.receive(packet);
						//System.out.println("receive: " + new String(packet.getData()).trim());
						String[] nameoperators = new String(packet.getData())
								.split(" ");

						if (nodeSet.alreadyIn(nameoperators[0] + " "
								+ nameoperators[1].trim())) {
							
							nodeSet.add(new Node(
									packet.getAddress(), 500, Integer.parseInt(nameoperators[2].trim()), nameoperators[0],
									nameoperators[1].trim(), config
											.getInt("node.timeout")));
						}
						lastpacket.put(nameoperators[0],
								System.currentTimeMillis());

						// System.out.println("packet received: "
						// + new String(packet.getData()).trim());

					} catch (IOException e) {
						System.out.println("Error occurred while waiting for/communicating with client");
						break;
					}
				}
			}
		});
	}

	// creates a new UDP socket and waits for new incoming connections
	private void createUDPSocket() {
		try {
			datagramSocket = new DatagramSocket(config.getInt("udp.port"));
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	// start tcpservice-Thread
	private void startTCPThread() {
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("tcpservice");

				while (true) {
					Socket clientSocket = null;
					try {
						clientSocket = serverSocket.accept();
						if (!clientSocket.equals(null)) {
							executorService.execute(new ClientWorker(
									clientSocket, cloudcontroller));
						}
					} catch (IOException e) {
						System.out.println("Error occurred while waiting for/communicating with client");
						break;
					}
				}

			}
		});
	}

	// creates a new TCP socket and waits for new incoming connections
	private void createTCPSocket() {
		try {
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
		} catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
	}

	// returns the nodes as a String
	@Override
	public String nodes() throws IOException {
		int count = 1;
		String result = "";

		for (Node entry : nodeSet.getSet()) {
			result += count++ + ". " + entry + "\n";
		}
		return result;
	}

	// returns the users as a String
	@Override
	public String users() throws IOException {
		int count = 1;
		String result = "";

		for (Map.Entry<String, User> entry : users.entrySet()) {
			result += count++ + ". " + entry.getValue() + "\n";
		}
		return result;
	}

	@Override
	public String exit() throws IOException {
		executorService.shutdownNow();
		timer.cancel();
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
				// Ignored because we cannot handle it
			}
		if (datagramSocket != null)
			datagramSocket.close();
		return "cloudcontroller shutdown";
	}

	public HashMap<String, User> getUsers() {
		return users;
	}

	public NodeSet getNodeSet() {
		return nodeSet;
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
