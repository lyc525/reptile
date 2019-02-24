package work.lycwood.WebMagic;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;

import javax.management.JMException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;


import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;


//实现PageProcessor接口
public class GetBooks implements PageProcessor {
	// 抓取网站的相关配置，可以包括编码、抓取间隔100ms、重试次数(5次)等
	private Site site = Site.me().setRetryTimes(5).setSleepTime(100)
			
							//.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
							//.addHeader("Accept-Language", "zh-CN,zh;q=0.9")
							.addHeader("Host", "www.xiabook.com")
							.addHeader("Referer", "www.xiabook.com")
							.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
							;
	//要爬网站的域名,之后会用与拼接链接(有些页面链接是相对地址,需拼接成绝对地址然后保存)
	private static final String HOSTURL = "http://www.xiabook.com";
	
	public static void main(String[] args) {

		GetBooks my = new GetBooks();
		//创建Spider实例,爬虫入口,添加要爬取的网站
		Spider spider = Spider.create(my).addUrl("http://www.xiabook.com");
		System.out.println("开始爬取...");
		try {
			//将爬虫添加到监控里
			SpiderMonitor.instance().register(spider);
		} catch (JMException e) {
			e.printStackTrace();
		}
		//启动爬虫,5个线程,非阻塞启动
		spider.thread(5).start();

	}
	
	@Override
	public Site getSite() {
		return site;
	}
	
	//抓取逻辑方法,对页面信息的提取
	@Override
	public void process(Page page) {
		
		String url = page.getUrl().toString();//获得该页面的url
		//对该页面的url进行判断是哪个页面,进行不同规则的提取信息
		if(url.equals(HOSTURL))
			getType(page);
		else if(page.getUrl().regex("http://www.xiabook.com/[a-z]{2,8}/$").toString()!=null)
			getNum(page);
		else if(page.getUrl().regex("http://www.xiabook.com/[a-z]{2,8}/index_?[0-9]{0,3}.html").toString()!=null)
			getBookUrl(page);
		else if(url.contains("/down/")&&
				(page.getUrl().regex("http://www.xiabook.com/down/[0-9]{1,3}-[0-9]{1,6}-[0-9]{1,3}.html$").toString()!=null))
			downBook(page);
		else if(!url.contains("/down/")&&
				(page.getUrl().regex("http://www.xiabook.com/[a-z]{2,8}/[0-9]{0,5}/$").toString()!=null))
			getBook(page);
	}
	
	

	public void getType(Page page) {
		//获取该页面的html文档信息
		Html html = page.getHtml();
		for(int i=1;i<=5;i++) {
			//对获得的html进行XPath规则提取信息(也可使用正则表达式和CSS选择器)
			Selectable lis = html.xpath("//div[@class=\"vk_icon_"+i+"\"]/ul");
			List<Selectable> nodes = lis.xpath("//li").nodes();
			for (Selectable li : nodes) {
				String type = li.xpath("//a/@href").get();
				if(type.contains("/down.html"))
					continue;
				//将提取到的新链接添加到队列中,优先级设置为0(最高)
				FileCacheQueueScheduler fScheduler = new FileCacheQueueScheduler("L:/books/");
				
				Request request = new Request(type).setPriority(0);
		        page.addTargetRequest(request);
			}
			
		}
	}
	
	public void getNum(Page page) {
		System.out.println("getNum");
		Html html = page.getHtml();
		int num = Integer.parseInt(html.xpath("//div[@class=\"pages\"]/a/b/text()").get());
		if((num/30.0)-(num/30)>0)
			num = num/30+1;
		else 
			num=num/30;
			
		Request request = new Request(page.getUrl().toString()+"index.html").setPriority(1);
        page.addTargetRequest(request);
		for(int i=2;i<=num;i++) {
			String url = page.getUrl().toString()+"index_"+i+".html";
			Request reques = new Request(url).setPriority(1);
	        page.addTargetRequest(reques);
		}
	}
	public void getBookUrl(Page page) {
		Html html = page.getHtml();
		List<Selectable> divs = html.xpath("//div[@id=\"zuo\"]/div[@class=\"bbox\"]").nodes();
		
		for (Selectable div : divs) {
			String url = div.xpath("//h3/a/@href").toString();
			Request request = new Request(HOSTURL+url).setPriority(2);
	        page.addTargetRequest(request);
		}
	}
	
	public void getBook(Page page) {
		Html html = page.getHtml();
		Selectable title = html.xpath("//div[@class=\"jianjie\"]");
		
		// 获取封面图片
		String bookimg = title.xpath("//img/@src").get();
		// 获得书名,作者,分类,字数,概要信息
		String name = title.xpath("//h1/text()").get();
		String author = title.xpath("//div[@class=\"xinxi\"]//li[1]/a/text()").get();
		String category = title.xpath("//div[@class=\"xinxi\"]//li[2]/a/text()").get();
		String size = title.xpath("//div[@class=\"xinxi\"]//li[3]/text()").get();
		size = size.substring(size.lastIndexOf("：")+1, size.length());
		//System.out.println(size);
		
		String summary = html.xpath("//div[@class=\"neirong\"]/div[2]/text()").get();
		
		//获取图书下载地址
		String downUrl = html.xpath("//div[@class=\"butt\"]/a[2]/@href").get();
		if(downUrl!=null) {
			Request request = new Request(downUrl).setPriority(3);
	        page.addTargetRequest(request);
		}
		//将提取的信息持久化到数据库
		sql(name, author, category, summary, size,bookimg);
		
	}
	
	public void downBook(Page page) {
		Html html = page.getHtml();
		
		String summary = html.xpath("//div[@class=\"bintr\"]/text()").get();
		
		summary = summary.replaceAll("&#[0-9]{1,5};", "");
		summary = summary.replace("ｗｗｗ．ｘｉａｂｏｏｋ．ｃｏｍ", "");
		summary = summary.replace("http://www.lzuowen.com", "");
		summary = summary.replace("ｗｗｗ．56wen．ｃｏｍ", "");
		summary = summary.replace("wwＷ．xiaＯBook.com", "");
		summary = summary.replace("wWw．Ｌｚｕｏｗｅｎ．ｃｏｍ", "");
		
		String name = html.xpath("//div[@class=\"wintro\"]/h1/text()").get();
		name = name.substring(name.indexOf("《")+1, name.lastIndexOf("》"));
		String downUrl = html.xpath("//div[@class=\"xzxx\"]/p[4]/a/@href").get();
		sqlWhere(name, summary,downUrl);
		/*try {
			saveBook(downUrl, name);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	/**
	 * 将获取的图片保存到本地L:/images/bookimgs/
	 * @param urls
	 * @param name
	 * @throws Exception
	 */
	public static void save(String urls, String name) throws Exception {

		CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
		
		HttpGet httpGet = new HttpGet(urls);
		//对于个别网站,需在这里添加请求头信息
		CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		InputStream in = entity.getContent();

		BufferedInputStream input = new BufferedInputStream(in);
		String suffix = urls.substring(urls.lastIndexOf("."), urls.length());
		FileOutputStream fileOutputStream = new FileOutputStream(new File("L:/images/bookimgs/" + name + suffix));
		byte[] b = new byte[1024];
		int read;
		while ((read = input.read(b, 0, b.length)) > 0) {
			fileOutputStream.write(b, 0, read);
		}
		fileOutputStream.flush();
		fileOutputStream.close();
		input.close();
	}
	/**
	 * 将获得的书籍链接保存到本地L:/images/books/
	 * @param urls
	 * @param name
	 * @throws Exception
	 */
	public static void saveBook(String urls, String name) throws Exception {

		CloseableHttpClient closeableHttpClient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(urls);
		//对于个别网站,需在这里添加请求头信息
		CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
		HttpEntity entity = response.getEntity();
		InputStream in = entity.getContent();

		BufferedInputStream input = new BufferedInputStream(in);
		FileOutputStream fileOutputStream = new FileOutputStream(new File("L:/images/books/" + name + ".txt"));
		byte[] b = new byte[4096];
		int read;
		while ((read = input.read(b, 0, b.length)) > 0) {
			fileOutputStream.write(b, 0, read);
		}
		fileOutputStream.flush();
		fileOutputStream.close();
		input.close();
	}
	
	/**
	 * 将提取到信息保存到数据库
	 * @param name
	 * @param author
	 * @param category
	 * @param summary
	 * @param size
	 * @param imgUrl
	 */
	public void sql(String name, String author, String category, String summary, String size,String imgUrl) {

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");

			conn = DriverManager.getConnection("jdbc:mysql:///library_v1", "root", "");
			String sql = "insert into books values(null,?,?,?,null,?,?,null,now(),now())";
			ps = conn.prepareStatement(sql);
			ps.setString(1, name);
			ps.setString(2, category);
			ps.setString(3, author);
			ps.setString(4, size);
			ps.setString(5, imgUrl);
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				ps.close();
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 将最后获得的书籍链接和书籍简介存入数据库
	 * @param name
	 * @param summary
	 * @param downUrl
	 */
	public void sqlWhere(String name,String summary,String downUrl) {

		Connection conn = null;
		PreparedStatement ps = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");

			conn = DriverManager.getConnection("jdbc:mysql:///library_v1", "root", "");
			String sql = "update books set summary=?, downurl=? where name=?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, summary);
			ps.setString(2, downUrl);
			ps.setString(3, name);
			ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				ps.close();
				conn.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
