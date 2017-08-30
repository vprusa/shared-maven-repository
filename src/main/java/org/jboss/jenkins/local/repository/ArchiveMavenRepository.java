package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@DataBoundConstructor
	public ArchiveMavenRepository() {
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			Result buildResult = build.getResult();
			if (buildResult != null && buildResult.isWorseThan(Result.FAILURE)) {
				listener.getLogger().println("Build ended with status worse than FAILURE. Maven repository wont be archived");
				return;
			}
			listener.getLogger().println("Archive private maven repository");
			List<FilePath> files = workspace.list();
			for (FilePath file : files) {
				if (file.isDirectory() && file.getName().equals(".repository")) {
					listener.getLogger().println("Found .repository folder");
					FilePath repoFile = new FilePath(workspace, "repository" + UUID.randomUUID().toString() + ".zip");
					if (!repoFile.exists()) {
						repoFile.touch(new Date().getTime());
						listener.getLogger().println("Created empty zip "+repoFile.toURI());
					}
					RepoFileFilter filter = new RepoFileFilter();
					filter.preparePath(new File(file.toURI()));
					OutputStream repoOutputStream = repoFile.write();
					try {
						listener.getLogger().println("Fill archive "+repoFile.toURI());
						file.archive(TrueZipArchiver.FACTORY, repoOutputStream, filter);
						listener.getLogger().println("Done!");
					} finally {
						repoOutputStream.close();
					}
					MasterMavenRepository.getInstance().uploadRepository(repoFile, listener);
					return;
				}
			}
			listener.getLogger().println(".repository folder not found");
		}

	}


	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

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
