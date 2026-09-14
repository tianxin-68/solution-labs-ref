package com.bi.queryer.sys.user;

import cn.hutool.core.util.StrUtil;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.config.SC;
import com.bi.queryer.sys.enums.RuntimeEnv;
import com.bi.queryer.sys.enums.TokenErrorType;
import com.bi.queryer.sys.user.vo.User;
import com.bi.queryer.util.BIConsts;
import com.bi.queryer.util.BIUtil;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.StringUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 登录token管理
 * @author contributor
 *
 */
public abstract class UserTokenManager {
	public static final String Cookies_Token = "_token";
	
	public static final String Cookies_Token_LastTime = "_token_lt";
	
	/**
	 * 创建token并写入cookies
	 * @param response
	 * @return
	 */
	public static String create(User user, HttpServletResponse response) {
		if(user == null) {
			return null;
		}
		String signCode = BIUtil.isEmpty(user.getSignCode()) ? user.getName() : user.getSignCode();
		String email = BIUtil.isEmpty(user.getEmail()) ? user.getName() + "@example.com" : user.getEmail();
		String token = JWT.create().withAudience(user.getName()).withClaim("email", email)
				.withClaim("iss", "https://sso.example.com")
                .sign(Algorithm.HMAC256(signCode));
		
		if(response != null) {
			// token
			BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token, token, response);
			
			// token的最新时间
			BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token_LastTime, System.currentTimeMillis() + "", response);
		}
		return token;
	}

	public static String createByUserName(String userName) {
		String email = userName + "@example.com";
		return JWT.create().withAudience(userName).withClaim("email", email)
				.withClaim("iss", "https://access.example.com").sign(Algorithm.HMAC256(userName));
	}

	/**
	 * 校验token是否合法
	 * @param request
	 * @param response
	 * @param isFreeCheck 不做用户合法性check
 	 * @return
	 */
	public static ResponseMessage check(HttpServletRequest request, HttpServletResponse response, boolean isFreeCheck) {
		ResponseMessage result = new ResponseMessage(true);
		// 先从request的参数中获取

		RuntimeEnv env = BIUtil.getRuntimeEnv();
		String token = "";
//		if(!RuntimeEnv.UT.getCode().equalsIgnoreCase(env.getCode())) {
//			token = request.getParameter(BIConsts.User_Token);
//		}

//		System.out.println("---------------1----------------" + token);
		// 再从request的header中获取
		if(BIUtil.isEmpty(token)) {
			token = request.getHeader(BIConsts.User_Token);
		}
//		System.out.println("---------------2----------------" + token);
		// 最后从cookies中获取
		if(BIUtil.isEmpty(token)) {
			token = BIUtil.getCookie(BIConsts.Cookies_Prefix + Cookies_Token, request);
		}
//		System.out.println("---------------3----------------" + token);
		if(BIUtil.isEmpty(token)) {
			result.set(false, TokenErrorType.None.getCode());
			return result;
		}
		
		User user = null;
		// 校验
		try {
			// 校验token有效时间
			/*
			String lasttimeStr = BIUtil.getCookie(BIConsts.Cookies_Prefix + Cookies_Token_LastTime, request);
			if(BIUtil.isNotEmpty(lasttimeStr)) {
				Long lastTime = Long.valueOf(lasttimeStr);
				Long currentTime = System.currentTimeMillis();
				Long tokenExpireMinute = Long.valueOf(SC.v("token.expire.minute", BIConsts.Token_Expire_Minute + ""));
				if((currentTime - lastTime) > (tokenExpireMinute * 60 * 1000)) {
					result.set(false, TokenErrorType.Expired.getCode());
					return result;
				}
			}
			*/

			DecodedJWT dt = JWT.decode(token);

			// 获取用户名
			Claim claim = JWT.decode(token).getClaim("email");

			Set<String> allEmails = new HashSet<String>();

			// 多个邮箱
			String[] emails = claim.asArray(String.class);
			if(emails != null && emails.length > 0) {
				allEmails.addAll(Arrays.asList(emails));
			}

			// 多个邮箱
			List<String> emailList = claim.asList(String.class);
			if(BIUtil.isNotEmpty(emailList)) {
				allEmails.addAll(emailList);
			}

			// 单个邮箱
			String email = claim.asString();
			if(BIUtil.isNotEmpty(email)) {
				allEmails.add(email);
			}

			if(BIUtil.isNotEmpty(allEmails)) {
				allEmails = allEmails.stream().filter(s -> {
					String[] excludePrefixs = SC.v("user.name.exclude.prefix", "dm").split(",");
					boolean found = false;
					for (String p : excludePrefixs) {
						if (s != null && s.toLowerCase().startsWith(p)) {
							found = true;
							break;
						}
					}
					return !found;
				}).collect(Collectors.toSet());
			}

			// 添加特殊账号映射配置：处理只有姓名没有邮箱的账号信息
			if(BIUtil.isEmpty(allEmails)) {
				String userRealName = dt.getClaim("name").asString();
				String mappingEmail = getEmailFromMapping(userRealName);
				if(BIUtil.isNotEmpty(mappingEmail)) {
					allEmails.add(mappingEmail);
				}
			}

			if(BIUtil.isEmpty(allEmails)) {
				result.set(false, TokenErrorType.None_User.getCode());
				return result;
			}

			String userName = allEmails.iterator().next().toLowerCase();//.replace("@example.com", "");
			userName = userName.substring(0, userName.indexOf("@"));

			if(isFreeCheck){
				user = new User(userName);
			}else {
				// 校验用户合法性
				UserService userService = (UserService) SpringContextUtil.getBean("userService");
				user = userService.queryByName(userName);
				if (user == null) {
					result.set(false, TokenErrorType.Invalid_User.getCode());
					return result;
				}
			}
			
			// 校验token
//			String signCode = BIUtil.isEmpty(user.getSignCode()) ? user.getName() : user.getSignCode();
//			JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(signCode)).buildSubQuerySql();
//			jwtVerifier.verify(token);
			
			// 将用户返回，避免重复查询
			user.setToken(token);

			//获取设备指纹
			String blackBox = request.getHeader(BIConsts.USER_BLACK_BOX);

			//异常场景处理。过长的设备指纹丢弃
			if(StrUtil.isNotEmpty(blackBox)) {
				if (blackBox.length() > 50) {
					blackBox = "";
				}
			}

			user.setBlackBox(blackBox);

			result.setData(user);
			
			// cookie中没有，则添加token
			BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token, token, response);
			// cookies中写入当前用户最新访问时间
//			BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token_LastTime, System.currentTimeMillis() + "", response);
		}catch(Exception e) {
			e.printStackTrace();
			result.set(false, TokenErrorType.Check_Error.getCode());
		}
		
		return result;
	}
	
	/**
	 * 删除token
	 */
	public static void remove(HttpServletRequest request, HttpServletResponse response) {
		BIUtil.removeCookie(BIConsts.Cookies_Prefix + Cookies_Token, request,  response);
		BIUtil.removeCookie(BIConsts.Cookies_Prefix + Cookies_Token_LastTime, request, response);
	}
	
	public static void add(HttpServletRequest request, HttpServletResponse response, String token) {
		BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token, token, response);
		BIUtil.addCookie(BIConsts.Cookies_Prefix + Cookies_Token_LastTime, System.currentTimeMillis() + "", response);
	}

	public static String getEmailFromMapping(String userRealName) {
		String userEmails = SC.v("user.email.mapping", "");
		if(BIUtil.isEmpty(userRealName) || StringUtil.isEmpty(userEmails)) {
			return "";
		}
		Optional<String> emailMapping = Arrays.asList(userEmails.split(",")).stream().filter(s -> {
			return s.startsWith(userRealName + "=");
		}).findFirst();

		String email = "";
		if(emailMapping != null && BIUtil.isNotEmpty(emailMapping.get())) {
			email = emailMapping.get().replace(userRealName + "=", "").trim();
		}
		return email;
	}
	
}
