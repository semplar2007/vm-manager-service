package net.xcordio.vmmanagerservice.report;

import java.util.ArrayList;
import java.util.List;

public interface StatusReporter {
	
	public class Multi implements StatusReporter {
		
		public List<StatusReporter> multi;
		
		public Multi() {
			this (new ArrayList<StatusReporter>());
		}
		
		public Multi(List<StatusReporter> m) {
			multi = new ArrayList<StatusReporter>(m);
		}
		
		@Override
		public void reportConfigIsBad(String configFilepath, Throwable e) {
			for (StatusReporter r : multi)
				r.reportConfigIsBad(configFilepath, e);
		}
		
		@Override
		public void reportConfigReload(String configFilepath) {
			for (StatusReporter r : multi)
				r.reportConfigReload(configFilepath);
		}
	}
	
	public void reportConfigIsBad(String configFilepath, Throwable e);
	
	public void reportConfigReload(String configFilepath);
	
	// report package installed
	// report instance allocated
	// report instance removed
}
