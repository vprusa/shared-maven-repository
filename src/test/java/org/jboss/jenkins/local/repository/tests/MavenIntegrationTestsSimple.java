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
public class MavenIntegrationTestsSimple extends MavenIntegrationTestsBase {

	private static final Logger log = Logger.getLogger(MavenIntegrationTestsSimple.class.getName());

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@Before
	public void before() throws IOException, InterruptedException {
		super.before();
	}

	@After
	public void after() throws IOException, InterruptedException {
		super.after();
	}

	/**
	 * default-workspace-root
	 */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownloadAndArchiveForWorkspaceAndRootPaths()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = j.jenkins.getRootPath() + "/workspace/" + projectName;
		String archivePath = tmpArchivePath + "/";

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':' " + downloadPath + "','archivePath':'" + archivePath + "'}}");
		Label.saveLabels();

		assertFalse("Mehotd Label.getLabelsPath() should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
	}

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

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'default':'default', '" + usedLabel1D + "':'" + usedLabel1D
				+ "','" + usedLabel1A + "':'" + usedLabel1A + "', '" + usedLabel2D + "':'" + usedLabel2D + "','"
				+ usedLabel2A + "':'" + usedLabel2A + "'}");
		Label.saveLabels();

		assertFalse("Mehotd Label.getLabelsPath() should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel1D, usedLabel1A);

		testFileName = "test-" + UUID.randomUUID().toString() + ".txt";

		configureBildAndVerify(2, usedLabel2D, usedLabel2A);
	}

	/** verify multiple builds - archive - download */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDifferentDownloadAndArchive()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabelD = "download";
		String usedLabelA = "archive";

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'default':'default', '" + usedLabelD + "':'" + usedLabelD
				+ "','" + usedLabelA + "':'" + usedLabelA + "'}");
		Label.saveLabels();

		assertFalse("Mehotd Label.getLabelsPath() should return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

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
