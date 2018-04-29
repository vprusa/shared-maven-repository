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
public class MavenIntegrationTests  extends MavenIntegrationTestsBase {

	private static final Logger log = Logger.getLogger(MavenIntegrationTests.class.getName());

	String tmpArchivePath = "/tmp/jenkins/archive.zip";

	/**
	 * default-workspace-root
	 */
	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultDownloadAndArchiveForRootPath()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String usedLabel = "label";
		String downloadPath = "{jenkinsRoot}/workspace/" + projectName + "/repository";
		String archivePath = "/tmp/jenkins/archive/";
		new File(archivePath).mkdirs();

		ArchiveMavenRepository.DescriptorImpl.setLabelsS("{'" + usedLabel + "':{ 'name': '" + usedLabel
				+ "','downloadPath':'" + downloadPath + "','archivePath':'" + archivePath + "'}}");

		assertFalse("Mehotd Label.getLabelsPath() should not return empty value by now", Label.getLabelsPath().isEmpty());
		assertTrue("File with path " + Label.getLabelsPath() + " should exist",
				(new File(Label.getLabelsPath()).exists()));

		// here new labels should be updated in configuration

		configureBildAndVerify(2, usedLabel, usedLabel);
	}

}
