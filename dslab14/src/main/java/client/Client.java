package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	Socket socket = null;
	PrintWriter serverWriter = null;
	BufferedReader serverReader = null;
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private ClientShell shell;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		// TODO
	}

	@Override
	public void run() {
		// TODO
		this.startShell();
		this.createTCPSocket();
		//this.startTCPThread();
	}
	
	// register this object at the shell and run the shell
	private void startShell() {
		this.shell = new ClientShell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		executorService.execute(new Runnable() {
			public void run() {
				Thread.currentThread().setName("shellservice");
				shell.run();
			}
		});
	}
	
	// creates a new TCP socket and waits for new incoming connections
	private void createTCPSocket() {
		try {
			socket = new Socket(config.getString("controller.host"), config.getInt("controller.tcp.port"));
			System.out.println("Socket created.");
		}	catch (IOException e) {
			System.out.println("Connection refused!");
			return;
		}
		// create a writer to send messages to the server
		try {
			serverWriter = new PrintWriter(
					socket.getOutputStream(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			serverReader = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
//	// start tcpservice-Thread
//	private void startTCPThread() {
//		executorService.execute(new Runnable() {
//			public void run() {
//				Thread.currentThread().setName("tcpservice");
//
//				while (true) {
//					Socket clientSocket = null;
//					try {
//						clientSocket = socket.accept();
//					} catch (IOException e) {
//						throw new RuntimeException(
//								"Error accepting client connection", e);
//					}
//					executorService.execute(new ClientWorker(clientSocket));
//				}
//
//			}
//		});
//	}
	
	@Override
	public String login(String username, String password) throws IOException {
		serverWriter.println("login " + username + " " + password);
		return serverReader.readLine();
	}

	@Override
	public String logout() throws IOException {
		serverWriter.println("logout");
		return serverReader.readLine();
	}

	@Override
	public String credits() throws IOException {
		serverWriter.println("credits");
		return serverReader.readLine();
	}

	@Override
	public String buy(long credits) throws IOException {
		serverWriter.println("buy" + credits);
		return serverReader.readLine();
	}

	@Override
	public String list() throws IOException {
		serverWriter.println("list");
		return serverReader.readLine();
	}

	@Override
	public String compute(String term) throws IOException {
		serverWriter.println(term);
		return serverReader.readLine();
	}

	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return "bye bye!";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		// TODO: start the client
		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
