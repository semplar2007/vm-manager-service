package net.xcordio.vmmanagerservice.model;

import net.xcordio.vmmanagerservice.ServiceMain;
import net.xcordio.vmmanagerservice.vps.VPSProvider;
import net.xcordio.vmmanagerservice.vps.linode.LinodeProvider;

public class ServerInfo {
	
	public int ram; // in megabytes
	public VPSProvider provider;
	
	public VPSProvider convertVPSProvider(String type) {
		return ServiceMain.instance.providersMap.get(type);
	}
}
