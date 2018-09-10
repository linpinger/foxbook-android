package com.linpinger.foxbook;

import java.io.File;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.view.Window;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Activity_Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);

		Intent itt = getIntent();

//		Toast.makeText(this, "acc: " + itt.getAction()
//				+ "\nscheme:" + itt.getScheme()
//				+ "\ntype:" + itt.getType()
//				+ "\ndata:" + itt.getDataString()
//				, Toast.LENGTH_LONG).show();

		if ( null == itt.getData() ) { // main 无fml
			startFragment( new Fragment_BookList() ); // fml/db3
		} else {
			File inFile = new File( itt.getData().getPath() ); // 从intent获取txt/zip/epub路径
			if ( inFile.exists() ) {
				if ( inFile.getName().endsWith(".fml") || inFile.getName().endsWith(".db3") ) {
					startFragment( Fragment_BookList.newInstance( inFile.getPath() ) ); // fml/db3
				} else {
					startFragment( Fragment_EBook_Viewer.newInstance( inFile.getPath() ) );
				}
			}
		}

	} // onCreate 结束

	@Override
	public void onBackPressed() {
		int fmc = getFragmentManager().getBackStackEntryCount() ;
		if ( fmc == 1 ) {
			getFragmentManager().popBackStack();
			super.onBackPressed();
		} else if ( fmc > 1 ) {
			getFragmentManager().popBackStack();
		} else if ( fmc == 0 ) {
			super.onBackPressed();
		} else {
			System.exit(0);
		}
	}

	void startFragment(Fragment fragmt) {
		getFragmentManager().beginTransaction().replace(android.R.id.content, fragmt).addToBackStack(null).commit();
		//		getFragmentManager().beginTransaction().hide(this).add(android.R.id.content, fragmt).addToBackStack(null).commit(); // Fragment里面启动Fragment用这个
	} // 返回功能调用Activity的onBackPressed: getActivity().onBackPressed();

}
