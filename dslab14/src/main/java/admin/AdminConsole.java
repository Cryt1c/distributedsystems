package admin;

import cli.Shell;
import client.ClientShell;
import controller.IAdminConsole;
import model.ComputationRequestInfo;
import util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements IAdminConsole, INotificationCallback, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private AdminShell shell;
	IAdminConsole adminService;
	Registry registry;

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
	public AdminConsole(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
	}

	@Override
	public void run() {
		this.startShell();
		try {
			registry = LocateRegistry.getRegistry(config.getString("controller.host"),
					config.getInt("controller.rmi.port"));
			adminService = (IAdminConsole) registry.lookup(config.getString("binding.name"));
			//System.out.println(config.getInt("controller.rmi.port"));
		} catch (RemoteException e) {
			throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException("Error while looking for server-remote-object.", e);
		}
	}
	
	//register this object at the shell and run the shell
	private void startShell() {
		this.shell = new AdminShell(componentName, userRequestStream,
				userResponseStream);
		shell.register(this);
		shell.run();
	}
	

	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		/*System.out.println(adminService.statistics().entrySet());
		System.out.println("Und jetzt als toString");
		System.out.println(adminService.statistics().toString());*/
		for (Iterator iter = adminService.statistics().keySet().iterator(); iter.hasNext();) {
			Character key =  (Character) iter.next();
			System.out.println(adminService.statistics().get(key)+" "+key);
		
		}
		
		
		return null;
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
	
	@Override
	public String test() throws RemoteException {
		return adminService.test();
	}

	
	@Override
	public void notify(String username, int credits) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config(
				"admin"), System.in, System.out);
		// TODO: start the admin console
		adminConsole.run();
	}
}
