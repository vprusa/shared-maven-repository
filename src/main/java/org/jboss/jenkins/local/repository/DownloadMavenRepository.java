package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class DownloadMavenRepository extends Builder implements SimpleBuildStep {

	private static final Logger log = Logger.getLogger(DownloadMavenRepository.class.getName());

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String usedLabel;

	public String getUsedLabel() {
		return usedLabel;
	}

	public void setUsedLabel(String usedLabel) {
		this.usedLabel = usedLabel;
	}

	public Label getUsedLabelById() {
		return Label.getUsedLabelById(getUsedLabel());
	}

	@DataBoundConstructor
	public DownloadMavenRepository(String usedLabel) {
		this.usedLabel = usedLabel;
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			listener.getLogger().println("Download maven repository from Jenkins master");
			FilePath jobRepo = new FilePath(workspace, "repository.zip");
			if (jobRepo.exists()) {
				listener.getLogger().println(jobRepo.getName() + " already exists, delete.");
				boolean deleted = jobRepo.delete();
				if (!deleted) {
					listener.error("Unable to delete repository.zip");
				}
			}
			String usedLabelId = getUsedLabel();
			Label usedLabel = Label.getUsedLabelById(usedLabelId);
			if (usedLabel == null) {
				listener.getLogger()
						.println("Jenkins master does not have any maven repository for label " + usedLabelId);
				return;
			}
			FilePath repoFile = usedLabel.getLatestRepoFile();
			if (repoFile == null) {
				listener.getLogger().println("Jenkins master does not have any maven repository");
				return;
			}
			repoFile.copyTo(jobRepo);
			jobRepo.unzip(workspace);
			listener.getLogger().println("Jenkins master repository unzipped.");
		}

	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		/**
		 * In order to load the persisted global configuration, you have to call load()
		 * in the constructor.
		 */
		public DescriptorImpl() {
			load();
			ArchiveMavenRepository.DescriptorImpl.getLabelsS();
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Download .m2 repository";
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

		public ListBoxModel doFillUsedLabelItems() {
			ListBoxModel items = new ListBoxModel();
			Label.getListInstances().stream().forEach(i -> {
				items.add(i.getName(), i.getId());
			});
			return items;
		}
	}

}
