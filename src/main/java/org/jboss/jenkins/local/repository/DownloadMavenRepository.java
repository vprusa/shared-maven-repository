package org.jboss.jenkins.local.repository;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.FilePath.TarCompression;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class DownloadMavenRepository extends Builder implements SimpleBuildStep {
	
	@DataBoundConstructor
	public DownloadMavenRepository() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if(jenkins != null) {
			listener.getLogger().println("Download maven repository from Jenkins master");
			FilePath jobRepo = new FilePath(workspace,"repository.tar");
			if(jobRepo.exists()) {
				listener.getLogger().println("repository.tar already exists, delete.");
				boolean deleted = jobRepo.delete();
				if(!deleted) {
					listener.error("Unable to delete repository.tar");
				}
			}
			FilePath repoFile = MasterMavenRepository.getInstance().getLatestRepo();
			if(repoFile == null) {
				listener.getLogger().println("Jenkins master does not have any maven repository");
				return;
			}
			repoFile.copyTo(jobRepo);
			jobRepo.untar(workspace, TarCompression.NONE);
			listener.getLogger().println("Jenkins master repository untarred.");
		}
		
	}
	
	@Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "download repository";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req,formData);
        }

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}
    }

}
