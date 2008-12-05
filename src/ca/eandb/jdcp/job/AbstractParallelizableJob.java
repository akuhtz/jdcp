/**
 *
 */
package ca.eandb.jdcp.job;

import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import ca.eandb.util.progress.ProgressMonitor;

/**
 * An abstract <code>ParallelizableJob</code> that provides a default
 * implementation for the <code>Job</code> interface.
 * @author bkimmel
 */
public abstract class AbstractParallelizableJob implements ParallelizableJob {

	private transient HostService host = null;

	public final void setHostService(HostService host) {
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.job.ParallelizableJob#initialize()
	 */
	public void initialize() throws Exception {
		/* nothing to do. */
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.job.ParallelizableJob#finish()
	 */
	public void finish() throws Exception {
		/* nothing to do. */
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.job.ParallelizableJob#suspend()
	 */
	public void suspend() throws Exception {
		/* nothing to do. */
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.job.ParallelizableJob#resume()
	 */
	public void resume() throws Exception {
		/* nothing to do. */
	}

	/**
	 * @param name
	 * @return
	 * @see ca.eandb.jdcp.job.HostService#createFileOutputStream(java.lang.String)
	 */
	protected FileOutputStream createFileOutputStream(String name) {
		return host.createFileOutputStream(name);
	}

	/**
	 * @param name
	 * @return
	 * @see ca.eandb.jdcp.job.HostService#createRandomAccessFile(java.lang.String)
	 */
	protected RandomAccessFile createRandomAccessFile(String name) {
		return host.createRandomAccessFile(name);
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jmist.framework.Job#go(ca.eandb.util.progress.ProgressMonitor)
	 */
	public boolean go(ProgressMonitor monitor) {

		try {

			/* Get the task worker. */
			TaskWorker		worker			= this.worker();
			int			 	taskNumber		= 0;

			/* Check to see if the process has been cancelled. */
			monitor.notifyIndeterminantProgress();

			/* Main loop. */
			while (!monitor.isCancelPending()) {

				/* Get the next task. */
				Object				task			= this.getNextTask();

				/* If there is no next task, then we're done. */
				if (task == null) {
					monitor.notifyComplete();
					return true;
				}

				/* Create a progress monitor to monitor the task. */
				String				taskDesc		= String.format("Task %d", ++taskNumber);
				ProgressMonitor		taskMonitor		= monitor.createChildProgressMonitor(taskDesc);

				/* Perform the task. */
				Object				results			= worker.performTask(task, taskMonitor);

				/* If the task was cancelled, then cancel the job. */
				if (taskMonitor.isCancelPending()) {
					break;
				}

				/* Submit the task results. */
				this.submitTaskResults(task, results, monitor);

			}

		} catch (Exception e) {

			e.printStackTrace();

		}

		/* If we get to this point, then the job was cancelled. */
		monitor.notifyCancelled();
		return false;

	}

}
