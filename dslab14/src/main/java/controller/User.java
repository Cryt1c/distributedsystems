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
		return "User [name=" + name + ", password=" + password + ", credits="
				+ credits + "]";
	}

	public String getName() {
		return name;		
	}
	
	
}
