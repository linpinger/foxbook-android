package com.linpinger.foxbook;

import com.linpinger.novel.NovelManager;

import android.app.Application;

public class FoxApp extends Application {
	public NovelManager nm ; // 管理内存中核心数据
/*
 *  + FoxApp.java 这个用来放置全局变量
 * 	+ AndroidManifest.xml : <application android:name=".FoxApp"
 *  + 使用: FoxApp fa = (FoxApp) getApplication();
 *          fa.nm.xx();
 */
}
