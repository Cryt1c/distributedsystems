package controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeSet {

	private Set<Node> nodes = new HashSet<Node>();


	public NodeSet() {

	}
	
	public boolean alreadyIn(String signature) {
		for (Node element : nodes) {
			if (signature.equals(element.getSignature()))
				return false;
		}
		return true;
	}
	
	public void add(Node node) {
		nodes.add(node);
	}
	
	public Set<Node> getSet() {
		return nodes;
	}
	
	public String getOperators() {
		String operators = "";
		for(Node element : nodes) {
			if(element.isOnline()) {
				operators += element.getOperators();
			}
		}
		return operators;
	}

	public void checkStatus(Map<String, Long> lastpacket) {
		for (Node element : nodes) {
			element.checkStatus(lastpacket);
		}
		
	}

}
