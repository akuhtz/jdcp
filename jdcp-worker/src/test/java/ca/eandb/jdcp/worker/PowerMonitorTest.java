package ca.eandb.jdcp.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import ca.eandb.jdcp.worker.policy.CourtesyMonitorFactory;
import ca.eandb.jdcp.worker.policy.PowerCourtesyMonitor;

public class PowerMonitorTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(PowerMonitorTest.class);
	
	@Test
	public void monitorTest() throws InterruptedException {
		LOGGER.info("Execute the test.");
		
		final PowerCourtesyMonitor powerMonitor = CourtesyMonitorFactory.INSTANCE.createPowerCourtesyMonitor();
		
		powerMonitor.setRequireAC(true);
		
		powerMonitor.allowTasksToRun();
		
		powerMonitor.waitFor();
		
		LOGGER.info("Test has finished.");
	}
	
}
