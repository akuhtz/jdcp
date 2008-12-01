/**
 *
 */
package org.jdcp.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdcp.job.Host;
import org.jdcp.job.JobExecutionException;
import org.jdcp.job.JobExecutionWrapper;
import org.jdcp.job.ParallelizableJob;
import org.jdcp.job.TaskDescription;
import org.jdcp.job.TaskWorker;
import org.jdcp.remote.JobService;
import org.jdcp.scheduling.TaskScheduler;
import org.jdcp.server.classmanager.ClassManager;
import org.jdcp.server.classmanager.ParentClassManager;
import org.selfip.bkimmel.io.FileUtil;
import org.selfip.bkimmel.progress.ProgressMonitor;
import org.selfip.bkimmel.rmi.Serialized;
import org.selfip.bkimmel.util.UnexpectedException;
import org.selfip.bkimmel.util.classloader.StrategyClassLoader;

/**
 * @author brad
 *
 */
public final class JobServer implements JobService {

	private static final int DEFAULT_IDLE_SECONDS = 10;

	private final ProgressMonitor monitor;

	private final TaskScheduler scheduler;

	private final ParentClassManager classManager;

	private final File outputDirectory;

	private final Map<UUID, ScheduledJob> jobs = new HashMap<UUID, ScheduledJob>();

	private final Logger logger;

	private final Executor executor;

	private TaskDescription idleTask = new TaskDescription(null, 0, DEFAULT_IDLE_SECONDS);

	/**
	 * Creates a new <code>JobServer</code>.
	 * @param outputDirectory The directory to write job results to.
	 * @param monitor The <code>ProgressMonitor</code> to report to.
	 * @param scheduler The <code>TaskScheduler</code> to use to assign
	 * 		tasks.
	 * @param classManager The <code>ParentClassManager</code> to use to
	 * 		store and retrieve class definitions.
	 * @param executor The <code>Executor</code> to use to run bits of code
	 * 		that should not hold up the remote caller.
	 */
	public JobServer(File outputDirectory, ProgressMonitor monitor, TaskScheduler scheduler, ParentClassManager classManager, Executor executor) throws IllegalArgumentException {
		if (!outputDirectory.isDirectory()) {
			throw new IllegalArgumentException("outputDirectory must be a directory.");
		}
		this.outputDirectory = outputDirectory;
		this.monitor = monitor;
		this.scheduler = scheduler;
		this.classManager = classManager;
		this.executor = executor;

		// TODO Replace this with a logger passed from the constructor.
		this.logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#createJob(java.lang.String)
	 */
	public UUID createJob(String description) throws SecurityException {
		ScheduledJob sched = new ScheduledJob(description, monitor);
		jobs.put(sched.id, sched);
		return sched.id;
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#submitJob(org.selfip.bkimmel.rmi.Envelope, java.util.UUID)
	 */
	public void submitJob(Serialized<ParallelizableJob> job, UUID jobId)
			throws IllegalArgumentException, SecurityException, ClassNotFoundException, JobExecutionException {
		ScheduledJob sched = jobs.get(jobId);
		if (sched == null || sched.job != null) {
			throw new IllegalArgumentException("No pending job with provided Job ID");
		}

		try {
			sched.initializeJob(job);
			sched.scheduleNextTask();
		} catch (JobExecutionException e) {
			handleJobExecutionException(e, jobId);
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#submitJob(org.selfip.bkimmel.rmi.Envelope, java.lang.String)
	 */
	public UUID submitJob(Serialized<ParallelizableJob> job, String description)
			throws SecurityException, ClassNotFoundException, JobExecutionException {
		ScheduledJob sched = new ScheduledJob(description, monitor);
		jobs.put(sched.id, sched);

		try {
			sched.initializeJob(job);
			sched.scheduleNextTask();
		} catch (JobExecutionException e) {
			handleJobExecutionException(e, sched.id);
			throw e;
		}

		return sched.id;
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#cancelJob(java.util.UUID)
	 */
	public void cancelJob(UUID jobId) throws IllegalArgumentException, SecurityException {
		if (!jobs.containsKey(jobId)) {
			throw new IllegalArgumentException("No job with provided Job ID");
		}

		removeScheduledJob(jobId, false);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#getTaskWorker(java.util.UUID)
	 */
	public Serialized<TaskWorker> getTaskWorker(UUID jobId)
			throws IllegalArgumentException, SecurityException {
		ScheduledJob sched = jobs.get(jobId);
		if (sched == null) {
			throw new IllegalArgumentException("No submitted job with provided Job ID");
		}

		return sched.worker;
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#requestTask()
	 */
	public synchronized TaskDescription requestTask() throws SecurityException {
		TaskDescription taskDesc = scheduler.getNextTask();
		if (taskDesc == null) {
			return idleTask;
		}

		ScheduledJob sched = jobs.get(taskDesc.getJobId());
		try {
			sched.scheduleNextTask();
		} catch (JobExecutionException e) {
			handleJobExecutionException(e, sched.id);
		}
		return taskDesc;
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#submitTaskResults(java.util.UUID, int, org.selfip.bkimmel.rmi.Envelope)
	 */
	public void submitTaskResults(final UUID jobId, final int taskId,
			final Serialized<Object> results) throws SecurityException {
		ScheduledJob sched = jobs.get(jobId);
		sched.submitTaskResults(taskId, results);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#getClassDefinition(java.lang.String, java.util.UUID)
	 */
	public byte[] getClassDefinition(String name, UUID jobId)
			throws SecurityException {
		ScheduledJob sched = jobs.get(jobId);
		if (sched == null) {
			throw new IllegalArgumentException("No job with provided Job ID");
		}

		ByteBuffer def = sched.classManager.getClassDefinition(name);
		if (def.hasArray() && def.arrayOffset() == 0) {
			return def.array();
		} else {
			byte[] bytes = new byte[def.remaining()];
			def.get(bytes);
			return bytes;
		}
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#getClassDigest(java.lang.String, java.util.UUID)
	 */
	public byte[] getClassDigest(String name, UUID jobId)
			throws SecurityException {
		ScheduledJob sched = jobs.get(jobId);
		if (sched == null) {
			throw new IllegalArgumentException("No job with provided Job ID");
		}

		return sched.classManager.getClassDigest(name);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#getClassDigest(java.lang.String)
	 */
	public byte[] getClassDigest(String name) throws SecurityException {
		return classManager.getClassDigest(name);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#setClassDefinition(java.lang.String, byte[])
	 */
	public void setClassDefinition(String name, byte[] def)
			throws SecurityException {
		classManager.setClassDefinition(name, def);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#setClassDefinition(java.lang.String, java.util.UUID, byte[])
	 */
	public void setClassDefinition(String name, UUID jobId, byte[] def)
			throws IllegalArgumentException, SecurityException {
		ScheduledJob sched = jobs.get(jobId);
		if (sched == null || sched.job != null) {
			throw new IllegalArgumentException("No pending job with provided Job ID");
		}

		sched.classManager.setClassDefinition(name, def);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#setIdleTime(int)
	 */
	public void setIdleTime(int idleSeconds) throws IllegalArgumentException,
			SecurityException {
		idleTask = new TaskDescription(null, 0, idleSeconds);
	}

	/* (non-Javadoc)
	 * @see org.jdcp.remote.JobService#setJobPriority(java.util.UUID, int)
	 */
	public void setJobPriority(UUID jobId, int priority)
			throws IllegalArgumentException, SecurityException {
		if (!jobs.containsKey(jobId)) {
			throw new IllegalArgumentException("No job with provided Job ID");
		}

		scheduler.setJobPriority(jobId, priority);
	}

	private void handleJobExecutionException(JobExecutionException e, UUID jobId) {
		logger.log(Level.WARNING, "Exception thrown from job " + jobId.toString(), e);
		removeScheduledJob(jobId, false);
	}

	private void removeScheduledJob(UUID jobId, boolean complete) {
		ScheduledJob sched = jobs.remove(jobId);
		if (sched != null) {
			if (complete) {
				sched.monitor.notifyComplete();
			} else {
				sched.monitor.notifyCancelled();
			}
			jobs.remove(jobId);
			scheduler.removeJob(jobId);
			classManager.releaseChildClassManager(sched.classManager);
		}
	}

	/**
	 * Represents a <code>ParallelizableJob</code> that has been submitted
	 * to this <code>JobMasterServer</code>.
	 * @author bkimmel
	 */
	private class ScheduledJob implements Host {

		/** The <code>ParallelizableJob</code> to be processed. */
		public JobExecutionWrapper				job;

		/** The <code>UUID</code> identifying the job. */
		public final UUID						id;

		/** A description of the job. */
		public final String						description;

		/** The <code>TaskWorker</code> to use to process tasks for the job. */
		public Serialized<TaskWorker>				worker;

		/**
		 * The <code>ProgressMonitor</code> to use to monitor the progress of
		 * the <code>Job</code>.
		 */
		public final ProgressMonitor			monitor;

		/**
		 * The <code>ClassManager</code> to use to store the class definitions
		 * applicable to this job.
		 */
		public final ClassManager				classManager;

		private final File						workingDirectory;

		/**
		 * Initializes the scheduled job.
		 * @param description A description of the job.
		 * @param monitor The <code>ProgressMonitor</code> from which to create a child
		 * 		monitor to use to monitor the progress of the
		 * 		<code>ParallelizableJob</code>.
		 */
		public ScheduledJob(String description, ProgressMonitor monitor) {

			this.id					= UUID.randomUUID();
			this.description		= description;

			//String title			= String.format("%s (%s)", this.job.getClass().getSimpleName(), this.id.toString());
			this.monitor			= monitor.createChildProgressMonitor(description);
			this.monitor.notifyStatusChanged("Awaiting job submission");

			this.classManager		= JobServer.this.classManager.createChildClassManager();

			this.workingDirectory	= new File(outputDirectory, id.toString());

		}

		public void initializeJob(Serialized<ParallelizableJob> job) throws ClassNotFoundException, JobExecutionException {
			ClassLoader loader	= new StrategyClassLoader(classManager, JobServer.class.getClassLoader());
			this.job			= new JobExecutionWrapper(job.deserialize(loader));
			this.worker			= new Serialized<TaskWorker>(this.job.worker());
			this.monitor.notifyStatusChanged("");

			this.workingDirectory.mkdir();
			this.job.initialize(this);
		}

		public void submitTaskResults(int taskId, Serialized<Object> results) {
			Object task = scheduler.remove(id, taskId);
			Runnable command = new TaskResultSubmitter(this, task, results, monitor);
			try {
				executor.execute(command);
			} catch (RejectedExecutionException e) {
				command.run();
			}
		}

		public void scheduleNextTask() throws JobExecutionException {
			Object task = job.getNextTask();
			if (task != null) {
				scheduler.add(id, task);
			}
		}

		/**
		 * Writes the results of a <code>ScheduledJob</code> to the output
		 * directory.
		 * @param sched The <code>ScheduledJob</code> to write results for.
		 * @throws JobExecutionException
		 */
		private void finalizeJob() throws JobExecutionException {

			assert(job.isComplete());

			job.finish();

			try {

				String				filename		= String.format("%s.zip", id.toString());
				File				outputFile		= new File(outputDirectory, filename);

				File				logFile			= new File(workingDirectory, "job.log");
				PrintStream			log				= new PrintStream(new FileOutputStream(logFile));

				log.printf("%tc: Job %s completed.", new Date(), id.toString());
				log.println();
				log.flush();
				log.close();

				FileUtil.zip(outputFile, workingDirectory);
				FileUtil.deleteRecursive(workingDirectory);

			} catch (IOException e) {
				logger.log(Level.WARNING, "Exception caught while finalizing job " + id.toString(), e);
			}

		}

		private File getWorkingFile(String path) {
			File file = new File(workingDirectory, path).getAbsoluteFile();
			try {
				if (!FileUtil.isAncestor(file, workingDirectory)) {
					throw new IllegalArgumentException("path must not reference parent directory.");
				}
			} catch (IOException e) {
				throw new UnexpectedException(e);
			}
			return file;
		}

		@Override
		public FileOutputStream createFileOutputStream(final String path) {
			return AccessController.doPrivileged(new PrivilegedAction<FileOutputStream>() {
				public FileOutputStream run() {
					File file = getWorkingFile(path);
					File dir = file.getParentFile();
					dir.mkdirs();
					try {
						return new FileOutputStream(file);
					} catch (FileNotFoundException e) {
						throw new UnexpectedException(e);
					}
				}
			});
		}

		@Override
		public RandomAccessFile createRandomAccessFile(final String path) {
			return AccessController.doPrivileged(new PrivilegedAction<RandomAccessFile>() {
				public RandomAccessFile run() {
					File file = getWorkingFile(path);
					File dir = file.getParentFile();
					dir.mkdirs();
					try {
						return new RandomAccessFile(file, "rw");
					} catch (FileNotFoundException e) {
						throw new UnexpectedException(e);
					}
				}
			});
		}

	}

	private class TaskResultSubmitter implements Runnable {

		private final ScheduledJob sched;
		private final Object task;
		private final Serialized<Object> results;
		private final ProgressMonitor monitor;

		/**
		 * @param sched
		 * @param task
		 * @param results
		 * @param monitor
		 */
		public TaskResultSubmitter(ScheduledJob sched, Object task,
				Serialized<Object> results, ProgressMonitor monitor) {
			this.sched = sched;
			this.task = task;
			this.results = results;
			this.monitor = monitor;
		}

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			ClassLoader cl = sched.job.getClass().getClassLoader();
			if (task != null) {
				try {
					sched.job.submitTaskResults(task,
							results.deserialize(cl), monitor);

					if (sched.job.isComplete()) {
						sched.finalizeJob();
						removeScheduledJob(sched.id, true);
					}
				} catch (JobExecutionException e) {
					handleJobExecutionException(e, sched.id);
				} catch (ClassNotFoundException e) {
					logger.log(Level.WARNING,
							"Exception thrown submitting results of task for job "
									+ sched.id.toString(), e);
					removeScheduledJob(sched.id, false);
				} catch (Exception e) {
					logger.log(Level.WARNING,
							"Exception thrown while attempting to submit task results for job "
									+ sched.id.toString(), e);
					removeScheduledJob(sched.id, false);
				}
			}
		}

	}

}
