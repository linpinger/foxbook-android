package com.linpinger.foxbook;

public interface AC {
	// 动作: 根据Activity不同而不同
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

	// 下面的常量是供Android各Activity使用的，它们会交换这些常量，并通过这些常量辨别网站类型
	public static final String searchEngine = "SearchEngine";
	public static final int SE_SOGOU = 1;
	public static final int SE_YAHOO = 2;
	public static final int SE_BING = 3;
}
