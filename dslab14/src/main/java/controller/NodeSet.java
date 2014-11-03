package controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeSet {

	private Set<Node> nodes = new HashSet<Node>();

	public NodeSet() {

	}

	public boolean add(Node node) {
		for (Node element : nodes) {
			if (node.signature().equals(element.signature()))
				return false;
		}
		nodes.add(node);
		return true;
	}

//	public Set<Node> getSet() {
//		Set<Node> copy = new HashSet<Node>();
//		Iterator<Node> iterator = nodes.iterator();
//		while (iterator.hasNext()) {
//			copy.add(iterator.next().clone());
//		}
//		return nodes;
//	}
	
	public Set<Node> getSet() {
		return nodes;
	}

	public void checkStatus(Map<String, Long> lastpacket) {
		for (Node element : nodes) {
			element.checkStatus(lastpacket);
		}
	}

}
