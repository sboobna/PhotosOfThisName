package com.ssb2171.photosofthisname;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ShowResultActivity extends Activity {
	String name;
	public static ArrayList<Photo> al = new ArrayList<Photo>();    //it contains the parsed xml data
	public static ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_result);
		Bundle contactName = getIntent().getExtras();    //gets the contact name from the previous view
		if (contactName != null) {
			name = contactName.getString("contactName");  
		}
		if (name.contains(" ")) {
			name = name.replace(" ", "%20");    //if the name contains spaces, it replaces it with %20
		}
		listView = (ListView) findViewById(R.id.displayResult);
		al.clear();
		ProgressDialog dialog = new ProgressDialog(ShowResultActivity.this);
		TalkToServer task = new TalkToServer();      //creates an Object of the TalkToServer class
		task.progressDialog = dialog;             //the process dialog is attached to the task object
		task.execute(name);                 //executes the doInBackground method of TalkToServer class in background

	}

	public void displayFeed() {

		if (al.size() == 0) {     //checks if the result is empty
			AlertDialog.Builder noResult = new AlertDialog.Builder(       //shows an alert box
					ShowResultActivity.this).setMessage("No Results Found")
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									onBackPressed();       //if OK pressed on alert, it goes back to main activity
								}
							});
			noResult.show();
		} else {
			listView.setAdapter(new MyAdapter(ShowResultActivity.this,
					R.layout.list_single, al));               //sets the adapter with list_single xml file

			listView.setOnItemClickListener(new OnItemClickListener() {     //sets an on click listener for each row

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					Intent openBrowser = new Intent(Intent.ACTION_VIEW, Uri
							.parse(al.get(position).photo_url_browser));       //displays an enlarged view of the image in the browser
					startActivity(openBrowser);
				}
			});
		}

	}

	private Bitmap loadImageFromNetwork(String url) {        //loads the image from the network in bitmap
		try {
			Bitmap bitmap = BitmapFactory.decodeStream((InputStream) new URL(
					url).getContent());
			return bitmap;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static class MyAdapter extends ArrayAdapter<Photo> {         //the adapter class that gets set to the list row

		ArrayList<Photo> flickr_result = new ArrayList<Photo>();

		public MyAdapter(Context context, int resource,
				ArrayList<Photo> flickr_result) {
			super(context, resource, flickr_result);
			this.flickr_result = flickr_result;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater layoutInflater = ((Activity) super.getContext())
						.getLayoutInflater();
				convertView = layoutInflater
						.inflate(R.layout.list_single, null);
			}
			TextView textView = (TextView) convertView
					.findViewById(R.id.ownername);

			textView.setText(flickr_result.get(position).owner_name);      //sets the owner name

			textView = (TextView) convertView.findViewById(R.id.datetaken);
			textView.setText(flickr_result.get(position).date_taken);        //sets the date taken of the photo

			textView = (TextView) convertView.findViewById(R.id.description);
			textView.setText(flickr_result.get(position).description);       //sets the description
			ImageView imageView = (ImageView) convertView
					.findViewById(R.id.img);
			ShowResultActivity getImage = new ShowResultActivity();
			try {
				Bitmap imageBitmap = getImage.new DownloadImageFromNetworkTask()
						.execute(flickr_result.get(position).photo_url).get();        //gets the photo url and calls a method in background
				if (imageBitmap != null) {
					imageView.setImageBitmap(imageBitmap);     //sets the image if it is not null
				} else {
					imageView.setImageResource(R.drawable.no_photo_available);     //sets a "no photo available" image
				}

			} catch (InterruptedException e) {

				e.printStackTrace();
			} catch (ExecutionException e) {

				e.printStackTrace();
			}

			return convertView;
		}
	}

	private class DownloadImageFromNetworkTask extends
			AsyncTask<String, Void, Bitmap> {               //downloads the image in background

		@Override
		protected Bitmap doInBackground(String... params) {
			if (params.length > 0)

				return loadImageFromNetwork(params[0]);
			return null;
		}

	}

	class TalkToServer extends AsyncTask<String, Void, Void> {
		String my_key = "893dcbfc83fe683f70b5f1899496d722";      
		public ProgressDialog progressDialog;

		@Override
		protected Void doInBackground(String... params) {

			String query = "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key="
					+ my_key
					+ "&tags="
					+ params[0]
					+ "&extras=date_taken,owner_name,description";         //rest api to query flickr
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(query);

			try {
				HttpResponse httpResponse = httpclient.execute(httpget);
				HttpEntity httpEntity = httpResponse.getEntity();
				if (httpEntity != null) {
					InputStream inputStream = httpEntity.getContent();
					XmlPullParser parser = Xml.newPullParser();         //it is used to parse the xml result
					parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
							false);
					parser.setInput(inputStream, null);
					parser.nextTag();

					int eventType = parser.next();

					Photo photo = new Photo();             //creates an object of the Photo class
					while (eventType != XmlPullParser.END_DOCUMENT
							&& ShowResultActivity.al.size() <= 30) {       //while loop till end of document or till it gets 30 result
						if (eventType == XmlPullParser.START_TAG
								&& (parser.getName().equals("photo"))) {      //true if it finds the photo tag that contains the details
							
							String photo_id, server_id, farm_id, secret;
							photo_id = parser.getAttributeValue(null, "id")
									.toString();                              //gets the photo id

							server_id = parser
									.getAttributeValue(null, "server")
									.toString();                                   //gets the server id

							farm_id = parser.getAttributeValue(null, "farm")
									.toString();                                 //gets the farm id

							secret = parser.getAttributeValue(null, "secret")
									.toString();                                   //gets the secret id
							photo.photo_url = "http://farm" + farm_id
									+ ".staticflickr.com/" + server_id + "/"
									+ photo_id + "_" + secret + "_s.jpg";        //forms the url to fetch the photo
							String date[] = parser
									.getAttributeValue(null, "datetaken")
									.toString().split(" ");            //gets the date taken of the photo and removes the time
							photo.date_taken = date[0];
							photo.photo_url_browser = "http://farm" + farm_id
									+ ".staticflickr.com/" + server_id + "/"
									+ photo_id + "_" + secret + "_c.jpg";     //forms the url to fetch the enlarged photo
							photo.owner_name = parser.getAttributeValue(null,
									"ownername").toString();

						}
						if (eventType == XmlPullParser.START_TAG
								&& (parser.getName().equals("description"))) {		//checks if the start tag is description				
							eventType = parser.next();
							if (eventType == XmlPullParser.TEXT) {				//if the description tag contains text				
								photo.description = parser.getText();					//gets the description			
							}							
						}
						if (eventType == XmlPullParser.END_TAG
								&& (parser.getName().equals("photo"))) {       //check if it reaches the end tag of photo
							ShowResultActivity.al.add(photo);             //adds the photo details to the arraylist
							photo = new Photo();                //creates an object for the next photo
						}
						eventType = parser.next();
					}
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {              //displays the progress dialog while the photo details are fetched and parsed
			progressDialog.setMessage("Retrieving photos...");
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							onBackPressed();       //a user can cancel the current task and go back to the main activity
						}
					});
			progressDialog.show();
		}

		@Override
		protected void onPostExecute(Void result) {      //method called when the background process is complete
			progressDialog.dismiss();                 //the progress dialog is dismissed when the result is parsed
			displayFeed();
		}
	}

	class Photo {                                       //class that contains the details of each photo
		String date_taken = "", owner_name = "",
				description = "No Description Available", photo_url = "",
				photo_url_browser = "";
	}
}
