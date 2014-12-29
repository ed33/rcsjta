package com.gsma.iariauth0.sample;

import java.io.InputStream;

import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String FINGER_PRINT = "22:6A:91:00:5B:0D:C3:2B:07:62:5A:CA:EE:9C:2D:9A:D7:D3:4B:08";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TextView result = (TextView) findViewById(R.id.result);

		boolean isValid = isExtensionAuthorized(this, "com.gsma.iariauth0.sample", FINGER_PRINT);
		result.setText(isValid+"");
	}


	private boolean isExtensionAuthorized(Context context, String pkgName, String sha1Sign) {
		try {
			InputStream iariDocStream = getAssets().open("extension0.xml");
			BKSTrustStore trustStore = new BKSTrustStore(getAssets().open("range-root-truststore.bks"));

			// Checking procedure
			boolean authorized = false;
			Log.d(TAG, "Application fingerprint: " + sha1Sign);
			Log.w(TAG, "Package name '" + pkgName+"'");
			PackageProcessor processor = new PackageProcessor(trustStore, pkgName, sha1Sign);
			ProcessingResult result = processor.processIARIauthorization(iariDocStream);
			if (result.getStatus() == ProcessingResult.STATUS_OK) {
				authorized = true;
				Log.d(TAG, "Extension is authorized");
			}
			else {
				Log.d(TAG, "Extension is not authorized: " + result.getStatus() + " " + result.getError().toString());
			}
			return authorized;

		}
		catch (Exception e) {
			Log.e(TAG, "Internal exception", e);
			return false;
		}
	}

	private static final String TAG = MainActivity.class.getName();

}
