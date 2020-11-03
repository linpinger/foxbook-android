package com.linpinger.misc;

import java.io.File;
import java.util.ArrayList;

import com.linpinger.tool.ToolAndroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

//在Activity中使用 View.setOnClickListener 绑定点击事件，免得自己来判断
public class View_FoxTextView extends View {

	private FoxBroadcastReceiver bc_rcv;
	private Context ctx ;
	private Paint p;

	// 配置
	String txt = "木有内容，稍等可能正在下载，如果长时间木有反应，请返回";
	String firstPageInfoL = "" ; // 第一页时，在左侧信息处显示的信息
	String infoL = "我是萌萌哒标题";
	String infoR = "15:55  0%";
	int batteryLevel = 0;
	float lineSpaceing = 1.5f ;
	float paddingMulti = 0.5f ;

//	private boolean bDrawSplitLine = false ; // 调试用的
	private float fontSize = 36.0f ; // E-ink:26 Mobile:34 Mi:26
	private boolean bUseUserFont = false ; // 使用用户字体
	private String userFontPath = "/sdcard/fonts/foxfont.ttf"; // 用户字体路径

//	private float clickX = 0 ;
//	private float clickY = 0 ;
	private int nowPageNum = 0 ; // 第一屏为0
	private boolean isLastPage = false ; //是否在最后一页
	private boolean isBodyBold = false ; // 正文是否加粗

	private ArrayList<String> lines ;
	private int lastTxtHashCode = 0 ;
	private float lastMaxWidth = 0 ;

	public View_FoxTextView(Context context) {
		super(context);
		ctx = context ;

		p = new Paint();
		p.setAntiAlias(true);
		p.setSubpixelText(true); 
		p.setColor(Color.BLACK); // 颜色

		fontSize = (float)ToolAndroid.sp2px(ctx, 18.5f);

		bc_rcv = new FoxBroadcastReceiver();
		ctx.registerReceiver(bc_rcv, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); // 电量变动
		ctx.registerReceiver(bc_rcv, new IntentFilter(Intent.ACTION_TIME_CHANGED)); // 时间变动 ACTION_TIME_TICK
	}

	public void setBodyBold(boolean bBold) {
		isBodyBold = bBold ;
	}

	@Override
	protected void onDetachedFromWindow() {
		ctx.unregisterReceiver(bc_rcv); // 关闭广播接收
		super.onDetachedFromWindow();
	}

	public int setPrevText() { // 需被覆盖
		setText("上章标题", "　　上章内容\n　　么么哒\n");
		return 0;
	}
	public int setNextText() { // 需被覆盖
		setText("下章标题", "　　下章内容\n　　么么哒\n");
		return 0;
	}

	public String getScreenText() { // For: TTS
		String oStr = "";
		for (int i = startLineNum; i <= endLineNum; ++i) {
			oStr += lines.get(i) + "\n";
		}
		return oStr;
	}
	public void clickPrev() {
		if ( nowPageNum == 0) { // 第一页，翻上一章
			if ( 0 == setPrevText() ) // 上一章
				nowPageNum = -6 ; // 回到上章尾部
		} else {
			-- this.nowPageNum ;
		}
		setInfoR();
		postInvalidate();
	}
	public void clickNext() {
		if ( isLastPage ) { // 下一章
			setNextText();
		} else {
			++ this.nowPageNum ;
		}
		setInfoR();
		postInvalidate();
	}

	public void setInfoR() {
		infoR = (new java.text.SimpleDateFormat("HH:mm")).format(new java.util.Date()) + "  " + batteryLevel + "%";
	}

	public void setText(String iTitle, String iTxt, String iFirstPageLinfo) {
		firstPageInfoL = iFirstPageLinfo;
		setText(iTitle, iTxt);
	}
	public void setText(String iTitle, String iTxt) {
		infoL = iTitle;
		txt = iTxt ;
		nowPageNum = 0;
		this.setInfoR();
	}

	public View setFontSize(float inFontSizePX) {
		fontSize = inFontSizePX;
		return this;
	}
	public View setLineSpaceing(String inLS) {// "1.5f"
		lineSpaceing = Float.valueOf(inLS);
		return this;
	}
	public View setPadding(String floatFontSizeMultiple) { // 0.5f
		paddingMulti = Float.valueOf(floatFontSizeMultiple) ;
		return this;
	}

	public boolean setUserFontPath(String fontPath) {
		File font1 = new File(fontPath);
		if ( font1.exists() ) {
			bUseUserFont = true ;
			userFontPath = fontPath;
		} else {
			bUseUserFont = false ;
			font1.getParentFile().mkdirs();
		}
		return bUseUserFont;
	}

//绘制文本API: http://www.cnblogs.com/tianzhijiexian/p/4297664.html
//canvas.drawColor(Color.WHITE); // 白色背景
//canvas.drawColor(Color.parseColor("#EFF8D6")); // 绿色
//Bitmap bg = BitmapFactory.decodeResource(getResources(), R.drawable.parchment_paper);
//canvas.drawBitmap(bg, new Rect(0,0,bg.getWidth(),bg.getHeight()), new Rect(0,0, cw, ch), p);
	int startLineNum = 0; // For: TTS
	int endLineNum = 0;
	@Override
	protected void onDraw(Canvas canvas) {
		// super.onDraw(canvas);
		// 计算参数
		float lineHeight = fontSize * lineSpaceing ;
		float padding = fontSize * paddingMulti ;

		// 获取画布的大小
		int cw = canvas.getWidth();
		int ch = canvas.getHeight();
//Log.e("OD", "画布: W=" + cw + " H=" + ch);

		// 画一个框
/*
		if ( bDrawSplitLine ) {
			p.setStyle(Paint.Style.STROKE) ; // 空心 FILL
			p.setStrokeWidth(1);
			Rect textRect = new Rect(new Float(padding).intValue(), new Float(padding).intValue(), new Float(cw - padding).intValue(), new Float(ch - padding).intValue());
			canvas.drawRect(textRect, p);
			canvas.drawLine(0, ch/3, cw, 3 + ch / 3, p); // 横线
			canvas.drawLine(cw/3*2, 0, 3+cw/3*2, ch, p); // 竖线

			if ( clickX > 0 ) { // 绘制点击坐标线
				canvas.drawLine(0, clickY, cw, clickY+6, p); // 横线
				canvas.drawLine(clickX, 0, clickX+6, ch, p); // 竖线
			}
		}
*/
		// 设置字体
		if ( bUseUserFont ) {
			p.setTypeface(Typeface.createFromFile(new File(userFontPath))); // 设置字体
		}

		p.setStyle(Paint.Style.FILL) ;
		p.setTextSize(fontSize);

		// 计算txt内容及maxwidth是否和上次相同，若有不同则重新生成lines，也就是说只在第一次生成，避免每次绘制都生成一次，这可是个耗时大户
		int nowTxtHashCode = txt.hashCode();
		float nowMaxWidth = cw - 2 * padding ;
		if ( ( lastTxtHashCode != nowTxtHashCode ) || ( lastMaxWidth != nowMaxWidth ) ) {
			lines = split2lines(txt, p, cw - 2 * padding); // 将内容拆成行
			lastTxtHashCode = nowTxtHashCode ;
			lastMaxWidth = nowMaxWidth ;
		}

		int lineCount = lines.size();

		// 计算每屏最多行数
		int linePerScreen = (int) Math.floor( (ch - padding) / lineHeight);
		int nowPageCount = (int) Math.ceil( lineCount / Double.valueOf((String.valueOf(linePerScreen) + ".0")) ); // 屏数
		if (nowPageNum == -6) // 向上翻，回到尾部
			nowPageNum = nowPageCount - 1 ;
		startLineNum = nowPageNum * linePerScreen ; // 0base:包含
		endLineNum = ( nowPageNum + 1 ) * linePerScreen - 1 ; // 0base:包含
		if ( endLineNum >= lineCount - 1 ) {
			endLineNum = lineCount - 1;
			isLastPage = true ; // 表示要向下翻页了
		} else {
			isLastPage = false ;
		}

//Log.e("XO", "LPS=" + linePerScreen + " STN=" + startLineNum + " ETN=" + endLineNum + " C=" + lineCount);
		// 按计算绘制
		int drawCount = 0;
		for ( int lineidx = startLineNum; lineidx < lineCount; lineidx++) {
//Log.e("XX", "LC=" + lineidx + " DC=" + drawCount + " text=" + line);
			if ( lineidx > endLineNum )
				break;
			++ drawCount;
			if ( lineidx == 0 ) { // 第一行当作标题
				p.setFakeBoldText(true);
				p.setTextSize(lineHeight - padding * 0.5f );
				float titleX = ( cw - p.measureText(infoL)) / 2;
				if ( titleX < 0 )
					titleX = padding ;
				canvas.drawText(infoL, titleX, lineHeight, p);
				p.setTextSize(fontSize);
				if ( ! isBodyBold )
					p.setFakeBoldText(false);
			} else {
				canvas.drawText(lines.get(lineidx), padding, lineHeight * drawCount, p);
			}
		}

		// 绘制底部信息
//		p.setColor(Color.DKGRAY); // 颜色
//		p.setTextSize(fontSize * 0.4f + padding * 0.5f);
//		float infoY = ch - padding / 2 ;
		p.setTextSize( (ch - linePerScreen * lineHeight) / 2 );
		float infoY = ch - (ch - linePerScreen * lineHeight) / 4 ;
		float infoRLen = p.measureText(infoR);
		canvas.drawText(infoR, cw - padding - infoRLen , infoY, p); // 右侧时间 电量
		// 左侧信息
		if ( 0 == nowPageNum ) { // 首页
			canvas.drawText("  1 / " + nowPageCount + "  " + firstPageInfoL, padding, infoY, p);
			// canvas.drawText("本章共 " + nowPageCount + " 页    " + firstPageInfoL, padding, ch - padding / 2, p);
		} else { // 非首页，截断过长标题
			String newInfoL = ( nowPageNum + 1 ) + " / " + nowPageCount + "  " + infoL ;
			int infoLen = p.breakText(newInfoL, true, cw - 2 * padding - infoRLen - fontSize, null);
			String addStr = "";
			if ( infoLen < newInfoL.length() )
				addStr = "…" ;
			canvas.drawText("  " + newInfoL.substring(0, infoLen) + addStr, padding, infoY, p);
		}
//		p.setColor(Color.BLACK); // 颜色
	} // onDraw结束

	private ArrayList<String> split2lines(String inText, Paint p, float maxWidth) {
		ArrayList<String> oLine = new ArrayList<String>(50);
		oLine.add(""); // 第一行当作标题
		String inLine[] = inText.replace("\r", "").split("\n");
		int lineLen = 0;
		int count = 0;
		for ( String line : inLine ) {
			 while(true) {
				lineLen = line.length(); // 行长
				count = p.breakText(line, true, maxWidth, null);
				if ( count >= lineLen ) {
					oLine.add(line);
					break ;
				} else {
					oLine.add(line.substring(0, count));
					line = line.substring(count);
				}
			}
		}

		// 删除尾部空行
		int oS = oLine.size();
		String aa ;
		for ( int i=1; i<5; i++) {
			aa = (String)oLine.get(oS-i) ;
			if ( 0 == aa.length() | aa.equalsIgnoreCase("　　") ) {
				oLine.remove(oS-i);
			} else {
				break;
			}
		}

		return oLine;
	}


	class FoxBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context ctx, Intent itt) {
			if( itt.getAction().equals(Intent.ACTION_BATTERY_CHANGED) ) {
				batteryLevel = itt.getIntExtra("level", 0);
				setInfoR();
				postInvalidate();
			}
			if( itt.getAction().equals(Intent.ACTION_TIME_CHANGED) ) {
				setInfoR();
				postInvalidate();
			}
		}
	}

} // 自定义View结束

