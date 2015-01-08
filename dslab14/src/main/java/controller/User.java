/**
 * This class represents an User
 */
package controller;

import java.rmi.RemoteException;

import admin.INotificationCallback;

/**
 * @author David
 * 
 */
public class User {
	private String name = "";
	private String password = "";
	private int credits;
	private boolean loggedin = false;
	private int subscribe = Integer.MIN_VALUE;
	private INotificationCallback callback = null;

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

	public int getCredits() {
		return credits;
	}

	public synchronized void setCredits(int amount) {
		if (this.callback != null) {
			if(amount < subscribe) {
				try {
					callback.notify(name, amount);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		this.credits = amount;
	}

	public User(String name, String password) {
		this.name = name;
		this.password = password;
	}

	@Override
	public String toString() {
		return name + (loggedin ? " online " : " offline ") + "Credits: "
				+ credits;
	}

	public String getName() {
		return name;
	}

	public synchronized void addCredits(int amount) {
		this.credits += amount;
	}

	public int getSubscribe() {
		return subscribe;
	}

	public void setSubscribe(int subscribe) {
		this.subscribe = subscribe;
	}

	public INotificationCallback getCallback() {
		return callback;
	}

	public void setCallback(INotificationCallback callback) {
		this.callback = callback;
	}

}
