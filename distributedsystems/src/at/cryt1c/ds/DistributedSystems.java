/**
 * 
 */
package at.cryt1c.ds;

/**
 * @author David Peherstorfer
 *
 */

import java.net.*;
import java.io.*;

public class DistributedSystems {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		String address = "stockholm.vitalab.tuwien.ac.at";
		int port = 9000;
		String command = "!login 0929021 30361";
		try {
			Socket socket = new Socket(address, port);
			
			
			BufferedReader input =	new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter output = new PrintWriter(socket.getOutputStream(),true);
			
			output.println(command);
			System.out.println(input.readLine());
			System.out.println(input.readLine());
			socket.close();
		
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			System.err.println("Don't know about host " + address);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("Couldn't get I/O for the connection to " + address);
			e.printStackTrace();
		}
	}

}
