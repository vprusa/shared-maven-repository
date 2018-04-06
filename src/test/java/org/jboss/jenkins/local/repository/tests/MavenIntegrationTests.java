package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.jboss.jenkins.local.repository.ArchiveMavenRepository;
import org.jboss.jenkins.local.repository.DownloadMavenRepository;
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

	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testMoreLabelsLabelsForMoreJobRuns()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		// verify multiple runs - archive - download
		verifyThatRunsAreSuccessful(2);
	}

	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownloadAndNoArchive()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		// verify multiple runs - download -> archive nothing
		verifyThatRunsAreSuccessful(2);
	}

	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		// verify multiple runs - wrong archive
		verifyThatRunsAreSuccessful(1);
	}

	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		// verify multiple runs - wrong download
		verifyThatRunsAreSuccessful(1);
	}

	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		// verify multiple runs - archive
		verifyThatRunsAreSuccessful(1);
	}

	// @Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		// verify multiple runs - download
		verifyThatRunsAreSuccessful(1);
	}

	public void verifyThatRunsAreSuccessful(int builds)
			throws IOException, SAXException, InterruptedException, ExecutionException {

		File projectWorkspace = new File(j.jenkins.getRootPath() + "/workspace/" + projectName);
		File projectWorkspaceRepository = new File(projectWorkspace, ".repository");

		projectWorkspaceRepository.mkdirs();

		File repositoryTestFile = new File(projectWorkspaceRepository, "test.txt");
		repositoryTestFile.createNewFile();

		project.save();

		FreeStyleBuild build = project.scheduleBuild2(0).get();

		waitAndVerifyRunSuccess(build, Result.SUCCESS);
	}

	public void waitAndVerifyRunSuccess(FreeStyleBuild build, Result expected) throws InterruptedException {

		int i = 0;
		while (i < WAIT_SECS_LIMIT && !build.getResult().isBetterOrEqualTo(expected)) {
			// TODO better solution
			Thread.sleep(1000);
			i++;
		}
		log.info("Test waited for job " + i + " seconds");
		assertTrue("Build did not passed with result: " + build.getResult().toString(),
				build.getResult().isBetterOrEqualTo(expected));
	}

}
