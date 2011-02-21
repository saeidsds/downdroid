package com.castillo.dd;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;

import android.app.Activity;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdView;

import de.mastacode.http.Http;

public class DownDroid extends ListActivity
{
	private ArrayList<PendingDownload> m_options = new ArrayList<PendingDownload>();
	private DownloadListAdapter m_adapter;
	
    public static final String PREFERENCES = "DownDroid_preferences";
	private static final int PROGRESS = 0xDEADBEEF;
	protected int mProgress;
	protected boolean mCancelled;
	int download_index=0;
	protected TextView tvHelp;
	
	private DSInterface dsInterface;
	
	NotificationManager notificationManager;
	
	public static Preferences prefs; 
	
	static final private int PASTE = Menu.FIRST;
	static final private int ADD = Menu.FIRST + 1;
	static final private int CLEAR = Menu.FIRST + 2;
	static final private int PAUSE = Menu.FIRST + 3;
	static final private int RESUME = Menu.FIRST + 4;	
	static final private int SETTINGS = Menu.FIRST + 5;	
    
	protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if ((msg.what == PROGRESS) && (!mCancelled)) {
            	for (int i=0;i<m_options.size();i++)
            	{
            		if (i==download_index)
            		{
            			try
            			{
	            			int status=dsInterface.getDownloadStatus(i);
	            			m_options.get(i).setStatus(status);
	            			m_options.get(i).setFilename(dsInterface.getDownloadFilename(i));
	            			if (status==Download.START)
	            				dsInterface.downloadFile(i);
	            			else if (status==Download.COMPLETE)// || status>=Download.ERROR)
	            			{
	            				download_index++;
	            				if (download_index>=m_options.size() || m_options.size()==0)
	            				{
	            	            	notificationManager.cancel(1);
	            	            	notification(getResources().getString(R.string.notification_finished),getResources().getString(R.string.notification_finished_desc));
	            	            	notificationManager.cancel(1);
	            				}
	            			}
	            			m_options.get(i).setLaunchTime(dsInterface.getDownloadLaunchTime(download_index));
	            			if (status!=Download.COMPLETE)
	            			{
	            				m_options.get(i).setProgress(dsInterface.getDownloadProgress(download_index));
	            				m_options.get(i).setEllapsedTime(dsInterface.getDownloadEllapsedTime(download_index));
	            				m_options.get(i).setRemainingTime(dsInterface.getDownloadRemainingTime(download_index));
	            				m_options.get(i).setSpeed(dsInterface.getDownloadSpeed(download_index));	            				
	            			}
	            			else
	            				m_options.get(i).setProgress(100);
            			}
            			catch (Exception e)
            			{
            				Log.e(getString(R.string.app_name), Log.getStackTraceString(e));
            			}
            		}
            	}            	
            	m_adapter.notifyDataSetChanged(); 
            	sendMessageDelayed(obtainMessage(PROGRESS), 50);
            }            
        }       
    };
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            dsInterface = DSInterface.Stub.asInterface((IBinder)service);           
        }
     
        public void onServiceDisconnected(ComponentName className) {
            dsInterface = null;
        }
    };
	
    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        
        ensureUi();
        
        this.bindService(new Intent(DownDroid.this,DownloadService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        
        new File("/sdcard/dd").mkdirs();        
                               
        mHandler.sendMessage(mHandler.obtainMessage(PROGRESS));
        
        LinearLayout ll0=(LinearLayout)findViewById(R.id.title);
        
        ImageView iv=new ImageView(getApplicationContext());
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.title));                
        ll0.addView(iv);
        
        AdView ad = new AdView(getApplicationContext());
        ad.setBackgroundColor(Color.BLACK);
        ad.setTextColor(Color.WHITE);
        ad.setVisibility(AdView.VISIBLE);
        ll0.addView(ad);
        
        String svcName = Context.NOTIFICATION_SERVICE;        
        notificationManager = (NotificationManager)getSystemService(svcName);
        
        prefs = loadPreferences();        
        
        LinearLayout ll1=(LinearLayout)findViewById(R.id.info);
        tvHelp=new TextView(getApplicationContext());
        tvHelp.setText(getResources().getText(R.string.welcome));
        tvHelp.setTextColor(Color.WHITE);
        tvHelp.setTextSize(14);        
        ll1.addView(tvHelp);
        
        new Handler().postDelayed(new Runnable() { 
            public void run() { 
                openOptionsMenu();                
            } 
        }, 1000); 
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
	    
	    MenuItem itemPaste = menu.add(0, PASTE, Menu.NONE,R.string.paste);
	    MenuItem itemAdd = menu.add(0, ADD, Menu.NONE,R.string.add);
	    MenuItem itemClear = menu.add(0, CLEAR, Menu.NONE,R.string.clear);
	    MenuItem itemPause = menu.add(0, PAUSE, Menu.NONE,R.string.pause);
	    MenuItem itemResume = menu.add(0, RESUME, Menu.NONE,R.string.resume);	    
	    MenuItem itemSettings = menu.add(0, SETTINGS, Menu.NONE,R.string.settings);
	    
	    itemPaste.setIcon(R.drawable.paste);	    
	    itemAdd.setIcon(R.drawable.add);
	    itemClear.setIcon(R.drawable.clear);
	    itemPause.setIcon(R.drawable.pause);
	    itemResume.setIcon(R.drawable.resume);	    
	    itemSettings.setIcon(R.drawable.settings);
	    
	    return true;	    
    }
    
    private void ensureUi() {
        this.m_adapter = new DownloadListAdapter(this, R.layout.generic_list_item, m_options);
        ListView listView = getListView();
        listView.setAdapter(this.m_adapter);
        listView.setSmoothScrollbarEnabled(true);               
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {
    	String url="";
    	 super.onActivityResult(requestCode, resultCode, intent); 
    	  switch(requestCode) { 
    	    case (99) :  
    	      if (resultCode == Activity.RESULT_OK) { 
    	      url = intent.getStringExtra("url");
    	      try
    	      {
    	    	  if (url!=null && url.startsWith("http"))
    	    	  {
    	    		  LinearLayout ll1=(LinearLayout)findViewById(R.id.info);
	    	    	  ll1.removeAllViews();
	    	    	  if (m_options.size()==0)
	    	    	  {
	    	    		  TextView titlePreferences=new TextView(getApplicationContext());
	    	    		  titlePreferences.setText(getResources().getString(R.string.download_list));
	    	    		  titlePreferences.setTextColor(Color.WHITE);
	    	    		  titlePreferences.setTextSize(18);
	    	    		  LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    	    		  ll1.addView(titlePreferences,params);
	    	    	  }
	    	    	  int i=dsInterface.getDownloadlistSize();
	    	    	  if (i==0)
	    	    		  i++;
	    	    	  dsInterface.addFileDownloadlist(url, i);
	    	    	  PendingDownload pd=new PendingDownload();
	    	    	  pd.setUrl(url);
	    	    	  m_options.add(pd);	
	    	    	  m_adapter.notifyDataSetChanged();
		    	      notification(getResources().getString(R.string.notification_started),getResources().getString(R.string.notification_started_desc));
    	    	  }
    	    	  else
    	    		  Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalid_link), Toast.LENGTH_LONG).show();
    	      }
    	      catch (Exception ex)
    	      {
    	    	  ex.printStackTrace();
    	      }    	      
    	      break; 
    	    } 
    	  }      
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    super.onOptionsItemSelected(item);
	    int i;
	    switch (item.getItemId())
	    {
	    	case PASTE :	    		
	    		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            	String clipboardText=clipboard.getText().toString();
            	StringTokenizer st=new StringTokenizer(clipboardText, " ");
            	if (st.hasMoreElements() && clipboardText.indexOf("http")>=0)
            	{
            		LinearLayout ll1=(LinearLayout)findViewById(R.id.info);
	    	    	ll1.removeAllViews();
		    		if (m_options.size()==0)
		    		{
		    			TextView titlePreferences=new TextView(getApplicationContext());
		    			titlePreferences.setText(getResources().getString(R.string.download_list));
		    			titlePreferences.setTextColor(Color.WHITE);
		    			titlePreferences.setTextSize(18);
		    			LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		    			ll1.addView(titlePreferences,params);
		    		}		    		
		    		i=0;
		    		try
		    		{
		    			i=dsInterface.getDownloadlistSize()+1;
		    		}
		    		catch (Exception e)
		    		{
		    			Log.e(getString(R.string.app_name), e.getMessage());
		    		}            	
	            	while (st.hasMoreElements())
	            	{
	            		String url=(String)st.nextElement();
	            		if (url!=null)
	            			url=url.trim();
	            		if (url.toLowerCase().startsWith("http"))
	            		{
	            			try
	            			{
	            				dsInterface.addFileDownloadlist(url, i);
	            				PendingDownload pd=new PendingDownload();
	            				pd.setUrl(url);
		    	    	        m_options.add(pd);		    	    	        
	            			}
	            			catch (Exception ex)
	            			{
	            				ex.printStackTrace();
	            			}
	            		}
	            	}
	            	notification(getResources().getString(R.string.notification_started),getResources().getString(R.string.notification_started_desc));
	            	m_adapter.notifyDataSetChanged();
            	}
            	else
            		Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_links_found_on_clipboard), Toast.LENGTH_LONG).show();
	    		break;
	    	case ADD :
	    		Intent addIntent = new Intent(getApplicationContext(), AddDownloadActivity.class);
	    		startActivityForResult(addIntent,99);
	    		break;
	    	case PAUSE :
	    		try
	    		{
	    			dsInterface.pause();
	    		}
	    		catch (Exception e)
	    		{
	    			Log.e(getString(R.string.app_name), e.getMessage());
	    		}
	    		break;
	    	case RESUME :
	    		try
	    		{
	    			dsInterface.resume();
	    		}
	    		catch (Exception e)
	    		{
	    			Log.e(getString(R.string.app_name), e.getMessage());
	    		}	    		
	    		break;
	    	case CLEAR :
	    		try
	    		{
	    			m_options.clear();
	    			m_adapter.notifyDataSetChanged();
	    			dsInterface.clearDownloadlist();
	    			LinearLayout ll1=(LinearLayout)findViewById(R.id.info);
	    	    	ll1.removeAllViews();                
	                download_index=0;
	                ll1.addView(tvHelp); 
	                notificationManager.cancel(1);            	
	                new Handler().postDelayed(new Runnable() { 
	                    public void run() { 
	                        openOptionsMenu(); 
	                    } 
	                }, 1000);
	    		}
	    		catch (Exception e)
	    		{
	    			Log.e(getString(R.string.app_name), e.getMessage());
	    		}	    		 
	    		break;
	    	case SETTINGS :
	    		  Intent settingsIntent = new Intent(getApplicationContext(), PreferencesActivity.class);
	    		  DownDroid.this.startActivity(settingsIntent);
	    		break;
	    }
	    return true;
    }
    
    protected void notification(String tickerText, String expandedText)
    {
    	int icon = R.drawable.icon;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        Context context = getApplicationContext();
        String expandedTitle = getResources().getString(R.string.notification_title);
     	Intent intent = new Intent(this, DownDroid.class);
     	PendingIntent launchIntent = PendingIntent.getActivity(context, 0, intent, 0);
     	notification.setLatestEventInfo(context,expandedTitle,expandedText,launchIntent);
     	int notificationRef = 1;
     	notificationManager.notify(notificationRef, notification);
    }
    
    public Preferences loadPreferences()
    {
    	int mode = Activity.MODE_PRIVATE;
    	SharedPreferences mySharedPreferences = getSharedPreferences(DownDroid.PREFERENCES,mode);
    	Preferences p=new Preferences();
    	p.setMegaupload_user(mySharedPreferences.getString("megaupload_user", null));
    	p.setMegaupload_password(mySharedPreferences.getString("megaupload_password", null));    	
    	return p;
    }
    
    public void savePreferences(Preferences preferences)
    {
    	int mode = Activity.MODE_PRIVATE;
    	SharedPreferences mySharedPreferences = getSharedPreferences(DownDroid.PREFERENCES,mode);
    	SharedPreferences.Editor editor = mySharedPreferences.edit();
    	editor.putString("megaupload_user", preferences.getMegaupload_user());
    	editor.putString("megaupload_password", preferences.getMegaupload_password());
    	editor.commit();
    	DownDroid.prefs=preferences;
    }
    
    // Methods to test the megaupload connection with this cookie    
    private void testMegauploadConnection(String cookie)
    {
    	String surl=getMegauploadURL("http://www.megaupload.com/?d=6Y5JD5FG", cookie);
    	downloadFromUrl(surl,"/sdcard/dd/OK.txt");
    	File f=new File("/sdcard/dd/OK.txt");
    	f.delete();
    }
    
    private void downloadFromUrl(String surl, String fileName) {
        try {
                URL url = new URL(surl);
                File file = new File(fileName);
                URLConnection ucon = url.openConnection();
                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                ByteArrayBuffer baf = new ByteArrayBuffer(50);
                int current = 0;
                while ((current = bis.read()) != -1) {
                        baf.append((byte) current);
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(baf.toByteArray());
                fos.close();                

        } catch (IOException e) {
                Log.e("DownDroid", "testMegauploadConnection DownloadFromUrl failed");
        }
    }
    
    private String getMegauploadCookie()
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
    		Log.e("DownDroid", "testMegauploadConnection getMegauploadCookie failed");
    	}
    	return ret;
    }    
    
    private String getMegauploadURL(String url, String cookie)
    {
    	String ret="";
    	HttpResponse response=null;
    	while (ret.length()==0)
    	{
	    	try
	    	{
	    		HttpClient client=new DefaultHttpClient();
    			client.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS,false);
		    	response = 
	    		    Http.get(url)
	    		        .use(client)
	    		        .header("Cookie","l=es; user="+cookie+"; __utma=216392970.1949298155801896700.1244535146.1247056407.1258354625.5;	__utmb=216392970.1.10.1258354625;	__utmc=216392970;	__utmz=216392970.1247041692.3.2.utmcsr=vagos.wamba.com|utmccn=(referral)|utmcmd=referral|utmcct=/showthread.php")
	    		        .asResponse();
		    	ret=((Header)response.getHeaders("location")[0]).getValue();    			    		
	    	}
	    	catch (Exception ex)
	    	{
	    		try
	    		{
	    			ret=((Header)response.getHeaders("Location")[0]).getValue();
	    		}
	    		catch (Exception ex2)
	    		{
	    			Log.e("DownDroid", "testMegauploadConnection getMegauploadURL failed");
	    		}
	    	}
    	}
    	return ret;
    }
}
