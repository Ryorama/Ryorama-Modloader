package com.ryorama.modloader;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

public class ModloaderLauncher {

	private static String dir = System.getProperty("user.dir");
	
	public static ModloaderLauncher INSTANCE = new ModloaderLauncher();
	
	public static Logger logger = new Logger();
	
	public static final ArrayList<Object> mods = new ArrayList<>();
	
	private static ModUrlLoader loader;
	
	private static final HashMap<File, HashMap<String, byte[]>> classFiles = new HashMap<>();
	
	public static String getDir() {
		return dir;
	}
	
	public static boolean isDev =
			new File(dir + "\\src").exists() &&
					(new File(dir + "\\build").exists() ||
							new File(dir + "\\build.gradle").exists()
					);
	
	protected static final ArrayList<String> additionalURLs = new ArrayList<>();

	public static boolean isServer = false;
	

	public static void main(String[] args) {
		
		String version = "1.16.5-RyoamaModloadr";
		String gameDir = dir;
		String main_class = null;
		boolean isVersion = false;
		boolean isDir = false;
		boolean isMain = false;
		
		if (args.length == 0) {
			args = new String[]{
					"--username", "RyoramaModloader", "--assetsDir", findMCDir(false) + "\\assets\\", "--accessToken", "", "--uuid", UUID.randomUUID().toString(), "--userType", "mojang", "--versionType", "release"
			};
		}
		

		for (String s : args) {
			if (s.equals("--version")) {
				isVersion = true;
			} else if (isVersion) {
				version = s;
				isVersion = false;
			} else if (s.equals("--gameDir")) {
				isDir = true;
			} else if (isDir) {
				gameDir = s;
				isDir = false;
			} else if (s.equals("--main_class")) {
				isMain = true;
			} else if (isMain) {
				if (s.contains("MinecraftServer"))
					isServer = true;
				main_class = s;
				isMain = false;
			}
		}
		
		if (main_class == null) {
			File version_config = new File(dir + "\\versions\\" + version + "\\options.txt");
			if (!version_config.exists()) {
				try {
					version_config.getParentFile().mkdirs();
					version_config.createNewFile();
					FileWriter writer = new FileWriter(version_config);
					writer.write("main_class:net.minecraft.client.main.Main");
					writer.close();
				} catch (Throwable err) {
				}
			}
			
			try {
				Scanner sc = new Scanner(version_config);
				while (sc.hasNextLine()) {
					String source_line = sc.nextLine();
					String line = source_line.toLowerCase();
					if (line.startsWith("main_class:")) {
						main_class = source_line.substring("main_class:".length());
					}
				}
				sc.close();
			} catch (Throwable err) {
			}
		}
		
		URL[] urls = new URL[mods.size() + 1 + additionalURLs.size()];
		
		loader = new ModUrlLoader(urls);
		
		try {
			loader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, (Object)args);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static String findMCDir(boolean isDev) {
		String home = System.getProperty("user.home", ".");
		String os = System.getProperty("os.name").toLowerCase();
		String dir;
		if (!isDev) {
			if (os.contains("win") && System.getenv("APPDATA") != null) {
				dir = System.getenv("APPDATA") + File.separator + ".minecraft";
			} else if (os.contains("mac")) {
				dir = home + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
			} else {
				dir = home + ".minecraft";
			}
		} else {
			dir = ModloaderLauncher.getDir()+ "\\run";
		}
		return dir;
	}
}
