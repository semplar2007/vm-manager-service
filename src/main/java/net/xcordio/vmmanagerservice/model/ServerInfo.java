package net.xcordio.vmmanagerservice.model;

import net.xcordio.vmmanagerservice.ServiceMain;
import net.xcordio.vmmanagerservice.vps.VPSProvider;
import net.xcordio.vmmanagerservice.vps.linode.LinodeProvider;

public class ServerInfo {
	
	public LoginInfo login;
	public String distribution;
	public int ram; // in MB
	public VPSProvider cloud;
	
	public VPSProvider allocVPSProvider(String type) {
		return ServiceMain.instance.providersMap.get(type);
	}
}
