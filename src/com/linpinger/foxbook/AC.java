package com.linpinger.foxbook;

public interface AC {
	// 动作
	public static final String action = "Action" ;

	public static final int aListBookPages = 11 ;
	public static final int aListAllPages = 12 ;
	public static final int aListLess1KPages = 13 ;

	public static final int aListSitePages = 16;
	public static final int aListQDPages = 17;

	public static final int aShowPageInMem = 31 ;
	public static final int aShowPageOnNet = 32 ;
	public static final int aShowPageInZip1024 = 33;

	public static final int aSearchBookOnSite = 46 ;
	public static final int aSearchBookOnQiDian = 47 ;
	
	public static final int aAnotherAppShowContent = 51;
	public static final int aAnotherAppShowOnePageIDX = 52;

	// 下面的常量是供Android各class使用的，它们会交换这些常量，并通过这些常量辨别网站类型
	public static final String searchEngine = "SearchEngine";
	public static final int SE_NONE  = 0; // 非搜索引擎，正常链接处理
	public static final int SE_SOGOU = 1;
	public static final int SE_YAHOO = 2;
	public static final int SE_BING = 3;
	
	public static final int SE_MEEGOQ = 51;
}
