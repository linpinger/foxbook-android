package com.linpinger.novel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.linpinger.tool.ToolBookJava;

public class Action_UpdateNovel {

//	public final int LINEBASE = 100 ;  //基数表示从该数往上递增表示线程数以及显示消息的行号 
	private final int downThread = 9 ; // 页面下载任务线程数

	public ArrayList<String> ANAME ;   // 方便更新所有fml
	private ArrayList<Long> ASIZE ;
	private ArrayList<Integer> ALeftThread ;
	private ArrayList<Integer> ANewPageCount ; // 多fml,新增章节计数
	private int upchacount = 0; // 单fml,新增章节计数
	private int leftThread = downThread ;

	public interface OnStatuChangeListener {
		public void OnStatuChange(int threadIDX, String msgOBJ) ;
	}
	private OnStatuChangeListener oscl;
	public void setOnStatuChangeListener(OnStatuChangeListener inListener) {
		oscl = inListener;
	}

	public void UpdateAllFMLs(String[] fmls, File fileCookie) {
		ANAME = new ArrayList<String>(8);
		ASIZE = new ArrayList<Long>(8);
		ALeftThread = new ArrayList<Integer>(8);
		ANewPageCount = new ArrayList<Integer>(8);
		ArrayList<File> fileList = new ArrayList<File>(8);

		File fileFML ;
		for ( String nowFML : fmls ) {
			System.out.println("A_UN: 检测: " + nowFML);
			if ( ! nowFML.contains(".fml") ) {
				System.err.println("A_UN: 不包含.fml: " + nowFML);
				continue ;
			}
			fileFML = new File(nowFML);
			if ( ! fileFML.exists() ) {
				System.err.println("A_UN: 不存在: " + nowFML);
				continue;
			}
			ANAME.add(fileFML.getName().replace(".fml", ""));
			ASIZE.add(fileFML.length());
			ALeftThread.add(downThread);
			ANewPageCount.add(0);
			fileList.add(fileFML);
		}

		// 分段防止初始化未完，已经开始更新造成的超出边界
		int i = -1 ;
		for ( File nowFile : fileList ) {
			++i;
			new Thread(new UpdateAllBook(new NovelManager(nowFile), fileCookie, i)).start();
		}
	}

	public class UpdateAllBook implements Runnable {
		NovelManager nm ;
		File cookiesFile;
		private boolean isUpFMLs = true ; // 是否更新所有的fmls模式
		int threadIDX = 0 ;
		private boolean isUpdateBlankPagesFirst = true;
		boolean isCompareShelf = true; // 更新前比较书架

		public UpdateAllBook(NovelManager inNM, File fileCookie, int inThreadIDX) { // 更新多个fmls
			this.isUpFMLs = true ;
			this.nm = inNM;
			this.cookiesFile = fileCookie;
			this.threadIDX = inThreadIDX ;
		}
		public UpdateAllBook(NovelManager inNM, File fileCookie, boolean isUpdateBlankPgFirst, boolean isCmpShelf) { // 更新单个fml
			this.isUpFMLs = false ;
			this.nm = inNM;
			this.cookiesFile = fileCookie;
			this.isUpdateBlankPagesFirst = isUpdateBlankPgFirst;
			this.isCompareShelf = isCmpShelf;
		}

		public void run() {
			if ( isUpdateBlankPagesFirst ) {
				for (Map<String, Object> blankPage : nm.getPageList(99) ) {
					oscl.OnStatuChange(this.threadIDX, "填空: " + (String)blankPage.get(NV.PageName));
					nm.updatePage((Integer)blankPage.get(NV.BookIDX), (Integer)blankPage.get(NV.PageIDX));
				}
			}

if ( isCompareShelf ) {
	oscl.OnStatuChange(this.threadIDX, "下载书架...");

			List<Map<String, Object>> nn = new NovelSite().compareShelfToGetNew(nm.getBookListForShelf(), cookiesFile);
			if ( nn != null ) {
				int nnSize = nn.size() ;
				if ( 0 == nnSize ) {
					if ( isUpFMLs ) {
						long newLen = closeNM(nm) - (Long)ASIZE.get(this.threadIDX) ;
						oscl.OnStatuChange(this.threadIDX, newLen + "B，●: 书架无更新");
					} else {
						oscl.OnStatuChange(this.threadIDX, "完毕: 书架无更新");
					}
					return ;
				} else {
					List<Thread> threadListA = new ArrayList<Thread>(30);
					int nowBookIDX = -1;
					String nowName, nowURL;
					Thread nowTTT;
					for ( Map<String, Object> mm : nn ) {
						nowBookIDX = (Integer)mm.get(NV.BookIDX);
						nowName = mm.get(NV.BookName).toString();
						nowURL = mm.get(NV.BookURL).toString();

						oscl.OnStatuChange(this.threadIDX, "更新: " + nowName);

						nowTTT = new Thread(new UpdateBook(nm, nowBookIDX, nowURL, nowName, true, threadIDX));
						nowTTT.start();
						threadListA.add(nowTTT);

					}
					for ( Thread nowThread : threadListA ) {
						try {
							nowThread.join();
						} catch (Exception ex) {
							System.out.println("等待线程错误: " + ex.toString());
						}
					}

					if ( isUpFMLs ) {
						long newLen = closeNM(nm) - (Long)ASIZE.get(this.threadIDX) ;
						oscl.OnStatuChange(this.threadIDX, newLen + "B，●: 已更 " + nnSize + " 书");
					} else {
						oscl.OnStatuChange(this.threadIDX, "完毕: " + nnSize + " 已更新");
					}

					return ;
				}
			}
} // isCompareShelf End

			List<Thread> threadList = new ArrayList<Thread>(30);
			Thread nowT;

			// 全部更新里面使用的变量
			int nowBookIDX = -1;
			String anowName, anowURL;

			for ( Map<String, Object> jj : nm.getBookList() ) {
				nowBookIDX = (Integer) jj.get(NV.BookIDX);
				anowURL = (String) jj.get(NV.BookURL);
				anowName = (String) jj.get(NV.BookName);
				if ( (Integer)jj.get(NV.BookStatu) != 1 ) {
					nowT = new Thread(new UpdateBook(nm, nowBookIDX, anowURL, anowName,true, threadIDX));
					threadList.add(nowT);
					nowT.start();
				}
			}

			for ( Thread nowThread : threadList ) {
				try {
					nowThread.join();
				} catch (Exception ex) {
					System.out.println("等待线程错误: " + ex.toString());
				}
			}

			if ( isUpFMLs ) {
				long newLen = closeNM(nm) - (Long)ASIZE.get(this.threadIDX) ;
				oscl.OnStatuChange(this.threadIDX, newLen + "B，●: 共 " + upchacount + " 新章节") ;
			} else {
				oscl.OnStatuChange(this.threadIDX, "共 " + upchacount + " 新章节，全部更新完毕") ;
			}

		}

		long closeNM(NovelManager nm) {
			nm.sortBooks(true);
			nm.simplifyAllDelList();
			nm.close();
			return nm.getShelfFile().length();
		}
	}

	public class UpdateBook implements Runnable { // 后台线程更新书
		private boolean isUpFMLs = true ;
		NovelManager nm ;
		int threadIDX = 0 ;

		private int bookIDX = 0 ;
		private String bookname ;
		private String bookurl ;
		private boolean bDownPage = true;

		public UpdateBook(NovelManager inNM, int inbookidx, String inBookURL, String inbookname, boolean bDownPage) { // FoxBook
			this(inNM, inbookidx, inBookURL, inbookname, bDownPage, 0);
		}
		public UpdateBook(NovelManager inNM, int inbookidx, String inBookURL, String inbookname, boolean bDownPage, int inThreadIDX) { // FoxTool
			this.nm = inNM ;
			this.bookIDX = inbookidx;
			this.bookurl = inBookURL;
			this.bookname = inbookname;
			this.bDownPage = bDownPage;
			this.threadIDX = inThreadIDX ;
			if ( inThreadIDX == 0 ) {
				this.isUpFMLs = false;
			} else {
				this.isUpFMLs = true;
			}
		}

		@Override
		public void run() {
			oscl.OnStatuChange(this.threadIDX, bookname + ": 下载目录页");

			String existList = nm.getPageListStr(bookIDX); // 得到旧 list
			List<Map<String, Object>> linkList;
			if ( bookurl.contains(".if.qidian.com") ) {
				linkList = new SiteQiDian().getTOC_Android7(ToolBookJava.downhtml(bookurl, "utf-8"));
			} else {
				linkList = new NovelSite().getTOC(ToolBookJava.downhtml(bookurl)); // 分析获取 list 所有章节
				if ( existList.length() > 3 ) {
					if ( nm.getBookInfo(bookIDX).get(NV.BookAuthor).toString().length() > 1 ) // 无作者名，表示为新书
						linkList = ToolBookJava.getLastNPage(linkList, 55); // 获取 list 最后55章
				}
			}

			List<Map<String, Object>> newPages = ToolBookJava.compare2GetNewPages(linkList, existList) ;
			int newpagecount = newPages.size(); // 新章节数，便于统计

			if (newpagecount == 0) {
				oscl.OnStatuChange(this.threadIDX, bookname + ": 无新章节");

				if ( ! bDownPage ) { //添加这个主要想在有空白章节时更新一下
					return;
				}
			} else {
				if ( isUpFMLs ) {
					ANewPageCount.set(threadIDX, ANewPageCount.get(threadIDX) + newpagecount) ;
				} else {
					upchacount += newpagecount;
				}
				oscl.OnStatuChange(this.threadIDX, bookname + ": 新章节数: " + String.valueOf(newpagecount));
			}

			List<Map<String, Object>> nbl = nm.addBookBlankPageList(newPages, bookIDX);
		if (bDownPage) {
			int cTask = nbl.size() ; // 总任务数

			if ( cTask > 25 ) { // 当新章节数大于 25章就采用多任务下载模式
				int nBaseCount = cTask / downThread ; //每线程基础任务数
				int nLeftCount = cTask % downThread ; //剩余任务数
				int aList[] = new int[downThread] ; // 每个线程中的任务数

				for ( int i = 0; i < downThread; i++ ) { // 分配任务数
					if ( i < nLeftCount )
						aList[i] = nBaseCount + 1 ;
					else
						aList[i] = nBaseCount ;
				}

				List<Map<String, Object>> subList ;
				int startPoint = 0 ;
				for ( int i = 0; i < downThread; i++ ) {
					if ( aList[i] == 0 ) { // 这种情况出现在总任务比线程少的情况下
						if ( isUpFMLs ) {
							ALeftThread.set(threadIDX, ALeftThread.get(threadIDX) - 1 );
						} else {
							--leftThread ;
						}

						continue ;
					}
					subList = new ArrayList<Map<String, Object>>(aList[i]);
					for ( int n = startPoint; n < startPoint + aList[i]; n++ )
						subList.add(nbl.get(n));
					(new Thread(new FoxTaskDownPage(subList, nm, threadIDX, isUpFMLs), "T" + i)).start() ;

					startPoint += aList[i] ;
				}
			} else { // 单线程循环更新页面
				int nowCount = 0;
				for (Map<String, Object> blankPage : nbl){
					++nowCount;
					oscl.OnStatuChange(this.threadIDX, bookname + ": 下载章节: " + nowCount + " / " + newpagecount) ;
					nm.updatePage(bookIDX, (Integer)blankPage.get(NV.PageIDX));
				}
			} // 单线程更新 end
		} // bDownPage

		oscl.OnStatuChange(this.threadIDX, bookname + ": 更新完毕");
		oscl.OnStatuChange(-9, bookname + ": 更新完毕，刷新LV"); // handler.sendEmptyMessage(DO_REFRESHLIST); // 更新完毕，通知刷新
		} // run end
	} // class UpdateBook end

	private class FoxTaskDownPage implements Runnable { // 多线程任务更新页面列表
		private boolean isUpFMLs = true ;
		private int nowLeftThread ;
		NovelManager nm;
		int threadIDX ;
		List<Map<String, Object>> taskList;

		public FoxTaskDownPage(List<Map<String, Object>> iTaskList, NovelManager inNM, int inThreadIDX, boolean isUpMultiFMLs) {
			this.taskList = iTaskList ;
			this.nm = inNM;
			this.threadIDX = inThreadIDX ;
			this.isUpFMLs = isUpMultiFMLs ;
		}

		public void run() {
			String thName = Thread.currentThread().getName();
			int locCount = 0 ;
			int allCount = taskList.size();
			for (Map<String, Object> tsk : taskList) {
				++ locCount ;

				if ( isUpFMLs ) {
					nowLeftThread = ALeftThread.get(threadIDX);
				} else {
					nowLeftThread = leftThread;
				}
				oscl.OnStatuChange(this.threadIDX, nowLeftThread + ":" + thName + ":" + locCount + " / " + allCount);

				nm.updatePage((Integer)tsk.get(NV.BookIDX), (Integer)tsk.get(NV.PageIDX));
			}

			if ( isUpFMLs ) {
				ALeftThread.set(threadIDX, ALeftThread.get(threadIDX) - 1 );
				nowLeftThread = ALeftThread.get(threadIDX);
			} else {
				--leftThread;
				nowLeftThread = leftThread;
			}
			if ( 0 == nowLeftThread ) { // 所有线程更新完毕
				oscl.OnStatuChange(this.threadIDX, "已更新完所有空白章节>25") ;
			}
		}
	}

}
