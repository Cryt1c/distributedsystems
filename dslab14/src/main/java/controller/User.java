/**
 * This class represents an User
 */
package controller;

/**
 * @author David
 *
 */
public class User {
	String name = "";
	String password = "";
	String credits = "";
	boolean loggedin = false;
	
	public boolean isLoggedin() {
		return loggedin;
	}

	public void setLoggedin(boolean loggedin) {
		this.loggedin = loggedin;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCredits() {
		return credits;
	}

	public void setCredits(String credits) {
		this.credits = credits;
	}

	public User(String name, String password) {
		this.name = name;
		this.password = password;
	}

	@Override
	public String toString() {
		return name + (loggedin ? " online " : " offline ") + "Credits: " + credits;
	}

	public String getName() {
		return name;		
	}
	
	
}
