package net.xcordio.vmmanagerservice.vps;

import java.io.IOException;

/**
 * Created by semplar on 9/9/16.
 */
public interface AbstractManager {
	
	public boolean installPackage(String packageName, String version) throws IOException;
	
	public boolean uninstallPackage(String packageName, String version) throws IOException;
	
	public boolean isPackageInstalled(String packageName) throws IOException;
}
