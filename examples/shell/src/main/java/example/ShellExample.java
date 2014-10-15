package example;

import java.io.InputStream;
import java.io.OutputStream;

import util.Config;
import cli.Command;
import cli.Shell;

public class ShellExample implements IShellExampleCli, Runnable {

	private Config config;
	private Shell shell;
	private String loggedInUser = null;

	public ShellExample(String componentName, Config config,
			InputStream userRequestStream, OutputStream userResponseStream) {
		this.config = config;

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

	@Override
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

	private boolean checkUser(String username, String pw) {
		String password = config.getString(username);
		if (password == null)
			return false;

		if (password.equals(pw))
			return true;

		return false;
	}

	@Override
	@Command
	public String login(String username, String password) {

		if (loggedInUser != null)
			return "You are already logged in!";

		if (checkUser(username, password)) {
			this.loggedInUser = username;
			return "Successfully logged in!";
		}

		return "Wrong username/password combination!";
	}

	@Override
	@Command
	public String whoami() {

		if (loggedInUser == null)
			return "You have to login first!";

		return this.loggedInUser;

	}

	@Override
	@Command
	public String logout() {

		if (loggedInUser == null)
			return "You have to login first!";

		loggedInUser = null;

		return "Successfully logged out!";

	}

	@Override
	@Command
	public String exit() {
		// First try to logout in case a user is still logged in
		logout();
		// Afterwards stop the Shell from listening for commands
		shell.close();
		return "Shut down completed! Bye ..";
	}

}
