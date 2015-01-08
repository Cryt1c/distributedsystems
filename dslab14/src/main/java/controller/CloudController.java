package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ComputationRequestInfo;
import util.Config;
import admin.INotificationCallback;

public class CloudController implements ICloudControllerCli, IAdminConsole, Runnable {

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
	private List<CloudWorker> workers;
	private boolean closed = true;
	private int rmax;
	private Registry registry;
	private int rmiPort;
	private String bindingName;
	private String host;
	private CloudWorker cw;

	// Timer for the node.isAlive check
	TimerTask task = new TimerTask() {

		@Override
		public void run() {
			// check if Nodes are still alive
			nodeSet.checkStatus(lastpacket);
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
		this.closed = false;
		//RMI
		try {
			startAdminComponent();
		} catch (AlreadyBoundException e) {
			e.printStackTrace();
		}
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
		rmax = config.getInt("controller.rmax");
		rmiPort = config.getInt("controller.rmi.port");
		host = config.getString("controller.host");
		bindingName = config.getString("binding.name");
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
				try {
					while (!datagramSocket.isClosed()) {
						byte[] buf = new byte[256];
						DatagramPacket packet = new DatagramPacket(buf,
								buf.length);

						// receive the packet, extracts the data and creates new
						// nodes if needed
						datagramSocket.receive(packet);
						String[] message = new String(packet.getData())
								.split(" ");

						if (message[0].contains("!hello")) {
							twoPhaseCommit(packet);
						} else {
							if (nodeSet.alreadyIn(message[0] + " "
									+ message[1].trim())) {

								nodeSet.add(new Node(packet.getAddress(),
										Integer.parseInt(message[2].trim()),
										message[0], message[1].trim(), config
												.getInt("node.timeout")));
							}
							lastpacket.put(message[0],
									System.currentTimeMillis());
						}
					}
				} catch (IOException e) {
					cloudcontroller.exit();
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
				workers = new ArrayList<CloudWorker>();
				try {
					while (!serverSocket.isClosed()) {
						Socket clientSocket = null;
						clientSocket = serverSocket.accept();
						if (!clientSocket.equals(null)) {
							workers.add(new CloudWorker(config.getString("hmac.key"), clientSocket,
									cloudcontroller));
							executorService.execute(workers.get(workers.size() - 1));
						}
					}
				} catch (IOException e) {
					cloudcontroller.exit();
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
		String result = "";

		for (Map.Entry<String, User> entry : users.entrySet()) {
			result += entry.getValue() + "\n";
		}
		return result;
	}

	// closes the sockets and cancels the timer and the executorService
	@Override
	public String exit() {
		if (!this.closed) {
			this.closed = true;
			stopAdminComponent();
			executorService.shutdownNow();
			timer.cancel();

			if (serverSocket != null)
				try {
					serverSocket.close();
				} catch (IOException e) {
					System.out
							.println("CloudController: could'nt close serverSocket");
					e.printStackTrace();
				}

			for (CloudWorker element : workers) {
				element.closeAll();
			}

			if (datagramSocket != null)
				datagramSocket.close();

		}
		return "cloudcontroller shutdown";
	}

	public HashMap<String, User> getUsers() {
		return users;
	}

	public NodeSet getNodeSet() {
		return nodeSet;
	}

	private void twoPhaseCommit(DatagramPacket commit) {
		String message = "!init" + "\n" + nodeSet.getIPPort() + rmax;
		//System.out.println(message);

		byte[] buf = message.getBytes();

		commit = new DatagramPacket(buf, buf.length, commit.getSocketAddress());

		try {
			datagramSocket.send(commit);

		} catch (IOException e) {
			System.out.println("couldn't send twoPhaseCommit");
		}
		return;
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

	@Override
	public String test() throws RemoteException {
		return "erfolgreicher test";
	}

	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		
		users.get(username).setSubscribe(credits);
		users.get(username).setCallback(callback);
//		if(credits>400){
//			callback.notify(username, credits);
//		}
		return false;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		String[] ipport = nodeSet.getIPPort().split("\n");
		List<ComputationRequestInfo> result = new ArrayList<ComputationRequestInfo>();

		// Produces a socketList with all the Sockets which have to be requested
		Socket[] socketList = new Socket[ipport.length];
		
		for (int i = 0; i < ipport.length; i++) {
			String tempData[] = ipport[i].split(":");
			int port = Integer.parseInt(tempData[1]);
			String IP = tempData[0].substring(1);
			try {
				Socket clientSocket = new Socket(IP, port);
				socketList[i] = clientSocket;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (int i = 0; i < socketList.length; i++) {
			try {
				PrintWriter writer = new PrintWriter(
						socketList[i].getOutputStream(), true);

				writer.println("!info");
				
				InputStream is = socketList[i].getInputStream();  
				ObjectInputStream ois = new ObjectInputStream(is);  
				ComputationRequestInfo to = (ComputationRequestInfo) ois.readObject();  
				if (to!=null){result.add(to);}  
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// close all sockets to the nodes
		for (int i = 0; i < socketList.length; i++) {
			try {
				if (socketList[i] != null) {
					socketList[i].close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	
	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		LinkedHashMap<Character, Long> stat = new LinkedHashMap<Character, Long>();
		if(cw.getMinus() > 0){
			stat.put('-',(long) cw.getMinus());
		}
		if(cw.getPlus() > 0){
			stat.put('+',(long) cw.getPlus());
		}
		if(cw.getMult() > 0){
			stat.put('*',(long) cw.getMult());
		}
		if(cw.getDiv() > 0){
			stat.put('/',(long) cw.getDiv());
		}

		return stat;
	}


	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	
	//=================RMI=================
	/**
	 *Exports the CloudController remote Object, creates the registry on the local host which will accept
	 * access on the rmiPort and rebinds the CloudController stub to the name residing in the binding name.
	 * @throws AlreadyBoundException 
	 */
	private void startAdminComponent() throws AlreadyBoundException{
		try {
			registry = LocateRegistry.createRegistry(rmiPort);
			IAdminConsole remote = (IAdminConsole) UnicastRemoteObject.exportObject(this, 0);
			registry.bind(bindingName, remote);
		} catch (RemoteException e) {
			throw new RuntimeException("Error while starting server.", e);
		}catch (AlreadyBoundException e) {
			throw new RuntimeException(
					"Error while binding remote object to registry.", e);
		}
	}
	
	public void stopAdminComponent(){
		try {
			// unexport the previously exported remote object
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			System.err.println("Error while unexporting object: "
					+ e.getMessage());
		}

		try {
			// unbind the remote object so that a client can't find it anymore
			registry.unbind(config.getString("binding.name"));
		} catch (Exception e) {
			System.err.println("Error while unbinding object: "
					+ e.getMessage());
		}
	}
	
	

}
