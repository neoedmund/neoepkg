package neoe.pkg;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import neoe.util.FileUtil;
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
		Log.log(String.format("load %s packages", pkgs.size()));
	}

	private void installBasic() {
		for (PkgInfo info : pkgs.values()) {
			if (info.category.equals("basic")) {
				install(info.name);
			}
		}
	}

	private void update() {
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

	private String getRootDir() {
		return System.getProperty("user.home") + "/.neoepkg";
	}

	private void list() {
		for (PkgInfo info : pkgs.values()) {
			System.out.println(String.format("%s\t-- %s", info.name, info.desc));
		}

	}

	private void install(String pkgname) {
		PkgInfo info = pkgs.get(pkgname);
		if (info == null) {
			Log.log("[E]cannot find package:" + pkgname);
			return;
		}
		if (pkgExists(pkgname)) {
			Log.log("[D]package:" + pkgname + " already exists, try to update.");
			update(pkgname);
		}
	}

	private void update(String pkgname) {
		// TODO Auto-generated method stub

	}

	private static void showHelp() {
		System.out.println("command: help  list  update  install  install-basic");
	}

}
