package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class ComputationRequestInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private List<String> data = new LinkedList<String>();

	public ComputationRequestInfo(String logdir, String name) {
		this.setName(name);
		File f = new File(logdir + "/");
		File[] filelist = f.listFiles();
		if (filelist != null) {
			for (File file : filelist) {
				if (file.isFile()) {
					try {
						FileReader fr = new FileReader(file);
						BufferedReader br = new BufferedReader(fr);
						String row1 = br.readLine();
						String row2 = br.readLine();
						String calc = row1 + " = " + row2;

						String timeStamp = file.getName().substring(0, 19);
						getData().add(timeStamp + " [" + name + "] " + calc + "\n");
						br.close();
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getData() {
		return data;
	}

	public void setData(List<String> data) {
		this.data = data;
	}
}
