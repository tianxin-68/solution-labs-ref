package com.bi.queryer.sys.db;

import java.util.Properties;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.encryption.DesEncryption;

public class PropertyPlaceHolder extends PropertyPlaceholderConfigurer{
	
	//  加密&解密
	protected String resolvePlaceholder(String placeholder, Properties props) {
		if(placeholder.indexOf("password") != -1){
			String password = props.getProperty(placeholder);
//			DesEncryption encry = new DesEncryption();
			password = DesEncryption.decrypt(password);
			return password;
		}else if(placeholder.indexOf("rmi.ip") != -1){
			String ip = props.getProperty(placeholder);
			if("auto".equalsIgnoreCase(ip)){
				ip = BIUtil.getServerIP();
			}
			return ip;
		}else{
			return super.resolvePlaceholder(placeholder, props);
		}
	}
	
	public static void main(String[] args) {
		String password = "A3CDB55308BDD5683FA43D780D9FB0D4";
		DesEncryption encry = new DesEncryption();
		password = encry.strDec(password, "password", "", "");
		System.out.println(password);
	}
}
