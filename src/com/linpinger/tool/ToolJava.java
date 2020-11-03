package com.linpinger.tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolJava {

	public static String getNREMatch(String iStr, String iRE, String oSpliter, int outFieldCount) {
		Matcher mat = Pattern.compile(iRE).matcher(iStr);
		String oStr = "";
		while (mat.find()) {
			oStr = mat.group(1);
			for (int i = 2; i < outFieldCount + 1; i++) {
				oStr += oSpliter + mat.group(i);
			}
		}
		return oStr;
	}

	public static String getOneREMatch(String iStr, String iRE) { // (?smi)\"(http[^\"]*)\"
		return getNREMatch(iStr, iRE, "\n", 1);
	}

	// { Properties读取，写入
/*
	public static Properties loadConfig(String cfgFilePath) {
		Properties pro = new Properties();
		try {
			FileInputStream inputFile = new FileInputStream(cfgFilePath);
//			pro.load(inputFile);
			pro.load(new InputStreamReader(inputFile, "UTF-8"));
			inputFile.close();
		} catch ( Exception e ) {
			System.err.println(e.toString());
		}
		return pro;
	}
*/
//	public static void saveConfig(Properties pro, String cfgFilePath) {
//		try {
//			FileOutputStream outputFile = new FileOutputStream(cfgFilePath, false);
//			pro.store(outputFile, "");
//			outputFile.close();
//		} catch ( Exception e ) {
//			System.err.println(e.toString());
//		}
//	}
	// } Properties读取，写入

	// { 通用文本读取，写入
	// 优先使用这个读取文本，快点，变量大小可以调整一下以达到最好的速度
	public static String readText(String filePath, String inFileEnCoding) {
		// 为了线程安全，可以替换StringBuilder 为 StringBuffer
		StringBuilder retStr = new StringBuilder(174080);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));

			char[] chars = new char[4096]; // 这个大小不影响读取速度
			int length = 0;
			while ((length = br.read(chars)) > 0) {
				retStr.append(chars, 0, length);
			}
/*
			// 下面这个效率稍低，但可以控制换行符
			String line = null;
			while ((line = br.readLine()) != null) {
				retStr.append(line).append("\n");
			}
*/
			br.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return retStr.toString();
	}

	public static void writeText(String iUtf8Str, String filePath) {
		writeText(iUtf8Str, filePath, "UTF-8");
	}
	public static void writeText(String iStr, String filePath, String oFileEncoding) {
		writeText(iStr, filePath, oFileEncoding, false);
	}
	// 写入指定编码，速度快点
	public static void writeText(String iStr, String filePath, String oFileEncoding, boolean bAppend) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, bAppend), oFileEncoding));
			bw.write(iStr);
			bw.flush();
			bw.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/*
	// 这个用在定长切割大文本上，已经在importQidianTxt中使用
	public static String readTextAndSplit(String filePath, String inFileEnCoding) {
		StringBuilder retStr = new StringBuilder(174080);
		StringBuilder chunkStr = new StringBuilder(65536);
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), inFileEnCoding));
			String line = null;
			int chunkLen = 0;
			while ((line = br.readLine()) != null) {
				chunkLen = chunkStr.length();
				if ( chunkLen > 3000 && ( line.length() == 0 || chunkLen > 6000 || line.startsWith("第") || line.contains("卷") || line.contains("章") || line.contains("节") ) ) {
					retStr.append(chunkStr).append("\n##################################################\n\n");
					chunkStr = new StringBuilder(65536);
				}
				chunkStr.append(line).append("\n");
			}
			if ( chunkStr.length() > 0 )
				retStr.append(chunkStr).append("\n#####LAST###########\n\n");
			br.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return retStr.toString();
	}
	*/

	public static String detectTxtEncoding(String txtPath) { // 猜测中文文本编码 返回: "GBK" 或 "UTF-8"
		byte[] b = new byte[256]; // 读取这么多字节，如果这么多字节都是英文那就悲剧了
		int loopTimes = 256 - 6 ; // 循环次数 = 字节数 - 6 避免越界
		try {
			FileInputStream in= new FileInputStream(txtPath);
			in.read(b);
			in.close();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		boolean isGBK = false ;
	if (b[0] == -17 && b[1] == -69 && b[2] == -65) { // UTF-8 BOM : EF BB BF
		isGBK = false ;
	} else {
		int aa = 0 ;
		int bb = 0 ;
		int cc = 0 ;
		int dd = 0 ;
		int ee = 0 ;
		int ff = 0 ;
		int i = 0 ;
		while ( i < loopTimes ) { // 2字节GBK与3字节UTF8公倍数为6，故取6个字符比较
			aa = b[i] & 0x000000FF ;
			if ( aa == 0 )
				break ;
			if ( aa < 128 ) {
				i = i + 1 ;
				continue ;
			}
			if ( aa < 192 || aa > 239 ) {
				isGBK = true ;
				break ;
			}

			bb = b[i+1] & 0x000000FF ;
			if ( bb < 128 || bb > 191 ) {
				isGBK = true ;
				break ;
			}

			// 第三字节:英文<128，GBK:129-254，UTF8:128-191 192-239
			cc = b[i+2] & 0x000000FF ;
			if ( cc > 239 ) {
				isGBK = true ;
				break ;
			} else if ( cc < 128 ) {
				i = i + 3 ;
				continue ;
			}

			dd = b[i+3] & 0x000000FF ;
			if ( dd < 64 ) {
				i = i + 4 ;
				continue ;
			}

			ee = b[i+4] & 0x000000FF ;
			if ( ee < 64 ) {
				i = i + 5 ;
				continue ;
			}

			ff = b[i+5] & 0x000000FF ;
			i = i + 6 ;
		// GBK: : 2 : 129-254 64-254
		// UTF8 : 2 : 192-223 128-191
		// UTF8 : 3 : 224-239 128-191 128-191
		} // while
	} // if
		if ( isGBK ) {
			return "GBK" ;
		} else {
			return "UTF-8" ;
		}
	}
/*
GBK	 : 1 : 81-FE = 129-254
GBK	 : 2 : 40-FE = 64-254

GBK -> UTF-8 :
UTF-8 2 : 1 : C2-D1 = 194-209
UTF-8 2 : 1 : 80-BF = 128-191

UTF-8 3 : 1 : E2-EF = 224-239
UTF-8 3 : 2 : 80-BF = 128-191
UTF-8 3 : 3 : 80-BF = 128-191
*/
// UTF-8 BOM : EF BB BF

/*
1字节 0xxxxxxx
2字节 110xxxxx 10xxxxxx
3字节 1110xxxx 10xxxxxx 10xxxxxx
4字节 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
5字节 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
6字节 1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx

UTF8 : 2 : 192-223 128-191
UTF8 : 3 : 224-239 128-191 128-191
*/

	// } 通用文本读取，写入

	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list(); // 递归删除目录中的子目录下
			for (int i = 0; i < children.length; i++) {
				if (! deleteDir(new File(dir, children[i])) ) {
					return false;
				}
			}
		} // 目录此时为空，可以删除
		boolean bDeleted = false ;
		try {
			bDeleted = dir.delete();
		} catch (Exception e) {
			System.err.println(e.toString());
		}
		return bDeleted;
	}

	public static boolean copyDir(File fromDir, File toDir) {
		if ( fromDir.isDirectory() ) { // 递归复制
			if ( ! toDir.exists() ) { if ( ! toDir.mkdirs() ) { return false; } }
			File nowTarF ;
			for ( File nowSrcF : fromDir.listFiles() ) { // 本目录下的所有
				nowTarF = new File(toDir, nowSrcF.getName());
				if ( nowSrcF.isDirectory() ) {
					if ( ! nowTarF.exists() ) { if ( ! nowTarF.mkdirs() ) { return false; } }
					if ( ! copyDir(nowSrcF, nowTarF) ) { return false; }
				}
				if ( nowSrcF.isFile() ) { if ( nowSrcF.length() != copyFile(nowSrcF, nowTarF) ) { return false; } }
			}
			return true;
		}

		if ( fromDir.isFile() ) { if ( fromDir.length() == copyFile(fromDir, toDir) ) { return true; } }

		return false;
	}

	public static long copyFile(File fromFile, File toFile) { // 使用channel复制更快，尤其是大文件更明显
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel in = null;
		FileChannel out = null;
		long cSize = 0;
		try {
			fis = new FileInputStream(fromFile);
			fos = new FileOutputStream(toFile);
			in = fis.getChannel();
			out = fos.getChannel();
			in.transferTo(0, in.size(), out); //连接两个通道，并且从in通道读取，然后写入out通道
		} catch (Exception e) {
			System.err.println(e.toString());
		} finally {
			try {
				cSize = out.size();
				fis.close();
				in.close();
				fos.close();
				out.close();
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		return cSize;
	}

	public static boolean renameIfExist(File tarFile) {
		return renameIfExist(tarFile, ".old");
	}
	public static boolean renameIfExist(File tarFile, String addSuffix) { // 如果目标文件存在就重命名，如果新名文件也存在就删除先
		if ( tarFile.exists() ) {
			File newFile = new File(tarFile.getPath() + addSuffix);
			if ( newFile.exists() )
				newFile.delete();
			return tarFile.renameTo(newFile);
		}
		return true;
	}
	public static boolean renameAFile(File srcFile, File tarFile) { // 重命名/移动 一个文件到文件/文件夹
		if ( tarFile.isDirectory() ) {
			if ( srcFile.isDirectory() ) {
				return srcFile.renameTo(tarFile);
			}
			File endFile = new File(tarFile, srcFile.getName());
			renameIfExist(endFile);
			return srcFile.renameTo(endFile);
		} else {
			renameIfExist(tarFile);
			return srcFile.renameTo(tarFile);
		}
	}

	// 将 IP 转为 广播ip: 例如: 192.168.1.22 -> 192.168.1.255
	public static String ip2bip(String iIPStr) { // only: Tool
		String ipHead = "";
		String RE = "^([0-9]*\\.[0-9]*\\.[0-9]*)\\.([0-9]*)$";
		Matcher m = Pattern.compile(RE).matcher(iIPStr);
		while (m.find())
			ipHead = m.group(1);
		if ( ipHead.contains(".") ) {
			return ipHead + ".255" ;
		} else {
			return "";
		}
	}

	/**
	 *
	 * @param file
	 * @param algorithm 所请求算法的名称  for example: MD5, SHA1, SHA-256, SHA-384, SHA-512 etc.
	 * @return
	 */
	public static String getFileHash(File file, String algorithm) { // only: FB
		if (!file.exists() || !file.isFile())
			return "";

		byte[] buffer = new byte[2048];
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			FileInputStream in = new FileInputStream(file);
			while (true) {
				int len = in.read(buffer, 0, 2048);
				if (len != -1)
					digest.update(buffer, 0, len);
				else
					break;
			}
			in.close();

			byte[] md5Bytes = digest.digest();
			StringBuilder hexValue = new StringBuilder();
			for (int i = 0; i < md5Bytes.length; i++) {
				int val = ((int) md5Bytes[i]) & 0xff;
				if (val < 16)
					hexValue.append("0");
				hexValue.append(Integer.toHexString(val));
			}
			return hexValue.toString();
		} catch (Exception e) {
			return "";
		}
	}


}
