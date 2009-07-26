/*
 * Copyright (c) 2008 Bradley W. Kimmel
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.eandb.jdcp.hub;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import ca.eandb.jdcp.job.TaskDescription;
import ca.eandb.jdcp.job.TaskWorker;
import ca.eandb.util.UnexpectedException;
import ca.eandb.util.rmi.Serialized;

/**
 * @author Brad
 *
 */
final class ServiceInfo {

	private static final Logger logger = Logger.getLogger(ServiceInfo.class);

	private final Executor executor;
	private final ServiceWrapper service;
	private final Map<UUID, JobInfo> jobs = new HashMap<UUID, JobInfo>();
	private boolean lastPollOk = true;
	private final DataSource dataSource;
	private final Queue<TaskDescription> pendingTasks = new LinkedList<TaskDescription>();

	private final Runnable getNextTask = new Runnable() {
		public void run() {
			TaskDescription task = null;
			if (!isIdle()) {
				task = service.requestTask();
				UUID jobId = task.getJobId();
				if (jobId == null) {
					try {
						int seconds = (Integer) task.getTask().deserialize();
						idle(seconds);
					} catch (ClassNotFoundException e) {
						throw new UnexpectedException(e);
					}
					task = null;
				} else {
					JobInfo job = getJobInfo(jobId);
					job.registerTask(task.getTaskId());
				}
			}
			if (task != null) {
				synchronized (pendingTasks) {
					pendingTasks.add(task);
				}
			}
		}
	};

	private Date idleUntil = new Date(0);

	public ServiceInfo(String host, String username, String password, DataSource dataSource, Executor executor) {
		this.service = new ServiceWrapper(host, username, password);
		this.dataSource = dataSource;
		this.executor = executor;
	}

	public static void prepareDataSource(DataSource ds) throws SQLException {
		JobInfo.prepareDataSource(ds);
	}
	public void shutdown() {
		service.shutdown();
	}

	public void pollActiveTasks() {
		ArrayList<UUID> jobIdList = new ArrayList<UUID>();
		ArrayList<Integer> taskIdList = new ArrayList<Integer>();
		for (JobInfo job : jobs.values()) {
			UUID jobId = job.getJobId();
			for (int taskId : job.getActiveTasks()) {
				jobIdList.add(jobId);
				taskIdList.add(taskId);
			}
		}
		if (jobIdList.size() > 0) {
			UUID[] jobIds = (UUID[]) jobIdList.toArray(new UUID[jobIdList.size()]);
			int[] taskIds = new int[taskIdList.size()];
			for (int i = 0, n = taskIdList.size(); i < n; i++) {
				taskIds[i] = taskIdList.get(i);
			}
			try {
				BitSet finished = service.getFinishedTasks(jobIds, taskIds);
				for (int i = finished.nextSetBit(0); i >= 0; i = finished.nextSetBit(i + 1)) {
					JobInfo job = jobs.get(jobIds[i]);
					job.removeTask(taskIds[i]);
				}
				lastPollOk = true;
			} catch (Exception e) {
				if (lastPollOk) {
					logger.error("Could not poll for finished tasks", e);
					lastPollOk = false;
				}
			}
		}
	}

	private boolean isIdle() {
		Date now = new Date();
		return now.before(idleUntil);
	}

	private synchronized void idle(int seconds) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, seconds);
		if (!isIdle()) {
			idleUntil = cal.getTime();
		} else {
			Date idleTime = cal.getTime();
			if (idleTime.before(idleUntil)) {
				idleUntil = idleTime;
			}
		}
	}

	private synchronized JobInfo getJobInfo(UUID id) {
		JobInfo job = jobs.get(id);
		if (job == null) {
			job = new JobInfo(id, service, dataSource, executor);
			jobs.put(id, job);
		}
		return job;
	}

	public byte[] getClassDefinition(String name, UUID jobId) {
		JobInfo job = getJobInfo(jobId);
		return job.getClassDefinition(name);
	}

	public byte[] getClassDigest(String name, UUID jobId) {
		JobInfo job = getJobInfo(jobId);
		return job.getClassDigest(name);
	}

	public boolean isTaskComplete(UUID jobId, int taskId) {
		JobInfo job = jobs.get(jobId);
		return (job == null) || job.isTaskComplete(taskId);
	}

	public Serialized<TaskWorker> getTaskWorker(UUID jobId)
			throws IllegalArgumentException {
		JobInfo job = getJobInfo(jobId);
		return job.getTaskWorker();
	}

	public void reportException(UUID jobId, int taskId, Exception e) {
		JobInfo job = getJobInfo(jobId);
		job.reportException(taskId, e);
	}

	public synchronized TaskDescription requestTask() {
		executor.execute(getNextTask);
		synchronized (pendingTasks) {
			return pendingTasks.poll();
		}
	}

	public void submitTaskResults(UUID jobId, int taskId,
			Serialized<Object> results) {
		JobInfo job = getJobInfo(jobId);
		job.submitTaskResults(taskId, results);
	}

}
