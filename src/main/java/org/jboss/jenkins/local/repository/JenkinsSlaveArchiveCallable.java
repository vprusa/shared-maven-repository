package org.jboss.jenkins.local.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

public class JenkinsSlaveArchiveCallable implements Callable<String, IOException>, Serializable {

	Label label;
	FilePath workspace;
	EnvVars env;

	public JenkinsSlaveArchiveCallable(Label label, FilePath workspace,
			EnvVars env) {
		this.label = label;
		this.workspace = workspace;
		this.env = env;
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		FilePath archivedLocalFile;
		
		label.clearFilePathsCache();
		try {

			if (label.getArchiveFilePath(workspace, env).isDirectory()) {
				// is dir so create new zip with unique name in this dir
				archivedLocalFile = new FilePath(label.getDownloadFilePath(workspace, env).getParent(),
						"archived-" + UUID.randomUUID().toString() + ".zip");
			} else {
				// is file so create new file next to old one
				archivedLocalFile = new FilePath(label.getDownloadFilePath(workspace, env).getParent(),
						label.getArchiveFilePath(workspace, env).getBaseName() + "-archived-"
								+ UUID.randomUUID().toString() + ".zip");
			}
			
			if (!archivedLocalFile.exists()) {
				// listener.getLogger().println("Creating empty zip " +
				// archivedLocalFile.toURI());
				try {
					try {
						archivedLocalFile.getParent().mkdirs();
					} catch (Exception e) {
						e.printStackTrace();
					}
					archivedLocalFile.touch(new Date().getTime());
				} catch (Exception e) {
					// https://github.com/openshift/jenkins-client-plugin/issues/121
					e.printStackTrace();
					try {
						new File(archivedLocalFile.getRemote()).createNewFile();
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}
			}

			FileFilter filter;
			if (label.isM2Repo()) {
				RepoFileFilter repoFilter = new RepoFileFilter();
				try {
					repoFilter.preparePath(new File(label.getDownloadFilePath(workspace, env).toURI()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				filter = repoFilter;
			} else {
				filter = new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return true;
					}
				};
			}
			OutputStream repoOutputStream;

			repoOutputStream = archivedLocalFile.write();
			try {
				// listener.getLogger().println("Fill archive " + archivedLocalFile.toURI());
				label.getDownloadFilePath(workspace, env).archive(TrueZipArchiver.FACTORY, repoOutputStream, filter);
				// listener.getLogger().println("Done!");
			} catch (IOException e) {
				repoOutputStream.close();
				return "Failed";
			}
			repoOutputStream.close();
			MasterMavenRepository.getInstance().uploadRepository(archivedLocalFile, workspace, /* listener, */ label,
					env);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return "Failed";
		}

		return "Done";
	}

}
