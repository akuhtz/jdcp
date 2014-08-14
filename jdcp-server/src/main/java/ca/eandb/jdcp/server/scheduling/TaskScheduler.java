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

package ca.eandb.jdcp.server.scheduling;

import java.util.UUID;

import ca.eandb.jdcp.job.TaskDescription;

/**
 * Represents an object that is responsible for determine in what order to
 * serve tasks to worker clients.
 * @author Brad Kimmel
 */
public interface TaskScheduler {

  /**
   * Sets the priority of a job.  The job priority is a hint to the scheduler
   * which it may use to determine the order in which tasks are served.
   * @param jobId The <code>UUID</code> identifying the job for which to set
   *     the priority.
   * @param priority The priority to set for the specified job.
   */
  void setJobPriority(UUID jobId, int priority);

  /**
   * Adds a task to be scheduled.
   * @param task An <code>TaskDescription</code> describing the task to be
   *     performed.
   */
  void add(TaskDescription task);

  /**
   * Removes a task from the schedule.
   * @param jobId The <code>UUID</code> identifying the job associated with
   *     the task to be removed.
   * @param taskId The identifier for the task to be removed.
   * @return The <code>TaskDescription</code> describing the specified task.
   */
  TaskDescription remove(UUID jobId, int taskId);

  /**
   * Retrieves the specified task.
   * @param jobId The <code>UUID</code> identifying the job associated with
   *     the task to retrieve.
   * @param taskId The identifier for the task to be retrieved.
   * @return The <code>TaskDescription</code> describing the specified task,
   *     or <code>null</code> if no such task exists.
   */
  TaskDescription get(UUID jobId, int taskId);

  /**
   * Determines if the specified task exists.
   * @param jobId The <code>UUID</code> identifying the job associated with
   *     the task to retrieve.
   * @param taskId The identifier for the task to be retrieved.
   * @return A value indicating if the specified task exists.
   */
  boolean contains(UUID jobId, int taskId);

  /**
   * Gets the next task to be served.
   * @return A <code>TaskDescription</code> describing the next task to be
   *     served.
   * @see ca.eandb.jdcp.job.TaskDescription
   */
  TaskDescription getNextTask();

  /**
   * Removes all tasks from the schedule that are associated with the
   * specified job.
   * @param jobId The <code>UUID</code> identifying the job for which all
   *     tasks are to be removed.
   */
  void removeJob(UUID jobId);

}
