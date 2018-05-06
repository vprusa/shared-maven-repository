package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.ComputerSet;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.model.Nodes;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ArchiveMavenRepository extends Builder implements SimpleBuildStep {
//public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep {

	private static final Logger log = Logger.getLogger(ArchiveMavenRepository.class.getName());

	private static final long serialVersionUID = 1L;

	public String usedLabel;

	@DataBoundConstructor
	public ArchiveMavenRepository(String usedLabel) {
		this.usedLabel = usedLabel;
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			Result buildResult = build.getResult();

			EnvVars env = build.getEnvironment(listener);

			Label used = Label.getUsedLabelById(getUsedLabel());
			if ((used.getDoNotArchiveIfWorseThen() == null && buildResult != null
					&& buildResult.isWorseThan(Result.FAILURE))
					|| (used.getDoNotArchiveIfWorseThen() != null
							&& (buildResult != null && buildResult.isWorseThan(used.getDoNotArchiveIfWorseThen())))) {
				listener.getLogger()
						.println("Build ended with status worse than "
								+ (used.getDoNotArchiveIfWorseThen() == null ? "FAILURE"
										: used.getDoNotArchiveIfWorseThen().toString())
								+ ". Maven repository wont be archived");
				return;
			}
			listener.getLogger().println("Archive private maven repository");

			if (used != null) {

				listener.getLogger()
						.println("Found files for path: " + used.getDownloadFilePath(workspace, env).getRemote());
			
				used.setChannel(null);

				// listener.getLogger().println("launcher.getChannel()");

				// label.updateChannel(launcher.getChannel());
				// https://stackoverflow.com/questions/9279898/can-hudson-slaves-run-plugins
				// Define what should be run on the slave for this build
				JenkinsSlaveArchiveCallable slaveTask = new JenkinsSlaveArchiveCallable(workspace, env, listener, used);
				// Get a "channel" to the build machine and run the task there
				// String status = launcher.getChannel().call(slaveTask);

				String status = JenkinsSlaveCallableBase.decideAndCall(used, launcher, slaveTask, listener);;
				listener.getLogger().println("Jenkins repository slave execution status: " + status);

				return;
			}
			listener.getLogger().println("Files not found");
		}

	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public String getUsedLabel() {
		return usedLabel;
	}

	public void setUsedLabel(String usedLabel) {
		this.usedLabel = usedLabel;
	}

	@Extension
	// public static final class DescriptorImpl extends
	// BuildStepDescriptor<Publisher> implements Serializable {
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> implements Serializable {

		/**
		 * In order to load the persisted global configuration, you have to call load()
		 * in the constructor.
		 */
		public DescriptorImpl() {
			load();
			getLabelsS();
		}

		static private String labels;

		public static String getLabelsS() {
			if (labels == null) {
				try {
					labels = (Label.labelsStringToJSON(Label.loadStringFromFile())).toString();
				} catch (IOException e) {
					// TODO fix because this may net be bulletproof
					// if everything goes wrong return empty JSON list
					e.printStackTrace();
					return "{}";
				}
			} else {
				// by default this getter is used to returns indented JSON string in global
				// configuration
				return Label.labelsStringToJSON(labels).toString(2);
			}
			return labels;
		}

		public static void setLabelsS(String str) {
			labels = str;
		}

		public String getLabels() {
			return getLabelsS();
		}

		public void setLabels(String labels) {
			try {
				// doCheck for JSON and List compatibility
				Label.labelsStringToList(labels);
				// static & file save
				ArchiveMavenRepository.DescriptorImpl.setLabelsS(labels);
				Label.saveLabels();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Archive repository";
		}

		@Override
		public boolean configure(final StaplerRequest req, JSONObject formData) throws FormException {
			String newLabels = formData.getString("labels");
			if (newLabels != null) {
				log.info("New shared-maven-repository labels:" + newLabels.toString());
			} else {
				log.info("New shared-maven-repository labels are empty!");
			}
			setLabels(newLabels);
			save();
			return super.configure(req, formData);
		}

		public FormValidation doCheckLabels(@QueryParameter String value) throws IOException, ServletException {
			try {
				// doCheck for JSON and List compatibility
				Label.labelsStringToJSON(labels);
				Label.labelsStringToList(labels);
				return FormValidation.ok();
			} catch (NumberFormatException e) {
				return FormValidation.error(
						"Shared-maven-repository Labels are not in valid JSON list format, check pluign github repo for more information");
			}
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
