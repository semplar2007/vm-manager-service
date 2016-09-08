package net.xcordio.vmmanagerservice.vps.debian;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.semplar.log.Logger;
import net.xcordio.vmmanagerservice.util.CSVReader;
import net.xcordio.vmmanagerservice.util.RemoteFileAccess;
import net.xcordio.vmmanagerservice.vps.AbstractManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Created by semplar on 9/9/16.
 */
public class DebianManager implements AbstractManager {
	
	private static Logger log = Logger.getc();
	
	protected String hostname;
	protected int port = 22;
	protected String username;
	protected String password;
	//
	private SSHClient sshClient;
	
	protected SSHClient getSSHClient() throws IOException {
		if (sshClient == null) {
			sshClient = new SSHClient();
			sshClient.loadKnownHosts();
			sshClient.connect(hostname, port);
			sshClient.authPassword(username, password);
		}
		return sshClient;
	}
	
	public final static String is_package_installed = "dpkg-query -f '${Status} @@ ${binary:Package}\n' -W | grep -v '^deinstall ok config-files @@ ' | grep '^.* @@ %s$' > /dev/null";
	public final static String install_package = "DEBIAN_FRONTEND=noninteractive apt-get install -y %s";
	public final static String uninstall_package = "DEBIAN_FRONTEND=noninteractive apt-get remove -y %s";
	
	@Override
	public boolean installPackage(String packageName, String version) throws IOException {
		Session.Command c = getSSHClient().startSession().exec(install_package.replaceFirst("%s", packageName));
		try {
			InputStream is = c.getInputStream();
			for (byte[] buf = new byte[4096]; is.read(buf) > 0; ) { /* empty */ }
			is.close();
		} finally {
			c.close();
		}
		return c.getExitStatus() == 0;
	}
	
	@Override
	public boolean uninstallPackage(String packageName, String version) throws IOException {
		Session.Command c = getSSHClient().startSession().exec(uninstall_package.replaceFirst("%s", packageName));
		try {
			InputStream is = c.getInputStream();
			for (byte[] buf = new byte[4096]; is.read(buf) > 0; ) { /* empty */ }
			is.close();
		} finally {
			c.close();
		}
		return c.getExitStatus() == 0;
	}
	
	@Override
	public boolean isPackageInstalled(String packageName) throws IOException {
		Session.Command c = getSSHClient().startSession().exec(is_package_installed.replaceFirst("%s", packageName));
		try {
			InputStream is = c.getInputStream();
			for (byte[] buf = new byte[4096]; is.read(buf) > 0; ) { /* empty */ }
			is.close();
		} finally {
			c.close();
		}
		return c.getExitStatus() == 0;
	}
}
