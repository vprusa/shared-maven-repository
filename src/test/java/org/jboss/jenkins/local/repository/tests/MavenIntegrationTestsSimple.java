package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
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
public class MavenIntegrationTestsSimple extends MavenIntegrationTestsBase {

	private static final Logger log = Logger.getLogger(MavenIntegrationTestsSimple.class.getName());

	/**
	 * verify multiple builds with changed labels - 2x(archive(1) - download(1)) -
	 * 2x(archive(2) - download(2))
	 */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDifferentDownloadAndArchiveForEachBuild()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel1D = "download1";
		String usedLabel1A = "archive1";

		String usedLabel2D = "download2";
		String usedLabel2A = "archive2";

		long timestmap = new Date().getTime();
		FilePath jenkinsRootPath = j.jenkins.getRootPath();
		File willBeWorkspace = new File(jenkinsRootPath.getRemote() + "/workspace/" + projectName);
		FilePath workspace = new FilePath(new FilePath(willBeWorkspace), "repository");
		workspace.mkdirs();
		new FilePath(workspace, usedLabel1D).touch(timestmap);
		FilePath usedArchive1 = new FilePath(workspace, usedLabel1A);
		usedArchive1.touch(timestmap);
		new FilePath(workspace, usedLabel2D).touch(timestmap);
		FilePath usedArchive2 = new FilePath(workspace, usedLabel2A);
		usedArchive2.touch(timestmap);

		String testConfigFileName = "testDifferentDownloadAndArchiveForEachBuildLabels.json";
		String config = loadTestConfig(testConfigFileName);

		assertFalse("Mehotd Label.getLabelsPath() should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		testFileName = usedArchive1.getBaseName();
		testFileDir = "";

		configureBildAndVerify(2, usedLabel1D, usedLabel1A);

		testFileName = usedArchive2.getBaseName();
		configureBildAndVerify(2, usedLabel2D, usedLabel2A);
	}

	/** verify multiple builds - archive - download */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDifferentDownloadAndArchive()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabelD = "download";
		String usedLabelA = "archive";


		long timestmap = new Date().getTime();
		FilePath jenkinsRootPath = j.jenkins.getRootPath();
		File willBeWorkspace = new File(jenkinsRootPath.getRemote() + "/workspace/" + projectName);
		FilePath workspace = new FilePath(new FilePath(willBeWorkspace), "repository");
		workspace.mkdirs();
		new FilePath(workspace, usedLabelD).touch(timestmap);
		FilePath usedArchive = new FilePath(workspace, usedLabelA);
		usedArchive.touch(timestmap);


		
		String testConfigFileName = "testDifferentDownloadAndArchiveLabels.json";
		String config = loadTestConfig(testConfigFileName);

		assertFalse("Mehotd Label.getLabelsPath() should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		testFileName = usedArchive.getBaseName();
		testFileDir = "";
		
		configureBildAndVerify(2, usedLabelD, usedLabelA);
	}

	/** verify builds - download -> archive nothing -> download -> archive */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownloadAndNoArchive()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(2);
	}

	/** verify build - wrong archive */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify build - wrong download */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testNotExistingDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "none";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify build - archive */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultArchive() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

	/** verify build - download */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownload() throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "default";

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		prepareStartAndVerifySuccessful(1);
	}

}
