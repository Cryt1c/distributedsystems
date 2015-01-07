/**
 * 
 */
package node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import util.Keys;

/**
 * @author David
 * 
 */
public class NodeWorker implements Runnable {

	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private String logdir;
	private String compName;
	private Node node;
	private String keydir;

	public NodeWorker(Socket socket, String logdir, String keydir, String name,
			Node node) {
		this.socket = socket;
		this.logdir = logdir;
		this.keydir = keydir;
		this.compName = name;
		this.node = node;
		Thread.currentThread().setName("controllerworker");
		try {
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			writer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		String input = null;

		String result;
		try {
			input = reader.readLine();

			if (input != null) {
				String[] temp = input.split(" ");
				// another Node is filing a share request
				if (temp[0].contains("!share")) {
					System.out.println("Receiver got: !share " + temp[1]);
					answerNodes(temp[1]);
					input = reader.readLine();

					temp = input.split(" ");
					if (temp[0].contains("!commit")) {
						System.out.println("Receiver got: !commit " + temp[1]);
						node.setRmax(Integer.parseInt(temp[1]));
					}
				}
				// another Node stopped the share request
				else if (temp[0].contains("!rollback")) {
					System.out.println("Receiver got: !rollback " + temp[1]);
				}

				// Received String is a calculation
				else {
					System.out.println("Node got: new calc");

					// check if HMAC is correct
					Key secretKey = Keys.readSecretKey(new File(keydir));
					Mac hMac = Mac.getInstance("HmacSHA256");
					hMac.init(secretKey);
					hMac.update((temp[1] + " " + temp[2] + " " + temp[3])
							.getBytes());
					byte[] computedHash = hMac.doFinal();
					byte[] receivedHash = Base64.decode(temp[0].getBytes());
					boolean validHash = MessageDigest.isEqual(computedHash,
							receivedHash);
					if (false) {
						temp = Arrays.copyOfRange(temp, 1, temp.length);
						result = calculate(temp);
						log(temp, result);
						writer.println(result);
					}
					else {
						result = new String(Base64.encode(computedHash)) + " !tampered " + temp[1] + " " + temp[2] + " " + temp[3];
						System.out.println(result);
						writer.println(result);
					}
				}
			}
		} catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}
		this.close();
	}

	private void answerNodes(String input) {
		System.out.println("share: " + input + " rmin: " + node.getRmin());
		if (Integer.parseInt(input) > node.getRmin()) {
			writer.println("!ok");
			System.out.println("sent: !ok");
		} else {
			writer.println("!nok");
			System.out.println("Receiver sent: !nok");
		}
	}

	// closes writer, reader and socket
	private void close() {
		try {
			if (writer != null)
				this.writer.close();
			if (reader != null)
				this.reader.close();
			if (socket != null && !socket.isClosed())
				this.socket.close();
		} catch (IOException e) {
			System.out.println("Error closing writer, reader or socket");
			e.printStackTrace();
		}
	}

	private static final ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		}
	};

	private void log(String[] input, String result) {
		Date now = new Date();
		String date = formatter.get().format(now);
		PrintWriter fileWriter = null;

		try {
			File file = new File(logdir + "/" + date + "_" + compName + ".log");
			file.getParentFile().mkdirs();
			fileWriter = new PrintWriter(file, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fileWriter.println(input[0] + " " + input[1] + " " + input[2]);
		fileWriter.println(result);
		fileWriter.close();

	}

	private String calculate(String[] calc) {
		int op1;
		try {
			op1 = Integer.parseInt(calc[0]);
		} catch (NumberFormatException e) {
			return "Error: Division by zero!";
		}
		int op2 = Integer.parseInt(calc[2]);
		int result = 0;

		if (calc[1].equals("+")) {
			result = op1 + op2;
		} else if (calc[1].equals("-")) {
			result = op1 - op2;
		} else if (calc[1].equals("*")) {
			result = op1 * op2;
		} else if (calc[1].equals("/")) {
			if (op2 == 0) {
				return "Error: division by 0";
			}
			result = Math.round((float) op1 / op2);
		}
		return "" + result;
	}
}
