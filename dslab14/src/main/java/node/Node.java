package node;

import util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import controller.ClientWorker;
import controller.CloudShell;


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

	// Sends isAlive packet
	TimerTask task = new TimerTask() {

		@Override
		public void run() {
			try {
				datagramSocket.send(packet);
				System.out.println("sendAlive");
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
		
		String message = componentName + config.getString("node.operators");
		byte[] buf = message.getBytes();
		String controllerhost = config.getString("controller.host");
		int controllerudpport = config.getInt("controller.udp.port");
		InetSocketAddress socketAdress = new InetSocketAddress(controllerhost, controllerudpport);
		this.packet = new DatagramPacket(buf, buf.length, socketAdress);
		// TODO
	}

	@Override
	public void run() {
		// TODO

		startShell();
		createTCPSocket();
		startTCPThread();
		createUDPSocket();
		startUDPThread();
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
	private void startUDPThread() {
		timer.schedule(task, config.getInt("node.alive"), config.getInt("node.alive"));
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("udpservice");
				byte[] buf = new byte[256];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				while(true) {
					try {
						System.out.println("datagramSocket receiving");
						datagramSocket.receive(packet);
					} catch (IOException e) {
						throw new RuntimeException("Cannot listen on UDP port.", e);
					}
				}
			}
		});
	}

	// creates a new UDP socket and waits for new incoming connections
	private void createUDPSocket() {
		try {
			datagramSocket = new DatagramSocket();
			System.out.println("datagramSocket created.");
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

				while(true) {
					Socket clientSocket = null;
					try {
						clientSocket = serverSocket.accept();
					} catch (IOException e) {
						throw new RuntimeException(
								"Error accepting client connection", e);
					}
					executorService.execute(new ControllerWorker(clientSocket));
				}

			}
		});
	}

	// creates a new TCP socket and waits for new incoming connections
	private void createTCPSocket() {
		try {
			serverSocket = new ServerSocket(config.getInt("tcp.port"));
			System.out.println("serverSocket created.");
		}	catch (IOException e) {
			throw new RuntimeException("Cannot listen on TCP port.", e);
		}
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
