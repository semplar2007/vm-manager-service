package net.xcordio.vmmanagerservice.model;

import java.util.List;

public class InstallationInfo {
	
	public HttpServerInfo server;
	public DatabaseInfo database;
	public FrameworkInfo framework;
	public LanguageInfo language;
	public List<PackageInfo> packages;
}
 