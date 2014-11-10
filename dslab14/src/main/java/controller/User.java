/**
 * This class represents an User
 */
package controller;

/**
 * @author David
 *
 */
public class User {
	private String name = "";
	private String password = "";
	private long credits;
	private boolean loggedin = false;
	
	public boolean isLoggedin() {
		return loggedin;
	}

	public User setLoggedin(boolean loggedin) {
		this.loggedin = loggedin;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public long getCredits() {
		return credits;
	}

	public synchronized void setCredits(long amount) {
		this.credits = amount;
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

	public synchronized void addCredits(long amount) {
		this.credits += amount;
	}
	
	
}
