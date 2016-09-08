package net.xcordio.vmmanagerservice.vps.linode;

import java.math.BigDecimal;

public class LinodePlanInfo {
	
	public int planId;
	public BigDecimal price;
	public BigDecimal hourly;
	public int ram;
	public int disk;
	public int cores;
	
	@Override
	public String toString() {
		return "LinodePlan[id=" + planId + ",price=" + price + ",hourly=" + hourly + ",ram=" + ram + ",disk=" + disk + ",cores=" + cores + "]";
	}
}
