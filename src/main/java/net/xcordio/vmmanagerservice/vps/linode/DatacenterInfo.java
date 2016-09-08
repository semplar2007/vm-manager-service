package net.xcordio.vmmanagerservice.vps.linode;

public class DatacenterInfo {
	
	public int id;
	public String location;
	public String abbr;
	
	public DatacenterInfo() {
	}
	
	@Override
	public String toString() {
		return "DatacenterInfo[id=" + id + ",location=" + location + ",abbr=" + abbr + "]";
	}
}
