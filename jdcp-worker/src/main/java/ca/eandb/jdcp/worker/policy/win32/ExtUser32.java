package ca.eandb.jdcp.worker.policy.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;


public interface ExtUser32 extends User32 {


    /** The instance. */
    ExtUser32 INSTANCE = (ExtUser32) Native.loadLibrary("user32", ExtUser32.class,
                                                  W32APIOptions.DEFAULT_OPTIONS);
	
    Pointer SetWindowLong(HWND hwnd, int index, StdCallLibrary.StdCallCallback wndProc);
	Pointer SetWindowLongPtr(HWND hWnd, int nIndex, StdCallLibrary.StdCallCallback wndProc);
}
