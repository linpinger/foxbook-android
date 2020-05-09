package com.linpinger.misc;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BackHandledFragment extends Fragment {
	protected BackHandledInterface mBackHandledInterface;

	// 所有继承BackHandledFragment的子类都将在这个方法中实现物理Back键按下后的逻辑
	public boolean onBackPressed(){
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!(getActivity() instanceof BackHandledInterface)) {
			throw new ClassCastException("Hosting Activity must implement BackHandledInterface");
		} else {
			this.mBackHandledInterface = (BackHandledInterface) getActivity();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		mBackHandledInterface.setSelectedFragment(this);
	}

	public void back() { // foxAdd: 返回
		mBackHandledInterface.setSelectedFragment(null);
		getActivity().onBackPressed();
	}
	public void startFragment(Fragment fragmt) {
//		getFragmentManager().beginTransaction().replace(android.R.id.content, fragmt).addToBackStack(null).commit();
		getFragmentManager().beginTransaction().hide(this).add(android.R.id.content, fragmt).addToBackStack(null).commit(); // Fragment里面启动Fragment用这个
	} // 返回功能调用Activity的onBackPressed: getActivity().onBackPressed();
}
