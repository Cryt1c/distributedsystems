package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.util.encoders.Base64;

import dslab14.Base64Channel;
import dslab14.TcpChannel;
import dslab14.iChannel;
import util.Config;

public class Client implements IClientCli, Runnable {

	// if true: use Base64Channel if true
	// if false: use TCP Channel as before
	private Boolean useBase64=false;
	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	Socket socket = null;
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private ClientShell shell;	

	private iChannel controllerChannel =null;

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
	}

	@Override
	public void run() {
		this.createTCPSocket();		
		this.startShell();
	}

	// register this object at the shell and run the shell
	private void startShell() {
		this.shell = new ClientShell(componentName, userRequestStream,
				userResponseStream);
		shell.register(this);
		shell.run();
	}

	// creates a new TCP socket and waits for new incoming connections
	private void createTCPSocket() {
		try {
			socket = new Socket(config.getString("controller.host"),
					config.getInt("controller.tcp.port"));
		} catch (IOException e) {
			System.out.println("Connection refused!");
			return;
		}
		
		try {
			this.controllerChannel=(useBase64)?
					new Base64Channel(socket):
					new TcpChannel(socket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	@Override
	public String login(String username, String password) throws IOException {
		this.controllerChannel.send("login " + username + " " + password);
		return this.controllerChannel.receive();				
	}
	


	@Override
	public String logout() throws IOException {
		this.controllerChannel.send("logout");
		return this.controllerChannel.receive();
	}

	@Override
	public String credits() throws IOException {
		this.controllerChannel.send("credits");
		return this.controllerChannel.receive();
	}

	@Override
	public String buy(long credits) throws IOException {
		this.controllerChannel.send("buy "+credits);
		return this.controllerChannel.receive();
	}

	@Override
	public String list() throws IOException {
		this.controllerChannel.send("list");
		return this.controllerChannel.receive();
	}

	@Override
	public String compute(String term) throws IOException {
		this.controllerChannel.send("compute " + term);
		return this.controllerChannel.receive();
	}

	@Override
	public String exit() {
		executorService.shutdownNow();
		
		try {
			this.controllerChannel.close();
		} catch (IOException e) {
			return "couldn't close reader, writer and socket";
		}
		return "client shutdown";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		 
		// generates a 32 byte secure random number
		 SecureRandom secureRandom = new SecureRandom();
		 final byte[] number = new byte[32];
		 secureRandom.nextBytes(number);
		 
		// encode into Base64 format 
		 
		 byte[] base64Challenge = Base64.encode(number);
		 Cipher c=null;
		 try {
			c=Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
		} catch (NoSuchAlgorithmException|NoSuchPaddingException e) {
			throw new IOException("Error when initializing Cipher");
		}
		this.controllerChannel.send("authenticate " + username+" "+base64Challenge);		 
		return this.controllerChannel.receive();
	}

}
