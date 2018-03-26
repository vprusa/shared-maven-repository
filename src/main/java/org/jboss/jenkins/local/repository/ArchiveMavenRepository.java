package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.jfree.util.Log;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep, Serializable {

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
			List<FilePath> files = workspace.list();
			for (FilePath file : files) {
				if (file.isDirectory() && file.getName().equals(".repository")) {
					listener.getLogger().println("Found .repository folder");
					FilePath repoFile = new FilePath(workspace, "repository-" + UUID.randomUUID().toString() + ".zip");
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
					MasterMavenRepository.getInstance().uploadRepository(repoFile, listener, getUsedLabelById());
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


	public Label getUsedLabelById() {
		return Label.getUsedLabelById(getUsedLabel());
	}
	
	public String getUsedLabel() {
		return usedLabel;
	}
	
	public void setUsedLabel(String usedLabel) {
		this.usedLabel = usedLabel;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {
		
		static private String labels;
				
		public static String getLabelsS() {
			return labels;
		}
		
		public static void setLabelsS(String str) {
			labels = str;
		}
		
		public String getLabels() {
			if(labels == null) {
				try {
					setLabels((Label.labelsStringToJSON(Label.loadStringFromFile())).toString());
				} catch (IOException e) {
					// if everything goes wrong return empty JSON list
					e.printStackTrace();
					return "{}";
				}
			}else {
				// by default this getter is used to returns indented JSON string in global configuration
				return Label.labelsStringToJSON(labels).toString(2);
			}
			return labels;
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
			log.info("configure");
			log.info(formData.toString());
			log.info(req.toString());
			String newLabels = formData.getString("labels");
			log.info("newLabels ");
			log.info(newLabels);
			setLabels(newLabels);
			save();
			return super.configure(req, formData);
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}

		public ListBoxModel doFillUsedLabelItems() {
			log.info("doFillUsedLabelItems");
			ListBoxModel items = new ListBoxModel();
			Label.getListInstances().stream().forEach(i -> {
				items.add(new Option(i.getName(), i.getId()));
			});

			return items;
		}

	}

}
