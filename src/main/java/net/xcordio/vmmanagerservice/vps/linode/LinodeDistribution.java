package net.xcordio.vmmanagerservice.vps.linode;

public class LinodeDistribution extends DistributionInfo {
	
	public int id;
	public String createDT;
	public int requiresPVOPSkernel;
	public int minImageSize; // MB
	public int is64Bit;
	
	@Override
	public String toString() {
		return "LinodeDistribution[id=" + id + ",type=" + type + ",version=" + version + ",createDT=" + createDT
				+ ",requiresPVOPSkernel=" + requiresPVOPSkernel + ",minImageSize=" + minImageSize + ",is64Bit=" + is64Bit + "]";
	}
}
