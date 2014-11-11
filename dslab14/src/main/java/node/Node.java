package node;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;


public class Node implements INodeCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ServerSocket serverSocket;
	private DatagramSocket datagramSocket;
	private Timer timer = new Timer();
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private NodeShell shell;
	private DatagramPacket packet;
	private Set<Socket> clientSockets = new HashSet<Socket>();

	// Sends isAlive packet
	TimerTask task = new TimerTask() {

		@Override
		public void run() {
			try {
				//System.out.println("sendAlive: " + new String(packet.getData()));
				datagramSocket.send(packet);
				//System.out.println("sendAlive");
			} catch (IOException e) {
				e.printStackTrace();
			}

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
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		String message = componentName + " " + config.getString("node.operators") + " " + config.getString("tcp.port");
		byte[] buf = message.getBytes();
		
		try {
			this.packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		startShell();
		createTCPSocket();
		startTCPThread();
		createUDPSocket();
		sendIsAlive();
	}

	// register this object at the shell and run the shell
	private void startShell() {
		this.shell = new NodeShell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("shellservice");
				shell.run();
			}
		});
	}

	// starts UDP-Thread
	private void sendIsAlive() {
		timer.schedule(task, config.getInt("node.alive"), config.getInt("node.alive"));
	}

	// creates a new UDP socket and waits for new incoming connections
	private void createUDPSocket() {
		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}

	// start tcpservice-Thread
	private void startTCPThread() {
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("tcpservice");
				Socket clientSocket = null;
				while(true) {
					try {
						clientSocket = serverSocket.accept();
						clientSockets.add(clientSocket);
						if (!clientSocket.equals(null)) {
							executorService.execute(new NodeWorker(
									clientSocket, config.getString("log.dir"), componentName));
						}
					} catch (IOException e) {
							try {
								serverSocket.close();
								System.out.println("Node: serverSocket closed");
							} catch (IOException e1) {
								System.out.println("Error closing Socket");
								e1.printStackTrace();
							}
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
		}	catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
	}
	
	

	@Override
	public String exit() throws IOException {
		executorService.shutdownNow();
		for(Socket element: clientSockets) {
			element.close();
		}
		timer.cancel();
		if (serverSocket != null) serverSocket.close();
		if (datagramSocket != null) datagramSocket.close();
		return null;
	}

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in,
				System.out);
		// TODO: start the node
		node.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String resources() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
