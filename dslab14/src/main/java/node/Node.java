package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
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
	private Node node;
	private int rmin;
	private int rmax;

	// Sends isAlive packet
	TimerTask task = new TimerTask() {
		@Override
		public void run() {
			try {
				datagramSocket.send(packet);
			} catch (IOException e) {
				System.out.println("couldn't send isAlive");
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
		String message = componentName + " "
				+ config.getString("node.operators") + " "
				+ config.getString("tcp.port");
		byte[] buf = message.getBytes();

		setRmin(config.getInt("node.rmin"));

		try {
			this.packet = new DatagramPacket(buf, buf.length,
					InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		node = this;
		startShell();
		createTCPSocket();
		startTCPThread();
		createUDPSocket();
		sendIsAlive();
	}

	// register this object at the shell and run the shell
	private void startShell() {
		this.shell = new NodeShell(componentName, userRequestStream,
				userResponseStream);
		shell.register(node);
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("shellservice");
				shell.run();
			}
		});
	}

	// starts UDP-Thread
	private void sendIsAlive() {
		if (twoPhaseCommit()) {
			timer.schedule(task, config.getInt("node.alive"),
					config.getInt("node.alive"));
		}
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
				try {
					while (true) {
						clientSocket = serverSocket.accept();
						clientSockets.add(clientSocket);
						if (!clientSocket.equals(null)) {
							executorService.execute(new NodeWorker(
									clientSocket, config.getString("log.dir"),
									componentName, node));
						}
					}
				} catch (IOException e) {
					node.exit();
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

	@Override
	public String exit() {
		executorService.shutdownNow();
		try {
			for (Socket element : clientSockets) {
				element.close();
			}
			timer.cancel();
			if (serverSocket != null)
				serverSocket.close();
		} catch (IOException e) {
			System.out.println("Node: couldn't close serverSocket");
			e.printStackTrace();
		}
		if (datagramSocket != null)
			datagramSocket.close();
		return "node shutdown";
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
		return "" + getRmax();
	}

	private boolean twoPhaseCommit() {
		String message = "!hello";
		byte[] buf = message.getBytes();
		DatagramPacket commit = null;

		try {
			commit = new DatagramPacket(buf, buf.length,
					InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			datagramSocket.send(commit);
		} catch (IOException e) {
			System.out.println("couldn't send !hello");
		}

		try {
			buf = new byte[4096];
			commit = new DatagramPacket(buf, buf.length,
					InetAddress.getByName(config.getString("controller.host")),
					config.getInt("controller.udp.port"));
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			datagramSocket.receive(commit);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String temp = new String(commit.getData(), commit.getOffset(),
				commit.getLength());

		String[] info = temp.split("\n");
		
		int average = Integer.parseInt(info[info.length - 1]) / (info.length - 1);
		
		// Tests if the node itself checks if the resource level is suficient
		
		if(this.getRmin() > average) {
			System.out.println("Not enough resources!");	
			return false;
		}
		
		// If this is the first node, it's always ok to register
		if (info.length == 2) {
			setRmax(Integer.parseInt(info[info.length - 1]));
			return true;
		}
		
		message = "!share " + average;
		Socket[] socketList = new Socket[info.length - 2];
		
		// Produces a socketList with all the Sockets which have to be requested
		for (int i = 1; i < info.length - 1; i++) {
			String tempData[] = info[i].split(":");
			int port = Integer.parseInt(tempData[1]);
			String IP = tempData[0].substring(1);
			try {
				Socket clientSocket = new Socket(IP, port);
				socketList[i - 1] = clientSocket;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return askNodes(socketList, average);

	}
	
	// Requests all the Nodes if they are ok with the new average
	private boolean askNodes(Socket[] socketList, int average) {
		for (int i = 0; i < socketList.length; i++) {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socketList[i].getInputStream()));
				PrintWriter writer = new PrintWriter(
						socketList[i].getOutputStream(), true);

				writer.println("!share " + average);
				System.out.println("Requester sent: !share " + average);
				String input = null;
				input = reader.readLine();

				if (input != null) {
					if (input.contains("!ok")) {
						System.out.println("Requester got: !ok #" + i);
						continue;
					} else
						for(int j = 0; j < socketList.length; j++) {
							PrintWriter rollbackWriter = new PrintWriter(
									socketList[j].getOutputStream(), true);
							rollbackWriter.println("!rollback " + average);
							System.out.println("Requester sent: !rollback" + average);
							socketList[j].close();
						}
					return false;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for(int j = 0; j < socketList.length; j++) {
			PrintWriter rollbackWriter;
			try {
				rollbackWriter = new PrintWriter(
						socketList[j].getOutputStream(), true);
				rollbackWriter.println("!commit " + average);
				System.out.println("Requester sent: !commit " + average);
				socketList[j].close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		this.setRmax(average);
		return true;
	}

	public synchronized int getRmin() {
		return rmin;
	}

	public synchronized void setRmin(int rmin) {
		this.rmin = rmin;
	}

	public synchronized int getRmax() {
		return rmax;
	}

	public synchronized void setRmax(int rmax) {
		this.rmax = rmax;
	}
}
