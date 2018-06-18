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
import java.io.Reader;
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

	// String tmpArchivePath = "/tmp/jenkins/archive.zip";

	/**
	 * label-root
	 */
	//@Test
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
	//@Test
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
	//@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testSerialization() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = "{{workspace}}/repository";
		String archivePath = "/tmp/jenkins/archive/";

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();
		Label label = new Label(usedLabel, usedLabel, downloadPath, archivePath, false, null, "Local");
		ArrayList<FreeStyleBuild> builds = configureBildAndVerify(1, usedLabel, usedLabel);

		FreeStyleBuild last = builds.get(0);

		EnvVars env = new EnvVars();
		FilePath workspace = project.getSomeWorkspace();
		// Label label, FilePath workspace, EnvVars env, TaskListener listene
		JenkinsSlaveArchiveCallable slaveTask = new JenkinsSlaveArchiveCallable(workspace, env, null, label);

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
		final String archivePath = "/tmp/jenkins/archive/";
		new File(archivePath).mkdirs();

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath  + "','preferedCall':'{Node{agent1}}'}}");
		Label.saveLabels();
		// String name, String nodeDescription, String remoteFS, String numExecutors,
		// Mode mode, String labelString, ComputerLauncher launcher, RetentionStrategy
		// retentionStrategy, List<? extends NodeProperty<?>> nodeProperties
		final String name = "agent1";
		String nodeDescription = "agent1";
		String jenkinsSlavePath = System.getProperty("user.dir");// "/home/jenkins/agent";
		final String remoteFS = "/home/jenkins/agent";
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

		String wgetCmd = "wget " + Jenkins.getInstance().getRootUrl() + "/jnlpJars/slave.jar -P /home/jenkins/";
		Process wget = Runtime.getRuntime().exec(wgetCmd);

	//	assertTrue("Cmd " + wgetCmd + " should have already finished", wget.waitFor(10, TimeUnit.SECONDS));

		Label.copyResourceTo("/dockerfile", jenkinsSlave.getAbsolutePath() + "/dockerfile", true);
		final String testFileDirPathOnSlave = remoteFS + "/workspace/unitTest-1/repository/";
		final String testFilePathOnSlave = testFileDirPathOnSlave + testFileName;
		
		log.info("testFilePathOnSlave: " + testFilePathOnSlave);
		
		//DockerThread threadDockerSlave = new DockerThread();
		//threadDockerSlave.start();
		
		Thread threadDockerSlave = new Thread() {
			@Override
			public void run() {
				
				try {

					// TODO stop on the end
					// docker stop $(docker ps -aq --filter ancestor=j:s)
					// docker exec -it $(docker ps | grep j:s | awk '{print $1}') bash

					//final String runDockerImageCmd = "docker run --net=host j:s";
					String runDockerImageCmd = "docker run --name js --net=host -v " + archivePath + ":" + archivePath + " j:s";
					//String runDockerImageCmd = "docker run --net=host -v " + archivePath + ":" + archivePath + " j:s mkdir -p "+testFileDirPathOnSlave+" && touch "+testFilePathOnSlave+"";
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave,"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave,"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c 'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash -c 'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +""};

					//Process buildAndRunDockerImage;
					Process buildAndRunDockerImage = Runtime.getRuntime().exec(runDockerImageCmd);

					startLogThread("SlaveErr", new InputStreamReader(buildAndRunDockerImage.getErrorStream()));
					startLogThread("SlaveOut", new InputStreamReader(buildAndRunDockerImage.getInputStream()));

					Thread threadDockerSlaveCreateTestFileDir = new Thread() {
						@Override 
						public void run() {
							try {
								Thread.currentThread().sleep(7000);
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							try {
								//String createTestFileOnSlaveCmd2 = "docker exec -it $(docker ps | grep j:s | awk '{print $1}') mkdir -p " + testFileDirPathOnSlave;
								//String createTestFileOnSlaveCmd2 = "docker exec -it \\$(docker ps | grep j:s | awk '{print \\$1}') bash -c 'mkdir -p "+testFileDirPathOnSlave+" && touch "+ testFilePathOnSlave +";'";
								//String createTestFileOnSlaveCmd2 = "docker exec -it js bash -c 'mkdir -p "+testFileDirPathOnSlave+"; touch "+ testFilePathOnSlave +";'";
								//String createTestFileOnSlaveCmd2 = "docker exec -i js bash -c 'mkdir -p "+testFileDirPathOnSlave+"; touch "+ testFilePathOnSlave +";'";
								String createTestFileOnSlaveCmd2 = "docker exec -i js mkdir -p "+testFileDirPathOnSlave;
								//String[] createTestFileOnSlaveCmd2 = new String[] {"docker","exec","-it","$(","docker","ps","|",		"grep","j:s","|","awk","'{","print","$1","}'",")","mkdir","-p",testFileDirPathOnSlave};
								
								Process createTestFileOnSlave2 = Runtime.getRuntime().exec(createTestFileOnSlaveCmd2);
								
								startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave2.getErrorStream()));
								startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave2.getInputStream()));
								
								Thread threadDockerSlaveCreateTestFile = new Thread() {
									@Override 
									public void run() {
										try {
											Thread.currentThread().sleep(3000);
										} catch (InterruptedException e1) {
											e1.printStackTrace();
										}
										try {
											String createTestFileOnSlaveCmd2 = "docker exec -i js touch "+testFilePathOnSlave;
											
											Process createTestFileOnSlave2 = Runtime.getRuntime().exec(createTestFileOnSlaveCmd2);
											
											startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave2.getErrorStream()));
											startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave2.getInputStream()));
											
											//assertTrue("Cmd " + createTestFileOnSlaveCmd2 + " should have already finished",createTestFileOnSlave2.waitFor(360, TimeUnit.SECONDS));
											
										}catch(Exception e) {e.printStackTrace();}
									}
								};
								threadDockerSlaveCreateTestFile.start();
								//assertTrue("Cmd " + createTestFileOnSlaveCmd2 + " should have already finished",createTestFileOnSlave2.waitFor(360, TimeUnit.SECONDS));
								
							}catch(Exception e) {e.printStackTrace();}
						}
					};
					threadDockerSlaveCreateTestFileDir.start();
					
					//assertTrue("Cmd " + runDockerImageCmd + " should have already finished",buildAndRunDockerImage.waitFor(3600, TimeUnit.SECONDS));
									//} catch (IOException | InterruptedException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
		threadDockerSlave.start();
		
		Thread.currentThread().sleep(11000);
		
		assertFalse("Mehotd Label.getLabelsPath() should not return empty value by now",
				Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
		
		// stop docker
		
		// docker stop $(docker ps -aq --filter ancestor=j:s) && docker rm $(docker ps -a | grep j:s | awk '{print $1}')
		/*String dockerStopAndRemoveCmd = "docker stop js && docker rm js";
		Thread threadDockerSlaveCreateTestFile = new Thread() {
			@Override 
			public void run() {
				try {
					Thread.currentThread().sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				try {
					String createTestFileOnSlaveCmd2 = "docker exec -i js touch "+testFilePathOnSlave;
					
					Process createTestFileOnSlave2 = Runtime.getRuntime().exec(createTestFileOnSlaveCmd2);
					
					startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave2.getErrorStream()));
					startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave2.getInputStream()));
					
					//assertTrue("Cmd " + createTestFileOnSlaveCmd2 + " should have already finished",createTestFileOnSlave2.waitFor(360, TimeUnit.SECONDS));
					
				}catch(Exception e) {e.printStackTrace();}
			}
		};
		threadDockerSlaveCreateTestFile.start();
		*/
	}
	
	public class DockerThread extends Thread{

			@Override
			public void run() {
				
				try {

					// TODO stop on the end
					// docker stop $(docker ps -aq --filter ancestor=j:s)
					// docker exec -it $(docker ps | grep j:s | awk '{print $1}') bash

					//final String runDockerImageCmd = "docker run --net=host j:s";
					String archivePath2 = "/tmp/jenkins/archive/";
					String runDockerImageCmd = "docker run --name js --net=host -v " + archivePath2 + ":" + archivePath2 + " j:s";
					//String runDockerImageCmd = "docker run --net=host -v " + archivePath + ":" + archivePath + " j:s mkdir -p "+testFileDirPathOnSlave+" && touch "+testFilePathOnSlave+"";
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave,"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'","mkdir","-p",testFileDirPathOnSlave,"&&","touch",testFilePathOnSlave,"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c","'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash","-c 'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","bash -c 'mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +"'"};
					//String[] runDockerImageCmd = new String[]{"docker","run","--net=host","-v", archivePath + ":" + archivePath, "j:s","mkdir -p " + testFileDirPathOnSlave +" && touch " + testFilePathOnSlave +""};

					//Process buildAndRunDockerImage;
					Process buildAndRunDockerImage = Runtime.getRuntime().exec(runDockerImageCmd);

					startLogThread("SlaveErr", new InputStreamReader(buildAndRunDockerImage.getErrorStream()));
					startLogThread("SlaveOut", new InputStreamReader(buildAndRunDockerImage.getInputStream()));

					//assertTrue("Cmd " + runDockerImageCmd + " should have already finished",buildAndRunDockerImage.waitFor(360, TimeUnit.SECONDS));
					//	remoteFS = "/home/jenkins/agent";
				
					
					Process createTestFileOnSlave2;
					//Process createTestFileOnSlave3;
					// create test file on slave
	
					
					// docker exec -it  $(docker ps | grep j:s | awk '{print $1}') bash -c 'mkdir -p /home/jenkins/agent/workspace/unitTest-1/ && touch /home/jenkins/agent/workspace/unitTest-1/test.txt'
					// docker exec -it  $(docker ps | grep j:s | awk '{print $1}') bash -c 'mkdir -p /home/jenkins/agent/workspace/unitTest-1/repository/ && touch /home/jenkins/agent/workspace/unitTest-1/repository/test.txt'
					//	String createTestFileOnSlaveCmd = "docker exec -it  $(docker ps | grep j:s | awk '{print $1}') bash -c 'mkdir -p " + testFileDirPathOnSlave + " ; touch "		+ testFilePathOnSlave + ";'";
					
					//createTestFileOnSlave = Runtime.getRuntime().exec(createTestFileOnSlaveCmd);
					
					//startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave.getErrorStream()));
					//startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave.getInputStream()));

					//assertTrue("Cmd " + createTestFileOnSlaveCmd + " should have already finished",
					//		createTestFileOnSlave.waitFor(60, TimeUnit.SECONDS));

					//String createTestFileOnSlaveCmd2 = "docker exec -it \\$(docker ps \\| grep j:s \\| awk '{print \\$1}') mkdir -p " + testFileDirPathOnSlave;
					//String createTestFileOnSlaveCmd2 = "docker exec -it $(docker ps | grep j:s | awk '{print $1}') mkdir -p " + testFileDirPathOnSlave;
					//String[] createTestFileOnSlaveCmd2 = new String[] {"docker","exec","-it","$(","docker","ps","|",		"grep","j:s","|","awk","'{","print","$1","}'",")","mkdir","-p",testFileDirPathOnSlave};
					
					//createTestFileOnSlave2 = Runtime.getRuntime().exec(createTestFileOnSlaveCmd2);
					
					//startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave.getErrorStream()));
					//startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave.getInputStream()));

					//assertTrue("Cmd " + createTestFileOnSlaveCmd2 + " should have already finished",createTestFileOnSlave2.waitFor(60, TimeUnit.SECONDS));
				
					//String createTestFileOnSlaveCmd3 = "docker exec -it \\$(docker ps | grep j:s | awk '{print $1}') touch " + testFilePathOnSlave;
					//String createTestFileOnSlaveCmd3 = "docker exec -it $(docker ps | grep j:s | awk '{print $1}') touch " + testFilePathOnSlave;
					
					//createTestFileOnSlave3 = Runtime.getRuntime().exec(createTestFileOnSlaveCmd3);
					
					//startLogThread("SlaveErrTestFile", new InputStreamReader(createTestFileOnSlave.getErrorStream()));
					//startLogThread("SlaveOutTestFile", new InputStreamReader(createTestFileOnSlave.getInputStream()));

					//assertTrue("Cmd " + createTestFileOnSlaveCmd3 + " should have already finished",createTestFileOnSlave3.waitFor(60, TimeUnit.SECONDS));
							
					
				//} catch (IOException | InterruptedException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
	}

	public void startLogThread(final String prefix, final Reader reader) {
		Thread threadLogOut = new Thread() {
			@Override
			public void run() {
				String line;
				BufferedReader input = new BufferedReader(reader);
				try {
					while ((line = input.readLine()) != null) {
						//log.info(prefix + line);
						//log.info(prefix + line);
						System.out.println(prefix + line);
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
	}

}
