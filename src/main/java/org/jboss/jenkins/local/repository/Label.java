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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jfree.util.Log;

import net.sf.json.JSONObject;
//import net.sf.json.;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
import net.sf.json.JSONSerializer;
import jenkins.model.Jenkins;

public class Label {

	private static ArrayList<Label> labelsStatic;

	private static JSONObject labels;
	private static String labelsPath = "";

	private static String resourcesLabelsPath = "/defaultLabels.json";

	private String name;
	private String id;

	public Label(String id, String name) {
		super();
		this.name = name;
		this.id = id;
	}

	public static List<Label> getListInstances() {
		// TODO: do not always override
		// return (labelsStatic == null ? (labelsStatic = (ArrayList<Label>)
		// loadFromFile()) : labelsStatic);
		try {
			return labelsStatic = (ArrayList<Label>) loadFromFile();
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
		}

		return sb.toString();
	}

	public static String getCurrentLabel() {

		return "default";
	}

	private static List<Label> loadFromFile() throws IOException {
		if (labelsPath.isEmpty()) {
			System.out.println("Jenkins.getInstance().getRootDir(): " + Jenkins.getInstance().getRootDir());
			File configFile = new File(
					Jenkins.getInstance().getRootDir().getAbsolutePath() + "/shared-maven-repository/config.json");
			if (!configFile.exists()) {
				configFile.getParentFile().mkdirs();
				if (!configFile.createNewFile()) {
					System.err.println("Unable to create new config file for plugin shared-maven-repository with path: "
							+ configFile.getAbsolutePath());
					return Collections.emptyList();
				}
				System.out.println("resourcesLabelsPath: " + resourcesLabelsPath);
				InputStream inputStream = Label.class.getResourceAsStream(resourcesLabelsPath);
				OutputStream outputStream = new FileOutputStream(configFile);

				System.out.println("configFile: " + configFile.getAbsolutePath());
				System.out.println("inputStream: " + getStringFromInputStream(inputStream));
				inputStream = Label.class.getResourceAsStream(resourcesLabelsPath);

				IOUtils.copy(inputStream, outputStream);
				inputStream.close();
				outputStream.close();
			}
			labelsPath = configFile.getAbsolutePath();
			System.out.println("labelsPath: " + labelsPath);
		}
		System.out.println("JSONParser.labelsPath: " + labelsPath);
		InputStream is = new FileInputStream(new File(labelsPath));

		labels = (JSONObject) JSONSerializer.toJSON(IOUtils.toString(is));

		List<Label> l = new ArrayList<Label>();

		((Collection<String>) (Collection<?>) labels.keySet()).stream().forEach(key -> {
			l.add(new Label(key, (String) labels.get(key)));
		});

		return l;
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
