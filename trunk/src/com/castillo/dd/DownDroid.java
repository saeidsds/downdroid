package com.castillo.dd;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class DownDroid extends Activity
{
	final public ArrayList<ARProgressBar> aps=new ArrayList<ARProgressBar>();
	
    public static final String PREFERENCES = "DownDroid_preferences";
	private static final int PROGRESS = 0xDEADBEEF;
	protected int mProgress;
	protected boolean mCancelled;
	int download_index=0;
	LinearLayout ll;		
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
            	for (int i=0;i<aps.size();i++)
            	{
            		if (i==download_index)
            		{
            			try
            			{
	            			ARProgressBar ap=aps.get(i);
	            			int status=dsInterface.getDownloadStatus(i);
	            			if (status==Download.START)
	            				dsInterface.downloadFile(i);
	            			else if (status==Download.COMPLETE || status>=Download.ERROR)
	            			{
	            				ap.invalidate();
	            				download_index++;
	            				if (download_index>=aps.size() || aps.size()==0)
	            				{
	            	            	notificationManager.cancel(1);
	            	            	notification(getResources().getString(R.string.notification_finished),getResources().getString(R.string.notification_finished_desc));
	            	            	notificationManager.cancel(1);
	            				}
	            			}
	            			
	            			if (status!=Download.COMPLETE)
	            			{
	            				try
	            				{
	            					ap.setPosition(dsInterface.getDownloadProgress(download_index));
	            				}
	            				catch (Exception ex){}
	            			}
	            			else
	            				ap.setPosition(100);
            			}
            			catch (Exception e)
            			{
            				Log.e(getString(R.string.app_name), Log.getStackTraceString(e));
            			}
            		}
            	}
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
        
        this.bindService(new Intent(DownDroid.this,DownloadService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        
        new File("/sdcard/dd").mkdirs();        
        
        LinearLayout ll0=new LinearLayout(getApplicationContext());
        ll0.setOrientation(LinearLayout.VERTICAL);
        
        ll=new LinearLayout(getApplicationContext());        
        ll.setOrientation(LinearLayout.VERTICAL);
        mHandler.sendMessage(mHandler.obtainMessage(PROGRESS));
        
        ImageView iv=new ImageView(getApplicationContext());
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.title));
                
        ll0.addView(iv);
        
        ll0.addView(ll);
        
        String svcName = Context.NOTIFICATION_SERVICE;        
        notificationManager = (NotificationManager)getSystemService(svcName);
        
        prefs = loadPreferences();        
        
        tvHelp=new TextView(getApplicationContext());
        tvHelp.setText(getResources().getText(R.string.welcome));
        tvHelp.setTextColor(Color.WHITE);
        tvHelp.setTextSize(14);        
        ll.addView(tvHelp);      
        
        ScrollView sv=new ScrollView(getApplicationContext());
        sv.addView(ll0);
        setContentView(sv);
        
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
	    	    	  ll.removeView(tvHelp);
	    	    	  if (aps.size()==0)
	    	    	  {
	    	    		  TextView titlePreferences=new TextView(getApplicationContext());
	    	    		  titlePreferences.setText(getResources().getString(R.string.download_list));
	    	    		  titlePreferences.setTextColor(Color.WHITE);
	    	    		  titlePreferences.setTextSize(18);
	    	    		  LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    	    		  ll.addView(titlePreferences,params);
	    	    	  }
	    	    	  int i=dsInterface.getDownloadlistSize();
	    	    	  if (i==0)
	    	    		  i++;
	    	    	  dsInterface.addFileDownloadlist(url, i);
	    	    	  ARProgressBar ap = (ARProgressBar) new ARProgressBar(getApplicationContext(),dsInterface,i);	        	
	    	    	  ll.addView(ap, 320, 200);
		    	      aps.add(ap);		    	      	    	    	 
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
            		ll.removeView(tvHelp);
		    		if (aps.size()==0)
		    		{
		    			TextView titlePreferences=new TextView(getApplicationContext());
		    			titlePreferences.setText(getResources().getString(R.string.download_list));
		    			titlePreferences.setTextColor(Color.WHITE);
		    			titlePreferences.setTextSize(18);
		    			LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		    			ll.addView(titlePreferences,params);
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
	            				ARProgressBar ap = (ARProgressBar) new ARProgressBar(getApplicationContext(),dsInterface,i++);
		    	    	        ll.addView(ap, 320, 200);
		    	    	        aps.add(ap);
	            			}
	            			catch (Exception ex)
	            			{
	            				ex.printStackTrace();
	            			}
	            		}
	            	}
	            	notification(getResources().getString(R.string.notification_started),getResources().getString(R.string.notification_started_desc));
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
	    			while (aps.size()>0)
	    				aps.remove(0);
	    			dsInterface.clearDownloadlist();
	                ll.removeAllViews();                
	                download_index=0;
	                ll.addView(tvHelp); 
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
    	p.setRapidshare_user(mySharedPreferences.getString("rapidshare_user", null));
    	p.setRapidshare_password(mySharedPreferences.getString("rapidshare_password", null));
    	return p;
    }
    
    public void savePreferences(Preferences preferences)
    {
    	int mode = Activity.MODE_PRIVATE;
    	SharedPreferences mySharedPreferences = getSharedPreferences(DownDroid.PREFERENCES,mode);
    	SharedPreferences.Editor editor = mySharedPreferences.edit();
    	editor.putString("megaupload_user", preferences.getMegaupload_user());
    	editor.putString("megaupload_password", preferences.getMegaupload_password());
    	editor.putString("rapidshare_user", preferences.getRapidshare_user());
    	editor.putString("rapidshare_password", preferences.getRapidshare_password());
    	editor.commit();
    	DownDroid.prefs=preferences;
    }
}
