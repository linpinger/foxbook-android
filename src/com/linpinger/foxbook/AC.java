package com.linpinger.foxbook;

public interface AC {
	// 动作
	String action = "Action" ;

	int aListBookPages = 11 ;
	int aListAllPages = 12 ;
	int aListLess1KPages = 13 ;

	int aListSitePages = 16;
	int aListQDPages = 17;

	int aShowPageInMem = 31 ;
	int aShowPageOnNet = 32 ;
	int aShowPageInZip1024 = 33;

	int aSearchBookOnSite = 46 ;
	int aSearchBookOnQiDian = 47 ;

	int aAnotherAppShowContent = 51;
	int aAnotherAppShowOnePageIDX = 52;

	// 下面的常量是供Android各class使用的，它们会交换这些常量，并通过这些常量辨别网站类型
	String searchEngine = "SearchEngine";
	int SE_NONE  = 0; // 非搜索引擎，正常链接处理
	int SE_SOGOU = 1;
	int SE_YAHOO = 2;
	int SE_BING = 3;

	int SE_MEEGOQ = 51;
}
