package net.xcordio.vmmanagerservice.vps.linode;

public class KernelInfo {
	
	public int id;
	public String label;
	public int isKvm;
	public int isXen;
	public int isPvops;
	
	@Override
	public String toString() {
		return "LinodeKernel[id=" + id + ",label=" + label + ",isKvm=" + isKvm + ",isXen=" + isXen + ",isPvops=" + isPvops + "]";
	}
}
