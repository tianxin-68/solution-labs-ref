package com.bi.queryer.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * 文件压缩、解压工具包
 * @author contributor
 *
 */
public class ZipUtil {
	
	/**
	 * 
	 * @param zipFileName 压缩文件名
	 * @param srcFileDir 需要压缩的源文件目录
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void zip(String zipFileName, String srcFileDir) throws FileNotFoundException, IOException{
		zip(zipFileName,srcFileDir, false);
	}
	/**
	 * 
	 * @param zipFileName 压缩文件名
	 * @param srcFileDir 需要压缩的源文件目录
	 * @param delSrcFile 是否删除源文件
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void zip(String zipFileName, String srcFileDir, boolean delSrcFile) throws FileNotFoundException, IOException{
		zip(zipFileName, "", srcFileDir, delSrcFile);
	}
	
	/**
	 * 根据源文件路径名列表压缩
	 * @param zipFileName 压缩文件名
	 * @param srcFilePathList 需要压缩的源文件绝对路径名，不能使目录
	 * @param delSrcFile 是否删除源文件
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception 
	 */
	public static void zip(String zipFileName, String[] srcFilePathList) throws FileNotFoundException, IOException{
		zip(zipFileName, srcFilePathList, false);
	}
	
	/**
	 * 根据源文件路径名列表压缩
	 * @param zipFileName 压缩文件名
	 * @param srcFilePathList 需要压缩的源文件绝对路径名，不能使目录
	 * @param delSrcFile 是否删除源文件
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception 
	 */
	public static void zip(String zipFileName, String[] srcFilePathList, boolean delSrcFile) throws FileNotFoundException, IOException{
		if(srcFilePathList == null || srcFilePathList.length == 0) return;
		File[] files = new File[srcFilePathList.length];
		for(int i = 0; i < srcFilePathList.length; i++){
			files[i] = new File(srcFilePathList[i]);
		}
		zip(zipFileName, files, delSrcFile);
	}
	
	/**
	 * 根据源文件列表压缩
	 * @param zipFileName 压缩文件名
	 * @param srcFileList 需要压缩的源文件，不能是目录
	 * @param delSrcFile 是否删除源文件
	 * @throws FileNotFoundException 
	 * @throws Exception
	 */
	public static void zip(String zipFileName, File[] srcFileList) throws FileNotFoundException, IOException{
		zip(zipFileName, srcFileList, false);
	}
	
	/**
	 * 根据源文件列表压缩
	 * @param zipFileName 压缩文件名
	 * @param srcFileList 需要压缩的源文件，不能是目录
	 * @param delSrcFile 是否删除源文件
	 * @throws FileNotFoundException 
	 * @throws Exception
	 */
	public static void zip(String zipFileName, File[] srcFileList, boolean delSrcFile) throws FileNotFoundException, IOException{
		if(srcFileList == null || srcFileList.length == 0) return;
		ZipOutputStream zos = null;
		try {
			zos = getZipOutputStream(zipFileName, "");
			for(File srcFile : srcFileList){
				zipFile(zos, srcFile, "");
			}
			for(File srcFile : srcFileList){
				deleteFile(srcFile.getAbsolutePath());
			}
		} catch (FileNotFoundException ex) {
			throw ex;
		} catch(IOException e){
			throw e;
		}finally {
			if (null != zos) {
				zos.close();
			}
		}
	}
	
	/**
	 * 获取zip文件名
	 * @param zipFileName
	 * @param srcFileDir
	 * @return
	 */
	protected static String getZipFileName(String zipFileName, String srcFileDir){
		String fileName = zipFileName;
		if (fileName == null || fileName.trim().equals("")) {
			if(srcFileDir == null || srcFileDir.trim().equals("")){
				return fileName;
			}
			File temp = new File(srcFileDir);
			if (temp.isDirectory()) {
				fileName = srcFileDir + ".zip";
			} else {
				if (srcFileDir.indexOf(".") > 0) {
					fileName = srcFileDir.substring(0, srcFileDir.lastIndexOf(".")) + "zip";
				} else {
					fileName = srcFileDir + ".zip";
				}
			}
		}
		
		return fileName;
	}
	
	/**
	 * 获取zip输出流
	 * @param zipFileName
	 * @param srcFileDir
	 * @return
	 * @throws FileNotFoundException
	 */
	protected static ZipOutputStream getZipOutputStream(String zipFileName, String srcFileDir) throws FileNotFoundException{
		String fileName = getZipFileName(zipFileName, srcFileDir);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fileName));
		zos.setEncoding("GBK");// 解决中文乱码
		return zos;
	}

	/**
	 * 
	 * @param zipFileName 压缩产生的zip包文件名--带路径,如果为null或空则默认按文件名生产压缩文件名
	 * @param relativePath 相对路径，默认为空
	 * @param srcFileDir 文件或目录的绝对路径
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void zip(String zipFileName, String relativePath, String srcFileDir, boolean delSrcFile) throws FileNotFoundException, IOException {
		ZipOutputStream zos = getZipOutputStream(zipFileName, srcFileDir);
		try {
			zip(zos, relativePath, srcFileDir);
			if(delSrcFile) {// 删除源文件
				delFileAll(srcFileDir);
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (null != zos) {
				zos.close();
			}
		}
	}

	/**
	 * 
	 * @param zos 压缩输出流 
	 * @param relativePath  压缩文件中的相对路径
	 * @param absolutPath 文件或文件夹绝对路径
	 * @throws IOException
	 */
	private static void zip(ZipOutputStream zos, String relativePath, String absolutPath) throws IOException {
		File file = new File(absolutPath);
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++) {
				File tempFile = files[i];
				if (tempFile.isDirectory()) {
					String newRelativePath = relativePath + tempFile.getName() + File.separator;
					createZipNode(zos, newRelativePath);
					zip(zos, newRelativePath, tempFile.getPath());
				} else {
					zipFile(zos, tempFile, relativePath);
				}
			}
		} else {
			zipFile(zos, file, relativePath);
		}
	}

	/**
	 * 生产文件 如果文件所在路径不存在则生成路径
	 * @param fileName 文件名 带路径
	 * @param isDirectory 是否为路径
	 * @return
	 */
	public static File buildFile(String fileName, boolean isDirectory) {
		File target = new File(fileName);
		if (isDirectory) {
			target.mkdirs();
		} else {
			if (!target.getParentFile().exists()) {
				target.getParentFile().mkdirs();
				target = new File(target.getAbsolutePath());
			}
		}
		return target;
	}

	/**
	 * 压缩文件
	 * @param zos 压缩输出流
	 * @param file 文件对象
	 * @param relativePath 相对路径
	 * @throws IOException
	 */
	private static void zipFile(ZipOutputStream zos, File file, String relativePath) throws IOException {
		if(file == null || !file.exists()) {
			return;
		}
		ZipEntry entry = new ZipEntry(relativePath + file.getName());
		zos.putNextEntry(entry);
		InputStream is = null;
		try {
			is = new FileInputStream(file);
			int BUFFERSIZE = 2 << 10;
			int length = 0;
			byte[] buffer = new byte[BUFFERSIZE];
			while ((length = is.read(buffer, 0, BUFFERSIZE)) >= 0) {
				zos.write(buffer, 0, length);
			}
			zos.flush();
			zos.closeEntry();
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (null != is) {
				is.close();
			}
		}
	}

	/**
	 * 创建压缩目录
	 * @param zos
	 * @param relativePath
	 * @throws IOException
	 */
	private static void createZipNode(ZipOutputStream zos, String relativePath) throws IOException {
		ZipEntry zipEntry = new ZipEntry(relativePath);
		zos.putNextEntry(zipEntry);
		zos.closeEntry();
	}

	/**
	 * 解压缩zip包
	 * @param zipFilePath  zip文件路径
	 * @param targetPath  解压缩到的位置，如果为null或空字符串则默认解压缩到跟zip包同目录跟zip包同名的文件夹下
	 * @throws IOException
	 */
	public static void unzip(String zipFilePath, String targetPath) throws IOException {
		OutputStream os = null;
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zipFilePath);
			String directoryPath = "";
			if (null == targetPath || "".equals(targetPath)) {
				directoryPath = zipFilePath.substring(0, zipFilePath.lastIndexOf("."));
			} else {
				directoryPath = targetPath;
			}
			Enumeration<?> entryEnum = zipFile.getEntries();
			if (null != entryEnum) {
				ZipEntry zipEntry = null;
				while (entryEnum.hasMoreElements()) {
					zipEntry = (ZipEntry) entryEnum.nextElement();
					if (zipEntry.isDirectory()) {
						directoryPath = directoryPath + File.separator + zipEntry.getName();
						System.out.println(directoryPath);
						continue;
					}
					if (zipEntry.getSize() > 0) {
						// 文件
						File targetFile = buildFile(directoryPath + File.separator + zipEntry.getName(), false);
						os = new BufferedOutputStream(new FileOutputStream(targetFile));
						is = zipFile.getInputStream(zipEntry);
						byte[] buffer = new byte[4096];
						int readLen = 0;
						while ((readLen = is.read(buffer, 0, 4096)) >= 0) {
							os.write(buffer, 0, readLen);
						}

						os.flush();
						os.close();
					} else {
						// 空目录
						buildFile(directoryPath + File.separator + zipEntry.getName(), true);
					}
				}
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (null != zipFile) {
				zipFile = null;
			}
			if (null != is) {
				is.close();
			}
			if (null != os) {
				os.close();
			}
		}
	}
	
    /**
     *  根据路径删除指定的目录或文件，无论存在与否
     *@param sPath  要删除的目录或文件
     *@return 删除成功返回 true，否则返回 false。
     */
    public static boolean delFileAll(String sPath) {
        Boolean flag = false;
        File file = new File(sPath);
        // 判断目录或文件是否存在
        if (!file.exists()) {  // 不存在返回 false
            return flag;
        } else {
            // 判断是否为文件
            if (file.isFile()) {  // 为文件时调用删除文件方法
                return deleteFile(sPath);
            } else {  // 为目录时调用删除目录方法
                return deleteDirectory(sPath);
            }
        }
    }
    
    /**
     * 删除单个文件
     * @param   sPath    被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }
    
    /**
     * 删除目录（文件夹）以及目录下的文件
     * @param   sPath 被删除目录的文件路径
     * @return  目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String sPath) {
        //如果sPath不以文件分隔符结尾，自动添加文件分隔符
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        //如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            //删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } //删除子目录
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前目录
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }
	
	public static void main(String[] args) {
		String zipFileName = "c:/中文4.zip";
		String relativePath = "";
		String directory = "D:/javaworkspace/biPortal/WebRoot/web/download/中文目录123";
		try {
			ZipUtil.zip(zipFileName, relativePath, directory, false);
			String[] paths = new String[]{"D:/javaworkspace/biPortal/WebRoot/web/download/中文目录123/属性值填充率_20140728210913.csv",
					"D:/javaworkspace/biPortal/WebRoot/web/download/中文目录123/属性值填充明细_20140728215921.csv"};
			try {
				ZipUtil.zip("c:/产品属性.zip", paths, true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
