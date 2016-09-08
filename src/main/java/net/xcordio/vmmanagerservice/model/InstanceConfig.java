package net.xcordio.vmmanagerservice.model;

import net.xcordio.vmmanagerservice.util.ReflectiveConfig;

public class InstanceConfig {
	
	@ReflectiveConfig.KeepInstance
	public ServerInfo server;
	
	@ReflectiveConfig.KeepInstance
	public InstallationInfo installation;
	
	@ReflectiveConfig.KeepInstance
	public ParametersInfo parameters;
	
}
