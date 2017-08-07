package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
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

public class ArchiveMavenRepository extends Recorder implements SimpleBuildStep {
	
	private Map<Path, File> highestVersions;

	@DataBoundConstructor
	public ArchiveMavenRepository() {
		highestVersions = new HashMap<>();
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null) {
			Result buildResult = build.getResult();
			if(buildResult != null && buildResult.isWorseThan(Result.FAILURE)) {
				listener.getLogger().println("Build ended with worse than FAILURE. Maven repository wont be archived");
				return;
			}
			listener.getLogger().println("Archive private maven repository");
			List<FilePath> files = workspace.list();
			File f = new File(workspace.absolutize().toURI());
			File finalFile = new File(f, "repository" + UUID.randomUUID().toString() + ".tar");
			if(!finalFile.createNewFile()) {
				listener.getLogger().println("Cannot create "+finalFile.getName()+". Aborting.");
				return;
			}
			FileOutputStream repositoryZip = null;
			try {
				repositoryZip = new FileOutputStream(finalFile);
				for (FilePath file : files) {
					if (file.isDirectory() && file.getName().equals(".repository")) {
						preparePath(new File(file.toURI()));
						listener.getLogger().println("Found .repository folder");
						file.tar(repositoryZip, new FileFilter() {

							@Override
							public boolean accept(File pathname) {
								Path pPath = pathname.toPath().getParent();
								if(pPath != null) {
									Path parentPath = pPath.getParent();
									if(highestVersions.containsKey(parentPath)) {
										return highestVersions.get(parentPath).equals(pathname.getParentFile());
									}
								}
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
	
	
	private void preparePath(File repositoryFolder) {
		List<File> allFiles = new ArrayList<>();
		listFiles(repositoryFolder.toPath(), allFiles);
		for(File file: allFiles) {
			if(file.isDirectory()) {
				//check if starts with digit - ie version 2.6.2.v20161117-2150
				if(Character.isDigit(file.getName().charAt(0))){
					//check if p2 is in path
					String[] pathParts =file.getAbsolutePath().split(File.separatorChar=='\\' ? "\\\\" : File.separator);
					boolean found = false;
					for(String part: pathParts) {
						if(part.equals("p2")) {
							found = true;
							break;
						}
					}
					if(found && !wasSearched(file)) {
						File latestFile = getLatestFile(file);
						highestVersions.put(latestFile.getParentFile().toPath(), latestFile);
					}
				}
			}
					
		}
	}
	
	private boolean wasSearched(File file) {
		Path filePath = file.toPath().getParent();
		if(filePath != null) {
			for(Path p: highestVersions.keySet()) {
				if(p.toAbsolutePath().startsWith(filePath)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void listFiles(Path path, List<File> allFiles) {
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
	        for (Path entry : stream) {
	            if (Files.isDirectory(entry)) {
	                listFiles(entry, allFiles);
	            }
	            allFiles.add(entry.toFile());
	        }
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private File getLatestFile(File pathname) {
		File highestVersionFile = pathname;
		File[] otherVersions = pathname.getParentFile().listFiles();
		if (otherVersions != null && otherVersions.length > 1) {
			for (File f : otherVersions) {
				if (f.equals(pathname)) {
					continue;
				}

				DefaultArtifactVersion otherVersion = getMainVersion(f.getName());
				DefaultArtifactVersion highVersion = getMainVersion(
						highestVersionFile.getName());
				int compResult = otherVersion.compareTo(highVersion);

				if (compResult > 0) {
					// found new high version
					highestVersionFile = f;
				} else if (compResult == 0) {
					// check part after 'v'
					long otherSecVersion = getSecondaryVersion(f.getName());
					long highSecVersion = getSecondaryVersion(
							highestVersionFile.getName());
					if (highSecVersion - otherSecVersion < 0) {
						highestVersionFile = f;
					} else if (highSecVersion - otherSecVersion == 0) {
						// check part after '-'
						long otherTercVersion = getTerciaryVersion(f.getName());
						long highTercVersion = getTerciaryVersion(
								highestVersionFile.getName());
						if (highTercVersion - otherTercVersion < 0) {
							highestVersionFile = f;
						}
					}
				}

			}
		}
		return highestVersionFile;
	}
	
	private DefaultArtifactVersion getMainVersion(String name) {
		String[] splitName = name.split("v");
		return new DefaultArtifactVersion(splitName[0]);
	}
	
	private long getSecondaryVersion(String name) {
		String[] splitName = name.split("v");
		if(splitName.length == 2) {
			String[] anotherSplit = splitName[1].split("-");
			return Long.parseLong(anotherSplit[0]);
		}
		return 0;
	}
	
	private long getTerciaryVersion(String name) {
		String[] splitName = name.split("-");
		if(splitName.length == 2) {
			return Long.parseLong(splitName[1]);
		}
		return 0;
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
