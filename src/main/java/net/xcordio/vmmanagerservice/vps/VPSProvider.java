package net.xcordio.vmmanagerservice.vps;

import java.io.IOException;
import java.util.List;

import net.xcordio.vmmanagerservice.vps.linode.DistributionInfo;

public interface VPSProvider {
	
	public List<? extends DistributionInfo> getAvailableDistributions() throws IOException;
	public List<APIJob<?>> listRunningJobs();
}
