package org.jboss.jenkins.local.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hudson.EnvVars;
import hudson.FilePath;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import jenkins.model.Jenkins;

public class Label {

	public static boolean GET_LATEST_ARCHIVE = true;
	public static boolean GET_LATEST_DOWNLOAD = false;

	private static final Logger log = Logger.getLogger(Label.class.getName());

	private static String labelsPath = "";

	public static String getLabelsPath() {
		return labelsPath;
	}

	private static String resourcesLabelsPath = "/defaultLabels.json";

	private String id;
	private String name;
	private boolean isM2Repo;

	public boolean isM2Repo() {
		return isM2Repo;
	}

	public void setM2Repo(boolean isM2Repo) {
		this.isM2Repo = isM2Repo;
	}

	// private FilePath latestRepoFileDownload;
	private FilePath latestRepoFileArchive;

	private String downloadPath;

	public String getDownloadPath() {
		return downloadPath;
	}

	private String archivePath;

	public String getArchivePath() {
		return archivePath;
	}

	public Label(String id, String name, String downloadPath, String archivePath, boolean isM2Repo) {
		this.name = name;
		this.id = id;
		this.downloadPath = downloadPath;
		this.archivePath = archivePath;
		this.isM2Repo = isM2Repo;
	}

	public static String decorate(String path, FilePath workspace, EnvVars env) {
		if (path == null) {
			return null;
		}
		String jenkinsRootPath = Jenkins.getInstance().getRootPath().getRemote(); // jenkinsRoot
		String jobWorkspacePath = workspace.getRemote(); // workspace

		for (Entry<String, String> entry : env.entrySet()) {
			String value = entry.getValue();
			String key = entry.getKey();
			if (path.contains("{^{" + key + "}}")) {
				path = path.replace("{^{" + key + "}}", value.toUpperCase());
			}
			if (path.contains("{!{" + key + "}}")) {
				path = path.replace("{!{" + key + "}}", value.toLowerCase());
			}
			if (path.contains("{{" + key + "}}")) {
				path = path.replace("{{" + key + "}}", value);
			}
		}

		return path.replace("{.{workspace}}", jobWorkspacePath).replace("{{workspace}}", jobWorkspacePath)
				.replace("{.{jenkinsRoot}}", jenkinsRootPath).replace("{{jenkinsRoot}}", jenkinsRootPath);
	}

	public FilePath getDownloadFilePath(FilePath workspace, EnvVars env) throws IOException, InterruptedException {
		if (getDownloadPath() == null)
			return null;
		return new FilePath(new File(Label.decorate(getDownloadPath(), workspace, env)));
	}

	public FilePath getArchiveFilePath(FilePath workspace, EnvVars env) throws IOException, InterruptedException {
		if (getArchivePath() == null)
			return null;
		return new FilePath(new File(Label.decorate(getArchivePath(), workspace, env)));
	}

	public FilePath getLatestRepoFileArchive(FilePath workspace, EnvVars env) throws IOException, InterruptedException {
		if (latestRepoFileArchive != null && latestRepoFileArchive.exists()) {
			return latestRepoFileArchive;
		}
		return latestRepoFileArchive = MasterMavenRepository.getLatestRepo(this, workspace, env);
	}

	/*
	 * public FilePath getLatestRepoFileDownload(FilePath workspace) throws
	 * IOException, InterruptedException { if (latestRepoFileDownload != null &&
	 * latestRepoFileDownload.exists()) { return latestRepoFileDownload; } return
	 * latestRepoFileDownload = MasterMavenRepository.getLatestRepo(this, workspace,
	 * GET_LATEST_DOWNLOAD); }
	 */
	public static Label getUsedLabelById(String label) {
		return Label.getListInstances().stream().filter(l -> l.getId().matches(label)).findAny().orElse(null);
	}

	public static List<Label> getListInstances() {
		// TODO: do not always override
		// return (labelsStatic == null ? (labelsStatic = (ArrayList<Label>)
		// loadFromFile()) : labelsStatic);
		try {
			String str = ArchiveMavenRepository.DescriptorImpl.getLabelsS();
			return (ArrayList<Label>) labelsStringToList(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}

	public static void copyResourceTo(String src, String target, boolean delete) throws IOException {
		File targetF = new File(target);

		targetF.getParentFile().mkdirs();

		if (delete == true) {
			targetF.delete();
			targetF.createNewFile();
		}

		InputStream inputStream = Label.class.getResourceAsStream(src);
		OutputStream outputStream = new FileOutputStream(target);

		IOUtils.copy(inputStream, outputStream);
		inputStream.close();
		outputStream.close();
	}

	public static String loadStringFromFile() throws IOException {
		return loadStringFromFile(
				Jenkins.getInstance().getRootDir().getAbsolutePath() + "/shared-maven-repository/config.json");
	}

	public static String loadStringFromFile(String path) throws IOException {
		if (labelsPath.isEmpty() || !labelsPath.matches(path)) {
			String configFileName = path;
			File configFile = new File(configFileName);

			if (!configFile.exists()) {
				copyResourceTo(resourcesLabelsPath, configFileName, false);
			}
			labelsPath = configFile.getAbsolutePath();
		}
		InputStream is = new FileInputStream(new File(labelsPath));
		return getStringFromInputStream(is);
	}

	public static void saveLabels() {
		try {
			FileUtils.writeStringToFile(new File(labelsPath), ArchiveMavenRepository.DescriptorImpl.getLabelsS());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static List<Label> labelsStringToList(String labels) throws IOException {
		JSONObject labelsJson = labelsStringToJSON(labels);

		List<Label> l = new ArrayList<Label>();

		((Collection<String>) (Collection<?>) labelsJson.keySet()).stream().forEach(key -> {
			if (labelsJson.has(key)) {
				Object obj = labelsJson.get(key);
				if (obj instanceof String) {
					l.add(new Label(key, (String) obj, null, null, false));
				} else if (obj instanceof JSONObject) {
					JSONObject label = (JSONObject) obj; // labelsJson.getJSONObject(key);
					l.add(new Label(key, label.getString("name"),
							label.has("downloadPath") ? label.getString("downloadPath") : null,
							label.has("archivePath") ? label.getString("archivePath") : null,
							label.has("isM2Repo") ? label.getBoolean("isM2Repo") : false));
				}
			}

		});

		return l;
	}

	public static JSONObject labelsStringToJSON(String labels) {
		return (JSONObject) JSONSerializer.toJSON(labels);
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Label-Id: " + getId() + ";Name:" + getName() + ";Archive:" + this.getArchivePath() + ";Download:"
				+ getDownloadPath();
	}

	public static void deleteDir(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteDir(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

}
