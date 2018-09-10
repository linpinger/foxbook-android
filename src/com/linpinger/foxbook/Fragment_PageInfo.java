package com.linpinger.foxbook;

import java.util.Map;

import com.linpinger.novel.NV;
import com.linpinger.novel.NovelManager;
import com.linpinger.tool.ToolAndroid;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Fragment_PageInfo extends Fragment {

	public static Fragment_PageInfo newInstance(NovelManager nvMgr, int inBookIDX, int inPageIDX) {
		Fragment_PageInfo fc = new Fragment_PageInfo();

		fc.nm = nvMgr;
		Bundle bd = new Bundle();
		bd.putInt(NV.BookIDX, inBookIDX);
		bd.putInt(NV.PageIDX, inPageIDX);

		fc.setArguments(bd);
		return fc;
	}

	private void init_controls(View v) { // 初始化各控件
		tv         = (TextView) v.findViewById(R.id.testTV);
		pi_bid     = (TextView) v.findViewById(R.id.pi_bid);
		pi_pid     = (TextView) v.findViewById(R.id.pi_pid);
		pi_pname   = (EditText) v.findViewById(R.id.pi_pname);
		pi_purl    = (EditText) v.findViewById(R.id.pi_purl);
		pi_content = (EditText) v.findViewById(R.id.pi_content);

		onViewClickListener cl = new onViewClickListener();
		tv.setOnClickListener(cl);
		v.findViewById(R.id.btnSave).setOnClickListener(cl);
		v.findViewById(R.id.btnCopyFocus).setOnClickListener(cl);
		v.findViewById(R.id.btnPasteFocus).setOnClickListener(cl);
		v.findViewById(R.id.btnClearContent).setOnClickListener(cl);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ctx = container.getContext();
		View v = inflater.inflate(R.layout.fragment_pageinfo, container, false); // 这个false很重要，不然会崩溃

		if ( ToolAndroid.isEink() ) {
			v.setBackgroundColor(Color.WHITE);
		} else {
			v.setBackgroundResource(R.drawable.background_color);
		}

		init_controls(v) ; // 初始化各控件

		Bundle bd = getArguments();
		// 通过intent获取数据
		bookIDX = bd.getInt(NV.BookIDX, -1);
		pageIDX = bd.getInt(NV.PageIDX, -1);

		Map<String, Object> info = nm.getPage(bookIDX, pageIDX);

		// 显示数据
		pi_bid.setText(String.valueOf(bookIDX)) ;
		pi_pid.setText(String.valueOf(pageIDX)) ;
		pi_pname.setText(info.get(NV.PageName).toString()) ;
		pi_purl.setText(info.get(NV.PageURL).toString()) ;
		pi_content.setText(info.get(NV.Content).toString()) ;

		foxtipL("返回，保存，复制，粘贴，清空内容");

		return v;
	}

	private class onViewClickListener implements View.OnClickListener { // 单击
		@Override
		public void onClick(View v) {
			switch ( v.getId() ) {
			case R.id.testTV:
				onBackPressed();
				break;
			case R.id.btnSave:
				Map<String, Object> info = nm.getBlankPage();
				info.put(NV.PageName, pi_pname.getText().toString());
				info.put(NV.PageURL, pi_purl.getText().toString());
				info.put(NV.Content, pi_content.getText().toString());
				info.put(NV.Size, pi_content.getText().toString().length());
				nm.setPage(info, bookIDX, pageIDX);

				onBackPressed();
				break;
			case R.id.btnCopyFocus:
				String toCopy = "";
				if ( pi_pname.isFocused() ) { toCopy = pi_pname.getText().toString() ; }
				if ( pi_purl.isFocused() ) { toCopy = pi_purl.getText().toString() ; }
				if ( pi_content.isFocused() ) { toCopy = pi_content.getText().toString() ; }
				ToolAndroid.setClipText(toCopy, ctx);
				foxtip("剪贴板: " + toCopy);
				break;
			case R.id.btnPasteFocus:
				String toPaste = ToolAndroid.getClipText(ctx);
				if ( pi_pname.isFocused() ) { pi_pname.setText(toPaste) ; }
				if ( pi_purl.isFocused() ) { pi_purl.setText(toPaste) ; }
				if ( pi_content.isFocused() ) { pi_content.setText("") ; }
				break;
			case R.id.btnClearContent:
				pi_content.setText("");
				break;
			}
		}
	}

	private void onBackPressed() {
		getActivity().onBackPressed();
	}

	private void foxtip(String sinfo) { // Toast消息
		Toast.makeText(ctx, sinfo, Toast.LENGTH_SHORT).show();
	}
	private void foxtipL(String sinfo) {
		tv.setText(sinfo);
	}

	private OnFinishListener lsn;
	public void setOnFinishListener(OnFinishListener ofl) {
		lsn = ofl;
	}
	@Override
	public void onDestroy() {
		if ( lsn != null) {
			lsn.OnFinish();
		}
		super.onDestroy();
	}

	private TextView tv;
	private NovelManager nm;
	private int bookIDX = -1;
	private int pageIDX = -1;
	private TextView pi_bid, pi_pid;
	private EditText pi_pname, pi_purl, pi_content;

	private Context ctx;
}
