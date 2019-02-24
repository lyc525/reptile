package work.lycwood.proxypool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.management.JMException;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.junit.experimental.theories.Theories;

import redis.clients.util.Pool;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

public class GetProxyIP implements PageProcessor{
	private static boolean count = true;
	private static ProxyPool pool = new ProxyPool();
	private Site site = Site.me()
							//.addHeader("Host", "www.89ip.cn")
							.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
							.addCookie("_ydclearance", "e9dd2f80eeed99747086e267-c016-4225-9fba-209d0cb0a874-1547813921")
							.addCookie("yd_cookie", "4f07389c-f145-471507ab9696e1402488f6ac080a47082014")
							.addCookie("Hm_lvt_8ccd0ef22095c2eebfe4cd6187dea829", "1547687241,1547796130")
							.addCookie("Hm_lpvt_8ccd0ef22095c2eebfe4cd6187dea829", "1547813834")
							.setRetryTimes(5).setSleepTime(100); 
		
	public static void main(String[] args) {
		
		//test("http://www.ip3366.net/?stype=1&page=3");
		
		try {
			System.out.println("开始爬取...");
			Spider spider = Spider.create(new GetProxyIP())
							 .addUrl("https://www.xicidaili.com/wn/")
							  .addUrl("https://www.xicidaili.com/wt/")
							  .setScheduler(new FileCacheQueueScheduler("L:/images/"))
							  //.addUrl("http://www.ip3366.net/?stype=1&page=3")
							  ; 
			
			//将爬虫添加到监控里
			SpiderMonitor.instance().register(spider);
			//启动爬虫,5个线程,非阻塞启动
			spider.thread(1).run();
			
			//用于查看池里的ip,需使用阻塞方式启动爬虫
			/*TreeSet<String> httpPool = pool.getHttpPool();
			for (String string : httpPool) {
				System.out.println(string);
			}
			TreeSet<String> httpsPool = pool.getHttpsPool();
			for (String string : httpsPool) {
				System.out.println(string);
			}*/
		} catch (JMException e) {
			e.printStackTrace();
		}
		
	}
	
	   
	@Override
	public void process(Page page) {
		String url = page.getUrl().toString();
		if(count) {
			count = false;
			getPage(page);
			
		}
		if("https://www.xicidaili.com/wn/".equals(url))
			xiciHttps(page);
		else if("https://www.xicidaili.com/wt/".equals(url))
			xiciHttp(page);
		else if(url.contains("http://www.ip3366.net/?stype=1&page="))
			ip3366(page);
		else if(url.contains("http://www.qydaili.com/free/?action=china&page="))
			qiYun(page);
		else if(url.contains("http://www.66ip.cn/areaindex_"))
			sixProxy(page);
		else if(url.contains("http://www.89ip.cn/index_"))
			eightProxy(page);
	}
	
	/**
	 * 86代理ip
	 * @param page
	 */
	private void eightProxy(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//div[@class=\"layui-form\"]//tbody/tr").nodes();
		for (Selectable tr : trs) {
			String hostname = tr.xpath("//td[1]/text()").toString().trim();
			String port = tr.xpath("//td[2]/text()").toString().trim();
			String hostandport = hostname + ":" + port;
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				pool.httpPush(hostandport+":"+flag);
				//System.out.println(hostandport);
			}
		}
	}

	/**
	 * 66代理
	 * @param page
	 */
	private void sixProxy(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//*[@id=\"footer\"]/div/table/tbody/tr").nodes();
		int num = trs.size();
		for(int i=2;i<num;i++) {
			String hostname = html.xpath("//*[@id=\"footer\"]/div/table/tbody/tr["+i+"]/td[1]/text()").toString().trim();
			String port = html.xpath("//*[@id=\"footer\"]/div/table/tbody/tr["+i+"]/td[2]/text()").toString().trim();
			String hostandport = hostname + ":" + port;
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				pool.httpPush(hostandport+":"+flag);
			}
		}
	}

	/**
	 * 旗云代理HTTP和HTTPS
	 * @param page
	 */
	private void qiYun(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//*[@id=\"content\"]/section/div[2]/table/tbody/tr").nodes();
		for (Selectable tr : trs) {
			String hostname = tr.xpath("//td[1]/text()").toString().trim();
			String port = tr.xpath("//td[2]/text()").toString().trim();
			String hostandport = hostname+":"+port;
			String type = tr.xpath("//td[4]/text()").toString();
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				if("HTTP".equals(type)) {
					pool.httpPush(hostandport+":"+flag);
					//System.out.println("旗云http:"+hostandport);
				}
				else {
					pool.httpsPush(hostandport+":"+flag);
					//System.out.println("旗云https:"+hostandport);
				}
			}
		}
	}

	/**
	 * ip3366HTTP和HTTPS代理
	 * @param page
	 */
	private void ip3366(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//*[@id=\"list\"]/table/tbody/tr").nodes();
		for (Selectable tr : trs) {
			String hostname = tr.xpath("//td[1]/text()").toString().trim();
			String port = tr.xpath("//td[2]/text()").toString().trim();
			String hostandport = hostname+":"+port;
			String type = tr.xpath("//td[4]/text()").toString();
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				if("HTTP".equals(type)) {
					pool.httpPush(hostandport+":"+flag);
					//System.out.println("3366http:"+hostandport);
				}
				else {
					pool.httpsPush(hostandport+":"+flag);
					//System.out.println("3366https:"+hostandport);
				}
			}
		}
	}

	/**
	 * 网站代理页
	 * @param page
	 */
	private void getPage(Page page) {
		String [] urls = {
						  "http://www.ip3366.net/?stype=1&page=",
						  "http://www.qydaili.com/free/?action=china&page=",
						  "http://www.66ip.cn/areaindex_",     // 32/1.html
						  "http://www.89ip.cn/index_"		 // 11.html
							};   
		for(int j=0;j<urls.length;j++) {
			String uri = urls[j];
			if("http://www.66ip.cn/areaindex_".equals(uri))
				for(int k=1;k<20;k++) {
					String url = uri + k + "/1.html";
					Request request = new Request(url).setPriority(0);
			        page.addTargetRequest(request);
				}
			else if("http://www.89ip.cn/index_".equals(uri)) 
				for(int k=1;k<31;k++) {
					String url = uri + k + ".html";
					Request request = new Request(url).setPriority(0);
			        page.addTargetRequest(request);
				}
			else
				for(int i=1;i<11;i++) {
					String url = uri+i;
					Request request = new Request(url).setPriority(1);
			        page.addTargetRequest(request);
			        //System.out.println(url);
				}
		}
	}

	/**
	 * 西刺网站http代理
	 * @param page
	 */
	private void xiciHttp(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//table[@id=\"ip_list\"]/tbody/tr[@class=\"odd\"]").nodes();
		for (Selectable tr : trs) {
			String host = tr.xpath("//td[2]/text()").toString().trim();
			String port = tr.xpath("//td[3]/text()").toString().trim();
			String hostandport = host+":"+port;
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				pool.httpPush(hostandport+":"+flag);
				//System.out.println("西刺http:"+hostandport);
			}
		}
	}
	
	/**
	 * 西刺网站https代理
	 * @param page
	 */
	private void xiciHttps(Page page) {
		Html html = page.getHtml();
		List<Selectable> trs = html.xpath("//table[@id=\"ip_list\"]/tbody/tr[@class=\"odd\"]").nodes();
		for (Selectable tr : trs) {
			String host = tr.xpath("//td[2]/text()").toString().trim();
			String port = tr.xpath("//td[3]/text()").toString().trim();
			String hostandport = host+":"+port;
			String flag = CheckProxyIp(hostandport);
			if(flag!=null) {
				pool.httpsPush(hostandport+":"+flag);
				//System.out.println("西刺https:"+hostandport);
			}
		}
	}
	
	/**
	 * 用于测试网址是否反爬
	 * @param url
	 */
	public static void test(String url) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30000)
															.setSocketTimeout(20000)
															.build();
		HttpGet httpGet = new HttpGet(url);
		httpGet.setConfig(requestConfig);
		try {
			CloseableHttpResponse response = httpClient.execute(httpGet);
			/*InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "gbk"));
			String line;
			while((line = reader.readLine())!=null)
				System.out.println(line);*/
			System.out.println(EntityUtils.toString(response.getEntity(), "gbk"));
			
		} catch (Exception e) {
			e.printStackTrace();
		
		}
	}
	
	/**
	 * 检查代理IP是否可用,响应时间低于3秒,返回ture
	 * @param hostandport
	 * @return
	 */
	public String CheckProxyIp(String hostandport) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		String hostname = hostandport.split(":")[0];
		int port = Integer.parseInt(hostandport.split(":")[1]);
		HttpHost proxy = new HttpHost(hostname,port);
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(3000)
															.setSocketTimeout(2000)
															.setProxy(proxy)
															.build();
		HttpGet httpGet = new HttpGet("http://2018.ip138.com/ic.asp");
		httpGet.setConfig(requestConfig);
		CloseableHttpResponse response = null;
		try {
			long t1 = System.currentTimeMillis();
			httpClient.execute(httpGet);
			t1 = System.currentTimeMillis() - t1;
			if(t1<3000) {
				//System.out.println(EntityUtils.toString(response.getEntity(), "gbk"));
				return t1+"";
			}
			return null;
		} catch (Exception e) {
			//e.printStackTrace();
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				//e.printStackTrace();
			}finally {
				httpClient = null;
			}
		}
		return null;
	}
	
	@Override
	public Site getSite() {
		return site;
	}

}
