package controller;

import java.net.InetAddress;
import java.util.Map;

public class Node {
	InetAddress ip;
	String name;
	String operators;
	boolean online = false;
	int usage;
	int port;
	int timeout;

	public Node(InetAddress ip, int port, String name, String operators,
			int timeout) {
		this.ip = ip;
		this.port = port;
		this.name = name;
		this.operators = operators;
		this.timeout = timeout;
	}

	public int getPort() {
		return port;
	}

	public InetAddress getIP() {
		return ip;
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

	public synchronized void setUsage(int usage) {
		this.usage = usage;
	}
	
	public String getOperators() {
		return operators;
	}
	
	public String getSignature() {
		return name + " " + operators;
	}

	@Override
	public String toString() {
		return name + " IP: " + ip + " Port: " + port
				+ (online ? " online " : " offline ") + "Usage: " + usage;
	}

	public boolean equals(Node node) {
		if (node.name == this.name)
			return true;
		return false;
	};
	
	// lastpacket represents the time of the last received packet
	// checks if lastpacket is older than timeoutrestriction
	public void checkStatus(Map<String, Long> lastpacket) {
		if (lastpacket.containsKey(this.name)) {
			long elapsedtime = System.currentTimeMillis() - lastpacket.get(name);
			if (elapsedtime > timeout) {
//				System.out.println("elapsed time: " + elapsedtime);
				setOnline(false);
				return;
			}
			setOnline(true);
		}
		return;
	}

}
