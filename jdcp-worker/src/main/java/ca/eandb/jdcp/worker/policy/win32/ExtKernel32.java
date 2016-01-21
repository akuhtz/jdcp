package ca.eandb.jdcp.worker.policy.win32;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.win32.W32APIOptions;

public interface ExtKernel32 extends Kernel32 {

    /** The instance. */
    ExtKernel32 INSTANCE = (ExtKernel32) Native.loadLibrary("kernel32",
            ExtKernel32.class, W32APIOptions.UNICODE_OPTIONS);
	
	class SYSTEM_POWER_STATUS extends Structure {
		public byte ACLineStatus;
		public byte BatteryFlag;
		public byte BatteryLifePercent;
		public byte Reserved1;
		public int BatteryLifeTime;
		public int BatteryFullLifeTime;
		
		@Override
		protected List getFieldOrder() {
			return Arrays.asList(new String[] { "ACLineStatus", "BatteryFlag", "BatteryLifePercent", "Reserved1", "BatteryLifeTime", "BatteryFullLifeTime" });
		}
	};

	void GetSystemPowerStatus(SYSTEM_POWER_STATUS status);

	int GetTickCount();
}
