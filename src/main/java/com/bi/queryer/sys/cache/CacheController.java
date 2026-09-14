package com.bi.queryer.sys.cache;

import com.bi.queryer.ssm.query.field.QueryFieldService;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterCacheManager;
import com.bi.queryer.ssm.query.filter.MultiSelectFilterDatasetProvider;
import com.bi.queryer.sys.base.BaseController;
import com.bi.queryer.sys.common.ResponseMessage;
import com.bi.queryer.sys.rmi.RMIServer;
import com.bi.queryer.sys.rmi.impl.MemoryCacheSyncSSMService;
import com.bi.queryer.sys.startup.SystemInitializer;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Scope("prototype")
@RequestMapping("cache")
public class CacheController extends BaseController {

    @Autowired
    private CacheService cacheService = null;

    @Autowired
    private QueryFieldService queryFieldService = null;
    /**
     * 刷新缓存页面
     */
    @RequestMapping(value="flushcache")
    public ModelAndView execute() {
        ModelAndView v = new ModelAndView("/common/error_404");
        return v;
    }
    
    @RequestMapping(value="flush")
	public void flush() {
        ResponseMessage returnMsg = new ResponseMessage();

		try {
            String _modules_key = stringValue(SystemInitializer.INIT_MODULE_KEY);
            if ("User".equalsIgnoreCase(_modules_key)) {
                String userName = stringValue("userName");
                cacheService.flushByUserName(userName);
            }else if(MultiSelectFilterCacheManager.cache_space_key.equalsIgnoreCase(_modules_key)) {
                MultiSelectFilterDatasetProvider.clearCache();
            }else {
                // 刷新所有服务器
                RMIServer.syncInvoke(MemoryCacheSyncSSMService.class, params);
            }

        }catch (Exception e){
            returnMsg = new ResponseMessage(false,e.getMessage());
        }
        writeJSON(JSONObject.fromObject(returnMsg));
	}

    @RequestMapping(value="flush/field")
    public void flushFieldCache() {
        ResponseMessage returnMsg = new ResponseMessage();

        try {
            String fieldId = stringValue("fieldId");
            MultiSelectFilterDatasetProvider.clearCache(fieldId);
        }catch (Exception e){
            returnMsg = new ResponseMessage(false,e.getMessage());
        }
        writeJSON(JSONObject.fromObject(returnMsg));
    }

    @RequestMapping(value="flush/fieldTree")
    public void flushFieldTreeCache(){
        queryFieldService.clearTreeCache(null);
        ResponseMessage returnMsg = new ResponseMessage();
        writeJSON(JSONObject.fromObject(returnMsg));
    }
    
    /**
     * 刷新本机缓存
     */
    @RequestMapping(value="flush/local")
    public void flashLocal() {
    	ResponseMessage returnMsg = new ResponseMessage();
        try{
            String _modules_key = stringValue(SystemInitializer.INIT_MODULE_KEY);
            if("User".equalsIgnoreCase(_modules_key)){
                String userName = stringValue("userName");
                cacheService.flushByUserName(userName);
            }else{
                SystemInitializer.initialize(params);
            }

        }catch (Exception e){
            returnMsg = new ResponseMessage(false,e.getMessage());
        }
        writeJSON(JSONObject.fromObject(returnMsg));
    }
}
