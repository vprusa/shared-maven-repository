package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.util.stream.Collectors;

import org.json.simple.parser.ParseException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
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
			FilePath jobRepo = new FilePath(workspace,"repository.zip");
			if(jobRepo.exists()) {
				listener.getLogger().println(jobRepo.getName() + " already exists, delete.");
				boolean deleted = jobRepo.delete();
				if(!deleted) {
					listener.error("Unable to delete repository.zip");
				}
			}
			FilePath repoFile = MasterMavenRepository.getInstance().getLatestRepo();
			if(repoFile == null) {
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
		
		public ListBoxModel doFillUseLabelItems() {
		    ListBoxModel items = new ListBoxModel();
		    try {
				Label.loadFromFile().stream().forEach(i->{items.add(i.getName(),i.getId());});
			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
		    return items;
		}
    }

}
