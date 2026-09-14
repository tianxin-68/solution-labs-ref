package com.bi.queryer.sys.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.encryption.DesEncryption;

public class JDBCPropertyPlaceHolder extends PropertyPlaceHolder{
	
	@Override
	protected Properties mergeProperties() throws IOException {
		Properties mergeProperties = super.mergeProperties();
		Properties runtimeProperies = new Properties();
		String fileName = BIUtil.getJDBCFileName();
		try {
			runtimeProperies.load(JDBCPropertyPlaceHolder.class.getResourceAsStream("/" + fileName));
			for(Object key : runtimeProperies.keySet()){
				mergeProperties.setProperty(key+"", runtimeProperies.get(key)+"");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("加载jdbc文件" + fileName + "出错");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("加载jdbc文件" + fileName + "出错");
		}
		
		return mergeProperties;
	}
	
	public static void main(String[] args) {
		String password = "A3CDB55308BDD5683FA43D780D9FB0D4";
		DesEncryption encry = new DesEncryption();
		password = encry.strDec(password, "password", "", "");
		System.out.println(password);
	}
}
