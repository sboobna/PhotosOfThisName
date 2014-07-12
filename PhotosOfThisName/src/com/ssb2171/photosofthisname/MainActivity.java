package com.ssb2171.photosofthisname;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private static final int PICK_CONTACT = 1234;
	EditText name;   //TextField to enter name
	Button openContact, search;
	String contactName;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		name = (EditText) findViewById(R.id.inputName);
		openContact = (Button) findViewById(R.id.openContactButton);
		search = (Button) findViewById(R.id.searchButton);
		search.setEnabled(true);    //enables the button true
		openContact.setOnClickListener(this);
		search.setOnClickListener(this);
		if(!isNetworkConnected()) {      //checks if there is internet activated
			Toast noInternet = Toast.makeText(MainActivity.this, "No Internet Connection, please go back", Toast.LENGTH_LONG);
			noInternet.show();			
			search.setEnabled(false);    //disables the button if no internet
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == PICK_CONTACT) && (resultCode == RESULT_OK)) {
			Uri contactData = data.getData();
			Cursor c = getContentResolver().query(contactData, null, null,
					null, null);
			if (c.moveToFirst()) {
				contactName = c
						.getString(c
								.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));   //gets the contact name
				name.setText((CharSequence) contactName);    //sets the name to the text field
			}
		}
	}
	

	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.openContactButton:  //if the import contact button is clicked
			Intent it = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
			startActivityForResult(it, PICK_CONTACT);
			break;
		case R.id.searchButton:     //if the search button is clicked
			if(name.getText().toString().equals("")) {    //checks if the input name is empty
				Toast errorMessage = Toast.makeText(MainActivity.this, "Please enter a name", Toast.LENGTH_LONG);
				errorMessage.show();
			}
			else {
				Intent i = new Intent("com.ssb2171.photosofthisname.SHOWRESULTACTIVITY");
				i.putExtra("contactName", name.getText().toString());
				startActivity(i);     //starts the next activity
			}
			
			break;
		}

	}
	
	
	
	@Override
	protected void onResume() {
		
		super.onResume();
		search.setEnabled(true);    //enables the button if it had been disabled earlier
		if(!isNetworkConnected()) {
			Toast noInternet = Toast.makeText(MainActivity.this, "No Internet Connection, please go back", Toast.LENGTH_LONG);
			noInternet.show();			
			search.setEnabled(false);
		}
	}

	@Override
	public void onBackPressed() {    //overrides the method that gets called when back button is pressed
		name.setText("");
		moveTaskToBack(true);
	}
	
	private boolean isNetworkConnected() {    //method to check if internet activated
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) {
			  return false;
		  } else
			  return true;
	}
}