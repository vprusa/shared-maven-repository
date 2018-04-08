package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.jboss.jenkins.local.repository.ArchiveMavenRepository;
import org.jboss.jenkins.local.repository.DownloadMavenRepository;
import org.jboss.jenkins.local.repository.Label;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.xml.sax.SAXException;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

/**
 * These tests cover scenarios following patter:
 * 
 * 1. Start jenkins with this plugin
 * 2. Create new simple project
 * 3. Set configuration (Download, Archive, necessary requirements - files, etc.)
 * 4. Verify results
 * 
 * @author vprusa Following https://wiki.jenkins.io/display/JENKINS/Unit+Test
 */
public class MavenIntegrationTests {

	private static final Logger log = Logger.getLogger(MavenIntegrationTests.class.getName());

	String projectName = "unitTest-1";
	WebClient wc;
	FreeStyleProject project;
	final int WAIT_SECS_LIMIT = 10;

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Before
	public void before() throws IOException {
		log.info("Preparing for test");

		wc = j.createWebClient();
		log.info("URL for jenkins is: " + j.getURL().toString());

		project = j.createFreeStyleProject(projectName);

	}

	@After
	public void after() throws IOException, InterruptedException {
		wc.close();
		project.delete();
	}

	/** verify multiple runs - archive - download */
	//@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDifferentDownloadAndArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel1 = "download";
		String usedLabel2 = "archive";

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'default':'default', '" + usedLabel1 + "':'" + usedLabel1
				+ "','" + usedLabel2 + "':'" + usedLabel2 + "'}");
		Label.saveLabels();

		assertFalse("Mehotd Label.getLabelsPath() Should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel1));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel2));

		project.save();
		prepareStartAndVerifySuccessful(2);
	}

	/** verify runs - download -> archive nothing -> download -> archive */
	//@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownloadAndNoArchive()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(2);
	}

	/** verify run - wrong archive */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify run - wrong download */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify run - archive */
	//@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify run - download */
	//@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	public void prepareStartAndVerifySuccessful(int runs)
			throws IOException, SAXException, InterruptedException, ExecutionException {

		File projectWorkspace = new File(j.jenkins.getRootPath() + "/workspace/" + projectName);
		File projectWorkspaceRepository = new File(projectWorkspace, ".repository");

		projectWorkspaceRepository.mkdirs();

		File repositoryTestFile = new File(projectWorkspaceRepository, "test.txt");
		repositoryTestFile.createNewFile();

		for (int runNo = 0; runNo < runs; runNo++) {
			FreeStyleBuild build = project.scheduleBuild2(0).get();
			waitAndVerifyRunSuccess(build, Result.SUCCESS);
		}
	}

	public void waitAndVerifyRunSuccess(FreeStyleBuild build, Result expected) throws InterruptedException {

		int i = 0;
		while (i < WAIT_SECS_LIMIT && !build.getResult().isBetterOrEqualTo(expected)) {
			// TODO better solution
			Thread.sleep(1000);
			i++;
		}
		log.info("Test waited for job " + i + " seconds");
		assertFalse("Build did not passed with result: " + build.getResult().toString(),
				!build.getResult().isBetterOrEqualTo(expected));
	}

}
