package com.github.marwinxxii.smsextractor;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.marwinxxii.smsextractor.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	static final Uri URI = Uri.parse("content://sms");
	static final String[] PROJECTION = new String[] { "address", "body" };
	static final String ORDER = "_id asc";
	private boolean extract = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		findViewById(R.id.extract).setOnClickListener(this);
	}

	public void onClick(View v) {
		String[] addresses = ((TextView) findViewById(R.id.number)).getText()
				.toString().split(",");
		new ExtractSMSTask().execute(addresses);
		extract = v.getId() == R.id.extract;
	}

	public class ExtractSMSTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... addresses) {
			ContentResolver resolver = getContentResolver();
			Cursor cursor;
			boolean outputAddress = addresses.length > 1;
			if (addresses.length != 0) {
				StringBuilder param = new StringBuilder();
				for (int i = 0; i < addresses.length; i++) {
					param.append('?');
					if (i != addresses.length - 1)
						param.append(',');
				}
				cursor = resolver.query(URI, PROJECTION, "address in (" + param
						+ ')', addresses, ORDER);
			} else {
				cursor = resolver.query(URI, PROJECTION, null, null, ORDER);
			}
			StringBuilder result = new StringBuilder();
			while (cursor.moveToNext()) {
				if (outputAddress) {
					result.append(cursor.getString(0)).append(":\n");
				}
				result.append(cursor.getString(1)).append("\n\n");
			}
			cursor.close();
			return result.toString();
		}

		@Override
		public void onPostExecute(String result) {
			if (extract) {
				File path = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());
				File output = new File(path, "smsextractor-" + timeStamp
						+ ".txt");
				try {
					path.mkdirs();
					FileOutputStream stream = new FileOutputStream(output);
					stream.write(result.getBytes());
					stream.close();
					Toast.makeText(MainActivity.this,
							getString(R.string.success), Toast.LENGTH_SHORT)
							.show();
				} catch (Exception e) {
					Toast.makeText(MainActivity.this,
							getString(R.string.error_file), Toast.LENGTH_SHORT)
							.show();
				}
			} else {
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, result);
				intent.putExtra(Intent.EXTRA_TITLE,
						getString(R.string.extracted_sms));
				startActivity(Intent.createChooser(intent,
						getString(R.string.select_target)));
			}
		}
	}
}
