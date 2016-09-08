package net.xcordio.vmmanagerservice.vps.linode;

import net.semplar.log.Logger;
import net.xcordio.vmmanagerservice.vps.APIJob;

import java.io.IOException;

/**
 * Represents remote Job.
 */
public class LinodeJob<T> implements APIJob<T> {
	
	private static final Logger log = Logger.getc();
	protected static int WAIT_PERIOD = 5000; // ms
	
	protected LinodeProvider provider;
	protected int linodeId;
	protected int jobId;
	protected String description;
	private Boolean done;
	private T result;
	
	public LinodeJob(LinodeProvider provider, int linodeId, int jobId, String description) {
		if (provider == null) throw new NullPointerException();
		this.provider = provider;
		this.linodeId = linodeId;
		this.description = description;
		this.jobId = jobId;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public boolean isComplete() {
		return done == Boolean.TRUE;
	}
	
	/**
	 * Override this to set up some result.
	 * @return result
	 */
	protected T jobDone() {
		return result;
	}
	
	@Override
	public T waitForResult() { // TODO: add timeout
		if (done == Boolean.TRUE) return result;
		for (;;) try {
			if (provider.listJobs(linodeId, jobId, true) == 0) break;
			if (WAIT_PERIOD > 0) Thread.sleep(WAIT_PERIOD);
		} catch (IOException ioe) {
			log.error("exception during linode job waiting", ioe);
		} catch (InterruptedException e) {
			throw new RuntimeException("waiting for job was interrupted");
		}
		done = true;
		return result = jobDone();
	}
}
