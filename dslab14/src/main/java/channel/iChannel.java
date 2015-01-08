package channel;

import java.io.IOException;
/**
 * interface for TcpChannel and Base64Channel, following the advice of assignment.
 * @author RomanT
 *
 */
public interface iChannel {
	/**
	 * send data to socket
	 * @throws IOException 
	 */
	void send(String message) throws IOException;
	
	/**
	 *  receive data from socket
	 *  @throws IOException on error
	 */
	String receive() throws IOException;

	/**
	 * closes socket 
	 * @throws IOException on error at closing operation
	 */
	void close() throws IOException;

	
}
