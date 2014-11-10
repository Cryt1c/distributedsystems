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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author David
 * 
 */
public class ControllerWorker implements Runnable {

	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private String logdir;
	private String compName;

	public ControllerWorker(Socket socket, String logdir, String name) {
		this.socket = socket;
		this.logdir = logdir;
		this.compName = name;
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

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (input != null) {
			result = calculate(input.split(" "));
			log(input.split(" "), result);
			writer.println(result);
		}
		
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// TODO: error divide by 0 not logged
	private synchronized void log(String[] input, String result) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		Date now = new Date();
		String date = sdf.format(now);
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
			if (op2 == 0)
				return "Error: Division by zero!";
			result = (int) ((double) op1 / (double) op2);
		}
		return "" + result;
	}
}
