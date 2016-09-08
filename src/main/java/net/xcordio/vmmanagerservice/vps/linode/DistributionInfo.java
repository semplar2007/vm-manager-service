package net.xcordio.vmmanagerservice.vps.linode;

public class DistributionInfo {
	
	public enum OSType {
		
		UBUNTU,
		CENTOS,
		DEBIAN,
		GENTOO,
		OPENSUSE,
		SLACKWARE,
		FEDORA,
		FREEBSD,
		OPENBSD,
		UNIX,
		WINDOWS,
		MACOS,
		OTHER;
	}
	
	public OSType type;
	public String version;
	
	public static OSType guessType(String label) {
		for (OSType t : OSType.values()) {
			String tn = t.name();
			if (tn.regionMatches(true, 0, label, 0, tn.length()))
				return t;
		}
		return OSType.OTHER;
	}
}
