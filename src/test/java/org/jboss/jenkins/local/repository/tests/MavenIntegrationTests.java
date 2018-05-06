package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang.SerializationUtils;
import org.jboss.jenkins.local.repository.ArchiveMavenRepository;
import org.jboss.jenkins.local.repository.DownloadMavenRepository;
import org.jboss.jenkins.local.repository.Label;
import org.jboss.jenkins.local.repository.JenkinsSlaveArchiveCallable;
import org.jboss.jenkins.local.repository.JenkinsSlaveDownloadCallable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.xml.sax.SAXException;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.console.ConsoleNote;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.model.Node.Mode;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

/**
 * These tests cover scenarios following patter:
 * 
 * 1. Start jenkins with this plugin 2. Create new simple project 3. Set
 * configuration (Download, Archive, necessary requirements - files, etc.) 4.
 * Verify results
 * 
 * @author vprusa Following https://wiki.jenkins.io/display/JENKINS/Unit+Test
 */
public class MavenIntegrationTests extends MavenIntegrationTestsBase {

	private static final Logger log = Logger.getLogger(MavenIntegrationTests.class.getName());

	String tmpArchivePath = "/tmp/jenkins/archive.zip";

	/**
	 * label-root
	 */
	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDownloadAndArchiveForRootPath()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = "{{jenkinsRoot}}/workspace/" + projectName + "/repository";
		String archivePath = "/tmp/jenkins/archive/";
		new File(archivePath).mkdirs();

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();
		assertFalse("Mehotd Label.getLabelsPath() should not return empty value by now",
				Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
	}

	/**
	 * label-workspace
	 */
	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDownloadAndArchiveForWorkspacePath()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = "{{workspace}}/repository";
		String archivePath = "/tmp/jenkins/archive/";
		new File(archivePath).mkdirs();

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();

		assertFalse("Mehotd Label.getLabelsPath() should not return empty value by now",
				Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
	}

	/**
	 * label serialization test
	 */
	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testSerialization() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = "{{workspace}}/repository";
		String archivePath = "/tmp/jenkins/archive/";

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();
		Label label = new Label(usedLabel, usedLabel, downloadPath, archivePath, false);
		ArrayList<FreeStyleBuild> builds = configureBildAndVerify(1, usedLabel, usedLabel);

		FreeStyleBuild last = builds.get(0);

		EnvVars env = new EnvVars();
		FilePath workspace = project.getSomeWorkspace();
		JenkinsSlaveArchiveCallable slaveTask = new JenkinsSlaveArchiveCallable(label, workspace, env);

		SerializationUtils.serialize(label);
		SerializationUtils.serialize(workspace);
		SerializationUtils.serialize(env);
		// SerializationUtils.serialize(listener);
		SerializationUtils.serialize(slaveTask);
	}

	/**
	 * label-workspace
	 * 
	 * @throws FormException
	 */
	@SuppressWarnings({ "deprecation", "rawtypes" })
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testSlave() throws IOException, InterruptedException, ExecutionException, SAXException, FormException {
		String usedLabel = "label";
		String downloadPath = "{{workspace}}/repository";
		String archivePath = "/tmp/jenkins/archive/";
		new File(archivePath).mkdirs();

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();
		// String name, String nodeDescription, String remoteFS, String numExecutors,
		// Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy
		// retentionStrategy, List<? extends NodeProperty<?>> nodeProperties
		final String name = "agent1";
		String nodeDescription = "agent1";
		String jenkinsSlavePath = System.getProperty("user.dir");// "/home/jenkins/agent";
		String remoteFS = "/home/jenkins/agent";
		String numExecutors = "1";
		Mode mode = DumbSlave.Mode.EXCLUSIVE;
		String labelString = "agent1";
		ComputerLauncher launcher = null;
		RetentionStrategy retentionStrategy = RetentionStrategy.Always.INSTANCE;

		ArrayList<NodeProperty> nodeProperties = new ArrayList<NodeProperty>();

		DumbSlave slave = new DumbSlave(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher,
				retentionStrategy);

		Jenkins.getInstance().addNode(slave);

		project.setAssignedNode(slave);

		// TODO use docker slave
		// Runtime.getRuntime().exec();
		File jenkinsSlave = new File(jenkinsSlavePath);
		// jenkinsSlave.mkdirs();

		new File(jenkinsSlave.getAbsolutePath() + "/slave.jar").delete();

		String wgetCmd = "wget " + Jenkins.getInstance().getRootUrl() + "/jnlpJars/slave.jar -P " + remoteFS;
		Process wget = Runtime.getRuntime().exec(wgetCmd);

		assertTrue("Cmd " + wgetCmd + " should have already finished", wget.waitFor(10, TimeUnit.SECONDS));

		Label.copyResourceTo("/dockerfile", jenkinsSlave.getAbsolutePath() + "/dockerfile", true);

		Thread threadDockerSlave = new Thread() {
			public void run() {
			
				// TODO stop on the end
				// docker stop $(docker ps -aq --filter ancestor=j:s)

				String runDockerImageCmd = "docker run --net=host j:s";
				
				final Process buildAndRunDockerImage;
				try {
					buildAndRunDockerImage = Runtime.getRuntime().exec(runDockerImageCmd);

					Thread threadLogErr = new Thread() {
						public void run() {
							String line;
							BufferedReader error = new BufferedReader(
									new InputStreamReader(buildAndRunDockerImage.getErrorStream()));
							try {
								while ((line = error.readLine()) != null) {
									log.info("SlaveErr" + line);
								}
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								try {
									error.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					};
					threadLogErr.start();

					Thread threadLogOut = new Thread() {
						public void run() {
							String line;
							BufferedReader input = new BufferedReader(
									new InputStreamReader(buildAndRunDockerImage.getInputStream()));
							try {
								while ((line = input.readLine()) != null) {
									log.info("SlaveOut" + line);
								}
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								try {
									input.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					};
					threadLogOut.start();

					assertTrue("Cmd " + runDockerImageCmd + " should have already finished",
							buildAndRunDockerImage.waitFor(360, TimeUnit.SECONDS));
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}

			}
		};
		threadDockerSlave.start();

		assertFalse("Mehotd Label.getLabelsPath() should not return empty value by now",
				Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
	}

}
