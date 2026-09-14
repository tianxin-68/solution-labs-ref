package com.bi.queryer.util.network;

import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.util.StringUtil;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtil {
	
	
	public static String doPost(String url, Map requestParameters, String contentType, Map<String, String> headers, int timeout){
		String response = "";
		try{
			HttpClient httpClient = new HttpClient();
			httpClient.setConnectionTimeout(timeout);
//			httpClient.getParams().setSoTimeout(timeout);
			PostMethod postMethod = new PostMethod(url);
			if(null != headers) {
				for(String hkey : headers.keySet()) {
					postMethod.addRequestHeader(hkey, headers.get(hkey));
				}
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.putAll(requestParameters);
			if(StringUtil.isEmpty(contentType)) {
				contentType = "application/json";
			}
			StringRequestEntity requestEntity = new StringRequestEntity(jsonObject.toString(), contentType, "utf-8");
			postMethod.setRequestEntity(requestEntity);
			httpClient.executeMethod(postMethod);
//			byte[] resultBytes = postMethod.getResponseBody();
//			response = new String(resultBytes, "utf-8");//postMethod.getResponseBodyAsString();
			InputStream inputStream = postMethod.getResponseBodyAsStream();   
	        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	        StringBuffer stringBuffer = new StringBuffer();   
	        String str= "";   
	        while((str = br.readLine()) != null){   
	            stringBuffer.append(str );   
	        } 
	        response = stringBuffer.toString();
		}catch(Exception e) {
			e.printStackTrace();
			throw new BIException(e);
		}
		return response;
	}

	public static String doPost(String url, Map requestParameters){
		String response = "";
		try{
	//		/1、创建HttpClient
			HttpClient httpClient = new HttpClient();
			//2、创建get或post请求方法
			PostMethod method = new PostMethod(url);
			//3、设置编码
			httpClient.getParams().setContentCharset("UTF-8");
			//4、设置请求消息头，为表单方式提交
			method.setRequestHeader("Content-Type","application/x-www-form-urlencoded;charset=UTF-8");
			
			//5、设置参数
			for(Object key : requestParameters.keySet()) {
				method.setParameter(key.toString(), requestParameters.get(key).toString());
			}
//			System.out.println("params is ======>" + requestParameters);
	//		6、执行提交
			httpClient.executeMethod(method);
//			System.out.println(method.getStatusLine());
//			System.out.println(method.getResponseBodyAsString());
//			byte[] resultBytes = method.getResponseBody();
//			response = new String(resultBytes, "utf-8");//postMethod.getResponseBodyAsString();
			InputStream inputStream = method.getResponseBodyAsStream();   
	        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
	        StringBuffer stringBuffer = new StringBuffer();   
	        String str= "";   
	        while((str = br.readLine()) != null){   
	            stringBuffer.append(str );   
	        } 
	        response = stringBuffer.toString();
		}catch(Exception e) {
			e.printStackTrace();
			throw new BIException(e);
		}
		return response;
	}
	public static String doPost(String url, Map requestParameters, String contentType){
		return doPost(url, requestParameters, contentType, null, 0);
	}
	
	public static String doGet(String url, Map requestParameters, String contentType, Map<String, String> headers, int timeout){
		String response = "";
		try{
			HttpClient httpClient = new HttpClient();
			httpClient.setConnectionTimeout(timeout);
//			httpClient.getParams().setSoTimeout(timeout);
			GetMethod getMethod = new GetMethod(url);
			if(null != headers) {
				for(String hkey : headers.keySet()) {
					getMethod.addRequestHeader(hkey, headers.get(hkey));
				}
			}
			if(StringUtil.isEmpty(contentType)) {
				contentType = "application/json";
			}
			String queryParams = "";
			if(requestParameters != null) {
				for(Object key : requestParameters.keySet()) {
					queryParams = queryParams + "&" + key + "=" + requestParameters.get(key);
				}
				queryParams = queryParams.replaceFirst("&", "");
			}
			if(url.contains("?")) {
				url = url + "&" + queryParams;
			}else {
				url = url + "?" + queryParams;
			}
//			StringRequestEntity requestEntity = new StringRequestEntity(jsonObject.toString(), contentType, "utf-8");
//			getMethod.setRequestEntity(requestEntity);
			httpClient.executeMethod(getMethod);
//			byte[] resultBytes = getMethod.getResponseBody();
//			response = new String(resultBytes, "utf-8");//postMethod.getResponseBodyAsString();
			InputStream inputStream = getMethod.getResponseBodyAsStream();   
	        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));   
	        StringBuffer stringBuffer = new StringBuffer();   
	        String str= "";   
	        while((str = br.readLine()) != null){   
	            stringBuffer.append(str );   
	        } 
	        response = stringBuffer.toString();
		}catch(Exception e) {
			e.printStackTrace();
			throw new BIException(e);
		}
		return response;
	}
	
    /**
     * 向指定URL发送GET方法的请求
     * 
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, String param, String contentType, String token) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            System.out.println("urlNameString ======>" + urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");  
            // 设置通用的请求属性
//            connection.setRequestProperty("accept", "*/*");
//            connection.setRequestProperty("connection", "Keep-Alive");
//            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            
            if(contentType != null && !"".equals(contentType)) {
            	connection.setRequestProperty("Content-Type", contentType);
            }
            if(token != null) {
            	connection.setRequestProperty("X-Tableau-Auth", token);
            }
            // 建立实际的连接
            connection.connect();
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            // 遍历所有的响应头字段
            String str = "get header:";
            for (String key : map.keySet()) {
            	str = str + "key=" + map.get(key) + "\t";
            }
            System.out.println(str);
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     * 
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    @Deprecated
    public static String sendPost(String url, String param, String contentType, String token) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
//            conn.setRequestProperty("accept", "*/*");
//            conn.setRequestProperty("connection", "Keep-Alive");
//            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            if(contentType != null && !"".equals(contentType)) {
            	conn.setRequestProperty("Content-Type", contentType);
            }
            if(token != null) {
            	conn.setRequestProperty("X-Tableau-Auth", token);
            }
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        return result;
    }

	/**
	 * post方法返回
	 * @param url
	 * @param requestParameters
	 * @param contentType
	 * @param headers
	 * @param timeout
	 * @return
	 */
    public static PostMethod doPostResponsePostMethod(String url, Map requestParameters, String contentType, Map<String, String> headers, int timeout){
		PostMethod postMethod = null;
		try{
			HttpClient httpClient = new HttpClient();
			httpClient.setConnectionTimeout(timeout);
//			httpClient.getParams().setSoTimeout(timeout);
			postMethod = new PostMethod(url);
			if(null != headers) {
				for(String hkey : headers.keySet()) {
					postMethod.addRequestHeader(hkey, headers.get(hkey));
				}
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.putAll(requestParameters);
			if(StringUtil.isEmpty(contentType)) {
				contentType = "application/json";
			}
			StringRequestEntity requestEntity = new StringRequestEntity(jsonObject.toString(), contentType, "utf-8");
			postMethod.setRequestEntity(requestEntity);
			httpClient.executeMethod(postMethod);

		}catch(Exception e) {
			e.printStackTrace();
			throw new BIException(e);
		}
		return postMethod;
	}

	public static OkHttpClient getUnsafeOkHttpClient(long timeoutSecond) {
		try {
			// 创建一个信任所有证书的TrustManager
			final TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
						}

						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new X509Certificate[0];
						}
					}
			};

			// 创建一个不验证证书的 SSLContext，并使用上面的TrustManager初始化
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

			// 使用上面创建的SSLContext创建一个SSLSocketFactory
			javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
			builder.hostnameVerifier((hostname, session) -> true);
			builder.readTimeout(timeoutSecond, TimeUnit.SECONDS);

			return builder.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
