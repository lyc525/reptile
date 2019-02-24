package work.lycwood.douban;

import java.util.List;

import javax.management.JMException;

import org.apache.http.MessageConstraintException;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.Selectable;

public class XiaoShuo implements PageProcessor{
	
	private Site site = new Site().me()
								  .addHeader("Referer", "https://book.douban.com/")
								  .setSleepTime(100).setRetryTimes(5);
	
	
	
	public static void main(String[] args) {
		
		Spider spider = Spider.create(new XiaoShuo())
							  .setScheduler(new FileCacheQueueScheduler("L:\\WebMagic\\douban\\xiaoshuo"))
							  .addUrl("https://book.douban.com/tag/?view=type&icn=index-sorttags-all")
							  ;
		
		try {
			SpiderMonitor.instance().register(spider);
		} catch (JMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		spider.thread(20).start();
		
	}

	@Override
	public void process(Page page) {
		String url = page.getUrl().toString();
		if("https://book.douban.com/tag/?view=type&icn=index-sorttags-all".equals(url))
			getTypeUrl(page);
		else if (url.matches("https://book.douban.com/tag/[\\u4E00-\\u9FA5A-Za-z.]{2,8}$")) 
			getTypePage(page);
		else if(url.contains("?start="))
			getBookPage(page);
		else if(url.matches("https://book.douban.com/subject/[0-9]{1,10}/$"))
			getBookMsg(page);
		
	}
	

	/**
	 * 
	 * 
	 * //*[@id="info"]/span[@class="pl"]
	 * @param page
	 */
	private void getBookMsg(Page page) {
		Html html = page.getHtml();
		
		String name = null;
		String author = null;
		String press = null;
		String oldName = null;
		String grade = null;
		String nums = null;
		String pages = null;
		String price = null;
		String ISBN = null;
		
		Selectable div = html.xpath("//*[@id=\"info\"]");
		name = html.xpath("//*[@id=\"wrapper\"]/h1/span/text()").toString();
		author = div.xpath("//span[1]/a/text()").toString();
		String[] mes = div.xpath("//*[@id=\"info\"]/text()").toString().trim().split("  ");
		String span = html.xpath("//*[@id=\"info\"]/span[@class=\"pl\"]/text()").nodes().toString();
		String[] types = span.split(",");
		for(int i=0;i<types.length;i++) {
			String type = types[i].trim();
			if(type.contains("出版社")) {
				press = mes[i].trim();
			}
			if(type.contains("原作名")) {
				oldName = mes[i].trim();
			}
			if(type.contains("页数")) {
				pages = mes[i].trim();
			}
			if(type.contains("定价")) {
				price = mes[i].trim().substring(0, mes[i].trim().length()-1);
			}
			if(type.contains("ISBN")) {
				ISBN = mes[i].trim();
			}
		}
		System.out.println(name+author+oldName+press+pages+price+ISBN);
		grade = html.xpath("//*[@id=\"interest_sectl\"]/div/div[2]/strong/text()").toString();
		nums = html.xpath("//*[@id=\"interest_sectl\"]/div/div[2]/div/div[2]/span/a/span/text()").toString();
		System.out.println(grade+nums);
		
	}
	
	private void getBookPage(Page page) {
		Html html = page.getHtml();
		List<Selectable> lis = html.xpath("//*[@id=\"subject_list\"]/ul/li").nodes();
		for (Selectable li : lis) {
			String url = li.xpath("//h2[@class]/a/@href").toString();
			Request request = new Request(url).setPriority(-1);
			page.addTargetRequest(request);
			//System.out.println(url);
		}
	}
	
	private void getTypePage(Page page) {
		Html html = page.getHtml();
		String uri = page.getUrl().toString();
		String totle = html.xpath("//*[@id=\"subject_list\"]/div[2]/a[10]/text()").toString();
		int num = Integer.parseInt(totle);
		for(int i=0;i<num*20;) {
			String url = uri + "?start=" + i + "&type=T";
			Request request = new Request(url).setPriority(0);
			page.addTargetRequest(request);
			i += 20;
		}
		
	}

	private void getTypeUrl(Page page) {
		Html html = page.getHtml();
		List<Selectable> divs = html.xpath("//*[@id=\"content\"]/div/div[1]/div[2]/div").nodes();
		for (Selectable div : divs) {
			List<Selectable> tds = div.xpath("//table/tbody//td").nodes();
			for (Selectable td : tds) {
				String url = td.xpath("//a/@href").toString();
				Request request = new Request("https://book.douban.com"+url).setPriority(1);
				page.addTargetRequest(request);
			}
		}
	}
 
	@Override
	public Site getSite() {
		return site;
	}
}
