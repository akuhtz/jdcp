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

package ca.eandb.jdcp.worker.policy.win32;


import java.awt.Window;

import javax.swing.JWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.win32.StdCallLibrary;

import ca.eandb.jdcp.worker.policy.AsyncCourtesyMonitor;
import ca.eandb.jdcp.worker.policy.PowerCourtesyMonitor;
import ca.eandb.jdcp.worker.policy.win32.ExtKernel32.SYSTEM_POWER_STATUS;
import ca.eandb.util.UnexpectedException;

/**
 * A <code>CourtesyMonitor</code> that monitors the power status of this
 * machine.
 * @author Brad Kimmel
 */
public final class Win32PowerCourtesyMonitor extends AsyncCourtesyMonitor
implements PowerCourtesyMonitor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Win32PowerCourtesyMonitor.class);

	/**
	 * A value indicating whether tasks should run only if A/C power is
	 * connected.
	 */
	private boolean requireAC = true;

	/**
	 * This value sets the battery life percentage below which tasks will be
	 * suspended.  If {@link #requireAC} is set, this value has no effect.
	 */
	private int minBatteryLifePercent = 0;

	/**
	 * This value sets the battery life percentage below which tasks will be
	 * suspended while the battery is charging.
	 */
	private int minBatteryLifePercentWhileCharging = 0;

	/** Receives WM_POWERBROADCAST messages from Windows. */
	@SuppressWarnings("unused")
	private final PowerBroadcastMonitor monitor;

	/**
	 * A <code>WindowProc</code> for receiving WM_POWERBROADCAST messages
	 * from Windows.
	 */
	private final class PowerBroadcastMonitor implements StdCallLibrary.StdCallCallback {

		/** A dummy <code>Window</code> to receive messages. */
		private final Window msgWindow = new JWindow();

		/** The original <code>WNDPROC</code> of the dummy window. */
		private final Pointer prevWndProc;

		private HWND hwnd;

		/**
		 * Creates a <code>PowerBroadcastMonitor</code>.
		 */
		public PowerBroadcastMonitor() {
			// The window needs to be made visible once to set its WindowProc.
			msgWindow.setVisible(true);
			msgWindow.setVisible(false);

			// Get a handle to the window.
			hwnd = new HWND();
			hwnd.setPointer(Native.getWindowPointer(msgWindow));

			// Set the WindowProc so that this instance receives window
			// messages.
			StdCallLibrary.StdCallCallback callback = (StdCallLibrary.StdCallCallback) this;

			if (Platform.is64Bit()) {
				prevWndProc = ExtUser32.INSTANCE.SetWindowLongPtr(hwnd, User32.GWL_WNDPROC, callback);
			}
			else {
				prevWndProc = ExtUser32.INSTANCE.SetWindowLong(hwnd, User32.GWL_WNDPROC, callback);
			}
		}


		private int WM_POWERBROADCAST = 0x0218;

		private static final int PBT_APMPOWERSTATUSCHANGE = 0x0A; 
		private static final int PBT_APMRESUMEAUTOMATIC = 0x12; 
		private static final int PBT_APMRESUMESUSPEND = 0x07; 
		private static final int PBT_APMSUSPEND = 0x04; 

		/**
		 * Handles window messages.
		 */
		public LRESULT callback(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
			// If the message is a WM_POWERBROADCAST, then update the
			// CourtesyMonitor.
			if (uMsg == WM_POWERBROADCAST) {

				switch (wParam.intValue()) {
				case PBT_APMPOWERSTATUSCHANGE:
					update();
					break;
				case PBT_APMRESUMEAUTOMATIC:
				case PBT_APMRESUMESUSPEND:
				case PBT_APMSUSPEND:
					updateSuspend(wParam.intValue());
					break;
				default:
					break;
				}
			}

			// Delegate other messages to the original WindowProc.
			return ExtUser32.INSTANCE.DefWindowProc(hWnd, uMsg, wParam, lParam);
		}

	};

	/**
	 * Creates a <code>Win32PowerCourtesyMonitor</code>.
	 */
	public Win32PowerCourtesyMonitor() {
		if (!Platform.isWindows()) {
			throw new UnexpectedException("This class requires Windows");
		}
		monitor = new PowerBroadcastMonitor();
		update();
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#isRequireAC()
	 */
	public synchronized final boolean isRequireAC() {
		return requireAC;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#setRequireAC(boolean)
	 */
	public synchronized final void setRequireAC(boolean requireAC) {
		this.requireAC = requireAC;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#getMinBatteryLifePercent()
	 */
	public synchronized final int getMinBatteryLifePercent() {
		return minBatteryLifePercent;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#setMinBatteryLifePercent(byte)
	 */
	public synchronized final void setMinBatteryLifePercent(
			int minBatteryLifePercent) {
		this.minBatteryLifePercent = minBatteryLifePercent;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#getMinBatteryLifePercentWhileCharging()
	 */
	public synchronized final int getMinBatteryLifePercentWhileCharging() {
		return minBatteryLifePercentWhileCharging;
	}

	/* (non-Javadoc)
	 * @see ca.eandb.jdcp.worker.policy.win32.PowerCourtesyMonitor#setMinBatteryLifePercentWhileCharging(byte)
	 */
	public synchronized final void setMinBatteryLifePercentWhileCharging(
			int minBatteryLifePercentWhileCharging) {
		this.minBatteryLifePercentWhileCharging = minBatteryLifePercentWhileCharging;
	}

	/**
	 * Updates the state of this <code>CourtesyMonitor</code>.
	 */
	public synchronized void update() {
		SYSTEM_POWER_STATUS status = new SYSTEM_POWER_STATUS();

		LOGGER.info("Get APM power status"); 
		
		ExtKernel32.INSTANCE.GetSystemPowerStatus(status);
		
		LOGGER.info("APM power has changed, status: {}", status);

		switch (status.ACLineStatus) {
		case 0: // battery
			allow(!requireAC
					&& (status.BatteryLifePercent < 0 || status.BatteryLifePercent >= minBatteryLifePercent));
			break;

		case 1:  // A/C
		default:
			allow((status.BatteryFlag & 0x8) == 0 // not charging
			|| (status.BatteryLifePercent < 0 || status.BatteryLifePercent >= minBatteryLifePercentWhileCharging));
			break;

		}
	}

	/**
	 * Updates the suspend state of this <code>CourtesyMonitor</code>.
	 */
	public synchronized void updateSuspend(int suspendValue) {
		LOGGER.info("The suspended state was delivered: {}", suspendValue);
	}

}
