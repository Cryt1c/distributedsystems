package client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import channel.Base64Channel;
import channel.SecurityHelper;
import channel.TcpChannel;
import channel.iChannel;


public class Client implements IClientCli, Runnable {

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
			File publicKeyFile=new File(config.getString("controller.key"));
		
			if(SecurityHelper.useSecurity) {
				Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
				
				Base64Channel c=new Base64Channel(socket);
				c.setPublicKey(util.Keys.readPublicPEM(publicKeyFile));
				this.controllerChannel=c;
			}
			else {
				this.controllerChannel=new TcpChannel(socket);
			}
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
		 Base64Channel channel =(Base64Channel)this.controllerChannel;		
		 byte[] base64Challenge = SecurityHelper.generateRandom64(new byte[32]);
		 File f=new File(config.getString("keys.dir")+"/"+username+".pem");
		 channel.setPrivateKey(util.Keys.readPrivatePEM(f));
		 
		 // send 1st message
		 String msg="authenticate " + username + " " + base64Challenge.toString();
		 System.out.println("Client: send 1st message "+msg);
		 channel.send(msg);
		 

		 
		 // receive 3nd message
		 msg=channel.receive();
		 System.out.println("Client: receive 3rd message "+msg);
		String[] message = channel.receive().split(" ");
		 
		if (message[0] != "!ok") {
			throw new IOException("2nd message:expected !ok but got "
					+ message[0]+".");
		}
		
		String clientChallenge = Base64.decode(message[1]).toString();		
		if (!clientChallenge.equals(base64Challenge.toString())) {
			throw new IOException("received an wrong clientchallenge!");
		}
		String controllerChallenge = Base64.decode(message[2]).toString();
	
		/*Key secretKey = new SecretKeySpec(Base64.decode(message2[3]).toString());
		String iv = Base64.decode(message2[4]).toString();

	 PrivateKey privateKey=util.Keys.readPrivatePEM(new File("client/"+username+".pem"));
		try {
			cipher=Cipher.getInstance("AES/CTR/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey ,iv);
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
		
		return "successfully logged in!";
	}

}
