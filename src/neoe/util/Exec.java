package neoe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import neoe.util.Log;

public class Exec {

	List<String> sb;

	public void setCmd(String executable) {
		sb = new ArrayList<>();
		sb.add(executable);
	}

	public void addArg(String s) {
		sb.add(s);
	}

	public void addArg(String s1, String s2) {
		sb.add(s1);
		sb.add(s2);
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	String dir = null;

	public int execute() throws Exception {
		Process p = new ProcessBuilder().directory(dir==null?null:new File(dir)).command(sb).start();
		StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "stderr");
		StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "stdout");
		outputGobbler.start();
		errorGobbler.start();
		return p.waitFor();
	}

	private class StreamGobbler extends Thread {
		InputStream is;
		String type;
		private PrintWriter out;

		private StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
			this.out = Log.getWriter();
		}

		@Override
		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null)
					out.println(type + "> " + line);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

}
