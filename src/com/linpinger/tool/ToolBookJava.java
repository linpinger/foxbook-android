package com.linpinger.tool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.linpinger.novel.NV;

public class ToolBookJava {

/*
	public static String simplifyDelList2(String DelList) { // 精简 DelList
		int qi = 0;
		int zhi = 0;
		if (DelList.contains("起止=")) {
			Matcher mat = Pattern.compile("(?i)起止=([0-9\\-]+),([0-9\\-]+)").matcher(DelList);
			while (mat.find()) {
				qi = Integer.valueOf(mat.group(1));
				zhi = Integer.valueOf(mat.group(2));
			}
		}
		DelList = DelList.replace("\r", "").replace("\n\n", "\n");
		String[] xx = DelList.split("\n");
		if (xx.length < 15) {
			return DelList;
		}
		int MaxLineCount = xx.length - 9;
	
		StringBuffer newList = new StringBuffer(1024);
		for (int i = 0; i < 9; i++) {
			newList.append(xx[MaxLineCount + i]).append("\n");
		}
		if (zhi > 0) {
			return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount - 1) + "\n" + newList.toString();
		} else {
			return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount) + "\n" + newList.toString();
		}
	}
*/
	

/*
	public static List compare2GetNewPages2(List<Map<String, Object>> xx, String existList) {
		existList = existList.toLowerCase();
		int xxSize = xx.size();
		if (existList.contains("起止=")) { // 根据 起止 过滤一下 xx
			Matcher mat = Pattern.compile("(?i)起止=([0-9-]+),([0-9-]+)").matcher(existList);
			int qz_1 = 0;
			int qz_2 = 0;
			while (mat.find()) {
				qz_1 = Integer.valueOf(mat.group(1));
				qz_2 = Integer.valueOf(mat.group(2));
			}

			ArrayList<Map<String, Object>> nXX = new ArrayList<Map<String, Object>>(30);
			// 下面的初始值及判断顺序最好不要随便变动
			int sIdx = 0;
			int eIdx = xxSize;
			int leftIdx = 0;
			if (qz_2 > 0) {
				sIdx = qz_2;
				leftIdx = eIdx - sIdx;
			}
			if (qz_1 < 0) {
				eIdx = eIdx + qz_1;
				leftIdx = leftIdx + qz_1;
			}
			if (leftIdx > 0) {
				int nSIdx = 0;
				for (int i = 0; i < leftIdx; i++) {
					nSIdx = sIdx + i;
					nXX.add(xx.get(nSIdx));
				}
				xx = nXX;
			} else { // 章节数量为负
				if (55 == xxSize) {
					String jj[] = existList.split("\n");
					if (jj.length > 2) { // 截取已删除记录中第一条之后的记录，如果新章节>55可能会悲剧
						String sToBeComp = jj[jj.length - 2];
						ArrayList<Map<String, Object>> nX2 = new ArrayList<Map<String, Object>>(30);
						Iterator itr = xx.iterator();
						String nowurl = "";
						boolean bFillArray = false;
						while (itr.hasNext()) {
							HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
							nowurl = mm.get("url").toString().toLowerCase();
							if (sToBeComp.contains(nowurl)) {
								bFillArray = true;
								nX2.add(mm);
							} else {
								if (bFillArray) {
									nX2.add(mm);
								}
							}
						}
						xx = nX2;
					} else {
						System.out.println("error: jj < 2 : " + jj.length);
					}
				} else {  // 下面放的代码是没有新章节的处理方法
					return new ArrayList<HashMap<String, Object>>();
				}
			}
		}

		// 比较得到新章节
		String nowURL;
		ArrayList<HashMap<String, Object>> newPages = new ArrayList<HashMap<String, Object>>();
		Iterator<Map<String, Object>> itr = xx.iterator();
		while (itr.hasNext()) {
			HashMap<String, Object> mm = (HashMap<String, Object>) itr.next();
			nowURL = (String) mm.get("url");
			if (!existList.contains(nowURL.toLowerCase() + "|")) { // 新章节
				newPages.add(mm);
			}
		}
		return newPages;
	}
*/
	public static String simplifyDelList(String DelList) { // 精简 DelList
		int nLastItem = 9;
		DelList = DelList.replace("\r", "").replace("\n\n", "\n");
		String[] xx = DelList.split("\n");
		if (xx.length < (nLastItem + 2)) {
			return DelList;
		}
		int MaxLineCount = xx.length - nLastItem;

		StringBuilder newList = new StringBuilder(4096);
		for (int i = 0; i < 9; i++) {
			newList.append(xx[MaxLineCount + i]).append("\n");
		}
		return newList.toString();
	}

	// 取倒数几个元素，被上面这个调用
	public static List<Map<String, Object>> getLastNPage(List<Map<String, Object>> inArrayList, int lastNpage) {
		int aSize = inArrayList.size();
		if (aSize <= lastNpage || lastNpage <= 0) {
			return inArrayList;
		}
		List<Map<String, Object>> outList = new ArrayList<Map<String, Object>>(100);
		for (int nowIdx = aSize - lastNpage; nowIdx < aSize; nowIdx++) {
			outList.add((HashMap<String, Object>) (inArrayList.get(nowIdx)));
		}
		return outList;
	}

	public static List<Map<String, Object>> compare2GetNewPages(List<Map<String, Object>> listURLName, String DelList) {
		int linkSize = listURLName.size();
		if ( 0 == linkSize ) // aHTML为空(可能网页下载有问题)
			return listURLName ;
		if ( ! DelList.contains("|") ) // 当DelList为空，返回原数组
			return listURLName ;
		
		// 获取 DelList 第一行的 URL : BaseLineURL
		int fFF = DelList.indexOf("|");
		String BaseLineURL = DelList.substring(1 + DelList.lastIndexOf("\n", fFF), fFF);
		
		// 查到数组aHTML中等于BaseLineURL的行号，并删除1到该行号的所有元素
		int EndIdx = 0 ;
		String nowURL ;
		for (int nowIdx = 0; nowIdx < linkSize; nowIdx++) {
			nowURL = listURLName.get(nowIdx).get(NV.PageURL).toString();
			if ( BaseLineURL.equalsIgnoreCase(nowURL) ) {
				EndIdx = nowIdx ;
				break ;
			}
		}
		for (int nowIdx = EndIdx; nowIdx >= 0; nowIdx--) {
			listURLName.remove(nowIdx);
		}
		linkSize = listURLName.size();
		
		// 对比剩余的aHTML和DelList，得到新的aNewRet并返回
		List<Map<String, Object>> aNewRet = new ArrayList<Map<String, Object>>(30);
		for (int nowIdx = 0; nowIdx < linkSize; nowIdx++) {
			nowURL = listURLName.get(nowIdx).get(NV.PageURL).toString();
			if ( ! DelList.contains("\n" + nowURL + "|") )
				aNewRet.add(listURLName.get(nowIdx));
		}
		
		return aNewRet ;
	}


/*
	public static String simplifyDelList2(String DelList) { // 精简 DelList
		int qi = 0;
		int zhi = 0;
		if (DelList.contains("起止=")) {
			Matcher mat = Pattern.compile("(?i)起止=([0-9\\-]+),([0-9\\-]+)").matcher(DelList);
			while (mat.find()) {
				qi = Integer.valueOf(mat.group(1));
				zhi = Integer.valueOf(mat.group(2));
			}
		}
		DelList = DelList.replace("\r", "").replace("\n\n", "\n");
		String[] xx = DelList.split("\n");
		if (xx.length < 15) {
			return DelList;
		}
		int MaxLineCount = xx.length - 9;

		StringBuffer newList = new StringBuffer(1024);
		for (int i = 0; i < 9; i++) {
			newList.append(xx[MaxLineCount + i]).append("\n");
		}
		if (zhi > 0) {
			return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount - 1) + "\n" + newList.toString();
		} else {
			return "起止=" + qi + "," + String.valueOf(zhi + MaxLineCount) + "\n" + newList.toString();
		}
	}
*/

	public static List<Map<String, Object>> getSearchEngineHref(String html, String KeyWord) { // String KeyWord = "三界血歌" ;
		List<Map<String, Object>> data = new ArrayList<Map<String, Object>>(64);
		Map<String, Object> item;

		html = html.replace("\t", "");
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replaceAll("(?i)<!--[^>]+-->", "");
		html = html.replace("<em>", "");
		html = html.replace("</em>", "");
		html = html.replace("<b>", "");
		html = html.replace("</b>", "");
		html = html.replace("<strong>", "");
		html = html.replace("</strong>", "");

		// 获取链接 并存入结构中
		Matcher mat = Pattern.compile("(?smi)href *= *[\"']?([^>\"']+)[\"']?[^>]*> *([^<]+)<").matcher(html);
		while (mat.find()) {
			if (2 == mat.groupCount()) {
				if (mat.group(1).length() < 5)
					continue;
				if (!mat.group(1).startsWith("http"))
					continue;
				if (mat.group(1).contains("www.sogou.com/web"))
					continue;
				if (!mat.group(2).contains(KeyWord))
					continue;

				item = new HashMap<String, Object>(2);
				item.put(NV.BookURL, mat.group(1));
				item.put(NV.BookName, mat.group(2));
				data.add(item);
			}
		}

		return data;
	}

	public static String getFullURL(String baseURL, String subURL) { // 获取完整路径
		String allURL = "";
		try {
			allURL = (new URL(new URL(baseURL), subURL)).toString();
		} catch (MalformedURLException e) {
			System.err.println(e.toString());
		}
		return allURL;
	}

	//获取网络文件，转存到outPath中，outPath需要带文件后缀名，返回文件大小
	public static int saveHTTPFile(String inURL, String outPath) {
		int oLen = 0 ;
		File toFile = new File(outPath);
		if (toFile.exists())
			toFile.delete();
		try {
			FileOutputStream fos = new FileOutputStream(toFile);
			byte[] hb = downHTTP(inURL, "GET", null);
			oLen = hb.length ;
			fos.write(hb);
			fos.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return oLen;
	}

	public static String downhtml(String inURL) {
		return downhtml(inURL, "");
	}

	public static String downhtml(String inURL, String pageCharSet) {
		return downhtml(inURL, pageCharSet, "GET");
	}

	public static String downhtml(String inURL, String pageCharSet, String PostData) {
		return downhtml(inURL, pageCharSet, PostData, null);
	}

	public static String downhtml(String inURL, String pageCharSet, String PostData, String iCookie) {
		byte[] buf = downHTTP(inURL, PostData, iCookie);
		if (buf == null)
			return "";
		try {
			String html = "";
			if (pageCharSet == "") {
				html = new String(buf, "gbk");
				if (html.matches("(?smi).*<meta[^>]*charset=[\"]?(utf8|utf-8)[\"]?.*")) // 探测编码
					html = new String(buf, "utf-8");
			} else {
				html = new String(buf, pageCharSet);
			}
			return html;
		} catch (Exception e) { // 错误 是神马
			System.err.println(e.toString());
			return "";
		}
	}

	public static byte[] downHTTP(String inURL, String PostData, String iCookie) {
		byte[] buf = null;
		try {
			URL url = new URL(inURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if ("GET" != PostData) {
//				System.out.println("I am Posting ...");
				conn.setDoOutput(true);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
			}

			if ( null != iCookie )
				conn.setRequestProperty("Cookie", iCookie);

			if (inURL.contains(".13xs."))
				conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26"); // 2015-10-27: qqxs使用加速宝，带Java的头会被和谐
			else
				conn.setRequestProperty("User-Agent", "ZhuiShuShenQi/3.26 Java/1.6.0_55"); // Android自带头部和IE8头部会导致yahoo搜索结果链接为追踪链接

			if (!inURL.contains("files.qidian.com")) // 2015-4-16: qidian txt 使用cdn加速，如果头里有gzip就会返回错误的gz数据
				conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
			else
				conn.setRequestProperty("Accept-Encoding", "*"); // Android 会自动加上gzip，真坑，使用*覆盖之，起点CDN就能正确处理了

			conn.setRequestProperty("Accept", "*/*");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);	// 读取超时5s
			conn.setUseCaches(false);	 // Cache-Control: no-cache	 Pragma: no-cache
			
			conn.connect();  // 开始连接
			if ("GET" != PostData) {  // 发送PostData
				conn.getOutputStream().write(PostData.getBytes("UTF-8"));
				conn.getOutputStream().flush();
				conn.getOutputStream().close();
			}
			
			// 这个判断返回状态，本来想判断错误，结果简单的重新connect不行，不如重新来过吧
			/*
			int responseCode = conn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
//				System.out.println("  Error Happend, responseCode: " + responseCode + "  URL: " + inURL);
				return buf;
			}
			*/

			// 判断返回的是否是gzip数据
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int len = 0;
			// 返回的字段: Content-Encoding: gzip/null 判断是否是gzip
			if (null == conn.getContentEncoding()) { // 不是gzip数据
				InputStream in = conn.getInputStream();
				while ((len = in.read(buffer)) != -1)
					outStream.write(buffer, 0, len);
				in.close();
			} else { // gzip 压缩处理
				InputStream in = conn.getInputStream();
				GZIPInputStream gzin = new GZIPInputStream(in);
				while ((len = gzin.read(buffer)) != -1)
					outStream.write(buffer, 0, len);
				gzin.close();
				in.close();
			}

			buf = outStream.toByteArray();
			outStream.close();
		} catch (Exception e) { // 错误 是神马
			System.err.println(e.toString());
		}
		return buf;
	}
	
	// Wget Cookie 转为HTTP头中Cookie字段
	public static String cookie2Field(String iCookie) {
		String oStr = "" ;
		Matcher mat = Pattern.compile("(?smi)\t[0-9]*\t([^\t]*)\t([^\r\n]*)").matcher(iCookie);
		while (mat.find()) {
			oStr = oStr + mat.group(1) + "=" + mat.group(2) + "; " ;
//			System.out.println(mat.group(1) + "=" + mat.group(2));
		}
		return oStr ;
	}

	/**
	 * 上传文件
	 * @param urlStr
	 * @param fileMap
	 * @return
	 */
	// String filepath = "Q:\\zPrj\\fb.zip";
	// String urlStr = "http://linpinger.eicp.net:58080/cgi-bin/ff.lua";
	// Map<String, String> textMap = new HashMap<String, String>();
	// textMap.put("name", "testname");
	// Map<String, String> fileMap = new HashMap<String, String>();
	// fileMap.put("f", filepath);
	// String ret = formUpload(urlStr, fileMap);
	// System.out.println(ret);
	public static String formUpload(String urlStr, Map<String, String> fileMap) {
		// http://blog.csdn.net/wangpeng047/article/details/38303865
		String res = "";
		HttpURLConnection conn = null;
		String BOUNDARY = "---------------------------123821742118716"; //boundary就是request头和上传文件内容的分隔符
		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(30000);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)");
			conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

			OutputStream out = new DataOutputStream(conn.getOutputStream());
			// text
//			Map<String, String> textMap = null;
//			if (textMap != null) {
//				StringBuffer strBuf = new StringBuffer();
//				Iterator<Map.Entry<String, String>> iter = textMap.entrySet().iterator();
//				while (iter.hasNext()) {
//					Map.Entry<String, String> entry = iter.next();
//					String inputName = (String) entry.getKey();
//					String inputValue = (String) entry.getValue();
//					if (inputValue == null) {
//						continue;
//					}
//					strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
//					strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"\r\n\r\n");
//					strBuf.append(inputValue);
//				}
//				out.write(strBuf.toString().getBytes());
//			}

			// file	
			if (fileMap != null) {
				Iterator<Map.Entry<String, String>> iter = fileMap.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<String, String> entry = iter.next();
					String inputName = (String) entry.getKey();
					String inputValue = (String) entry.getValue();
					if (inputValue == null)
						continue;
					File file = new File(inputValue);
					String filename = file.getName();

					StringBuffer strBuf = new StringBuffer();
					//strBuf.append("\r\n").append("--").append(BOUNDARY).append("\r\n");
					strBuf.append("--").append(BOUNDARY).append("\r\n");  // 多一个\n会使nanohttpd崩溃的亲
					strBuf.append("Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + filename + "\"\r\n");
					strBuf.append("Content-Type: application/octet-stream\r\n\r\n");

					out.write(strBuf.toString().getBytes());

					DataInputStream in = new DataInputStream(new FileInputStream(file));
					int bytes = 0;
					byte[] bufferOut = new byte[1024];
					while ((bytes = in.read(bufferOut)) != -1)
						out.write(bufferOut, 0, bytes);
					in.close();
				}
			}

			byte[] endData = ("\r\n--" + BOUNDARY + "--\r\n").getBytes();
			out.write(endData);
			out.flush();
			out.close();

			// 读取返回数据
			StringBuffer strBuf = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null)
				strBuf.append(line).append("\n");
			res = strBuf.toString();
			reader.close();
			reader = null;
		} catch (Exception e) {
			System.err.println("发送POST请求出错。" + urlStr + "\n" + e.toString());
		} finally {
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}
		}
		return res;
	}

} // 类结束
