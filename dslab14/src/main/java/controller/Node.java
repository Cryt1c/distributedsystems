package controller;

import java.util.List;


public class Node {
	String ip;
	boolean online = false;
	int usage = 0;
	int port;
	
	
	
	public Node(String ip, boolean online, int usage, int port) {
		this.ip = ip;
		this.usage = usage;
		this.port = port;
	}
	public String getIP() {
		return ip;
	}
	public void setIP(String ip) {
		this.ip = ip;
	}
	public boolean isOnline() {
		return online;
	}
	public void setOnline(boolean online) {
		this.online = online;
	}
	public int getUsage() {
		return usage;
	}
	public void setUsage(int usage) {
		this.usage = usage;
	}
	@Override
	public String toString() {
		return "IP: " + ip + " Port: " + port + (online ? " online " : " offline ") + "Usage: " + usage;
	}
	public void checkStatus(List<String> isalives) {
		for(String alive: isalives) {
			if(toString().equals(alive)) {
				setOnline(true);
				return;
			}
		}
		setOnline(false);
		return;
	}
	
	
}
