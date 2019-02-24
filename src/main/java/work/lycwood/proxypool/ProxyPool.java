package work.lycwood.proxypool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;




public class ProxyPool {
	
	private  Comparator comparator = null ;
	{
		comparator = new Comparator<String>() {
	        @Override
	        public int compare(String t1, String t2) {
	        	int o1 = Integer.parseInt(t1.split(":")[2]);
	        	int o2 = Integer.parseInt(t2.split(":")[2]);
	          return o1-o2;
	       }
	     };
	}
	private TreeSet<String> httpPool = new TreeSet<>(comparator);
	private TreeSet<String> httpsPool = new TreeSet<>(comparator);
	
	public ProxyPool() {}
	
	public ProxyPool(TreeSet<String> httpPool,TreeSet<String> httpsPool) {
		this();
		this.httpPool = httpPool;
		this.httpsPool = httpsPool;
	}
	
	public TreeSet<String> getHttpPool() {
		return httpPool;
	}

	public void setHttpPool(TreeSet<String> httpPool) {
		this.httpPool = httpPool;
	}

	public TreeSet<String> getHttpsPool() {
		return httpsPool;
	}

	public void setHttpsPool(TreeSet<String> httpsPool) {
		this.httpsPool = httpsPool;
	}

	public synchronized String httpPull() {
		String hostandport = null;
		if(httpPool.size()>0) {
			hostandport = httpPool.pollFirst();
		}
		return hostandport;
	}

	public synchronized void httpPush(String hostandport) {
		boolean flag = httpPool.add(hostandport);
		if(flag)
			toLocalImgUrl(hostandport);
	}

	public synchronized String httpsPull() {
		String hostandport = null;
		if(httpsPool.size()>0) 
			hostandport = httpsPool.pollFirst();
		return hostandport;
	}
	
	public synchronized void httpsPush(String hostandport) {
		boolean flag = httpsPool.add(hostandport);
		if(flag)
			toLocalImgUrl(hostandport);
	}
	
	public static void toLocalImgUrl(String hostandport) {

		try {
			File file = new File("L:/images/ippools.txt");
			FileWriter fw = new FileWriter(file, true);
			fw.write(hostandport);
			fw.write("\n");
			fw.close();
		} catch (IOException e) {
			 e.printStackTrace();
		}
	}
}
