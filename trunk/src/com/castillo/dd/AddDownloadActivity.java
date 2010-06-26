package com.castillo.dd;

import android.app.Activity;
import android.content.Intent;
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

public class AddDownloadActivity extends Activity
{
	/** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        
        LinearLayout ll0=new LinearLayout(getApplicationContext());
        ll0.setOrientation(LinearLayout.VERTICAL);
        
        ImageView iv=new ImageView(getApplicationContext());
        iv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.title));        
        
        LinearLayout ll=new LinearLayout(getApplicationContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        
        ll0.addView(iv);
        
        ll0.addView(ll);
        
        TextView titlePreferences=new TextView(getApplicationContext());
		titlePreferences.setText(getResources().getString(R.string.add_download));
		titlePreferences.setTextColor(Color.WHITE);
		titlePreferences.setTextSize(18);
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		ll.addView(titlePreferences,params);
		
		TextView lurl=new TextView(getApplicationContext());
		lurl.setText("URL : ");
		EditText turl=new EditText(getApplicationContext());
		turl.setTag("url");
		turl.setTextSize(14);
		turl.setText("http://");
		ll.addView(lurl,params);
		ll.addView(turl,params);	    
		Button bSave=new Button(getApplicationContext());
		bSave.setText(getResources().getString(R.string.add_to_download_list));	    
		bSave.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {			
				Intent i = new Intent(v.getContext(), DownDroid.class);
				LinearLayout ll=(LinearLayout)v.getParent().getParent();
			    i.putExtra("url", ((TextView)ll.findViewWithTag("url")).getText().toString()); 
			    setResult(RESULT_OK, i); 
				finish();
			}
		});
		LinearLayout llButtons=new LinearLayout(getApplicationContext());
		llButtons.addView(bSave,params);
		ll.addView(llButtons);
        
		setContentView(ll0);        
    }
    
}
