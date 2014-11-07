/**
 * 
 */
package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cli.Command;
import cli.Shell;

/**
 * @author David
 * 
 */
public class ClientShell {

	private Shell shell;
	private Client client;

	public ClientShell(String componentName, InputStream userRequestStream,
			OutputStream userResponseStream) {

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

	// beendet den Cloudcontroller
	@Command
	public String exit() {
		// Afterwards stop the Shell from listening for commands
		shell.close();
		try {
			client.exit();
		} catch (IOException e) {
			System.out.println("couldn't log out!");
			e.printStackTrace();
		}
		return "Shut down completed! Bye ..";
	}

	@Command
	public String login(String username, String password) {
		try {
			return client.login(username, password);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return("couldn't login!");
		}
	}
	
	@Command
	public String logout() {
		try {
			return client.logout();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return("couldn't logout!");
		}
	}
	
	@Command
	public String credits() {
		try {
			return client.credits();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return("couldn't lookup credits!");
		}
	}
	
	@Command
	public String list() {
		try {
			return client.list();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return("couldn't list operations");
		}
	}
	
	@Command
	public String compute(String term) {
		try {
			return client.compute(term);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return("couldn't compute " + term);
		}
	}

	public void register(Client mainclass) {
		client = mainclass;
	}

}