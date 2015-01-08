package admin;

import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.LinkedHashMap;

import cli.Command;
import cli.Shell;

/**
 * @author Philipp Ambros
 */
public class AdminShell {
	
	private Shell shell;
	private AdminConsole adminconsole;
	
	
	public AdminShell(String componentName,
			InputStream userRequestStream, OutputStream userResponseStream) {

		/*
		 * First, create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		shell.register(this);
	}
	
	public void run() {
		/*
		 * Finally, make the Shell process the commands read from the
		 * InputStream by invoking Shell.run(). Note that Shell implements the
		 * Runnable interface. Thus, you can run the Shell asynchronously by
		 * starting a new Thread:
		 * 
		 * Thread shellThread = new Thread(shell); shellThread.start();
		 * 
		 * In that case, do not forget to terminate the Thread ordinarily.
		 * Otherwise, the program will not exit.
		 */
		new Thread(shell).start();
		System.out.println(getClass().getName()
				+ " up and waiting for commands!");
	}
	
	// beendet den Admincontroller
	@Command
	public String exit() {
		// Afterwards stop the Shell from listening for commands
		shell.close();
		return "closed";
		//return adminconsole.exit();
	}
	
	@Command
	public LinkedHashMap<Character, Long> statistics(){
		try {
			return adminconsole.statistics();
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Command
	public String test(){
		try {
			return adminconsole.test();
		} catch (RemoteException e) {
			return ("error in Adminshell");
		}
	}
	
	public void register(AdminConsole mainclass) {
		adminconsole = mainclass;
	}
}
