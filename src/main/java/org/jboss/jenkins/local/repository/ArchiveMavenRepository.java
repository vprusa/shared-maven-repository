package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
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
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep {

	private static final Logger log = Logger.getLogger(ArchiveMavenRepository.class.getName());

	/**
	 * 
	 */
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
			if (buildResult != null && buildResult.isWorseThan(Result.FAILURE)) {
				listener.getLogger()
						.println("Build ended with status worse than FAILURE. Maven repository wont be archived");
				return;
			}
			listener.getLogger().println("Archive private maven repository");
			Label label = Label.getUsedLabelById(getUsedLabel());
			if (label != null) {
				List<FilePath> files = workspace.list();
				for (FilePath file : files) {
					if (file.isDirectory() && file.getName().equals(".repository")) {
						listener.getLogger().println("Found .repository folder");
						FilePath repoFile = new FilePath(workspace,
								"repository-" + UUID.randomUUID().toString() + ".zip");
						if (!repoFile.exists()) {
							repoFile.touch(new Date().getTime());
							listener.getLogger().println("Created empty zip " + repoFile.toURI());
						}
						RepoFileFilter filter = new RepoFileFilter();
						filter.preparePath(new File(file.toURI()));
						OutputStream repoOutputStream = repoFile.write();
						try {
							listener.getLogger().println("Fill archive " + repoFile.toURI());
							file.archive(TrueZipArchiver.FACTORY, repoOutputStream, filter);
							listener.getLogger().println("Done!");
						} finally {
							repoOutputStream.close();
						}

						MasterMavenRepository.getInstance().uploadRepository(repoFile, listener, label);
						return;
					}
				}
			}
			listener.getLogger().println(".repository folder not found");
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
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

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
			return "Archive .m2 repository";
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
						"Shared-maven-repository Labels are not in valid JSON Tuples list format, used format {\"key\":\"value\", ...}");
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
