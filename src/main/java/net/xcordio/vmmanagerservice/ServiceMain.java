package net.xcordio.vmmanagerservice;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.semplar.log.Logger;
import net.xcordio.vmmanagerservice.model.InstanceConfig;
import net.xcordio.vmmanagerservice.report.RemoteHttpReporter;
import net.xcordio.vmmanagerservice.report.StatusReporter;
import net.xcordio.vmmanagerservice.util.ReflectiveConfig;
import net.xcordio.vmmanagerservice.vps.VPSProvider;
import net.xcordio.vmmanagerservice.vps.linode.LinodeProvider;

/**
 * Root class that aggregates project components, following ones:
 * - remote listeners ({@link RemoteConfigHandler}) which listen to vm configurations;
 * - status reporters ({@link StatusReporter}) which sends activity records to specific destinations,
 *   like "instance created", "instance destroyed", "instance reconfigured";
 * - vps providers ({@link VPSProvider});
 *
 * Whole class is configured with use of {@link ReflectiveConfig} from a single JSON file.
 */
@SuppressWarnings("unused")
public class ServiceMain {
	
	private static Logger log = Logger.getc();
	
	/** Temporary singleton instance. */
	public static ServiceMain instance;
	
	public static void main(String[] args) throws Exception {
		log.info("VM manager service starting...");
		try {
			new ServiceMain().reconfigure();
		} catch (IOException ioe) {
			throw new RuntimeException("service initialization failed", ioe);
		}
	}
	
	public ServiceMain() {
		/* Setting up temporary singleton reference (static field) */
		if (instance != null) throw new IllegalStateException("singleton is already allocated");
		instance = this;
	}
	
	/** Re-reads configuration and applies it to object tree.
	 * @throws IOException when any I/O exception occurs */
	public void reconfigure() throws IOException {
		log.info("reconfiguring service...");
		ReflectiveConfig.getInstance().applyConfig(this, ServiceConfig.loadConfig());
	}
	
	@ReflectiveConfig.KeepInstance
	public List<RemoteConfigHandler> vmconfigs;
	// called reflectively by {@link ReflectiveConfig} before {@link #listeners} is changed
	public void onListenersChange(List<RemoteConfigHandler> l) throws IOException {
		// removing previous
		for (RemoteConfigHandler ll : vmconfigs) ll.setMain(null);
		for (RemoteConfigHandler ll : l) ll.setMain(this);
	}
	
	/** This is a single service-wide reporter. */
	public transient final StatusReporter.Multi reporter = new StatusReporter.Multi();
	/** Virtual attribute. It's called reflectively by {@link ReflectiveConfig}. */
	@ReflectiveConfig.KeepInstance
	public void onStatusReportsChange(List<StatusReporter> l) throws IOException {
		reporter.multi = l;
	}
	/** Binds provider fullType to provider classes. Is called reflectively. */
	public StatusReporter allocStatusReporter(String type) {
		if ("httppost".equals(type)) return new RemoteHttpReporter();
		throw new IllegalArgumentException("unsupported status reporter fullType: " + type);
	}
	
	public transient Map<String, InstanceConfig> instanceMap = new HashMap<String, InstanceConfig>();
	
	@ReflectiveConfig.KeepInstance
	public List<VPSProvider> vmproviders;
	/** Binds provider fullType to provider classes. Is called reflectively. */
	public VPSProvider allocVPSProvider(String type) {
		if ("linode".equals(type)) {
			return new LinodeProvider();
		}
		throw new IllegalArgumentException("unsupported vps provider: " + type);
	}
	
	public transient Map<String, VPSProvider> providersMap = new HashMap<String, VPSProvider>();
}
