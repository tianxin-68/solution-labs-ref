package com.bi.queryer.sys.error;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.interceptor.FreeCheckAuthority;

@FreeCheckAuthority
@Controller
public class ErrorPageController extends BaseController{
	@RequestMapping(value = "/error/{code}")
    public String error(@PathVariable int code, Model model) {
        String pager = "";
        switch (code) {
            case 403:
                model.addAttribute("code", 404);
                pager = "/common/error_403";
                break;
            case 404:
                model.addAttribute("code", 404);
                pager = "/common/error_404";
                break;
            case 500:
                model.addAttribute("code", 500);
                model.addAttribute("errorInfo", this.params.get("errorInfo"));
                pager = "/common/error_500";
                break;
        }
        return pager;
    }
}
