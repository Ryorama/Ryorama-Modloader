package com.ryorama.modloader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.UUID;

public class RyoramaModloader {

    private static String dir = System.getProperty("user.dir");

    public static final Logger logger = LogManager.getLogger("Ryorama-ModLoader");

    public static boolean isDev =
            new File(dir + File.separator + "src").exists() &&
                    (new File(dir + File.separator + "build").exists() ||
                            new File(dir + File.separator + "build.gradle").exists()
                    );

    public static String[] gameArgs;
    public static URLLoader loader;

    public static ArrayList<String> mods = new ArrayList<>();

    public static boolean isServer = false;

    private static final ModLoader modLoader = new ModLoader();
    protected static final ArrayList<String> additionalURLs = new ArrayList<>();

    public static String getDir() {
        return dir;
    }

    public static void main(String[] args) {

        if (isDev) dir = dir + File.separator + "run";

        String version = "1.16.5";
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
                    logger.error("Failed to find main class", err);
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
                logger.error("Failed to find main class (step 2)", err);
            }
        }

        modLoader.loadMods();
        logger.info("Successfully loaded mods!");

        URL[] urls = new URL[mods.size() + 1 + additionalURLs.size()];

        logger.info("Setting up...");

        try {
            for (int i = 0; i < mods.size(); i++) {
                String s = mods.get(i);
                urls[i + 1] = new File(s).toURL();
            }

            for (int i = 0; i < additionalURLs.size(); i++) {
                String s = additionalURLs.get(i);
                urls[i + mods.size() + 1] = new File(s).toURL();
            }
        } catch (Throwable err) {
            logger.error("Failed to setup mods", err);
        }

        loader = new URLLoader(urls);

        logger.info("Starting Minecraft");
        try {
            loader.loadClass("net.minecraft.client.main.Main").getMethod("main", String[].class).invoke(null, (Object)args);
        } catch (Throwable err) {
            logger.error("Failed to start Minecraft", err);
        }
    }

    public static String findMCDir(boolean b) {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase();
        String mcDir;
        if (os.contains("win") && System.getenv("APPDATA") != null) {
            mcDir = System.getenv("APPDATA") + File.separator + ".minecraft";
        } else if (os.contains("mac")) {
            mcDir = home + File.separator + "Library" + File.separator + "Application Support" + File.separator + "minecraft";
        } else {
            mcDir = home + File.separator + ".minecraft";
        }
        return mcDir;
    }
}
