package net.xcordio.vmmanagerservice.vps.linode;

import java.io.IOException;

import org.json.JSONObject;

public class LinodeAPIError extends IOException {
	
	private static final long serialVersionUID = 8764725799186958993L;
	
	protected int errorCode;
	protected String action;
	
	public LinodeAPIError() {
	}
	
	public LinodeAPIError(JSONObject obj) {
		this (obj, null);
	}
	
	public LinodeAPIError(Throwable t) {
		super (t);
	}
	
	public LinodeAPIError(JSONObject obj, Throwable t) {
		super(obj.getString("ERRORMESSAGE"), t);
		errorCode = obj.getInt("ERRORCODE");
	}
	
	public LinodeAPIError withAction(String action) {
		this.action = action;
		return this;
	}
	
	public String toString() {
		return super.toString() + (errorCode != 0 ? ", error code: " + errorCode : "")
				+ (action != null ? ", action: " + action : "");
	}
}
