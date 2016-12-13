package com.linpinger.foxbook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ToolJava {

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
		} catch (IOException e) {
			System.out.println(e.toString());
		}
		return retStr.toString();
	}

	public static void writeText(String iUtf8Str, String filePath) {
		writeText(iUtf8Str, filePath, "UTF-8");
	}
	// 写入指定编码，速度快点
	public static void writeText(String iStr, String filePath, String oFileEncoding) {
		boolean bAppend = false;
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath, bAppend), oFileEncoding));
			bw.write(iStr);
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e.toString());
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
        } catch (IOException e) {
            System.out.println(e.toString());
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
        	System.out.println(e.toString());
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
GBK     : 1 : 81-FE = 129-254
GBK     : 2 : 40-FE = 64-254

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


//	public static void createTxtFile(File txtFile, String cc) { // 创建Txt文件
//		try {
//			txtFile.createNewFile();
//			FileOutputStream outImgStream = new FileOutputStream(txtFile);
//			outImgStream.write(cc.getBytes("UTF-8"));
//			outImgStream.close();
//		} catch (Exception e) {
//			System.out.println(e.toString());
//		}
//	}

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
			System.out.println(e.toString());
		}
		return bDeleted;
	}

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     *
     * @param sPath 要删除的目录或文件
     * @return 删除成功返回 true，否则返回 false。
     */
//	public static boolean DeleteFolder(String sPath) {
//        boolean flag = false;
//        File file = new File(sPath);
//        // 判断目录或文件是否存在  
//        if (!file.exists()) {  // 不存在返回 false  
//            return flag;
//        } else {
//            // 判断是否为文件  
//            if (file.isFile()) {  // 为文件时调用删除文件方法  
//                return deleteFile(sPath);
//            } else {  // 为目录时调用删除目录方法  
//                return deleteDirectory(sPath);
//            }
//        }
//    }

    /**
     * 删除单个文件
     *
     * @param sPath 被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
//	public static boolean deleteFile(String sPath) {
//        boolean flag = false;
//        File file = new File(sPath);
//        // 路径为文件且不为空则进行删除  
//        if (file.isFile() && file.exists()) {
//            file.delete();
//            flag = true;
//        }
//        return flag;
//    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     *
     * @param sPath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
//	public static boolean deleteDirectory(String sPath) {
//        //如果sPath不以文件分隔符结尾，自动添加文件分隔符  
//        if (!sPath.endsWith(File.separator)) {
//            sPath = sPath + File.separator;
//        }
//        File dirFile = new File(sPath);
//        //如果dir对应的文件不存在，或者不是一个目录，则退出  
//        if (!dirFile.exists() || !dirFile.isDirectory()) {
//            return false;
//        }
//        boolean flag = true;
//        //删除文件夹下的所有文件(包括子目录)  
//        File[] files = dirFile.listFiles();
//        for (int i = 0; i < files.length; i++) {
//            //删除子文件  
//            if (files[i].isFile()) {
//                flag = deleteFile(files[i].getAbsolutePath());
//                if (!flag) {
//                    break;
//                }
//            } //删除子目录  
//            else {
//                flag = deleteDirectory(files[i].getAbsolutePath());
//                if (!flag) {
//                    break;
//                }
//            }
//        }
//        if (!flag) {
//            return false;
//        }
//        //删除当前目录  
//        if (dirFile.delete()) {
//            return true;
//        } else {
//            return false;
//        }
//    }

}
