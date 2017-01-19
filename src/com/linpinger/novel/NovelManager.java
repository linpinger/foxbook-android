package com.linpinger.novel;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.linpinger.tool.FoxZipReader;
import com.linpinger.tool.ToolBookJava;
import com.linpinger.tool.ToolJava;

public class NovelManager {
	public static final int SQLITE3 = 3;
	public static final int FML = 24;
	public static final int ZIP = 26;
	public static final int ZIP1024 = 1024 ;
	public static final int EPUB = 500 ;      // 普通 epub文件 处理方式待定
	public static final int EPUBFOXMAKE = 506; //我生成的 epub
	public static final int EPUBQIDIAN = 517; // 起点 epub
	public static final int TXT = 200 ;       // 普通txt
	public static final int TXTQIDIAN = 217 ; // 起点txt

	private File shelfFile ;
	private int nowShelfNum = 0 ;
	private List<Novel> shelf ;
	private int bookStoreType = 0;
	
	private boolean sortBookDesc = true ;
	private int saveFormat = NovelManager.FML ; // 默认保存格式

	public NovelManager(File inShelfFile) {
		this.open(inShelfFile);
	}

	public void open(File inShelfFile) {
		this.shelfFile = inShelfFile;

		String nameLow = this.shelfFile.getName().toLowerCase();
		if ( nameLow.endsWith(".fml") ) {
			this.bookStoreType = NovelManager.FML ;
			this.shelf = new Stor().load(this.shelfFile);
		} else if ( nameLow.endsWith(".db3") ) {
			this.bookStoreType = NovelManager.SQLITE3 ;
			// Todo: 根据系统类型，选择不同的平台导入方式，需要反射来loadClass
			this.shelf = new StorDB3().load(this.shelfFile);
		} else if ( nameLow.endsWith(".zip") ) {
			loadZip(this.shelfFile);
		} else if ( nameLow.endsWith(".epub") ) {
			this.bookStoreType = NovelManager.EPUB ;
			this.shelf = new StorEpub().load(this.shelfFile);
		} else if ( nameLow.endsWith(".txt") ) {
			this.bookStoreType = NovelManager.TXT ;
			this.shelf = new StorTxt().load(this.shelfFile);
		}
		System.out.println("NM:储存类型=" + this.bookStoreType);
	}

	public void setSaveFormat(int inSaveFormat) {
		this.saveFormat = inSaveFormat ;
	}
	public void close() { // 退出时需保存的资源
		String outExt ;
		if ( this.saveFormat == NovelManager.SQLITE3 )
			outExt = ".db3";
		else
			outExt = ".fml";

		String newFileName = this.shelfFile.getName().replace(".db3", "").replace(".fml", "") + outExt;
		File saveShelfFile = new File(this.shelfFile.getParentFile(), newFileName);
		ToolJava.renameIfExist(saveShelfFile);

		if ( this.saveFormat == NovelManager.SQLITE3 )
			this.exportAsDB3(saveShelfFile);
		else
			this.exportAsFML(saveShelfFile);
	}
	public File switchShelf() { // 切换文件
		this.close();

		String listExt = ".fml";
		if ( this.shelfFile.getName().endsWith(".db3") )
			listExt = ".db3";
		ArrayList<File> shelfsList = getShelfsList(this.shelfFile, listExt);
		++nowShelfNum;
		if ( nowShelfNum >= shelfsList.size() )
			nowShelfNum = 0 ;

		this.open(shelfsList.get(nowShelfNum));
		return this.shelfFile;
	}

	public void exportAsTxt(File oFile) {
		new StorTxt().save(this.shelf, oFile);
	}
	public void exportAsEpub(File oFile) {
		new StorEpub().save(this.shelf, oFile);
	}
	public void exportAsFML(File oFile) {
		new Stor().save(this.shelf, oFile);
	}
	public void exportAsDB3(File oFile) {
		new StorDB3().save(this.shelf, oFile);
	}

	public File getShelfFile() {
		return this.shelfFile;
	}
//	public List<Novel> getShelf() {
//		return this.getShelf();
//	}

	private ArrayList<File> getShelfsList(File oldShelfFile, final String shelfFileExt) {
		File DBDir = oldShelfFile.getAbsoluteFile().getParentFile();
		final String defaultShelfFileName = "FoxBook" + shelfFileExt;

		ArrayList<File> retList = new ArrayList<File>(4);
		retList.add(new File(DBDir.getAbsolutePath() + File.separator + defaultShelfFileName));

		File[] fff = DBDir.listFiles(new FileFilter() {
			public boolean accept(File ff) {
				if (ff.isFile()) {
					if (ff.toString().endsWith(shelfFileExt)) {
						if (ff.getName().equalsIgnoreCase(defaultShelfFileName)) {
							return false;
						} else {
							return true;
						}
					}
				}
				return false;
			}
		});

		for (int i = 0; i < fff.length; i++)
			retList.add(fff[i]);
		return retList;
	}
	private void loadZip(File inShelfFile) {
		this.bookStoreType = NovelManager.ZIP ;
		this.shelf = new ArrayList<Novel>(1);
		int tmpZipBookIDX = this.addBook(inShelfFile.getName(), "zip://" + inShelfFile.getName(), "0");
		List<Map<String, Object>> chapters = new ArrayList<Map<String, Object>>();
		Map<String, Object> page;
		FoxZipReader z = new FoxZipReader(inShelfFile);
		for( Map<String, Object> zItem : z.getFileList() ) {
			page = this.getBlankPage();
			page.put(NV.PageName, zItem.get("name"));
			page.put(NV.Size, zItem.get("count"));
			chapters.add(page);
		}
		z.close();
		this.shelf.get(tmpZipBookIDX).setChapters(chapters);
	}
	public Map<String, Object> getBlankBookInfo() { // 模版 根据NV中字段调整
		Map<String, Object> info = new HashMap<String, Object>(7);
		info.put(NV.BookName, "");
		info.put(NV.BookURL, "");
		info.put(NV.BookAuthor, "");
		info.put(NV.DelURL, "");
		info.put(NV.BookStatu, 0);
		info.put(NV.QDID, "");
		return info;
	}
	public Map<String, Object> getBlankPage() { // 模版 根据NV中字段调整
		Map<String, Object> page = new HashMap<String, Object>(5);
		page.put(NV.PageName, "");
		page.put(NV.PageURL, "");
		page.put(NV.Content, "");
		page.put(NV.Size, 0);
		return page;
	}
	public int addBook(String bookName, String bookURL, String qidianID) {
		Novel newBook = new Novel();
		Map<String, Object> info = this.getBlankBookInfo();
		info.put(NV.BookName, bookName);
		info.put(NV.BookURL, bookURL);
		info.put(NV.QDID, qidianID);
		newBook.setInfo(info);
		this.shelf.add(newBook);
		return this.shelf.indexOf(newBook);
	}
	public int addPage(Map<String, Object> page, int bookIDX, int pageIDX) { // 在pageIDX后追加新章
		List<Map<String, Object>> pages = this.shelf.get(bookIDX).getChapters();
		if ( pageIDX >= pages.size() )
			pages.add(page);
		else
			pages.add(pageIDX, page);
		return pages.indexOf(page);
	}

	public List<Map<String, Object>> getBookList(){ // 获取书籍列表
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(20);
		Map<String, Object> item;
		int nowBookIDX = -1 ;
		for (Novel book : this.shelf) {
			++ nowBookIDX ;
			item = new HashMap<String, Object>(9);
			item.putAll(book.getInfo());
			item.put(NV.PagesCount, book.getChapters().size());
			item.put(NV.BookIDX, nowBookIDX);
			data.add(item);
		}
		return data ;
	}

	public List<Map<String, Object>> getPageList(int sMode) { // 与BookID无关
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(20);
		Map<String, Object> item;
//sMode = 0: "select name, ID, URL,Bookid, length(content) from page " + sqlWhereStr
//	""
//999:	"where length(content) < 999 order by bookid,id"
//99:	"where length(content) < 99 or content is null order by bookid,id"
//sMode = 1: "order by bookid,id"
		int CLen = 0 ;
		int bookIDX = -1 ;
		int pageIDX ;
		String bookName ;
		for (Novel book : this.shelf) {
			++ bookIDX;
			bookName = book.getInfo().get(NV.BookName).toString();
			pageIDX = -1 ;
			for ( Map<String, Object> page : book.getChapters() ) {
				++ pageIDX;
				item = new HashMap<String, Object>(6);
				if ( 0 == pageIDX & 1 == sMode )
					item.put(NV.PageName, "★" + bookName + "★" + page.get(NV.PageName));
				else
					item.put(NV.PageName, page.get(NV.PageName));

				item.put(NV.BookName, bookName);
				item.put(NV.PageURL, page.get(NV.PageURL));
				item.put(NV.BookIDX, bookIDX);
				item.put(NV.PageIDX, pageIDX);
				CLen = Integer.valueOf(page.get(NV.Size).toString());
				if ( 999 == sMode && CLen < 999 )
					item.put(NV.Size, CLen);
				else if ( 99 == sMode && CLen < 99 )
					item.put(NV.Size, CLen);
				else if ( sMode < 2 )
					item.put(NV.Size, CLen);
				else
					continue ;
				data.add(item);
			}
		}
		return data ;
	}
	public List<Map<String, Object>> getBookPageList(int inBookIDX) {
		List<Map<String, Object>> pages = this.shelf.get(inBookIDX).getChapters();
		Map<String, Object> page ;
		List<Map<String, Object>> oList = new ArrayList<Map<String, Object>>(20);
		Map<String, Object> item;
//sMode = 0: "select name, ID, URL,Bookid, length(content) from page " + sqlWhereStr
//	where bookid = ?
//	"where bookid = " + iBookID + " order by bookid,id"
//sMode = 26: 0 "select name, ID, URL,Bookid, CharCount from page  where bookid=" + bookid
		for ( int pageIDX = 0; pageIDX < pages.size(); pageIDX++) {
			page = pages.get(pageIDX);
			item = new HashMap<String, Object>(6);
			item.put(NV.BookIDX,	inBookIDX);
			item.put(NV.PageIDX,	pageIDX);
			item.put(NV.PageName,	page.get(NV.PageName));
			item.put(NV.PageURL,	page.get(NV.PageURL));
			item.put(NV.Size,		page.get(NV.Size));
			oList.add(item);
		}
		return oList;
	}

	public void deleteBook(int bookIdx) { // 已: 验证 position 基本等于 idx
		this.shelf.remove(bookIdx);
	}
	
	public void sortBooks(boolean isDesc){
		this.sortBookDesc = isDesc ;
		Collections.sort(this.shelf, new ComparePageCount()); // 排序
	}

	public class ComparePageCount implements Comparator<Object>{
		@Override
		public int compare(Object objA, Object objB) {
			Novel hm0 = (Novel)objA;
			Novel hm1 = (Novel)objB;
			Integer order0 = hm0.getChapters().size();
			Integer order1 = hm1.getChapters().size();
			if ( order0 < order1) {
				if ( sortBookDesc )
					return 1;
				else
					return -1;
			}
			if ( order0 > order1 ) {
				if ( sortBookDesc )
					return -1;
				else
					return 1;
			}
			if ( order0 == order1 ) {  // 当类型相同时，比较名称
				String name0 = (String) hm0.getInfo().get(NV.BookName);
				String name1 = (String) hm1.getInfo().get(NV.BookName);
				return name0.compareTo(name1);
			}
			return 0;
		}
	}

	public void simplifyAllDelList(){ // 精简所有书的DelURL
		String delStr ;
		for (Novel book : this.shelf) {
			delStr = book.getInfo().get(NV.DelURL).toString();
			if ( delStr.length() > 128 ) {
				delStr = ToolBookJava.simplifyDelList(delStr);
				book.getInfo().put(NV.DelURL, delStr);
			}
		}
	}
	public Map<String, Object> getBookInfo(int bookIDX) {
		return this.shelf.get(bookIDX).getInfo();
	}
	public void setBookInfo(Map<String, Object> info, int bookIDX) {
		this.shelf.get(bookIDX).setInfo(info);
	}

	public Map<String, Object> getPage(int bookIDX, int pageIDX) {
		return this.shelf.get(bookIDX).getChapters().get(pageIDX) ;
	}
	public void setPage(Map<String, Object> page, int bookIDX, int pageIDX) {
		this.shelf.get(bookIDX).getChapters().get(pageIDX).putAll(page);
	}
	public void setPageContent(String content, int bookIDX, int pageIDX) { // synchronized
		Map<String, Object> hm = this.shelf.get(bookIDX).getChapters().get(pageIDX);
		hm.put(NV.Content, content);
		hm.put(NV.Size, content.length());
	}

	private Map<String, Object> getPrevBookPage(int bookIDX) {
		Map<String, Object> ret = new HashMap<String, Object>() ;
		int prevBookIDX = bookIDX - 1;
		if ( prevBookIDX < 0 ) { // 没有上一本书了，返回null，表示无了
			return null ;
		} else { // >=0 , 检测是否有章节, 有则返回PrevBookIDX 最后一个PageIDX + 
			int prevPageSize = this.shelf.get(prevBookIDX).getChapters().size();
			if ( prevPageSize > 0 ) { // 有章节，返回
				ret.putAll(getPage(prevBookIDX, prevPageSize - 1));
				ret.put(NV.BookIDX, prevBookIDX);
				ret.put(NV.PageIDX, prevPageSize - 1);
			} else { // 无章节
				ret = getPrevBookPage(prevBookIDX);
			}
		}
		return ret;
	}
	public Map<String, Object> getPrevPage(int bookIDX, int pageIDX) { // null=无上一章了  否则返回Map
		Map<String, Object> ret = new HashMap<String, Object>() ;
		int prevPageIDX = pageIDX - 1;
		if ( prevPageIDX < 0 ) {  // 上本书
			ret = getPrevBookPage(bookIDX);
		} else { // >= 0  , 正常返回prevPageIDX所在page +put(bookIDX) + put(PageIDX)
			ret.putAll(getPage(bookIDX, prevPageIDX));
			ret.put(NV.BookIDX, bookIDX);
			ret.put(NV.PageIDX, prevPageIDX);
		}
		return ret;
	}
	private Map<String, Object> getNextBookPage(int bookIDX) {
		Map<String, Object> ret = new HashMap<String, Object>() ;
		int nextBookIDX = bookIDX + 1 ;
		if ( nextBookIDX == this.shelf.size() ) { // 超出书界，返回null，表示木有下一章了
			return null ;
		} else { // nextBookIDX < 书数，检测是否有章节，有则返回nextBookIDX第一章+ ，否则
			int nextPageSize = this.shelf.get(nextBookIDX).getChapters().size();
			if ( nextPageSize > 0 ) { // 有章节，返回
				ret.putAll(getPage(nextBookIDX, 0));
				ret.put(NV.BookIDX, nextBookIDX);
				ret.put(NV.PageIDX, 0);
			} else {
				ret = getNextBookPage(nextBookIDX);
			}
		}
		return ret;
	}
	public Map<String, Object> getNextPage(int bookIDX, int pageIDX) {
		Map<String, Object> ret = new HashMap<String, Object>() ;
		int nextPageIDX = pageIDX + 1;
		if ( nextPageIDX == this.shelf.get(bookIDX).getChapters().size() ) { // 超过边界了
			ret = getNextBookPage(bookIDX);
		} else { // nextPageIDX < pageCount 正常返回nextPageIDX所在page +
			ret.putAll(getPage(bookIDX, nextPageIDX));
			ret.put(NV.BookIDX, bookIDX);
			ret.put(NV.PageIDX, nextPageIDX);
		}
		return ret;
	}

	public String getPageListStr(int bookIDX) { // 获取 url,name 列表
		return this.shelf.get(bookIDX).getInfo().get(NV.DelURL).toString() + getPageListStr_notDel(bookIDX);
	}
	private String getPageListStr_notDel(int bookIDX) {
		StringBuilder ret = new StringBuilder();
		for(Map<String, Object> page : this.shelf.get(bookIDX).getChapters())
			ret.append(page.get(NV.PageURL)).append("|").append(page.get(NV.PageName)).append("\n");
		return ret.toString();
	}

	public void clearPage(int bookIDX, int pageIDX, boolean bUpdateDelList) { // 清空单章
		if (bUpdateDelList) { // 修改 DelURL
			String oldDelList = this.shelf.get(bookIDX).getInfo().get(NV.DelURL).toString();
			Map<String, Object> page = this.shelf.get(bookIDX).getChapters().get(pageIDX);
			this.shelf.get(bookIDX).getInfo().put(NV.DelURL, oldDelList + page.get(NV.PageURL) + "|" + page.get(NV.PageName) + "\n" );
		}
		this.shelf.get(bookIDX).getChapters().remove(pageIDX);
	}
	public void clearBook(int bookIDX, boolean bUpdateDelList) { // 清空1书
		if (bUpdateDelList) // 修改 DelURL
			this.shelf.get(bookIDX).getInfo().put(NV.DelURL, getPageListStr(bookIDX));
		this.shelf.get(bookIDX).getChapters().clear();
	}
	public void clearShelf(boolean bUpdateDelList) { // 清空书架
		for (Novel book : this.shelf) {
			if ( book.getChapters().size() > 0 ) // 有章节的才清空
				clearBook(this.shelf.indexOf(book), bUpdateDelList);
		}
	}
	public void clearBookPages(int bookIDX, int pageIDX, boolean bUP, boolean bUpdateDelList) {  // 清空书籍n章之上或之下
		int delCount = 0;
		int nowPageIDX = -1 ;
		StringBuilder ss = new StringBuilder();

		for( Map<String, Object> page : this.shelf.get(bookIDX).getChapters()) {
			++ nowPageIDX;
			if ( nowPageIDX < pageIDX ) {
				if ( bUP ) {
					++delCount;
					ss.append(page.get(NV.PageURL)).append("|").append(page.get(NV.PageName)).append("\n");
				}
			} else if ( nowPageIDX == pageIDX ) {
				++delCount;
				ss.append(page.get(NV.PageURL)).append("|").append(page.get(NV.PageName)).append("\n");
			} else { // nowPageIDX > pageIDX
				if ( ! bUP ) {
					++delCount;
					ss.append(page.get(NV.PageURL)).append("|").append(page.get(NV.PageName)).append("\n");
				}
			}
		}
		if (bUpdateDelList) { // 修改 DelURL
			String oldDelList = this.shelf.get(bookIDX).getInfo().get(NV.DelURL).toString();
			this.shelf.get(bookIDX).getInfo().put(NV.DelURL, oldDelList + ss.toString() );
		}
		// 删除页面
		List<Map<String, Object>> pages = this.shelf.get(bookIDX).getChapters();
		if ( bUP ) {
			for (int i = 0; i < delCount; i++)
				pages.remove(0);
		} else {
			for (int i = 0; i < delCount; i++)
				pages.remove(pageIDX);
		}
	}
	public void clearShelfPages(int bookIDX, int pageIDX, boolean bUP, boolean bUpdateDelList) {  // 清空书籍n章之上或之下
		for (int nowBookIDX = 0; nowBookIDX < this.shelf.size(); nowBookIDX++) {
			if ( nowBookIDX < bookIDX ) {
				if ( bUP )
					this.clearBook(nowBookIDX, bUpdateDelList);
			} else if ( nowBookIDX == bookIDX ) {
				this.clearBookPages(bookIDX, pageIDX, bUP, bUpdateDelList);
			} else { // nowBookIDX > bookIDX
				if ( ! bUP )
					this.clearBook(nowBookIDX, bUpdateDelList);
			}
		}
	}

	public int getLess1KCount(){
		int count = 0 ;
		for (Novel book : this.shelf) {
			for (Map<String, Object> page : book.getChapters()) {
				if ( Integer.valueOf(page.get(NV.Size).toString()) < 999 )
					++count;
			}
		}
		return count;
	}

	public String getPagePosAtShelfPages(int bookIDX, int pageIDX) {
		int all = 0 ; // 页面总数
		int pos = 0 ; // 页面在总页数的位置
		int nowBookPagesCount = 0 ;
		int nowBookIDX = -1 ;
		for (Novel book : this.shelf) {
			nowBookPagesCount = book.getChapters().size() ;
			all += nowBookPagesCount ;
			++ nowBookIDX;
			if ( nowBookIDX == bookIDX ) {
				pos += pageIDX + 1 ;
			} else if ( nowBookIDX < bookIDX ) {
				pos += nowBookPagesCount;
			}
		}
		return pos + " / " + all ;
	}

	public List<Map<String, Object>> getBookListForShelf() { // 比较网站书架用
		List<Map<String, Object>> oo = new ArrayList<Map<String, Object>>();
		Map<String, Object> obook ;
		// "select id,name,url,DelURL from book where ( isEnd isnull or isEnd = '' or isEnd < 1 )"
		int nowStatu ;
		int bookIDX = -1;
		for (Novel book : this.shelf) {
			++bookIDX;
			nowStatu = (Integer)book.getInfo().get(NV.BookStatu);
			if ( nowStatu == 1 ) // 状态为1，表示不更新
				continue;
			obook = new HashMap<String, Object>();
			obook.putAll(book.getInfo());
			obook.put(NV.BookIDX, bookIDX);
			obook.put(NV.DelURL, this.getPageListStr(bookIDX));
			oo.add(obook);
		}
		return oo;
	}
	public List<Map<String, Object>> addBookBlankPageList(List<Map<String, Object>> data, int inBookIDX) {
		Novel book = this.shelf.get(inBookIDX);
		String bookURL = book.getInfo().get(NV.BookURL).toString();
	
		List<Map<String, Object>> chapters = book.getChapters();
		Map<String, Object> nPage;
	
		List<Map<String, Object>> oList = new ArrayList<Map<String, Object>>();
		Map<String, Object> oPage;
	
		String nowTitle;
		String nowPageURL;
		int pageIDX;
		for ( Map<String, Object> blankPage : data ) {
			nowTitle = blankPage.get(NV.PageName).toString();
			nowPageURL = blankPage.get(NV.PageURL).toString();
	
			nPage = this.getBlankPage();
			nPage.put(NV.PageName, nowTitle);
			nPage.put(NV.PageURL, nowPageURL);
			chapters.add(nPage);
			pageIDX = chapters.size() - 1;
	
			oPage = new HashMap<String, Object>(8);
			oPage.putAll(nPage);
			oPage.put(NV.PageFullURL, ToolBookJava.getFullURL(bookURL, nowPageURL) );
			oPage.put(NV.BookIDX, inBookIDX);
			oPage.put(NV.PageIDX, pageIDX);
			oList.add(oPage);
		}
		return oList;
	}

	public int updatePage(int bookIDX, int pageIDX) { // 写入对象，更新用
		Novel book = this.shelf.get(bookIDX);
		String pageFullURL = ToolBookJava.getFullURL(book.getInfo().get(NV.BookURL).toString()
				, book.getChapters().get(pageIDX).get(NV.PageURL).toString());
		String text = this.updatePage(pageFullURL);

		this.setPageContent(text, bookIDX, pageIDX);
		return text.length();
	}
	public String updatePage(String pageFullURL) { // 返回内容，不写入对象，用于在线查看
		String text = "";
		String html = "" ;

		if ( pageFullURL.contains("files.qidian.com") ) { // 起点手机站直接用txt地址好了
			html = ToolBookJava.downhtml(pageFullURL, "GBK"); // 下载json
			text = new SiteQiDian().getContent_Desk6(html);
		} else if ( pageFullURL.contains(".qidian.com") ) {
			String nURL = new SiteQiDian().getContentURL_Desk5(ToolBookJava.downhtml(pageFullURL)) ; // 2015-11-17: 起点地址变动，只能下载网页后再获取txt地址
			if ( nURL.equalsIgnoreCase("") ) {
				text = "" ;
			} else {
				html = ToolBookJava.downhtml(nURL);
				text = new SiteQiDian().getContent_Desk6(html);
			}
		} else {
			html = ToolBookJava.downhtml(pageFullURL); // 下载url
			text = new NovelSite().getContent(html);       // 分析得到text
		}

		return text;
	}

	public static void main(String[] args) {
		long sTime = System.currentTimeMillis();

//		NovelManager nm = new NovelManager(new File("Q:\\zPrj\\FoxBook.fml"));

//		nm.close();
		System.out.println("Time=" + (System.currentTimeMillis() - sTime));
	}

}
