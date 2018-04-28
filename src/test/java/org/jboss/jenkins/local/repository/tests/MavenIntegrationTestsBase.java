package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
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

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

/**
 * These tests cover scenarios following patter:
 * 
 * 1. Start jenkins with this plugin 2. Create new simple project 3. Set
 * configuration (Download, Archive, necessary requirements - files, etc.) 4.
 * Verify results
 * 
 * @author vprusa Following https://wiki.jenkins.io/display/JENKINS/Unit+Test
 */
public class MavenIntegrationTestsBase {

	private static final Logger log = Logger.getLogger(MavenIntegrationTestsBase.class.getName());

	String projectName = "unitTest-1";
	WebClient wc;
	FreeStyleProject project;
	final int WAIT_SECS_LIMIT = 10;

	String testFileName = "test.txt";
	String tmpUnzippedPath = "/tmp/jenkins/unzipped";
	String tmpArchivePath = "/tmp/jenkins/archive";

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Before
	public void before() throws IOException, InterruptedException {
		log.info("Preparing for test");

		testFileName = "test-" + UUID.randomUUID().toString() + ".txt";

		wc = j.createWebClient();
		log.info("URL for jenkins is: " + j.getURL().toString());

		project = j.createFreeStyleProject(projectName);
	}

	@After
	public void after() throws IOException, InterruptedException {
		wc.close();
		project.delete();
	}

	public ArrayList<FreeStyleBuild> prepareStartAndVerifySuccessful(int buildsCount)
			throws IOException, SAXException, InterruptedException, ExecutionException {

		File projectWorkspace = new File(j.jenkins.getRootPath() + "/workspace/" + projectName);
		File projectWorkspaceRepository = new File(projectWorkspace, ".repository");

		projectWorkspaceRepository.mkdirs();

		File repositoryTestFile = new File(projectWorkspaceRepository, testFileName);
		repositoryTestFile.createNewFile();

		ArrayList<FreeStyleBuild> builds = new ArrayList<FreeStyleBuild>();

		for (int buildNo = 0; buildNo < buildsCount; buildNo++) {
			FreeStyleBuild build = project.scheduleBuild2(0).get();
			builds.add(build);
			waitAndVerifyBuildSuccess(build, Result.SUCCESS);
		}

		return builds;
	}

	public void waitAndVerifyBuildSuccess(FreeStyleBuild build, Result expected) throws InterruptedException {
		int i = 0;
		while (i < WAIT_SECS_LIMIT && !build.getResult().isBetterOrEqualTo(expected)) {
			// TODO better solution
			Thread.sleep(1000);
			i++;
		}
		log.info("Test waited for job " + i + " seconds");
		assertFalse("Build did not passed with result: " + build.getResult().toString() + " with expected result: "
				+ expected.toString(), !build.getResult().isBetterOrEqualTo(expected));
	}

	public void verifyThatArchiveConstainsTestFile(Label label, boolean archiveOrDownload)
			throws IOException, InterruptedException {
		FilePath workspace = project.getSomeWorkspace(); // getWorkspace();
		FilePath path = archiveOrDownload ? label.getLatestRepoFileArchive(workspace)
				: label.getLatestRepoFileDownload(workspace);

		String tmpUnzippedTestPath = tmpUnzippedPath + "/.repository/" + testFileName;
		File unzipTest = new File(tmpUnzippedPath);

		if (unzipTest.exists()) {
			unzipTest.delete();
		}
		unzipTest.mkdirs();

		path.unzip(new FilePath(unzipTest));
		assertTrue(
				"Test file does not exists in unzipped directory. Label path: "
						+ (archiveOrDownload ? label.getLatestRepoFileArchive(workspace)
								: label.getLatestRepoFileDownload(workspace))
						+ " , file path: " + tmpUnzippedTestPath,
				new File(tmpUnzippedTestPath).exists());
	}

	public void configureBildAndVerify(int buildsCount, String labelD, String labelA)
			throws IOException, SAXException, InterruptedException, ExecutionException {
		project.getBuildersList().add(new DownloadMavenRepository(labelD));
		project.getPublishersList().add(new ArchiveMavenRepository(labelA));

		project.save();

		prepareStartAndVerifySuccessful(buildsCount);
		verifyThatArchiveConstainsTestFile(Label.getUsedLabelById(labelA), Label.GET_LATEST_ARCHIVE);
	}

}
