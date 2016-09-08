package net.xcordio.vmmanagerservice.vps.linode;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import net.semplar.log.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import net.xcordio.vmmanagerservice.ServiceConfig;
import net.xcordio.vmmanagerservice.vps.APIJob;
import net.xcordio.vmmanagerservice.vps.VPSProvider;
import net.xcordio.vmmanagerservice.vps.linode.DistributionInfo.OSType;

/**
 * A client for Linode vps provider, and a {@link VPSProvider} implementation at the same time.
 * @author xcordio
 */
public class LinodeProvider implements VPSProvider {
	
	private static Logger log = Logger.getc();
	
	public static void main(String[] args) throws IOException {
		LinodeProvider p = new LinodeProvider();
		p.apiKey = "fQXt9hhePJhp759JdCPxVfEpg7o99IBDUNreCISGNd8GSFdzJfSnGWCwdmiCEF55";
//		for (KernelInfo ki : p.getAvailableKernels())
//			System.out.println(ki);
		int linodeId = p.doCreateAndBootSample();
		p.doDeleteSample(linodeId);
	}
	
	protected int doCreateAndBootSample() throws IOException {
		log.info("Creating new instance");
		int linodeId = createInstance(1 /* cheapest plan */, 2 /* dallas */);
		try {
			log.info("Creating root disk");
			createDiskFromDistribution(linodeId, "ROOT", 2048, 140, "ebe02426").waitForResult();
			log.info("Creating swap");
			createDisk(linodeId, "SWAP", DiskType.SWAP, 256).waitForResult();
			log.info("Getting IP address");
			String ipAddress = null;
			for (IPAddressInfo ip : listIPs(linodeId))
				if (ip.isPublic) {
					ipAddress = ip.address;
					break;
				}
			if (ipAddress == null) throw new IOException("assertion failed: no IP address allocated?");
			log.info(".. IP address is " + ipAddress);
			log.info("Creating Linode config");
			LinodeConfig lc = new LinodeConfig();
			lc.linodeId = linodeId;
			lc.label = "default";
			List<Integer> diskIDs = listDiskIDs(linodeId);
			lc.diskList = diskIDs.get(0) + "," + diskIDs.get(1);
			lc.helperDistro = true;
			lc.devtmpfsAutomount = true;
			lc.helperNetwork = true;
			lc.rootDeviceRO = true;
			lc.ramLimit = 0;
			lc.kernelId = 237;
			lc.comments = "A default configuration containing root & swap disks, includes helper and no ram limitations";
			lc = createConfig(lc);
			log.info("Booting up instance");
			bootup(linodeId, lc.id).waitForResult();
			return linodeId;
		} catch (Exception e) {
			log.error("Exception caught; deleting linode");
			doDeleteSample(linodeId);
			return -1;
		}
	}
	
	protected void doDeleteSample(int linodeId) throws IOException {
		// deleting linode
		shutdown(linodeId).waitForResult();
		for (Integer diskId : listDiskIDs(linodeId))
			deleteDisk(linodeId, diskId).waitForResult();
		deleteLinode(linodeId);
	}
	
	public String apiUrl = "https://api.linode.com/";
	public String apiKey;
	
	public LinodeProvider() {
	}
	
	@SuppressWarnings("unused")
	public JSONObject doEcho(JSONObject obj) throws IOException {
		return (JSONObject) doGET("test.echo", obj);
	}
	
	public void deleteLinode(int linodeId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		doGET("linode.delete", params);
	}
	
	@Override
	public List<LinodeDistribution> getAvailableDistributions() throws IOException {
		JSONArray array = (JSONArray) doGET("avail.distributions", null);
		/* Api returns list of records like:
		 * {
			  "CREATE_DT":"2016-06-22 15:03:38.0",
			  "REQUIRESPVOPSKERNEL":1,
			  "LABEL":"Fedora 24",
			  "DISTRIBUTIONID":149,
			  "MINIMAGESIZE":1024,
			  "IS64BIT":1
			} */
		List<LinodeDistribution> osTypes = new ArrayList<>();
		for (int i = 0; i < array.length(); i ++) {
			JSONObject jo = array.getJSONObject(i);
			LinodeDistribution oi = new LinodeDistribution();
			String label = jo.getString("LABEL");
			OSType type = LinodeDistribution.guessType(label);
			if (type != OSType.OTHER)
				label = label.substring(type.name().length()).trim();
			oi.id = jo.getInt("DISTRIBUTIONID");
			oi.type = type;
			oi.version = label;
			oi.createDT = jo.getString("CREATE_DT");
			oi.requiresPVOPSkernel = jo.getInt("REQUIRESPVOPSKERNEL");
			oi.minImageSize = jo.getInt("MINIMAGESIZE");
			oi.is64Bit = jo.getInt("IS64BIT");
			osTypes.add(oi);
		}
		return osTypes;
	}
	
	@SuppressWarnings("unused")
	public List<KernelInfo> getAvailableKernels() throws IOException {
		JSONArray array = (JSONArray) doGET("avail.kernels", null);
		List<KernelInfo> kinfos = new ArrayList<KernelInfo>();
		for (int i = 0; i < array.length(); i ++) {
			JSONObject jo = array.getJSONObject(i);
			KernelInfo ki = new KernelInfo();
			ki.id = jo.getInt("KERNELID");
			ki.label = jo.getString("LABEL");
			ki.isKvm = jo.getInt("ISKVM");
			ki.isXen = jo.getInt("ISXEN");
			ki.isPvops = jo.getInt("ISPVOPS");
			kinfos.add(ki);
		}
		return kinfos;
	}
	
	@SuppressWarnings("unused")
	public List<LinodePlanInfo> getAvailablePlans() throws IOException {
		JSONArray array = (JSONArray) doGET("avail.linodeplans", null);
		List<LinodePlanInfo> pinfos = new ArrayList<LinodePlanInfo>();
		for (int i = 0; i < array.length(); i ++) {
			JSONObject jo = array.getJSONObject(i);
			System.out.println(jo);
			LinodePlanInfo pi = new LinodePlanInfo();
			pi.planId = jo.getInt("PLANID");
			pi.price = jo.getBigDecimal("PRICE");
			pi.hourly = jo.getBigDecimal("HOURLY");
			pi.ram = jo.getInt("RAM");
			pi.disk = jo.getInt("DISK");
			pi.cores = jo.getInt("CORES");
			pinfos.add(pi);
		}
		return pinfos;
	}
	
	@SuppressWarnings("unused")
	public List<DatacenterInfo> getAvailableDatacenters() throws IOException {
		JSONArray array = (JSONArray) doGET("avail.datacenters", null);
		List<DatacenterInfo> dinfos = new ArrayList<DatacenterInfo>();
		for (int i = 0; i < array.length(); i ++) {
			JSONObject jo = array.getJSONObject(i);
			DatacenterInfo di = new DatacenterInfo();
			di.location = jo.getString("LOCATION");
			di.abbr = jo.getString("ABBR");
			di.id = jo.getInt("DATACENTERID");
			dinfos.add(di);
		}
		return dinfos;
	}
	
	protected int createInstance(int planId, int datacenterId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("PlanID", planId);
		params.put("DatacenterID", datacenterId);
		// TODO: add payment term
		JSONObject result = (JSONObject) doGET("linode.create", params);
		return result.getInt("LinodeID");
	}
	
	protected APIJob<Void> createDiskFromDistribution(int linodeId, String label, int sizeMb,
												int distributionID, String rootPass) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		params.put("Label", label);
		params.put("size", sizeMb);
		params.put("DistributionID", distributionID);
		params.put("rootPass", rootPass); // TODO: enhance security
		JSONObject result = (JSONObject) doGET("linode.disk.createfromdistribution", params);
		return new LinodeJob(this, linodeId, result.getInt("JobID"), "Allocating linode disk from distribution");
	}
	
	protected APIJob<Void> createDisk(int linodeId, String label, DiskType type, int sizeMb) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		params.put("Label", label);
		params.put("Type", type.name().toLowerCase());
		params.put("size", sizeMb);
		JSONObject result = (JSONObject) doGET("linode.disk.create", params);
		return new LinodeJob(this, linodeId, result.getInt("JobID"), "Allocating linode disk from distribution");
	}
	
	protected List<Integer> listDiskIDs(int linodeId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		JSONArray result = (JSONArray) doGET("linode.disk.list", params);
		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < result.length(); i ++)
			list.add(result.getJSONObject(i).getInt("DISKID"));
		return list;
	}
	
	protected APIJob<Void> deleteDisk(int linodeId, int diskId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		params.put("DiskID", diskId);
		JSONObject result = (JSONObject) doGET("linode.disk.delete", params);
		return new LinodeJob<>(this, linodeId, result.getInt("JobID"), "Deleting linode disk");
	}
	
	protected List<IPAddressInfo> listIPs(int linodeId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		JSONArray result = (JSONArray) doGET("linode.ip.list", params);
		List<IPAddressInfo> ips = new ArrayList<>();
		for (int i = 0; i < result.length(); i ++) {
			JSONObject jo = result.getJSONObject(i);
			IPAddressInfo ip = new IPAddressInfo();
			ip.isPublic = jo.getInt("ISPUBLIC") != 0;
			ip.address = jo.getString("IPADDRESS");
			ips.add(ip);
		}
		return ips;
	}
	
	protected LinodeConfig createConfig(LinodeConfig config) throws IOException {
		if (config.id >= 0) throw new IllegalArgumentException("config already has ID: config is already created");
		JSONObject params = new JSONObject();
		params.put("LinodeID", config.linodeId);
		params.put("Label", config.label);
		params.put("DiskList", config.diskList);
		params.put("helper_distro", config.helperDistro);
		params.put("devtmpfs_automount", config.devtmpfsAutomount);
		params.put("helper_network", config.helperNetwork);
		params.put("RootDeviceRO", config.rootDeviceRO);
		params.put("RAMLimit", config.ramLimit);
		params.put("KernelID", config.kernelId);
		params.put("Comments", config.comments);
		JSONObject result = (JSONObject) doGET("linode.config.create", params);
		config.id = result.getInt("ConfigID");
		return config;
	}
	
	protected APIJob<Void> bootup(int linodeId, int configId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		params.put("ConfigID", configId);
		JSONObject result = (JSONObject) doGET("linode.boot", params);
		return new LinodeJob<Void>(this, linodeId, result.getInt("JobID"), "Booting up linode instance");
	}
	
	protected APIJob<Void> shutdown(int linodeId) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		JSONObject result = (JSONObject) doGET("linode.shutdown", params);
		return new LinodeJob<Void>(this, linodeId, result.getInt("JobID"), "Booting up linode instance");
	}
	
	protected int listJobs(int linodeId, int jobId, boolean pendingOnly) throws IOException {
		JSONObject params = new JSONObject();
		params.put("LinodeID", linodeId);
		params.put("JobID", jobId);
		params.put("pendingOnly", pendingOnly ? 1 : 0);
		JSONArray result = (JSONArray) doGET("linode.job.list", params);
		return result.length();
	}
	
	protected Object doGET(String action, JSONObject params) throws IOException {
		StringBuilder bd = new StringBuilder(apiUrl);
		bd.append("?");
		String apiKey = this.apiKey;
		if (apiKey != null) bd.append("api_key=").append(URLEncoder.encode(apiKey, "utf-8")).append("&");
		bd.append("api_action=").append(URLEncoder.encode(action, "utf-8"));
		if (params != null)
			for (String key : params.keySet()) {
				String value = params.get(key).toString();
				bd.append('&').append(URLEncoder.encode(key, "utf-8")).append('=').append(URLEncoder.encode(value, "utf-8"));
			}
		URL url = new URL(bd.toString());
		HttpURLConnection conn = null;
		InputStream is = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			log.trace(">> " + action + (params != null ? ": " + params.toString() : ""));
			conn.setRequestMethod("GET");
			conn.setRequestProperty("User-Agent", "VM Manager Service/0.1");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Accept-Charset", "utf-8");
			is = conn.getInputStream();
			JSONObject responseJson = new JSONObject(ServiceConfig.readUtfStringFromStream(is));
			log.trace("<< " + action + ": " + responseJson.toString());
			if (!responseJson.get("ACTION").equals(action)) throw new IOException("action mismatch");
			JSONArray errors = responseJson.getJSONArray("ERRORARRAY");
			if (errors.length() > 0)
				throw new LinodeAPIError(errors.getJSONObject(0)).withAction(action);
			return responseJson.get("DATA");
		} finally {
			if (is != null) try {
				is.close();
			} catch (IOException ioe) { /* nop */ }
			if (conn != null)
				conn.disconnect();
		}
	}
	
	@Override
	public List<APIJob<?>> listRunningJobs() {
		// TODO Auto-generated method stub
		return null;
	}
}
