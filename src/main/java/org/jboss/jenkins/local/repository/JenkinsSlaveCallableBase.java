package org.jboss.jenkins.local.repository;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.jenkinsci.remoting.RoleChecker;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.slaves.SlaveComputer;

public class JenkinsSlaveCallableBase implements Callable<String, IOException>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	TaskListener listener;
	FilePath workspace;
	EnvVars env;
	String logDirPath;
	Logger logger;
	PrintStream ps;
	Label label;

	public JenkinsSlaveCallableBase(TaskListener listener, FilePath workspace, EnvVars env, Label label) {
		this.listener = listener;
		this.workspace = workspace;
		this.env = env;
		this.label = label;
		this.logDirPath = workspace.getRemote();
		// this.logDirPath = "/local/git/";
	}

	public static String decideAndCall(Label used, Launcher launcher, JenkinsSlaveCallableBase slaveTask,
			TaskListener listener) throws IOException, InterruptedException {

		try {
			if (Computer.currentComputer() != null) {
				listener.getLogger().println("Executing remote call on Computer "
						+ Computer.currentComputer().getDisplayName()
						+ ((Computer.currentComputer().getNode() != null
								&& Computer.currentComputer().getNode().getChannel() != null)
										? " (" + Computer.currentComputer().getNode().getChannel().toString() + ")"
										: ""));
				/*
				 * listener.getLogger().println("Executing remote call on Computer");
				 * listener.getLogger().println("getDisplayName():" +
				 * Computer.currentComputer().getDisplayName());
				 * listener.getLogger().println("getHostName():" +
				 * Computer.currentComputer().getHostName().toString()); listener.getLogger()
				 * .println("getNode().getDisplayName():" +
				 * Computer.currentComputer().getNode().getDisplayName()); listener.getLogger()
				 * .println("getNode().getSearchName():" +
				 * Computer.currentComputer().getNode().getSearchName());
				 * listener.getLogger().println( "getNode().getChannel():" +
				 * Computer.currentComputer().getNode().getChannel().toString());
				 */
			}
		} catch (Exception e) {
			listener.getLogger().println(e.getMessage());
		}

		String status = "";
		if (used.getPreferedCall() == null || used.getPreferedCall().matches("Local")) {
			listener.getLogger().println("Executing remote call on local channel");
			status = slaveTask.call();
		} else if (used.getPreferedCall().matches("CurrentChannel") && Channel.current() != null) {
			listener.getLogger().println("Executing remote call on Current channel");
			status = Channel.current().call(slaveTask);
		} else if (used.getPreferedCall().matches("SlaveComputer") && SlaveComputer.currentComputer() != null) {
			listener.getLogger().println("Executing remote call on Slave computer");
			status = SlaveComputer.currentComputer().getNode().getChannel().call(slaveTask);
		} else if (used.getPreferedCall().matches("Launcher")) {
			listener.getLogger().println("Executing remote call on Launcher");
			status = launcher.getChannel().call(slaveTask);
		} else {
			listener.getLogger().println("Executing remote call on local channel (last call)");
			status = slaveTask.call();
		}
		return status;
	}

	public void info(String msg) {
		if (logger != null) {
			logger.info(msg);
		}
		if (ps != null) {
			ps.println(msg);
		}
		if (ps == null && logger == null) {
			System.out.print(msg);
		}
	}

	public void monitorFileSizeChange(final String filePath) {
		Thread asynchFileSizeProgressLogging = new Thread() {
			@Override
			public void run() {
				info("Monitoring file size changes for file: " + filePath);
				VirtualChannel channel = Label.getCurrentChannel();
				FilePath monitored = new FilePath(channel, filePath);
				long currentSize = 0;
				long lastSize = 0;
				int sleepSecs = 5;
				int monitorTimeLimitMins = 10;
				// 120; // 60<s>/5<s sleep>*10<mins max wait for copy> = 60*2
				int timeLimit = (60 / sleepSecs) * monitorTimeLimitMins;
				for (int counter = 0; counter < timeLimit; counter++) {
					try {
						monitored = new FilePath(channel, filePath);
						currentSize = monitored.length();
						if (counter > 1 && (currentSize <= lastSize || !monitored.exists())) {
							info("Final file path: " + filePath + " with size: " + currentSize + " B (last size "
									+ lastSize + " B)");
							return;
						}
						info("Counter: " + counter + "; File path: " + filePath + " with size: " + currentSize
								+ " B (last size " + lastSize + " B)");
						lastSize = currentSize;
					} catch (IOException | InterruptedException e) {
						e.printStackTrace();
						info(e.getMessage());
					}
					try {
						Thread.sleep(sleepSecs * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						info(e.getMessage());
					}
				}
			}
		};
		asynchFileSizeProgressLogging.start();
	}

	public void initSlave(String loggerName) {
		this.workspace = new FilePath(label.getChannel(), workspace.getRemote());
		String path = this.logDirPath + "/" + loggerName + ".log";
		if (listener == null) {
			logger = Logger.getLogger(loggerName);
			FileHandler fh;
			try {
				// This block configure the logger with handler and formatter
				fh = new FileHandler(path);
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);
				info("Logging as java.util.logging.Logger into: " + path);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			ps = listener.getLogger();
			info("Loggin as java.util.logging.Logger into: " + path);
		}
		try {

			info("Executing remote call on Computer " + Computer.currentComputer().getDisplayName()
					+ ((InetAddress.getLocalHost() != null && InetAddress.getLocalHost().getHostAddress() != null)
							? " address: " + InetAddress.getLocalHost().getHostAddress().toString()
							: "")
					+ ((Computer.currentComputer() != null && Computer.currentComputer().getNode() != null
							&& Computer.currentComputer().getNode().getChannel() != null)
									? " (" + Computer.currentComputer().getNode().getChannel().toString() + ")"
									: ""));
			/*
			 * info("Executing remote call on current computer with:");
			 * info("getDisplayName():" + Computer.currentComputer().getDisplayName());
			 * info("getHostName():" + Computer.currentComputer().getHostName().toString());
			 * info("getNode().getDisplayName():" +
			 * Computer.currentComputer().getNode().getDisplayName());
			 * info("getNode().getSearchName():" +
			 * Computer.currentComputer().getNode().getSearchName());
			 * info("getNode().getChannel():" +
			 * Computer.currentComputer().getNode().getChannel().toString()); //
			 * label.setChannel(Computer.currentComputer().getNode().getChannel());
			 * 
			 */
		} catch (/* UnknownHostException | */IOException /* | InterruptedException */ e) {
			e.printStackTrace();
			info(e.getMessage());
		}
	}

	@Override
	public void checkRoles(RoleChecker arg0) throws SecurityException {
	}

	@Override
	public String call() throws IOException {
		initSlave("shared-maven-repository-slave-base");
		return "DoneBase";
	}

}
