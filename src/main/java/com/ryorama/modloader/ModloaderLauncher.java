package com.ryorama.modloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JFrame;

import com.ryorama.modloader.api.IMod;

public class ModloaderLauncher {

	private static String dir = System.getProperty("user.dir");
	
	public static ModloaderLauncher INSTANCE = new ModloaderLauncher();
	
	public static Logger logger = new Logger();
	
	public static final ArrayList<Object> modsList = new ArrayList<>();
	
	private static ModUrlLoader loader;
	
	private static final HashMap<File, HashMap<String, byte[]>> classFiles = new HashMap<>();
	
	public static ArrayList<Class<?>> lockedClasses = new ArrayList<>();
	
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
	
	public static String[] gameArgs = null;

	public static void main(String[] args) throws ZipException, IOException {
		
		JFrame frame = null;
		
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
		ArrayList<String> mods = new ArrayList<>();
						
		lockedClasses.forEach(c -> {
			logger.append(c.getName() + '\n');
			try {
				loader.load(c.getName(), false);
			} catch (ClassNotFoundException ignored) {
			}
		});
		
		File modDir = new File(getDir() + "/mods");
		
		if (!modDir.exists()) modDir.mkdirs();
		else {
			for (File modFile : Objects.requireNonNull(modDir.listFiles())) {
				mods.add(modFile.getPath());
				ZipFile fileZip = new ZipFile(modFile);
				Stream<ZipEntry> entryStream = (Stream<ZipEntry>) fileZip.stream();
				HashMap<String, byte[]> entryBytes = new HashMap<>();
				classFiles.put(modFile.getAbsoluteFile(), entryBytes);
				entryStream.forEach((entry) -> {
					if (entry.isDirectory()) return;
					InputStream stream = null;
					ByteArrayOutputStream outStream = new ByteArrayOutputStream();
					try {
						stream = fileZip.getInputStream(entry);
						int b;
						while ((b = stream.read()) != -1) outStream.write(b);
					} catch (Throwable ignored) {
					}
					entryBytes.put(entry.toString(), outStream.toByteArray());
					if (stream != null) {
						try {
							stream.close();
						} catch (Throwable err) {
							err.printStackTrace();
						}
					}
					try {
						outStream.flush();
						outStream.close();
					} catch (Throwable ignored) {
					}
				});
			}
		}
		
		URL[] urls = new URL[mods.size() + 1 + additionalURLs.size()];
		
		for (int i = 0; i < mods.size(); i++) {
			String s = mods.get(i);
			urls[i + 1] = new File(s).toURL();
		}
		
		for (int i = 0; i < additionalURLs.size(); i++) {
			String s = additionalURLs.get(i);
			urls[i + mods.size() + 1] = new File(s).toURL();
		}
		
		loader = new ModUrlLoader(urls);
		
		try {
			for (String s : mods) {
				File fi1 = new File(s);
				try {
					Object mod = loader.load("entries." + fi1.getName().split("-")[0].replace("-", "").replace(".zip", "").replace(".jar", "") + ".Main", false).newInstance();
					modsList.add(mod);
				} catch (Throwable err) {
				}
			}
		} catch (Throwable err) {
		}
		
		try {
			setupMods(loader, args);
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
	
	public static void setupMods(ModUrlLoader loader, String[] args) throws ZipException, IOException {

		try {
			loader.load("com.ryorama.modloader.api.IMod", false).getMethod("onInitialize", null).invoke(null, (Object) args);
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
}
