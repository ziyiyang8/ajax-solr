import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Main crawler class.
 * The web crawler is initialized with one seed, max pages to crawl, and a domain (optional).
 * The crawler will save all textual content in a folder called repository (created during initialization if it did not exist already).
 * It will also store additional attributes in an html file called report.html.
 * Afterwards, it will continue to crawl all outlinks.  The crawler will not crawl a link if robots.txt disallows it.
 *
 */
public class WebCrawler {
	private Queue<String> seeds; 		// queue of URLs to visit (frontier)
	private Set<String> pagesVisited; 	// set of URLs that have already been visited
	private int maxPages;				// max number of pages to crawl for
	private String domain;				// optional domain to limit our search to
	
	// use an user agent so web browsers do not get confused at our crawler
	private final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	
	/**
	 * Initialize our crawler based on input file.
	 * 
	 * @param input	specifications.csv
	 * @throws IllegalArgumentException bad input file
	 * @throws IOException 
	 */
	public WebCrawler(FileReader input) throws IllegalArgumentException, IOException
	{
		seeds = new LinkedList<String>();	
		pagesVisited = new HashSet<String>();
		
		// Create the repository directory, if it does not exist, to store our crawled data in.
		File dir = new File("repository");
		if (!dir.exists())
			dir.mkdirs();
		// if directory already exists, then clean out all the files in it to save space on our disk
		else
		{
			for(File file: dir.listFiles()) 
				file.delete();
		}
		
		// create our html report to display URL attributes, etc.
		File report = new File("report.html");
		// create new report every time crawler is run
		if (report.exists())
			report.delete();
		report.createNewFile();
		
		BufferedReader br = new BufferedReader(input);
		try {
			// read line from file and remove any unnecessary whitespaces
			String line = br.readLine().replaceAll("\\s+","");
			// input file will be comma separated
			String [] fields = line.split(",");
			// valid input file
			if (fields.length >= 2 && fields.length <= 3)
			{
			    // add a '/' to end of url if it does not have it. This is to ensure our matching later is accurate
//				if (fields[0].charAt(fields[0].length() -1) != '/')
//					fields[0] = fields[0] + "/";
				seeds.offer(fields[0]); // put first seed URL on queue
				maxPages = Integer.parseInt(fields[1]); 
				if (fields.length == 3) // a domain is specified
				{
					// remove www if it's specified
					if (fields[2].startsWith("www"))
						domain = fields[2].substring("www".length()+1);
					else
						domain = fields[2];
				}
			}
			// throw exception if bad input file
			else
				throw new IllegalArgumentException("Invalid input file");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Start our crawler and crawl until we hit our max page limit.
	 * @throws IOException 
	 * 
	 */
	public void crawl() throws IOException
	{
		BufferedWriter output = null;
		
		// keep crawling until the number of pages visited is greater than max pages to crawl.
		while (pagesVisited.size() < maxPages)
		{
			// get next valid URL
			String url = nextURL();
			// if no valid url then stop crawl
			if (url == null || url.length() == 0)
				break;
			// add url to our visited set
			pagesVisited.add(url);
			// only connect if safe to crawl based on robots.txt and our domain
			if (domainSafe(url))
			{
				try {
					System.out.println("Crawling... " + url);
					// use Jsoup to connect to url.  Specify an user agent so web server won't restrict us.
					// maxBodySize is specified so we can grab the whole page. Set the timeout to 10 seconds
					// ignore HTTP errors so we can grab all types of HTTP status codes
					Connection.Response connection = Jsoup.connect(url).userAgent(USER_AGENT).maxBodySize(Integer.MAX_VALUE).ignoreHttpErrors(true).timeout(10*1000).execute();
					// if HTTP Status Code is 200 then we can parse the page
					if (true)
					{
						Document page = connection.parse();

						// get all outlinks and store number of links on page
						Elements linksOnPage = page.select("a[href]");
						for (Element link : linksOnPage)
							// add each link to seeds queue
							seeds.offer(link.absUrl("href"));
	
						// get all image elements on the page to use for our report.html statistics
						Elements imagesOnPage = page.select("img");
						
						// save only html tags, don't download any images
						for (Element e : imagesOnPage)
						{
							Attributes imgAttrs = e.attributes();
							for (Attribute a : imgAttrs)
								e.removeAttr(a.getKey());
						}
						// get other html tags that have src as an attribute
						page.select("frame[src").attr("src", " ");
						page.select("iframe[src]").attr("src", " ");
						page.select("script[src]").attr("src", " ");
						page.select("input[src]").attr("src", " ");
						page.select("embed[src]").attr("src", " ");
						
						// check if there is url in metadata
						boolean metaUrlFlag = false;
						Elements metadata = page.select("meta");
						for (Element meta : metadata)
						{
							if (meta.attr("property").equals("url"))
							{
								metaUrlFlag = true;
								break;
							}
						}
						if (!metaUrlFlag)
							page.head().appendElement("meta").attr("property", "url").attr("content", url);
						// get all textual content of the page
						String textContent = page.outerHtml();
								
						// save the text content of the page to a file into the repository folder.
						// we use createTempFile to get a nice unique name for each url.
						File cachedContent = File.createTempFile("URL", ".html", new File("repository"));

						output = new BufferedWriter(new FileWriter(cachedContent));
				        output.write(textContent);				        
				        // write to our report.html with url statistics
				        // url, file name, number of outlinks, HTTP status Code, number of images
				        writeToReport(url, cachedContent.getName(), linksOnPage.size(), connection.statusCode(), imagesOnPage.size());
					}
					// not HTTP status code 200 then write what the status code is to report.html
					else
						writeToReport(url, "", 0, connection.statusCode(), 0);
			    
				// if we can not get a connection, then continue onto the next URL.
				} catch (IOException e) {
				}
				finally {
					// close our BufferedWriter stream
					if ( output != null ) 
		            	output.close();
				}
			}
		}
		System.out.println("Finished crawl.");
	}

	/**
	 * For politeness policy.
	 * Check if a url is safe to crawl by checking robots.txt
	 * 
	 * @param url
	 * @return
	 */
	public boolean robotSafe(String url)
	{
		URL aURL;			// we use URL object to easily parse for domain part of URL
		String host;		// string to store domain of URL
		String robotsTXT;	// string to store robots.txt
			
	    // add a '/' to end of url if it does not have it. This is to ensure our matching later is accurate
		if (url.charAt(url.length() -1) != '/')
			url = url + "/";
		
		// parse input URL to access <host>/robots.txt file
		try {
			aURL = new URL(url);
			// get only the domain part of URL
			host = aURL.getHost();	
			// have to use secure communication (https) because some robots.txt are located there
			robotsTXT = "https://" + host + "/robots.txt";
		// if anything bad happens, then assume url not safe to crawl
		} catch (MalformedURLException e) {
			return false;
		}
		
		try {
			// use Jsoup to grab robots.txt into a string. We use an user agent because some web servers
			// will refuse the connection and return a 403 status code.
			Document robot = Jsoup.connect(robotsTXT).userAgent(USER_AGENT).get();
			String robotStr = robot.text();
			
			// get category part of url to later match with disallowed categories.
			// www.<host>.com/<category>
			String category = aURL.getFile();
			
			// parse through robot.txt string, checking only for disallowed categories
			int index = 0;
			while ((index = robotStr.indexOf("Disallow:", index)) != -1) 
			{
			    index += "Disallow:".length();
			    String strPath = robotStr.substring(index);
			    StringTokenizer st = new StringTokenizer(strPath);

			    // no more tokens, we are done
			    if (!st.hasMoreTokens())
			    	break;
			    
			    // get the disallowed category
			    String disallowed = st.nextToken();
			    
			    // if the URL category matches with a disallowed path, we can not crawl the URL
			    if (category.indexOf(disallowed) == 0)
			    {
			    	System.out.println(url + " disallowed by robots.txt");
			    	return false;
			    }
			}
		// if something bad happened, assume we should not crawl url
		} catch (Exception e) {
			e.printStackTrace();
	    	return false;
		}
		// if everything still all good, then url must be safe to crawl
		return true;
	}
	
	/**
	 * Check if the url is within our domain, if a domain was specified as input
	 * 
	 * @param url
	 * @return boolean
	 */
	private boolean domainSafe(String url)
	{
		// no domain specified then always return true;
		if (domain == null || domain.length() == 0)
			return true;
		// url not within domain then is not safe
		if (!url.contains(domain))
		{
			System.out.println(url + " not within domain.");
			return false;
		}
		else
			return true;
	}
	
	/**
	 * Get the next URL to crawl to, making sure that URL has not been visited yet.
	 * 
	 * @return url
	 */
	private String nextURL()
	{
		String url;
		// pop off an url off of our queue otherwise return null
		if (!seeds.isEmpty())
			url = seeds.poll();
		else
			return null;
		// keep popping off queue until we get a valid URL not yet visited
		while (url == null || url.length() == 0 || pagesVisited.contains(url))
		{
			// if no more urls then return null
			if (seeds.isEmpty())
				return null;
			url = seeds.poll();
		}
		
		return url;
	}

	/**
	 * Write to reports.html with url statistics for each url we crawl.
	 * Link to actual website url, Link to downloaded page, http status code,
	 * number of outlinks on page, and number of images on page.
	 * 
	 * @param url
	 * @param fileName
	 * @param numLinks
	 * @param httpStatusCode
	 * @param numImages
	 */
	private void writeToReport(String url, String fileName, int numLinks, int httpStatusCode, int numImages)
	{
		try {
			// basic HTML code, ugly formatting
			BufferedWriter bw = new BufferedWriter(new FileWriter("report.html", true));
			bw.write("<html>");
			bw.write("<a href=\"" + url + "\">" + url + "</a>" );
			bw.write("<br/>");
			bw.write("<a href=\"repository\\" + fileName + "\">" + "Link to downloaded page" + "</a>");
			bw.write("<br/>");
			bw.write("HTTP Status Code: " + httpStatusCode);
			bw.write("<br/>");
			bw.write("Number of outlinks: " + numLinks);
			bw.write("<br/>");
			bw.write("Number of images: " + numImages);
			bw.write("</html>");
			bw.write("<hr>");
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Main method
	 * 
	 */
	public static void main(String[] args) {
		

			// allow Jsoup to connect to HTTPS sites
			// http://stackoverflow.com/questions/2793150/using-java-net-urlconnection-to-fire-and-handle-http-requests/2793153#2793153
		    TrustManager[] trustAllCertificates = new TrustManager[] {
		        new X509TrustManager() {
					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
							throws CertificateException {						
					}
					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
							throws CertificateException {						
					}
					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
		        }
		    };

		    HostnameVerifier trustAllHostnames = new HostnameVerifier() {
		        @Override
		        public boolean verify(String hostname, SSLSession session) {
		            return true; // Just allow them all.
		        }
		    };

		    try {
		        System.setProperty("jsse.enableSNIExtension", "false");
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCertificates, new SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		        HttpsURLConnection.setDefaultHostnameVerifier(trustAllHostnames);
		    }
		    catch (GeneralSecurityException e) {
		        throw new ExceptionInInitializerError(e);
		    }
		
		// kick off the crawling
		try {
			FileReader file = new FileReader("specification.csv");
			WebCrawler crawler = new WebCrawler(file);
			crawler.crawl();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}