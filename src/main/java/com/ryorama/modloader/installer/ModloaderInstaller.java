package com.ryorama.modloader.installer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.TextArea;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.ryorama.modloader.ModJson;
import com.ryorama.modloader.ModloaderLauncher;

import net.lingala.zip4j.ZipFile;

public class ModloaderInstaller {
	
	private static final ModloaderInstaller INSTANCE = new ModloaderInstaller();

	public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		AtomicBoolean clicked = new AtomicBoolean(false);
		JFrame mainFrame = new JFrame("Ryorama Modloader Installer");
		mainFrame.setPreferredSize(new Dimension(1000, 400));
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(0, 255, 255));
		JPanel textPanel = new JPanel();
		textPanel.setSize(320, 140);
		textPanel.setBackground(new Color(0, 255, 255));
		JTextField setVersionPath = new JTextField();
		setVersionPath.setSize(320, 140);
		setVersionPath.setText(InstallerUtils.findVersionsDir() + File.separator + "1.16.5");
		textPanel.add(setVersionPath);

		JPanel installPanel = new JPanel();
		installPanel.setBackground(new Color(0, 255, 255));
		JButton installButton = new JButton("Install for 1.16.5");
		installButton.addActionListener(e -> {
			setVersionPath.setEnabled(false);
			installButton.setEnabled(false);
			clicked.set(true);
		});
		installPanel.setLocation(0, -100);
		installPanel.add(installButton);

		JPanel logPanel = new JPanel();
		logPanel.setSize(50, 50);
		logPanel.setMinimumSize(logPanel.getSize());
		logPanel.setMaximumSize(logPanel.getSize());
		TextArea log = new TextArea();
		log.setBackground(new Color(0, 204, 204));
		log.setEditable(false);
		logPanel.add(log);

		panel.add(textPanel);
		panel.add(installPanel);
		panel.add(logPanel);

		mainFrame.add(panel);
		mainFrame.pack();
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);

		INSTANCE.install(mainFrame, log, clicked, setVersionPath, installButton);
	}

	public void install(JFrame mainFrame, TextArea log, AtomicBoolean clicked, JTextField setVersionPath, JButton installButton) {
		while (mainFrame.isVisible()) {

			while (installButton.isEnabled()) {
				String versionPath = setVersionPath.getText();
				String versionNumber = new File(versionPath).getName();
				installButton.setText("Install for " + versionNumber);
			}
			while (!clicked.get());

			clicked.set(false);

			try  {
				AtomicBoolean downloadedFromUrl = new AtomicBoolean(false);
				log.setText("");
				log.append("\nStart Installation");
				long start = System.nanoTime();
				Gson gson = new Gson();
				String versionPath = setVersionPath.getText();
				String versionNumber = new File(versionPath).getName();
				File modloaderInstaller = new File(ModloaderInstaller.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
				File inputMinecraftJar = new File(versionPath + File.separator + versionNumber + ".jar");
				String versions = InstallerUtils.readUrl("https://launchermeta.mojang.com/mc/game/version_manifest.json");
				File jsonIn = new File(versionPath + File.separator + versionNumber + ".json");
				File outputModloaderDir = new File(versionPath + "-RyoramaModloader");
				File fullOutput = new File(outputModloaderDir + File.separator + versionNumber + "-RyoramaModloader.jar");
				if (!jsonIn.exists()) {
					log.setForeground(Color.yellow);
					log.append("\nWARN:No " + versionNumber + " json file found! The installer will try to download it from web.\nBe sure to have internet connection.");
					MinecraftVersionMeta meta = gson.fromJson(versions, MinecraftVersionMeta.class);
					boolean found = false;
					for (MinecraftVersionMeta.Version version : meta.versions) {
						if (version.id.equals(versionNumber)) {
							jsonIn.getParentFile().mkdirs();
							jsonIn.createNewFile();
							InstallerUtils.downloadFromUrl(version.url, jsonIn.getPath());
							found = true;
							log.append("\nJson downloaded!");
							log.setForeground(Color.black);
							break;
						}
					}
					if (!found) {
						log.setForeground(Color.red);
						log.append("\nERROR:No " + versionNumber + " json found! VERSION NOT EXISTING!!!");
						throw new IOException("Version not existing.");
					}
				}
				if (!outputModloaderDir.exists()) outputModloaderDir.mkdirs();
				if (!inputMinecraftJar.exists()) {
					log.setForeground(Color.yellow);
					log.append("\nWARN:No " + versionNumber + " version file found, but Json exists! The installer will try to download the jar from web.\nBe sure to have internet connection.");
					JsonParser parser = new JsonParser();
					JsonElement tree = parser.parse(Files.newBufferedReader(jsonIn.toPath()));

					JsonObject downloads = InstallerUtils.readJsonObject(tree.getAsJsonObject(), s -> s.equals("downloads"));
					JsonObject client = InstallerUtils.readJsonObject(Objects.requireNonNull(downloads).getAsJsonObject(), s -> s.equals("client"));
					for (Map.Entry<String, JsonElement> clientEntry : Objects.requireNonNull(client).entrySet()) {
						if (clientEntry.getKey().equals("url")) {
							InstallerUtils.downloadFromUrl(clientEntry.getValue().getAsString(), fullOutput.getPath());
							downloadedFromUrl.set(true);
							break;
						}
					}
				}

				File modloaderTmpDir = new File(outputModloaderDir + File.separator + "tmp");
				File jsonOut = new File(outputModloaderDir + File.separator + versionNumber + "-RyoramaModloader.json");
				if (!downloadedFromUrl.get() && fullOutput.exists()) {
					fullOutput.delete();
					fullOutput.createNewFile();
				}

				log.setForeground(Color.black);
				if ((!modloaderTmpDir.exists() || modloaderTmpDir.length() == 0)) {
					if (downloadedFromUrl.get() || fullOutput.length() == 0) {
						log.append("\nUnzipping modloader jar...");
						InstallerUtils.unzip(modloaderTmpDir.getPath(), modloaderInstaller.getPath(), name -> (name.startsWith("com/ryorama/modloader") && name.endsWith(".class")));
						log.append("\nUnzipping finished");
					}
				}

				ZipFile zipFile = new ZipFile(fullOutput);

				if (!downloadedFromUrl.get()) {
					log.append("\nCopying Minecraft jar...");
					Files.copy(Files.newInputStream(inputMinecraftJar.toPath()), fullOutput.toPath(), StandardCopyOption.REPLACE_EXISTING);
					log.append("\nMinecraft jar copied");
				}

				log.append("\nZipping RyoramaModloader");
				File f = new File(modloaderTmpDir + File.separator + "com");
				if (!f.exists())
					f.mkdirs();
				zipFile.addFolder(f);
				log.append("\nZipping finished");

				if (!jsonOut.exists()) {
					log.append("\nWriting Json");
					ModJson launchJson = new ModJson(versionNumber + "-RyoramaModloader", versionNumber, "com.ryorama.modloader.ModloaderLauncher");
					launchJson.arguments.game = new ArrayList<>();
					String mavenUrl = "https://repo1.maven.org/maven2/";
					String asmRepo = "org.ow2.asm:asm";
					String asmVer = ":8.0.1";
					launchJson.libraries.add(new ModJson.Library(asmRepo + asmVer, mavenUrl));
					launchJson.libraries.add(new ModJson.Library(asmRepo + "-commons" + asmVer, mavenUrl));
					launchJson.libraries.add(new ModJson.Library(asmRepo + "-tree" + asmVer, mavenUrl));
					launchJson.libraries.add(new ModJson.Library(asmRepo + "-util" + asmVer, mavenUrl));
					launchJson.libraries.add(new ModJson.Library("org.apache.bcel:bcel:6.0", mavenUrl));

					try (Writer writer = Files.newBufferedWriter(jsonOut.toPath())) {
						JsonElement tree = gson.toJsonTree(launchJson);
						JsonWriter jsonWriter = new JsonWriter(writer);
						gson.toJson(tree, jsonWriter);
					}
					log.append("\nJson written");
				} else {
					log.append("\nJson already generated");
				}

				if (modloaderTmpDir.exists()) {
					log.append("\nDeleting RyoramaModloader temps...");
					Files.walk(Paths.get(modloaderTmpDir.getPath()))
							.sorted(Comparator.reverseOrder())
							.map(Path::toFile)
							.forEach(File::delete);
					log.append("\nRyoramaModloader Temps deleted");
				}
				if (downloadedFromUrl.get() && jsonIn.getParentFile().exists()) {
					log.append("\nDeleting download temps...");
					Files.walk(Paths.get(jsonIn.getParentFile().getPath()))
							.sorted(Comparator.reverseOrder())
							.map(Path::toFile)
							.forEach(File::delete);
					log.append("\nDownload Temps deleted");
				}
				long stop = System.nanoTime();
				log.append("\nDone!\n");
				installButton.setEnabled(true);
				setVersionPath.setEnabled(true);
				long timePassed = (stop - start) / 1000000;
				String timePass = Long.toString(timePassed);
				log.append("\nInstallation took " + timePass + " milliseconds.\n");
				downloadedFromUrl.set(false);
			} catch (Throwable err) {
				log.append("\n" + err.getMessage());
				for (StackTraceElement element : err.getStackTrace()) {
					log.append("\n" + element);
				}
				log.setForeground(Color.red);
				log.append("\nRestart installer");
				throw new RuntimeException(err);
			}
		}
	}
}
