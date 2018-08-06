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
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

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
			TaskListener listener, FilePath workspace, EnvVars env) throws IOException, InterruptedException {

		try {
			infoAboutExecutor(listener, null);
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
		} else if (used.getPreferedCall().startsWith("{")) {
			if (Jenkins.getInstance() != null) {
				String assumedNodeName = Label.decorate(used.getPreferedCall(), workspace, env);
				if (used.getPreferedCall().toLowerCase().startsWith("{node{")) {
					assumedNodeName = assumedNodeName.replaceAll("\\{.*\\{", "").replaceAll("}}", "");
				}
				listener.getLogger().println("Trying remote call to node " + assumedNodeName);
				Node n = Jenkins.getInstance().getNode(assumedNodeName);
				if (n != null) {
					status = n.createLauncher(listener).decorateFor(n).getChannel().call(slaveTask);
					return status;
				}
				listener.getLogger().println("Node '" + assumedNodeName + "' not found");
			}
			listener.getLogger().println("Attempted all possible tagets for prefered call but non matched");
			status = "Call target not found"; // slaveTask.call();
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

	public static void infoAboutExecutor(TaskListener listener, JenkinsSlaveCallableBase logger)
			throws UnknownHostException {
		String message = "Executing remote call on Computer "
				+ (Computer.currentComputer() == null || Computer.currentComputer().getDisplayName() == null ? ""
						: Computer.currentComputer().getDisplayName())
				+ ((InetAddress.getLocalHost() != null && InetAddress.getLocalHost().getHostAddress() != null)
						? " with address: " + InetAddress.getLocalHost().getHostAddress().toString()
						: "")
				+ ((Computer.currentComputer() != null && Computer.currentComputer().getNode() != null
						&& Computer.currentComputer().getNode().getChannel() != null)
								? " and channel (" + Computer.currentComputer().getNode().getChannel().toString() + ")"
								: "")
				+ ((logger != null && logger.env != null && logger.env.size() != 0 && logger.env.entrySet() != null
						&& !logger.env.entrySet().isEmpty()) ? " Env: " + logger.env.entrySet().toString() : "");
		if (listener != null) {
			listener.getLogger().println(message);
		}
		if (logger != null) {
			logger.info(message);
		}
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
			info("Loggin as java.util.logging.Logger into: " + path);
		}
		try {
			infoAboutExecutor(null, this);
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
