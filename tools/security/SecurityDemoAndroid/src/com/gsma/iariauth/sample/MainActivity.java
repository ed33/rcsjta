package com.gsma.iariauth.sample;

import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.gsma.iariauth.validator.PackageProcessor;
import com.gsma.iariauth.validator.ProcessingResult;

public class MainActivity extends Activity {

	private static final String FINGER_PRINT = "C0:23:13:91:C0:46:FB:39:E9:86:8B:E6:4E:4C:B0:E6:57:65:7E:43";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TextView result = (TextView) findViewById(R.id.result);

		boolean isValid = isExtensionAuthorized(this, "com.gsma.iariauth.sample", FINGER_PRINT);
		result.setText(isValid+"");
	}


	private boolean isExtensionAuthorized(Context context, String pkgName, String sha1Sign) {
		try {
			InputStream iariDocStream = getAssets().open("iari-range-test.xml");
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
