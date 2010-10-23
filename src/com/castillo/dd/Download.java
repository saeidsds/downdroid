package com.castillo.dd;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.util.Log;

// This class downloads a file from a URL.
class Download extends Observable implements Runnable {
    
	// Max size of download buffer.
    private static final int MAX_BUFFER_SIZE = 1024;
    
    // These are the status names.
    public static final String STATUSES[] = {"Downloading",
    "Paused", "Complete", "Cancelled", "Error"};
    
    // These are the status codes.
    public static final int START = -1;
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;
    public static final int MEGAUPLOAD = 5;
    public static final int RAPIDSHARE = 6;
    
    private URL url; // download URL
    private int size; // size of download in bytes
    private int downloaded; // number of bytes downloaded
    private int status; // current status of download
    private String fileName;
    private int i;
    private long launchTime=0;
    private long startTime=0;
    
    Preferences prefs;
    
    public String getOrder()
    {
    	String ret=new Integer(i).toString();
    	if (ret.length()==1)
    		ret="0"+ret;
    	return ret;
    }
    
    // Constructor for Download.
    public Download(Preferences prefs, URL url, int i) {
        this.prefs=prefs;
    	this.url = url;
        this.i=i;
        size = -1;
        downloaded = 0;
        status = START;        
        
        // Begin the download.
        //download();
    }
    
    // Get this download's URL.
    public String getUrl() {
        return url.toString();
    }
    
    // Get this download's size.
    public int getSize() {
        return size;
    }
    
    // Get this download's progress.
    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }
    
    // Get this download's status.
    public int getStatus() {
        return status;
    }
    
    // Get this download's launch Time
    public long getLaunchTime() {
        return launchTime;
    }
    
    // Get this download's ellapsed time
    public String getEllapsedTime() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    	dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    	Calendar cal = Calendar.getInstance();
    	long elapsed = cal.getTimeInMillis();
    	elapsed=elapsed-startTime;
    	cal.setTimeInMillis(elapsed);
    	return dateFormat.format(cal.getTime());
    }
    
    // Get this download's speed
    public float getSpeed() {
    	float s=downloaded/1024;
    	Calendar cal = Calendar.getInstance();
    	long elapsed = cal.getTimeInMillis();
    	elapsed=elapsed-startTime;
    	elapsed=elapsed/1000;
    	s=s/elapsed;
    	return s;
    }

    // Get this download's remaining time
    public String getRemainingTime() {
    	SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    	dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    	Calendar cal = Calendar.getInstance();
    	float kbs=(size-downloaded)/1024;
    	float seconds=kbs/getSpeed();
    	long millis=(long)(seconds*1000);
    	cal.setTimeInMillis(millis);    	
    	return dateFormat.format(cal.getTime());
    }
    
    // Pause this download.
    public void pause() {
        status = PAUSED;
        stateChanged();
    }
    
    // Resume this download.
    public void resume() {
        status = DOWNLOADING;
        stateChanged();
        download();
    }
    
    // Cancel this download.
    public void cancel() {
        status = CANCELLED;
        stateChanged();
    }
    
    // Mark this download as having an error.
    private void error() {
        status = ERROR;
        stateChanged();
    }
    
    private void megaupload() {
        status = MEGAUPLOAD;
        stateChanged();
    }
    
    private void rapidshare() {
        status = RAPIDSHARE;
        stateChanged();
    }
    
    // Start or resume downloading.
    private void download() {
        Thread thread = new Thread(this);
        thread.start();
    }
    
    // Get file name portion of URL.
    public String getFileName(URL url) {
        fileName = url.getFile();
        fileName=URLDecoder.decode(fileName.substring(fileName.lastIndexOf('/') + 1));
        return fileName;
    }
    
    public String getFileName() {
        if (fileName==null || fileName.length()==0)
        {
        	fileName = url.getFile();
        	fileName=URLDecoder.decode(fileName.substring(fileName.lastIndexOf('/') + 1));
        }
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public boolean isPremium(String cookie) throws IOException 
    {
    	URL urlmegaupload=new URL("http://megaupload.com/?c=account");
    	HttpURLConnection connection =(HttpURLConnection) urlmegaupload.openConnection();
    	connection.setRequestProperty("Cookie","l=en; user="+cookie+";");
    	connection.connect();
    	BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String line;
		String content = "";
		while ((line = rd.readLine()) != null) {
			content = content + line + "\n";
		}
		rd.close();		
		
		Matcher matcher = Pattern.compile("<TD>Account type:</TD>.*?<TD><b>(.*?)</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content);
		matcher.find();
		String type = matcher.group(1);
    	if (type == null || type.equalsIgnoreCase("regular"))
    		return false;
    	else
   	        return true;   	
    }
    
    private boolean megauploadFileCheck(String url) {
        try {
        	URL urlmegaupload=new URL(url);
        	HttpURLConnection connection =(HttpURLConnection) urlmegaupload.openConnection();
        	connection.setRequestProperty("Cookie","l=en;");
        	connection.connect();
        	BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    		String line;
    		String content = "";
    		while ((line = rd.readLine()) != null) {
    			content = content + line + "\n";
    		}
    		rd.close();
    		Matcher matcher = Pattern.compile("location='http://www\\.megaupload\\.com/\\?c=msg", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content);
    		if (matcher.matches())
    			return false;
            if (content.indexOf("The file has been deleted because it was violating")>=0) {                
                return false;
            }
            if (content.indexOf("Invalid link")>=0) {
                return false;
            }
            
            matcher = Pattern.compile("<font style=.*?>Filename:</font> <font style=.*?>(.*?)</font><br>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content);
            matcher.find();
            String filename = matcher.group(1).trim();
            matcher = Pattern.compile("<font style=.*?>File size:</font> <font style=.*?>(.*?)</font>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(content);
            matcher.find();
            String filesize = matcher.group(1).trim();
            if (filename == null || filesize == null) {
                return false;
            } else {                
                return true;
            }            
        } catch (Exception e) {
            Log.e("CHECKLINK","Megaupload blocked this IP(2): 25 mins");
            return false;
        }        
    }
    
    public String getMegauploadCookie()
    {
    	String ret="";
    	try
    	{
	    	DefaultHttpClient httpclient = new DefaultHttpClient();
	    	CookieStore cookies = httpclient.getCookieStore();    	
	
	    	httpclient.getParams().setParameter(ClientPNames.COOKIE_POLICY,CookiePolicy.BROWSER_COMPATIBILITY);
	    	List<Header> headers=new ArrayList<Header>();
	    	headers.add(new BasicHeader("User-Agent", "Mozilla/5.0(Windows; U; Android 1.0;Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13"));
	    	httpclient.getParams().setParameter(ClientPNames.DEFAULT_HEADERS, headers);
	    	HttpPost loginpostmethod = new HttpPost("http://megaupload.com/?c=login");
	    	ArrayList<BasicNameValuePair> loginnvpairs = new ArrayList<BasicNameValuePair>();
	    	loginnvpairs.add(new BasicNameValuePair("login","1"));
	    	loginnvpairs.add(new BasicNameValuePair("redir","1"));
	    	loginnvpairs.add(new BasicNameValuePair("username",DownDroid.prefs.getMegaupload_user()));
	    	loginnvpairs.add(new BasicNameValuePair("password",DownDroid.prefs.getMegaupload_password()));
	    			
	    	UrlEncodedFormEntity p_entity = new UrlEncodedFormEntity(loginnvpairs);
	    	loginpostmethod.setEntity(new UrlEncodedFormEntity(loginnvpairs, HTTP.UTF_8));
	    	loginpostmethod.setEntity(p_entity);
	    	ResponseHandler<String> responseHandler = new BasicResponseHandler();
	    	httpclient.execute(loginpostmethod,responseHandler);
	    	cookies = httpclient.getCookieStore();
	    	for (int i=0;i<cookies.getCookies().size();i++)
	    	{
	    		Cookie c=cookies.getCookies().get(i);
	    		if(c.getName().equalsIgnoreCase("user"))
	    			return c.getValue();
	    	}
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    	return ret;
    }    
    
    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
                return true;
        }
    };
    
    private static void trustAllHosts() {
	        // Create a trust manager that does not validate certificate chains
	        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                        return new java.security.cert.X509Certificate[] {};
	                }
	
	                public void checkClientTrusted(X509Certificate[] chain,
	                                String authType) throws CertificateException {
	                }
	
	                public void checkServerTrusted(X509Certificate[] chain,
	                                String authType) throws CertificateException {
	                }
	        } };
	
	        // Install the all-trusting trust manager
	        try {
	                SSLContext sc = SSLContext.getInstance("TLS");
	                sc.init(null, trustAllCerts, new java.security.SecureRandom());
	                HttpsURLConnection
	                                .setDefaultSSLSocketFactory(sc.getSocketFactory());
	        } catch (Exception e) {
	                e.printStackTrace();
	        }
	}
    
    public String getRapidshareCookie()
    {
    	String ret="";
    	try
    	{
    		HttpURLConnection http = null;

            URL urlr=new URL("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login="+DownDroid.prefs.getRapidshare_user()+"&password="+DownDroid.prefs.getRapidshare_password());
    		
    		if (urlr.getProtocol().toLowerCase().equals("https")) {
                trustAllHosts();
                    HttpsURLConnection https = (HttpsURLConnection) urlr.openConnection();
                    https.setHostnameVerifier(DO_NOT_VERIFY);
                    http = https;
            } else {
                    http = (HttpURLConnection) urlr.openConnection();
            }

    		String headerName=null;
    		for (int i=1; (headerName = http.getHeaderFieldKey(i))!=null; i++) {
             	if (headerName.equalsIgnoreCase("set-cookie")) {                  
             		String cookie = http.getHeaderField(i);
             		if (cookie.startsWith("enc"))
             			return cookie.substring(0, cookie.indexOf(';')+1);
             	}
    		}
	    		    	
    	}
    	catch (Exception ex)
    	{
    		ex.printStackTrace();
    	}
    	return ret;
    }
    
    // Download file.
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        
        try {
        	if (launchTime==0)
        		launchTime=Calendar.getInstance().getTimeInMillis();
            // Open connection to URL.
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            
            connection.setInstanceFollowRedirects(true);
            
            if (url.getHost().indexOf("megaupload")>=0)
            {
            	if (!megauploadFileCheck(url.toString()))
            		error();
            	else
            	{
	            	String cookie="";
	            	boolean isPremium=false;
	            	if (DownDroid.prefs.megaupload_user!=null && DownDroid.prefs.megaupload_user.trim().length()>0)
	            	{
		            	cookie=getMegauploadCookie();	            	
		            	isPremium=isPremium(cookie);
	            	}
	            	if (cookie.length()!=0 && isPremium)            	
	            		connection.setRequestProperty("Cookie","l=es; user="+cookie+"; __utma=216392970.1949298155801896700.1244535146.1247056407.1258354625.5;	__utmb=216392970.1.10.1258354625;	__utmc=216392970;	__utmz=216392970.1247041692.3.2.utmcsr=vagos.wamba.com|utmccn=(referral)|utmcmd=referral|utmcct=/showthread.php");
	            	else	            	
	            		megaupload();	            	
            	}
            }
            else if (url.getHost().indexOf("rapidshare")>=0)
            {
            	String cookie="";
            	if (DownDroid.prefs.rapidshare_user!=null && DownDroid.prefs.rapidshare_user.trim().length()>0)
            		cookie=getRapidshareCookie();
            	if (cookie.length()!=0)            	
            		connection.setRequestProperty("Cookie",cookie);
            	else
            	{
            		rapidshare();
            		return;
            	}
            }
            
            if (status<ERROR)
            {
	            // Specify what portion of file to download.
	            connection.setRequestProperty("Range",
	                    "bytes=" + downloaded + "-");
	            
	            // Connect to server.
	            connection.connect();
	            
	            // Make sure response code is in the 200 range.
	            if (connection.getResponseCode() / 100 != 2) {
	                error();
	            }
	            
	            // Check for valid content length.
	            int contentLength = connection.getContentLength();
	            if (contentLength < 1 || (connection.getResponseMessage().equalsIgnoreCase("OK") && url.getHost().indexOf("rapidshare")>=0)) {
	            	if (url.getHost().indexOf("megaupload")>=0)
	            	{
	            		try
	            		{
		            		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		        			String line;
		        			String content = "";
		        			while ((line = rd.readLine()) != null) {
		        				content = content + line + "\n";
		        			}
		        			rd.close();
		        			content = content.substring(
		        					content.indexOf("id=\"downloadlink\">") + 29, content
		        							.indexOf("id=\"downloadlink\">") + 200);
		        			content = content.substring(0, content.indexOf("onclick=\"") - 2);
		        			url=new URL(content);
		        			connection=(HttpURLConnection)url.openConnection();
		        			contentLength = connection.getContentLength();
		                    if (contentLength < 1)
		                    	error();
	            		}
	            		catch (Exception ex)
	            		{
	            			error();
	            		}
	            	}
	            	else if (url.getHost().indexOf("rapidshare")>=0)
	            	{
	            		try
	            		{
		            		BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		        			String line;
		        			String content = "";
		        			while ((line = rd.readLine()) != null) {
		        				content = content + line + "\n";
		        			}
		        			rd.close();
		        			content = content.substring(
		        					content.indexOf("<form id=\"ff\" action=\"") + 22, content
		        							.indexOf("<form id=\"ff\" action=\"") + 200);
		        			content = content.substring(0, content.indexOf("method=\"post\">") - 2);
		        			
		        			String data = URLEncoder.encode("dl.start", "UTF-8") + "=" + URLEncoder.encode("PREMIUM", "UTF-8");	        		     
		        		    
		        		    // Send data
		        		    URL urlintermedia = new URL(content);
		        		    URLConnection conn = urlintermedia.openConnection();
		        		    String cookie=getRapidshareCookie();
		                    if (cookie.length()!=0)            	
		                    	conn.setRequestProperty("Cookie",cookie);
		                 	
		        		    conn.setDoOutput(true);
		        		    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		        		    wr.write(data);
		        		    wr.flush();
		        		     
		        		    // Get the response
		        		    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		        		    content = "";
		        		    while ((line = rd.readLine()) != null) {
		        		    	 content = content + line + "\n";
		        		    }
		        		    wr.close();
		        		    rd.close();
		        		     
		        		    content = content.substring(
		        					content.indexOf("<form name=\"dlf\" action=\"") + 25, content
		        							.indexOf("<form name=\"dlf\" action=\"") + 200);
		        			content = content.substring(0, content.indexOf("method=\"post\">") - 2);
		        			
		        			url=new URL(content);
		        			connection=(HttpURLConnection)url.openConnection();
		        			connection.setRequestProperty("Cookie",cookie);
		        			contentLength = connection.getContentLength();
		                    if (contentLength < 1)
		                    	error();
	            		}
	            		catch (Exception ex)
	            		{
	            			error();
	            		}
	            	}
	            	else
	            		error();
	            }
	            
	      /* Set the size for this download if it
	         hasn't been already set. */
	            if (size == -1) {
	                size = contentLength;
	                stateChanged();
	            }
	            
	            // Open file and seek to the end of it.
	            fileName=getFileName(connection.getURL());
	            if (startTime==0)
	            	startTime=Calendar.getInstance().getTimeInMillis();
	            file = new RandomAccessFile("/sdcard/dd/"+fileName, "rw");
	            file.seek(downloaded);
	            
	            stream = connection.getInputStream();
	            while (status == DOWNLOADING) {
	        /* Size buffer according to how much of the
	           file is left to download. */
	                byte buffer[];
	                if (size - downloaded > MAX_BUFFER_SIZE) {
	                    buffer = new byte[MAX_BUFFER_SIZE];
	                } else {
	                    buffer = new byte[size - downloaded];
	                }
	                
	                // Read from server into buffer.
	                int read = stream.read(buffer);
	                if (read == -1)
	                    break;
	                
	                // Write buffer to file.
	                file.write(buffer, 0, read);
	                downloaded += read;
	                stateChanged();
	            }
	            
	      /* Change status to complete if this point was
	         reached because downloading has finished. */
	            if (status == DOWNLOADING) {
	                status = COMPLETE;
	                stateChanged();
	            }            
            }            
        } catch (Exception e) {            
        	error();
        } finally {
            // Close file.
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }
            
            // Close connection to server.
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }
    
    // Notify observers that this download's status has changed.
    private void stateChanged() {
        setChanged();
        notifyObservers();
    }
}