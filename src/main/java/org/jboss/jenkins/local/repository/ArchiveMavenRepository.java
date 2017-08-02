package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep {

	@DataBoundConstructor
	public ArchiveMavenRepository() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			listener.getLogger().println("Archive private maven repository");
			List<FilePath> files = workspace.list();
			File f = new File(workspace.absolutize().toURI());
			File finalFile = new File(f, "repository" + UUID.randomUUID().toString() + ".tar");
			FileOutputStream repositoryZip = null;
			try {
				repositoryZip = new FileOutputStream(finalFile);
				for (FilePath file : files) {
					if (file.isDirectory() && file.getName().equals(".repository")) {
						listener.getLogger().println("Found .repository folder");
						file.tar(repositoryZip, new FileFilter() {

							@Override
							public boolean accept(File pathname) {
								return true;
							}
						});
						MasterMavenRepository.getInstance().uploadRepository(new FilePath(finalFile), listener);
						return;
					}
				}
				listener.getLogger().println(".repository folder not found");
			} finally {
				if (repositoryZip != null) {
					repositoryZip.close();
				}
			}
		}

	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		/**
		 * In order to load the persisted global configuration, you have to call load()
		 * in the constructor.
		 */
		public DescriptorImpl() {
			load();
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "archive repository";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			save();
			return super.configure(req, formData);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}
	}

}
