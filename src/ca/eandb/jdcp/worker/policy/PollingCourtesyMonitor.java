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

package ca.eandb.jdcp.worker.policy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Brad
 *
 */
public final class PollingCourtesyMonitor implements BlockingCourtesyMonitor {

	private static final int CORE_THREAD_POOL_SIZE = 2;

	private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(CORE_THREAD_POOL_SIZE);

	private final CourtesyMonitor inner;

	private boolean allow = true;

	private final Runnable poll = new Runnable() {
		public void run() {
			allow = inner.allowTasksToRun();
			synchronized (PollingCourtesyMonitor.this) {
				if (allow) {
					PollingCourtesyMonitor.this.notifyAll();
				}
			}
		}
	};

	public PollingCourtesyMonitor(long initialDelay, long period, TimeUnit unit, CourtesyMonitor inner) {
		this.inner = inner;
		executor.scheduleAtFixedRate(poll, initialDelay, period, unit);
	}

	public PollingCourtesyMonitor(long period, TimeUnit unit, CourtesyMonitor inner) {
		this(0, period, unit, inner);
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.CourtesyMonitor#allowTasksToRun()
	 */
	public boolean allowTasksToRun() {
		return allow;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.BlockingCourtesyMonitor#waitFor()
	 */
	public synchronized void waitFor() throws InterruptedException {
		if (!allow) {
			wait();
		}
	}

}
