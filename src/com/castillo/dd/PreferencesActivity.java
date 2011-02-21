package com.castillo.dd;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PreferencesActivity extends Activity
{
	public static Preferences prefs;
	
	/** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        
        prefs = loadPreferences();
        
        LinearLayout ll0=new LinearLayout(getApplicationContext());
        ll0.setOrientation(LinearLayout.VERTICAL);
        
        ImageView iv=new ImageView(getApplicationContext());
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.title));        
        
        LinearLayout ll=new LinearLayout(getApplicationContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        
        ll0.addView(iv);
        
        ll0.addView(ll);
        
        TextView titlePreferences=new TextView(getApplicationContext());
		titlePreferences.setText(getResources().getText(R.string.preferences));
		titlePreferences.setTextColor(Color.WHITE);
		titlePreferences.setTextSize(18);
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		ll.addView(titlePreferences,params);
		
		TextView lmegauploaduser=new TextView(getApplicationContext());
		lmegauploaduser.setText(getResources().getString(R.string.megaupload_user));
		EditText tmegauploaduser=new EditText(getApplicationContext());
		tmegauploaduser.setTag("megauploaduser");
		ll.addView(lmegauploaduser,params);
		ll.addView(tmegauploaduser,params);	    
		tmegauploaduser.setText(prefs.getMegaupload_user());
		TextView lmegauploadpassword=new TextView(getApplicationContext());
		lmegauploadpassword.setText(getResources().getString(R.string.megaupload_password));
		EditText tmegauploadpassword=new EditText(getApplicationContext());
		tmegauploadpassword.setText(prefs.getMegaupload_password());
		tmegauploadpassword.setTag("megauploadpassword");
		ll.addView(lmegauploadpassword,params);
		ll.addView(tmegauploadpassword,params);
		
		Button bSave=new Button(getApplicationContext());
		bSave.setText(getResources().getString(R.string.save_preferences));	    
		bSave.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {			
				Preferences preferences=new Preferences();
				LinearLayout ll=(LinearLayout)v.getParent().getParent();
				preferences.setMegaupload_user(((TextView)ll.findViewWithTag("megauploaduser")).getText().toString());
				preferences.setMegaupload_password(((TextView)ll.findViewWithTag("megauploadpassword")).getText().toString());
				savePreferences(preferences);
				finish();
			}
		});
		LinearLayout llButtons=new LinearLayout(getApplicationContext());
		llButtons.addView(bSave,params);
		ll.addView(llButtons);
        
		setContentView(ll0);        
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
}
