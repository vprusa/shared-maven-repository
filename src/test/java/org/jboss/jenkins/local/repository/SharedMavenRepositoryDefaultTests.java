package org.jboss.jenkins.local.repository;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;
import org.xml.sax.SAXException;

import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.recipes.WithPlugin;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

/**
 * 
 * Following https://wiki.jenkins.io/display/JENKINS/Unit+Test
 */
public class SharedMavenRepositoryDefaultTests {

	private static final Logger log = Logger.getLogger(SharedMavenRepositoryDefaultTests.class.getName());

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
				"/jenkins/job/" + projectName
						+ "/descriptorByName/org.jboss.jenkins.local.repository.DownloadMavenRepository/fillUsedLabelItems",
				"<option value=\"default\" selected=\"selected\">" };
		
		project.getBuildersList().add(new DownloadMavenRepository(usedLabel));
		
		verifyPageFor(expectedConfigurationPageTexts, "job/" + projectName + "/configure");
	}

	@Test
	@WithRemoveDuplicatedPlugin("shared-maven-repository")
	public void testDefaultLabelsForArchiveStep()
			throws IOException, InterruptedException, ExecutionException, SAXException {
		String[] expectedConfigurationPageTexts = { (new ArchiveMavenRepository.DescriptorImpl()).getDisplayName(),
				"Use label",
				"/jenkins/job/" + projectName
						+ "/descriptorByName/org.jboss.jenkins.local.repository.ArchiveMavenRepository/fillUsedLabelItems",
				"<option value=\"default\" selected=\"selected\">" };

		project.getPublishersList().add(new ArchiveMavenRepository(usedLabel));
		
		verifyPageFor(expectedConfigurationPageTexts, "job/" + projectName + "/configure");
	}

	
	public void verifyPageFor(String[] expectedConfigurationPageTexts, String configurationUrl) throws IOException, SAXException {
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
