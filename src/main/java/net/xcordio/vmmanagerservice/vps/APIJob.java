package net.xcordio.vmmanagerservice.vps;

public interface APIJob<T> {
	
	public String getDescription();
	public boolean isComplete();
	public T waitForResult() throws IllegalStateException;
}
