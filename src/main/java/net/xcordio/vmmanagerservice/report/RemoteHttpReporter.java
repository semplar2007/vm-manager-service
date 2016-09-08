package net.xcordio.vmmanagerservice.report;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import net.semplar.log.Logger;

public class RemoteHttpReporter implements StatusReporter {
	
	private static Logger log = Logger.getc();
	
	public String url;
	
	public RemoteHttpReporter() {
	}
	
	@Override
	public void reportConfigIsBad(String configFilepath, Throwable e) {
	}
	
	@Override
	public void reportConfigReload(String configFilepath) {
	}
	
	protected void postHttp(Map<String, String> params) {
//		String rawData = "id=10";
//		String fullType = "application/x-www-form-urlencoded";
//		String encodedData = URLEncoder.encode( rawData ); 
//		URL u = new URL("http://www.example.com/page.php");
//		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
//		conn.setDoOutput(true);
//		conn.setRequestMethod("POST");
//		conn.setRequestProperty( "Content-Type", fullType );
//		conn.setRequestProperty( "Content-Length", String.valueOf(encodedData.length()));
//		OutputStream os = conn.getOutputStream();
//		os.write(encodedData.getBytes());
	}
	
	protected HttpURLConnection acquireConn() {
		URL url;
		try {
			url = new URL(this.url);
		} catch (MalformedURLException me) {
			log.error("failed to parse url: " + this.url, me);
			return null;
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			return conn;
		} catch (IOException ioe) {
			log.error("failed to report data", ioe);
			return null;
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}
}
