package org.jboss.jenkins.local.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jfree.util.Log;

import hudson.FilePath;
import net.sf.json.JSONObject;
//import net.sf.json.;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
import net.sf.json.JSONSerializer;
import jenkins.model.Jenkins;

public class Label {

    private static final Logger log = Logger.getLogger(Label.class.getName());

	private static String labelsPath = "";

	private static String resourcesLabelsPath = "/defaultLabels.json";

	private String name;
	private String id;
	private FilePath latestRepoFile;

	public Label(String id, String name) {
		super();
		this.name = name;
		this.id = id;
	}

	public FilePath getLatestRepoFile() {
		return latestRepoFile;
	}

	public void setLatestRepoFile(FilePath latestRepoFile) {
		this.latestRepoFile = latestRepoFile;
	}
	
	
	public static Label getUsedLabelById(String label) {
		return Label.getListInstances().stream().filter(l->l.getId().matches(label)).findAny().orElse(null);
	}

	public static List<Label> getListInstances() {
		// TODO: do not always override
		// return (labelsStatic == null ? (labelsStatic = (ArrayList<Label>)
		// loadFromFile()) : labelsStatic);
		try {
			//return labelsStatic = (ArrayList<Label>) loadFromFile();
			return (ArrayList<Label>) labelsStringToList(ArchiveMavenRepository.DescriptorImpl.getLabelsS());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	// convert InputStream to String
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

	public static String getCurrentLabel() {
		return "default";
	}
	
	public static String loadStringFromResourceFile() throws IOException {
		log.info("loadStringFromResourceFile: " + resourcesLabelsPath);
		return getStringFromInputStream(Label.class.getResourceAsStream(resourcesLabelsPath));
	}
	
	public static String loadStringFromFile() throws IOException {
		if (labelsPath.isEmpty()) {
			log.info("Jenkins.getInstance().getRootDir(): " + Jenkins.getInstance().getRootDir());
			File configFile = new File(
					Jenkins.getInstance().getRootDir().getAbsolutePath() + "/shared-maven-repository/config.json");
			if (!configFile.exists()) {
				configFile.getParentFile().mkdirs();
				if (!configFile.createNewFile()) {
					System.err.println("Unable to create new config file for plugin shared-maven-repository with path: "
							+ configFile.getAbsolutePath());
					return "";
				}
				log.info("resourcesLabelsPath: " + resourcesLabelsPath);
				InputStream inputStream = Label.class.getResourceAsStream(resourcesLabelsPath);
				OutputStream outputStream = new FileOutputStream(configFile);

				log.info("configFile: " + configFile.getAbsolutePath());
				//log.info("inputStream: " + getStringFromInputStream(inputStream));
				inputStream = Label.class.getResourceAsStream(resourcesLabelsPath);
				
				IOUtils.copy(inputStream, outputStream);
				inputStream.close();
				outputStream.close();
			}
			labelsPath = configFile.getAbsolutePath();
			log.info("labelsPath: " + labelsPath);
		}
		log.info("JSONParser.labelsPath: " + labelsPath);
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
	
	/*
	public static List<Label> loadFromFile() throws IOException {
		labels = (JSONObject) JSONSerializer.toJSON(loadStringFromFile());

		List<Label> l = new ArrayList<Label>();

		((Collection<String>) (Collection<?>) labels.keySet()).stream().forEach(key -> {
			l.add(new Label(key, (String) labels.get(key)));
		});

		return l;
	}
	*/

	public static List<Label> labelsStringToList(String labels) throws IOException {
		JSONObject labelsJson = labelsStringToJSON(labels);

		List<Label> l = new ArrayList<Label>();

		((Collection<String>) (Collection<?>) labelsJson.keySet()).stream().forEach(key -> {
			l.add(new Label(key, (String) labelsJson.get(key)));
		});

		return l;
	}


	public static JSONObject labelsStringToJSON(String labels) {
		return (JSONObject) JSONSerializer.toJSON(labels);
	}

	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}
