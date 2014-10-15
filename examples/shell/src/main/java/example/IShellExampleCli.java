package example;

import cli.Shell;

/**
 * Contains some methods, which will be used to demonstrate the {@link Shell}
 * functionality.
 */
public interface IShellExampleCli {

	/**
	 * Authenticates the user with the given credentials.
	 * 
	 * @param username
	 *            the name of the user
	 * @param password
	 *            the user's password
	 * @return status whether the authentication was successful or not
	 */
	String login(String username, String password);

	/**
	 * Returns the currently logged in user
	 * 
	 * @return the username
	 */
	String whoami();

	/**
	 * Performs a logout if necessary
	 * 
	 * @return status whether logout was successful or not
	 */
	String logout();

	/**
	 * Performs a shutdown and a release of all resources.
	 * 
	 * @return bye message
	 */
	String exit();

}
