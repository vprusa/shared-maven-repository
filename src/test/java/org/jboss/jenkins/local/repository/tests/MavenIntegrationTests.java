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

	String usedLabel = "default";
	String projectName = "unitTest-1";
	WebClient wc;
	FreeStyleProject project;

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
	public void testDefaultLabelsForDownloadStep()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		
		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		verifyThatSingleRunIsSuccessful();
	}

	public void verifyThatSingleRunIsSuccessful()
			throws IOException, SAXException, InterruptedException, ExecutionException {
		String tmpMavenRepositoryPath = "/tmp/.m2/repository";

		File m2repository = new File(tmpMavenRepositoryPath);
		m2repository.mkdirs();


		File projectWorkspace = new File(j.jenkins.getRootPath() + "/workspace/" + projectName);
		File projectWorkspaceRepository = new File(projectWorkspace, ".repository");

		projectWorkspaceRepository.mkdirs();

		File repositoryTestFile = new File(projectWorkspaceRepository, "test.txt");
		repositoryTestFile.createNewFile();

		project.save();

		FreeStyleBuild build = project.scheduleBuild2(0).get();

		int WAIT_SECS_LIMIT = 10;
		int i = 0;
		while (i < WAIT_SECS_LIMIT && !build.getResult().isBetterOrEqualTo(Result.SUCCESS)) {
			// TODO better solution
			Thread.sleep(1000);
			i++;
		}
		log.info("Test waited for job " + i + " seconds");
		assertTrue("Build did not passed with result: " + build.getResult().toString(),
				build.getResult().isBetterOrEqualTo(Result.SUCCESS));

	}

}
