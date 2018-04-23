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
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hudson.FilePath;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import jenkins.model.Jenkins;

public class Label {

	private static final Logger log = Logger.getLogger(Label.class.getName());

	private static String labelsPath = "";

	public static String getLabelsPath() {
		return labelsPath;
	}

	private static String resourcesLabelsPath = "/defaultLabels.json";

	private String id;
	private String name;

	private FilePath latestRepoFileDownload;
	private FilePath latestRepoFileArchive;

	private String downloadPath;

	public String getDownloadPath() {
		return downloadPath;
	}

	private String archivePath;

	public String getArchivePath() {
		return archivePath;
	}

	public Label(String id, String name, String downloadPath, String archivePath) {
		this.name = name;
		this.id = id;
		this.downloadPath = downloadPath;
		this.archivePath = archivePath;
	}

	public FilePath getLatestRepoFileArchive() throws IOException, InterruptedException {
		if (latestRepoFileArchive != null && latestRepoFileArchive.exists()) {
			return latestRepoFileArchive;
		}
		return latestRepoFileArchive = MasterMavenRepository.getLatestRepo(this, true);
	}
	
	public FilePath getLatestRepoFileDownload() throws IOException, InterruptedException {
		if (latestRepoFileDownload != null && latestRepoFileDownload.exists()) {
			return latestRepoFileDownload;
		}
		return latestRepoFileDownload = MasterMavenRepository.getLatestRepo(this, false);
	}

	public static Label getUsedLabelById(String label) {
		return Label.getListInstances().stream().filter(l -> l.getId().matches(label)).findAny().orElse(null);
	}

	public static List<Label> getListInstances() {
		// TODO: do not always override
		// return (labelsStatic == null ? (labelsStatic = (ArrayList<Label>)
		// loadFromFile()) : labelsStatic);
		try {
			return (ArrayList<Label>) labelsStringToList(ArchiveMavenRepository.DescriptorImpl.getLabelsS());
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
		if (labelsPath.isEmpty()) {
			String configFileName = Jenkins.getInstance().getRootDir().getAbsolutePath()
					+ "/shared-maven-repository/config.json";
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
			JSONObject label = labelsJson.getJSONObject(key);
			l.add(new Label(key, label.getJSONObject("name").toString(), label.getJSONObject("downloadPath").toString(),
					label.getJSONObject("archivePath").toString()));
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

}
