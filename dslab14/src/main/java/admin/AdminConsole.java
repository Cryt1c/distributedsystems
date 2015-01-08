package admin;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.ComputationRequestInfo;
import util.Config;
import controller.IAdminConsole;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements INotificationCallback, Runnable {

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
			INotificationCallback callback = (INotificationCallback) UnicastRemoteObject.exportObject(this,0);
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
	


	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
			adminService.subscribe(username, credits, this);
		return false;
	}


	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		List<ComputationRequestInfo> info = adminService.getLogs();
		return info;
	}


	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		/*System.out.println(adminService.statistics().entrySet());
		System.out.println("Und jetzt als toString");
		System.out.println(adminService.statistics().toString());*/
		
		Map<Character, Long> sortedMap = mapSortedByValues(adminService.statistics());
	    //System.out.println(sortedMap);
				
		
		for (Iterator<Character> iter = sortedMap.keySet().iterator(); iter.hasNext();) {
			Character key =  (Character) iter.next();
			System.out.println(sortedMap.get(key)+" "+key);
		
		}		
		
		return null;
	}


	

	public String test() throws RemoteException {
		return adminService.test();
	}

	
	@Override
	public void notify(String username, int credits) throws RemoteException {
		System.out.println("Notification: "+username+" has less than "+credits+" credits.");
		
	}
	
	//sorter fuer statistics
	public Map<Character, Long> mapSortedByValues(Map<Character, Long> map) {
	    List<Map.Entry<Character, Long>> entryList = new LinkedList<Map.Entry<Character, Long>>(map.entrySet());
	    Collections.sort(entryList,
	            new Comparator<Map.Entry<Character, Long>>() {
	                @Override
	                public int compare(Map.Entry<Character, Long> e1, Map.Entry<Character, Long> e2) {
	                    return (e1.getValue()).compareTo(e2.getValue());
	                }
	            }
	    );
	    Collections.reverse(entryList); //Reverse value DESC
	    Map<Character, Long> sortedMap = new LinkedHashMap<Character, Long>();
	    for (Map.Entry<Character, Long> entry : entryList)
	        sortedMap.put(entry.getKey(), entry.getValue());
	    return sortedMap;
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
