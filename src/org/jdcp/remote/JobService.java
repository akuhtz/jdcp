/**
 *
 */
package org.jdcp.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

import org.jdcp.job.JobExecutionException;
import org.jdcp.job.ParallelizableJob;
import org.jdcp.job.TaskDescription;
import org.jdcp.job.TaskWorker;
import org.selfip.bkimmel.rmi.Serialized;

/**
 * A remote service for accepting <code>ParallelizableJob</code>s,
 * managing the distribution of tasks to workers, and aggregating the results
 * submitted by workers.
 * @author bkimmel
 */
public interface JobService extends Remote {

	public static final int DEFAULT_PRIORITY = 20;

	/* **************************************************************
	 * Worker client methods (Task distribution and result gathering)
	 */

	/**
	 * Gets the task worker for a job.
	 * @param jobId The <code>UUID</code> of the job to obtain the task worker
	 * 		for.
	 * @return The <code>TaskWorker</code> to use to process tasks for the job
	 * 		with the specified <code>UUID</code>, or <code>null</code> if that
	 * 		job is no longer available.
	 */
	Serialized<TaskWorker> getTaskWorker(UUID jobId)
			throws IllegalArgumentException, SecurityException, RemoteException;

	/**
	 * Gets a task to perform.
	 * @return A <code>TaskDescription</code> describing the task to be
	 * 		performed.
	 */
	TaskDescription requestTask() throws SecurityException, RemoteException;

	/**
	 * Submits the results of a task.
	 * @param jobId The <code>UUID</code> identifying the job for which the
	 * 		task was performed.
	 * @param taskId The ID of the task that was performed.
	 * @param results The results of the task.
	 * @throws ClassNotFoundException
	 */
	void submitTaskResults(UUID jobId, int taskId, Serialized<Object> results)
			throws SecurityException, RemoteException;

	/**
	 * Report that an exception was thrown during the execution of an assigned
	 * task.
	 * @param jobId The <code>UUID</code> identifying the job for which the
	 * 		task was being performed.
	 * @param taskId The ID of the task that was being performed, or zero (0)
	 * 		if the exception was not thrown during the processing of a task
	 * 		(for example, when deserializing the TaskWorker).
	 * @param e The <code>Exception</code> raised by the task.
	 * @throws SecurityException If the caller does not have permission to call
	 * 		this method.
	 * @throws RemoteException If a problem with the connection is encountered.
	 */
	void reportException(UUID jobId, int taskId, Exception e)
			throws SecurityException, RemoteException;


	/* **********************
	 * Job submission methods
	 */

	UUID createJob(String description) throws SecurityException, RemoteException;

	void submitJob(Serialized<ParallelizableJob> job, UUID jobId)
			throws IllegalArgumentException, SecurityException,
			ClassNotFoundException, RemoteException, JobExecutionException;

	/**
	 * Submits a new job to be processed.
	 * @param job The <code>ParallelizableJob</code> to be processed.
	 * @param priority The priority to assign to the job.
	 * @return The <code>UUID</code> assigned to the job, or <code>null</code>
	 * 		if the job was not accepted.
	 * @throws ClassNotFoundException
	 * @throws JobExecutionException
	 */
	UUID submitJob(Serialized<ParallelizableJob> job, String description)
			throws SecurityException, ClassNotFoundException, RemoteException,
			JobExecutionException;

	void cancelJob(UUID jobId) throws IllegalArgumentException, SecurityException, RemoteException;


	/* ************************
	 * Class management methods
	 */

	byte[] getClassDigest(String name, UUID jobId) throws SecurityException, RemoteException;

	byte[] getClassDigest(String name) throws SecurityException, RemoteException;

	byte[] getClassDefinition(String name, UUID jobId) throws SecurityException, RemoteException;

	void setClassDefinition(String name, byte[] def) throws SecurityException, RemoteException;

	void setClassDefinition(String name, UUID jobId, byte[] def) throws IllegalArgumentException, SecurityException, RemoteException;


	/* **********************
	 * Administrative methods
	 */

	/**
	 * Sets the amount of time (in seconds) that workers should idle when there
	 * are no tasks to be performed.
	 * @param idleSeconds The amount of time (in seconds) that workers should
	 * 		idle when there are no tasks to be performed.
	 */
	void setIdleTime(int idleSeconds) throws IllegalArgumentException, SecurityException, RemoteException;

	void setJobPriority(UUID jobId, int priority) throws IllegalArgumentException, SecurityException, RemoteException;

}
