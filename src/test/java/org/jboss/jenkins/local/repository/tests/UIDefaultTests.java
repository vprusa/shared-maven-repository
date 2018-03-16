package org.jboss.jenkins.local.repository.tests;

import static org.junit.Assert.assertTrue;

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

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleProject;

/**
 * @author vprusa Following https://wiki.jenkins.io/display/JENKINS/Unit+Test
 */
public class UIDefaultTests {

	private static final Logger log = Logger.getLogger(UIDefaultTests.class.getName());

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
		String[] expectedConfigurationPageTexts = { (new DownloadMavenRepository.DescriptorImpl()).getDisplayName(),
				"Use label",
				"job/" + projectName
						+ "/descriptorByName/org.jboss.jenkins.local.repository.DownloadMavenRepository/fillUsedLabelItems",
				"<option label=\"default\" value=\"default\" selected=\"selected\">" };

		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));

		verifyPageFor(expectedConfigurationPageTexts, "job/" + projectName + "/configure");
	}

	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultLabelsForArchiveStep()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String[] expectedConfigurationPageTexts = { (new ArchiveMavenRepository.DescriptorImpl()).getDisplayName(),
				"Use label",
				"job/" + projectName
						+ "/descriptorByName/org.jboss.jenkins.local.repository.ArchiveMavenRepository/fillUsedLabelItems",
				"<option label=\"default\" value=\"default\" selected=\"selected\">" };

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));

		verifyPageFor(expectedConfigurationPageTexts, "job/" + projectName + "/configure");
	}

	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultGlobalConfiguration()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String[] expectedConfigurationPageTexts = { "Shared maven repository", "Add new label",
				"descriptorByName/org.jboss.jenkins.local.repository.ArchiveMavenRepository/checkLabels" };

		verifyPageFor(expectedConfigurationPageTexts, "configure");
	}

	public void verifyPageFor(String[] expectedConfigurationPageTexts, String configurationUrl)
			throws IOException, SAXException {
		project.save();
		HtmlPage page = wc.goTo(configurationUrl);
		String pageText = page.asXml();
		log.info("Going to check page texts at URI '" + page.getUrl() + "'");
		//log.info(pageText);
		for (String expectedText : expectedConfigurationPageTexts) {
			assertTrue("Page '" + page.getUrl() + "' should contain text '" + expectedText + "' but does not",
					pageText.contains(expectedText));
		}
	}

}
