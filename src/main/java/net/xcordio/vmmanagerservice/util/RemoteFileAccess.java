package net.xcordio.vmmanagerservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.semplar.log.Logger;

public class RemoteFileAccess implements FileAccess {
	
	private static final Logger log = Logger.getc();
	
	public static void main(String[] args) throws IOException {
		RemoteFileAccess l = new RemoteFileAccess();
		l.hostname = "semplar.net";
		l.username = "testuser";
		l.password = "testpass_xMVdPP@~";
		l.listenTo("/tmp/a b", new Handler() {
			@Override
			public void handleEvents(FileAccess fa, String dir, List<String> actions, String file) {
				System.out.println(actions + ": " + dir + file);
			}
		});
	}
	
	protected static class HandleRecord {
		
		public final String directory;
		public final Handler handler;
		public final Thread thread;
		
		public HandleRecord(String dir, Handler h, Thread t) {
			this.directory = dir;
			this.handler = h;
			this.thread = t;
		}
	}
	
	public String hostname;
	public int port = 22;
	public String username;
	public String password;
	//
	protected SSHClient sshClient;
	// next fields are synchronized on {@code this}
	protected final Map<String, List<HandleRecord>> handlerDirs = new HashMap<String, List<HandleRecord>>();
	
	public RemoteFileAccess() {
	}
	
	public RemoteFileAccess(String hostname, String username, String password) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
	}
	
	protected SSHClient getSSHClient() throws IOException {
		if (sshClient == null) {
			sshClient = new SSHClient();
			sshClient.loadKnownHosts();
			sshClient.connect(hostname, port);
			sshClient.authPassword(username, password);
		}
		return sshClient;
	}
	
	protected static final AtomicInteger listenerCounter = new AtomicInteger(1);
	public synchronized void listenTo(final String directory, final Handler handler) {
		if (handler == null) throw new NullPointerException();
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				log.verbose("started listening to " + directory);
				try {
					Command c = getSSHClient().startSession().exec("inotifywait -m -c " + quotedFilename(directory));
					InputStreamReader isr = new InputStreamReader(c.getInputStream(), "utf-8");
					CSVReader csvr = new CSVReader(isr);
					for (List<String> csv; ; ) {
						if (Thread.currentThread().isInterrupted()) {
							log.error("thread was interrupted, exiting");
							break;
						}
						if ((csv = csvr.nextCsv()) == null) {
							log.error("inotifywait was terminated, exiting");
							break;
						}
						if (csv.size() != 3) throw new IOException("corrupt inotify format: expected 3 values: dir, events and filename, but got size " + csv.size() + ": " + csv);
						handler.handleEvents(RemoteFileAccess.this, csv.get(0), Arrays.asList(csv.get(1).split(",")), csv.get(2));
					}
					isr.close();
					c.close();
				} catch (IOException ioe) {
					throw new RuntimeException();
				}
				log.verbose("stopped listening to " + directory);
			}
		});
		thread.setDaemon(false);
		thread.setName("Remote Listener #" + listenerCounter.getAndIncrement());
		HandleRecord rec = new HandleRecord(directory, handler, thread);
		List<HandleRecord> rl = handlerDirs.get(directory);
		if (rl == null) handlerDirs.put(directory, rl = new ArrayList<HandleRecord>());
		rl.add(rec);
		thread.start();
	}
	
	@SuppressWarnings("unused")
	public void stopListen() throws IOException {
		getSSHClient().close();
	}
	
	@Override
	public synchronized void cancelListen(String dir, Handler handler) {
		List<HandleRecord> rl = handlerDirs.get(dir);
		if (rl != null)
			for (Iterator<HandleRecord> hi = rl.iterator(); hi.hasNext(); ) {
				HandleRecord hr = hi.next();
				if (hr.handler == handler) {
					hr.thread.interrupt();
					hi.remove();
				}
			}
	}
	
	@Override
	public synchronized void cancelListen(String dir) {
		List<HandleRecord> rl = handlerDirs.get(dir);
		if (rl == null || rl.isEmpty()) return;
		for (Iterator<HandleRecord> hi = rl.iterator(); hi.hasNext(); ) {
			HandleRecord hr = hi.next();
			hr.thread.interrupt();
			hi.remove();
		}
	}
	
	@Override
	public InputStream readFile(String filename) throws IOException {
		Command c = getSSHClient().startSession().exec("cat " + quotedFilename(filename));
		return c.getInputStream();
	}
	
	@Override
	public OutputStream writeFile(String filename) throws IOException {
		Command c = getSSHClient().startSession().exec("cat > " + quotedFilename(filename));
		return c.getOutputStream();
	}
	
	@Override
	public boolean isFile(String filename) throws IOException {
		Command c = getSSHClient().startSession().exec("test -f " + quotedFilename(filename));
		return c.getExitStatus() == 0;
	}
	
	@Override
	public boolean isDir(String filename) throws IOException {
		Command c = getSSHClient().startSession().exec("test -d " + quotedFilename(filename));
		return c.getExitStatus() == 0;
	}
	
	/** @return filename ready to be added to shell command. */
	protected static String quotedFilename(String filename) {
		// adds leading and trailing single quotes (') and replaces all quotes inside filename to '\'' combination
		return "'" + filename.replace("\'", "'\\''") + "'";
	}
}
