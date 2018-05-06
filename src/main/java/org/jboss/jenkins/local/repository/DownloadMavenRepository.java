package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class DownloadMavenRepository extends Builder implements SimpleBuildStep {

	private static final Logger log = Logger.getLogger(DownloadMavenRepository.class.getName());

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
			EnvVars env = build.getEnvironment(listener);
			String usedId = getUsedLabel();
			Label used = Label.getUsedLabelById(usedId);
			if (used == null) {
				listener.getLogger().println("Jenkins master does not have any maven repository for label " + usedId);
				return;
			}
			used.setChannel(null);


			// https://stackoverflow.com/questions/9279898/can-hudson-slaves-run-plugins
			// Define what should be run on the slave for this build
			JenkinsSlaveDownloadCallable slaveTask = new JenkinsSlaveDownloadCallable(listener, workspace, env, used);
			// Get a "channel" to the build machine and run the task there
			// String status = launcher.getChannel().call(slaveTask);
			String status = JenkinsSlaveCallableBase.decideAndCall(used, launcher, slaveTask, listener);;

			listener.getLogger().println("Jenkins repository slave execution status: " + status);
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
			return "Download repository";
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
