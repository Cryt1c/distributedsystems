package controller;

import java.util.Map;

public class Node {
	String ip;
	String name;
	String operators;
	boolean online = false;
	int usage = 0;
	int port;
	int timeout = 0;

	public Node(String ip, int usage, int port, String name, String operators,
			int timeout) {
		this.ip = ip;
		this.usage = usage;
		this.port = port;
		this.name = name;
		this.operators = operators;
		this.timeout = timeout;
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

	public String signature() {
		return name + " " + operators;
	}

	@Override
	public String toString() {
		return "IP: " + ip + " Port: " + port
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
