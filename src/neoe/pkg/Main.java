package neoe.pkg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import neoe.util.Config;
import neoe.util.Exec;
import neoe.util.FileUtil;
import neoe.util.FindJDK;
import neoe.util.Log;
import neoe.util.PyData;

public class Main {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			showHelp();
			return;
		}
		String cmd = args[0];
		Log.stdout = true;
		if ("help".equals(cmd)) {
			showHelp();
		} else if ("install-basic".equals(cmd)) {
			new Main().installBasic();
		} else if ("update".equals(cmd)) {
			new Main().update();
		} else if ("list".equals(cmd)) {
			new Main().list();
		} else if ("install".equals(cmd)) {
			new Main().install(args[1]);
		} else if ("install-all".equals(cmd)) {
			new Main().installAll();
		}

	}

	private void installAll() throws Exception {
		for (PkgInfo info : pkgs.values()) {
			install(info.name);
		}
	}

	private Map conf;
	private Map<String, PkgInfo> pkgs;

	Main() throws Exception {
		conf = (Map) PyData.parseAll(FileUtil.readString(getClass().getResourceAsStream("pkgs"), null), false);
		pkgs = new HashMap();
		for (Object o : (List) conf.get("pkgs")) {
			List row = (List) o;
			PkgInfo info = new PkgInfo();
			// name, desc, category, cmmand, git
			info.name = row.get(0).toString();
			info.desc = row.get(1).toString();
			info.category = row.get(2).toString();
			info.cmmand = row.get(3).toString();
			info.git = row.get(4).toString();
			pkgs.put(info.name, info);

		}
		Log.log(String.format("read %s packages", pkgs.size()));
	}

	private void installBasic() throws Exception {
		for (PkgInfo info : pkgs.values()) {
			if (info.category.equals("basic")) {
				install(info.name);
			}
		}
	}

	private void update() throws Exception {
		for (PkgInfo info : pkgs.values()) {
			if (pkgExists(info.name)) {
				update(info.name);
			}
		}
	}

	private boolean pkgExists(String name) {
		String rootDir = getRootDir();
		File f = new File(rootDir, name);
		return (f.isDirectory());
	}

	private static String getRootDir() {
		return System.getProperty("user.home") + "/.neoepkg";
	}

	private static String getHomeDir() {
		return System.getProperty("user.home");
	}

	private void list() {
		for (PkgInfo info : pkgs.values()) {
			System.out.println(String.format("%s\t-- %s", info.name, info.desc));
		}

	}

	private void install(String pkgname) throws Exception {
		PkgInfo info = pkgs.get(pkgname);
		if (info == null) {
			Log.log("[E]cannot find package:" + pkgname);
			return;
		}
		if (pkgExists(pkgname)) {
			Log.log("[D]package:" + pkgname + " already exists, try to update.");
			update(pkgname);
			return;
		}
		{// git clone
			// String cmd = String.format("git clone %s %s/%s", info.git,
			// getRootDir(), info.name);
			Exec exec = new Exec();
			exec.setCmd("git");
			exec.addArg("clone");
			exec.addArg("--depth", "1");
			exec.addArg(info.git);
			exec.addArg(getRootDir() + "/" + info.name);
			int ret = exec.execute();
			Log.log("exec ret=" + ret);
			if (ret == 0) {
				installScript(pkgname);
			}
		}
	}

	private void installScript(String pkgname) throws Exception {
		PkgInfo info = pkgs.get(pkgname);
		if (info.cmmand == null || info.cmmand.isEmpty())
			return;
		String rootdir = getRootDir();
		String bin = rootdir + "/bin/";
		String fn = bin + info.cmmand;
		File binDir = new File(bin);
		if (!binDir.exists()) {
			binDir.mkdirs();
		}

		String os;
		if (FindJDK.isWindows) {
			addToUserPathWindows(binDir);
			fn = fn + ".cmd";
			os = "windows";
			rootdir = rootdir.replace('/', '\\');
		} else {
			addToUserPathLinux(binDir);
			os = "linux";
		}
		String path;
		String txt = (String) Config.get(conf, path = String.format("scripts.%s.%s", pkgname, os));
		if (txt == null) {
			Log.log("path in config not found:" + path);
			return;
		}
		txt = replaceAll(txt, "$path", rootdir);
		FileUtil.save(txt.getBytes("utf8"), fn);

		if (!FindJDK.isWindows) {
			Set perms = new HashSet();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(new File(fn).toPath(), perms);
		}
	}

	private static String replaceAll(String txt, String a, String b) {
		int p1 = txt.indexOf(a);
		if (p1 < 0)
			return txt;
		else
			return txt.substring(0, p1) + b + replaceAll(txt.substring(p1 + a.length()), a, b);
	}

	private void addToUserPathLinux(File binDir) throws IOException {
		appendToFile(getHomeDir() + "/.bashrc",
				String.format("\nPATH=%s:$PATH\nexport PATH\n", binDir.getAbsolutePath()));
	}

	private void appendToFile(String fn, String txt) throws IOException {
		if (new File(fn).exists()) {
			String s = FileUtil.readString(new FileInputStream(fn), null);
			if (s.indexOf(txt) >= 0) {
				Log.log("path already added in " + fn);
				return;
			}
		}
		FileOutputStream out = new FileOutputStream(fn, true);
		out.write(txt.getBytes("utf8"));
		out.close();
		Log.log("path added to " + fn);
	}

	private void addToUserPathWindows(File binDir) throws Exception {
		String key = "Path";
		String path = System.getenv(key);
		if (path == null) {
			key = "PATH";
			path = System.getenv(key);
		}
		if (path == null)
			path = "";
		if (!path.endsWith(";"))
			path = path + ";";
		String me = binDir.getAbsolutePath();
		if (path.indexOf(me) < 0) {
			path = path + me;
			Exec exec = new Exec();
			exec.setCmd("setx");
			exec.addArg("PATH");
			exec.addArg(path);
			// exec.addArg("/M");
			int ret = exec.execute();
			Log.log("addToUserPath ret=" + ret);
		} else {
			Log.log("addToUserPath already added");
		}

	}

	private void update(String pkgname) throws Exception {
		PkgInfo info = pkgs.get(pkgname);
		if (info == null) {
			Log.log("[E]cannot find package:" + pkgname);
			return;
		}
		Log.log("update " + pkgname);
		Exec exec = new Exec();
		exec.setDir(getRootDir() + "/" + info.name);
		exec.setCmd("git");
		exec.addArg("pull");
		int ret = exec.execute();
		Log.log("exec ret=" + ret);
		if (ret == 0) {
			installScript(pkgname);
		}
	}

	private static void showHelp() {
		System.out.println("command: help  list  update  install  install-basic install-all");
	}

}
