package com.castillo.dd;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;
import de.mastacode.http.Http;

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
    
    public String getMegauploadCookie()
    {
    	String ret="";
    	try
    	{
	    	HttpClient client=new DefaultHttpClient();
	    	client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS,false);
	    	HttpResponse response = 
    		    Http.post("http://www.megaupload.com")
    		        .use(client)
    		        .data("login", "1")
    		        .data("redir", "1")
    		        .data("username", DownDroid.prefs.getMegaupload_user())
    		        .data("password", DownDroid.prefs.getMegaupload_password())
    		        .asResponse();
	    	ret=((Header)response.getHeaders("Set-Cookie")[0]).getValue();
    		ret=ret.substring(ret.indexOf("=")+1, ret.indexOf(";"));    		
    	}
    	catch (Exception ex)
    	{
    		Log.e("DownDroid", "(Megaupload) Cookie failed");
    	}
    	return ret;
    }    
    
    public String getMegauploadURL(String url, String cookie)
    {
    	String ret="";
    	HttpResponse response=null;
    	while (ret.length()==0)
    	{
	    	try
	    	{
	    		HttpClient client=new DefaultHttpClient();
	    		if (cookie==null || cookie.trim().length()==0)
	    		{
	    			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS,true);
	    			response = 
		    		    Http.get(url)
		    		        .use(client)
		    		        .asResponse();
	    			Thread.sleep(45000);
			    	ret=Http.asString(response, "utf-8");
			    	ret=ret.substring(ret.indexOf("downloadlink")+23);
			    	ret=ret.substring(0, ret.indexOf("\""));
	    		}
	    		else
	    		{	    				    				    	
			    	client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS,false);
			    	response = 
		    		    Http.get(url)
		    		        .use(client)
		    		        .header("Cookie","l=es; user="+cookie+"; __utma=216392970.1949298155801896700.1244535146.1247056407.1258354625.5;	__utmb=216392970.1.10.1258354625;	__utmc=216392970;	__utmz=216392970.1247041692.3.2.utmcsr=vagos.wamba.com|utmccn=(referral)|utmcmd=referral|utmcct=/showthread.php")
		    		        .asResponse();
			    	if (response.containsHeader("location"))
			    		ret=((Header)response.getHeaders("location")[0]).getValue();
			    	else
			    	{
			    		ret=Http.asString(response, "utf-8");
				    	ret=ret.substring(ret.indexOf("down_ad_pad1")+37);
				    	ret=ret.substring(0, ret.indexOf("\""));
			    	}
	    		}
	    	}
	    	catch (Exception ex)
	    	{
	    		try
	    		{
	    			ret=((Header)response.getHeaders("Location")[0]).getValue();
	    		}
	    		catch (Exception ex2)
	    		{
	    			Log.e("DownDroid", "(Megaupload) Download url failed");
	    		}
	    	}
    	}
    	return ret;
    }
    
    // Download file.
    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        String cookie="";
        
        try {
        	if (launchTime==0)
        		launchTime=Calendar.getInstance().getTimeInMillis();
            // Open connection to URL.
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            
            connection.setInstanceFollowRedirects(false);
            
            if (url.getHost().indexOf("megaupload")>=0)
            {
            	if (DownDroid.prefs.megaupload_user!=null && DownDroid.prefs.megaupload_user.trim().length()>0)
	            	cookie=getMegauploadCookie();	            		            	
            	if (cookie.length()!=0)            	
            		connection.setRequestProperty("Cookie","l=es; user="+cookie+"; __utma=216392970.1949298155801896700.1244535146.1247056407.1258354625.5;	__utmb=216392970.1.10.1258354625;	__utmc=216392970;	__utmz=216392970.1247041692.3.2.utmcsr=vagos.wamba.com|utmccn=(referral)|utmcmd=referral|utmcct=/showthread.php");          	
            }
            
            if (status<ERROR)
            {
	            // Specify what portion of file to download.
	            connection.setRequestProperty("Range",
	                    "bytes=" + downloaded + "-");
	            
	            // Connect to server.
	            connection.connect();
	            
	            // Check for valid content length.
	            int contentLength = connection.getContentLength();
	            if (contentLength < 1 || (connection.getResponseMessage().equalsIgnoreCase("OK") && url.getHost().indexOf("rapidshare")>=0)) {
	            	if (url.getHost().indexOf("megaupload")>=0)
	            	{	            		
            			while (contentLength < 1)
            			{
            				String newURL=getMegauploadURL(url.toString(),cookie);
            				Log.e("DownDroid", newURL);
            				url=new URL(newURL);            				            				
            				connection=(HttpURLConnection)url.openConnection();
            				contentLength = connection.getContentLength();
            				Log.e("DownDroid", String.valueOf(contentLength));
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