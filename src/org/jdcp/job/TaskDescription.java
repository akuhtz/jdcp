/**
 *
 */
package org.jdcp.job;

import java.io.Serializable;
import java.util.UUID;

import org.jdcp.remote.JobService;
import org.selfip.bkimmel.rmi.Serialized;

/**
 * A description of a task assigned by a <code>JobMasterService</code>.
 * @author bkimmel
 * @see {@link JobService#requestTask()}.
 */
public final class TaskDescription implements Serializable {

	/**
	 * Initializes the task description.
	 * @param jobId The <code>UUID</code> of the job that the task is for.
	 * @param taskId The ID of the task to be performed.
	 * @param task An <code>Object</code> describing the task to be performed.
	 * 		This should be passed to <code>TaskWorker.performTask</code>.
	 * @see {@link org.jmist.framework.TaskWorker#performTask(Object, org.jmist.framework.ProgressMonitor)}.
	 */
	public TaskDescription(UUID jobId, int taskId, Object task) {
		this.jobId = jobId;
		this.taskId = taskId;
		this.task = new Serialized<Object>(task);
	}

	/**
	 * Gets the <code>Object</code> describing the task to be performed.  This
	 * should be passed to <code>TaskWorker.performTask</code> for the
	 * <code>TaskWorker</code> corresponding to the job with the
	 * <code>UUID</code> given by {@link #getJobId()}.  The <code>TaskWorker</code>
	 * may be obtained by calling {@link JobService#getTaskWorker(UUID)}.
	 * @return The <code>Object</code> describing the task to be performed.
	 * @see {@link #getJobId()},
	 * 		{@link org.jmist.framework.TaskWorker#performTask(Object, org.jmist.framework.ProgressMonitor)},
	 * 		{@link JobService#getTaskWorker(UUID)}.
	 */
	public Serialized<Object> getTask() {
		return this.task;
	}

	/**
	 * Gets the <code>UUID</code> of the job whose <code>TaskWorker</code>
	 * should perform this task.  Call {@link JobService#getTaskWorker(UUID)}
	 * to get the <code>TaskWorker</code> to use to perform this task.
	 * @return The <code>UUID</code> of the job that this task is associated
	 * 		with.
	 * @see {@link JobService#getTaskWorker(UUID)}.
	 */
	public UUID getJobId() {
		return this.jobId;
	}

	/**
	 * The ID of the task to be performed.  This should be passed back to
	 * <code>JobMasterService.submitTaskResults</code> when submitting the
	 * results of this task.
	 * @return The ID of the task to be performed.
	 * @see {@link JobService#submitTaskResults(UUID, int, Object)}.
	 */
	public int getTaskId() {
		return this.taskId;
	}

	/** The <code>UUID</code> of the job that this task is a part of. */
	private final UUID jobId;

	/** The ID of this task. */
	private final int taskId;

	/** The <code>Object</code> describing the task to be performed. */
	private final Serialized<Object> task;

	/**
	 * Serialization version ID.
	 */
	private static final long serialVersionUID = 295569474645825592L;

}
