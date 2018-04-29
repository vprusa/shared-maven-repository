package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
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
	
			String usedLabelId = getUsedLabel();
			Label usedLabel = Label.getUsedLabelById(usedLabelId);
			if (usedLabel == null) {
				listener.getLogger()
						.println("Jenkins master does not have any maven repository for label " + usedLabelId);
				return;
			}
			
			listener.getLogger()
			.println("Downloading files for label: " + usedLabel.toString());
			FilePath downloadDest = usedLabel.getDownloadFilePath(workspace);
			// remove old zip

			FilePath downloadZipFile = new FilePath(downloadDest.getParent(), "file.zip");
			/*
			if(downloadDest.isDirectory()) {
				downloadZipFile = new FilePath(downloadDest.getParent(), "file.zip");
			}else {
				downloadZipFile = downloadDest.getParent();
			}
			*/
			if (downloadZipFile.exists()) {
				listener.getLogger().println(downloadZipFile.getRemote() + " already exists, delete.");
				boolean deleted = downloadZipFile.delete();
				if (!deleted) {
					listener.error("Unable to delete file: " + downloadZipFile.getRemote());
				}
			}	
			FilePath archivedFile = usedLabel.getArchiveFilePath(workspace); 
			//usedLabel.getDownloadFilePath(workspace);
			//usedLabel.getLatestRepoFileDownload(workspace);
		
			FilePath archivedZipFile = usedLabel.getLatestRepoFileArchive(workspace);
			
			if (archivedFile == null || !archivedFile.exists() || archivedZipFile == null || !archivedZipFile.exists() ) {
				listener.getLogger().println("Jenkins does not have any file with path: " + archivedFile.getRemote());
				return;
			}
		/*	
			listener.getLogger().println("archivedZipFile");
			listener.getLogger().println(archivedZipFile);
			listener.getLogger().println("downloadZipFile");
			listener.getLogger().println(downloadZipFile);
			listener.getLogger().println("downloadDest");
			listener.getLogger().println(downloadDest);
			*/
			archivedZipFile.copyTo(downloadZipFile);
			//new File(downloadZipFile.getParent().getRemote(), "file.zip").
			//new File(downloadZipFile.getParent().getRemote(), archivedZipFile.getName()).renameTo(new File(downloadZipFile.getRemote()));
			
			Label.deleteDir(new File(downloadDest.getRemote()));
			//downloadDest.mkdirs();
			downloadZipFile.unzip(downloadDest.getParent());
			listener.getLogger().println("Jenkins master repository unzipped.");
			/*
			FilePath downloadFile;
			if (usedLabel.getDownloadPath().endsWith("/")) {
				// is dir so create new zip with unique name in this dir
				downloadFile = new FilePath(usedLabel.getDownloadFilePath(workspace),
						"file.zip");
			} else {
				// is file so create new file next to old one
				downloadFile = new FilePath(usedLabel.getDownloadFilePath(workspace).getParent(),
						usedLabel.getDownloadFilePath(workspace).getBaseName() + "-" + UUID.randomUUID().toString() + ".zip");
			}
			
			FilePath archiveFile = usedLabel.getDownloadFilePath(workspace);//usedLabel.getLatestRepoFileDownload(workspace);
			listener.getLogger().println("Jenkins does not have any file with path: " + archiveFile.getRemote());
			if (archiveFile == null || !archiveFile.exists()) {
				listener.getLogger().println("Jenkins does not have any file with path: " + archiveFile.getRemote());
				return;
			}
			
			listener.getLogger().println("Download maven repository");
			//FilePath downloadFile = usedLabel.getDownloadFilePath(workspace); //new FilePath(usedLabel.getDownloadFilePath(workspace) , "file.zip");
			// todo store zip into parent folder if exists - it should
			FilePath downloadZipFile = new FilePath(downloadFile.getParent(), "file.zip");
			if (downloadZipFile.exists()) {
				listener.getLogger().println(downloadZipFile.getRemote() + " already exists, delete.");
				boolean deleted = downloadZipFile.delete();
				if (!deleted) {
					listener.error("Unable to delete file: " + downloadZipFile.getRemote());
				}
			}
			
			archiveFile.copyTo(downloadZipFile);
			downloadZipFile.unzip(downloadFile);
			listener.getLogger().println("Jenkins master repository unzipped.");
			*/
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
