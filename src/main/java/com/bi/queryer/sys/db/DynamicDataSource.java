package com.bi.queryer.sys.db;

import com.bi.queryer.sys.base.BaseDao;
import com.bi.queryer.sys.exception.BIException;
import com.bi.queryer.sys.startup.Initializable;
import com.bi.queryer.util.SpringContextUtil;
import com.bi.queryer.util.encryption.DesEncryption;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DynamicDataSource extends AbstractRoutingDataSource implements Initializable {

    /**
     * 数据源应用平台
     */
    private final String DB_PLATFORM = "api";

    /**
     * 系统初始化时加载
     * @param initParams
     * @throws BIException
     */
    @Override
    public void initialize(Map<String, ?> initParams) throws BIException {
        try{
            BaseDao dao = (BaseDao) SpringContextUtil.getBean("baseDao");

            Map<String,Object> map = new HashMap<>();
            map.put("type",DB_PLATFORM);
            List<DataSourceBean> dataSourceBeanList = (List<DataSourceBean>) dao.queryObjectList("datasource.queryAllDataSource", map);
            Map<Object, Object> targetSourceMap = this.getTargetSource();

            // 清理数据源类型列表
            DataSourceType.clear();
            DataSourceType types[] = DataSourceType.values();
            for(DataSourceBean dataSourceBean : dataSourceBeanList) {
                Object ds = createDataSource(dataSourceBean);
                targetSourceMap.put(dataSourceBean.getId(), ds);

                // 添加到数据源类型中
                String dsId = dataSourceBean.getId();
                DataSourceType.add(new DataSourceType(dsId, dsId, dataSourceBean.getName(), dataSourceBean.getDbType(), dsId));
            }
            types = DataSourceType.values();
            super.afterPropertiesSet();

        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 连接数据源前,调用该方法
     */
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getType();
    }
    /**
     * 根据数据源信息在spring中创建bean,并返回
     * @param dataSourceBean 数据源信息
     * @return
     * @throws IllegalAccessException
     */
    public Object createDataSource(DataSourceBean dataSourceBean) throws IllegalAccessException {
        //1.将applicationContext转化为ConfigurableApplicationContext
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) SpringContextUtil.getContext();
        //2.获取bean工厂并转换为DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory =  (DefaultListableBeanFactory) context.getBeanFactory();
        /*
         * 3.本文用的是DruidDataSource,所有在这里我们获取的是该bean的BeanDefinitionBuilder,
         * 通过BeanDefinitionBuilder来创建bean定义
         */
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(XBasicDataSource.class);
        /**
         * 4.获取DataSourceBean里的属性和对应值,并将其交给BeanDefinitionBuilder创建bean的定义
         */
        /*
        Map<String, Object> propertyKeyValues = getPropertyKeyValues(DataSourceBean.class, dataSourceBean);
        for(Map.Entry<String,Object> entry : propertyKeyValues.entrySet()) {
            beanDefinitionBuilder.addPropertyValue(entry.getKey(), entry.getValue());
        }
         */
        DBType dbType = DBType.getType(dataSourceBean.getDbType());
        String url = String.format(dbType.getUrl(), dataSourceBean.getDbIp(), dataSourceBean.getDbPort(), dataSourceBean.getDbName());
        beanDefinitionBuilder.addPropertyValue("driverClassName", dbType.getDriverClass());
        beanDefinitionBuilder.addPropertyValue("url", url);
        beanDefinitionBuilder.addPropertyValue("username", dataSourceBean.getDbUser());
        String pwd = DesEncryption.decrypt(dataSourceBean.getDbPassword());
        beanDefinitionBuilder.addPropertyValue("password", pwd);
        beanDefinitionBuilder.addPropertyValue("validationQuery", dbType.getValidationQuery());
        beanDefinitionBuilder.addPropertyValue("initialSize", 2);
        beanDefinitionBuilder.addPropertyValue("maxTotal", 100);
        beanDefinitionBuilder.addPropertyValue("maxIdle", 20);
        beanDefinitionBuilder.addPropertyValue("maxWaitMillis", 30000);
        beanDefinitionBuilder.addPropertyValue("defaultAutoCommit", true);
        beanDefinitionBuilder.addPropertyValue("removeAbandonedTimeout", 180);
        beanDefinitionBuilder.addPropertyValue("logAbandoned", true);
        beanDefinitionBuilder.addPropertyValue("testOnBorrow", true);
        beanDefinitionBuilder.addPropertyValue("testOnReturn", true);
        beanDefinitionBuilder.addPropertyValue("testWhileIdle", true);
        beanDefinitionBuilder.addPropertyValue("minEvictableIdleTimeMillis", 180000);
        beanDefinitionBuilder.addPropertyValue("timeBetweenEvictionRunsMillis", 60000);

        //5.bean定义好以后,将其交给beanFactory注册成bean对象，由spring容器管理
        beanFactory.registerBeanDefinition(dataSourceBean.getId(), beanDefinitionBuilder.getBeanDefinition());
        //6.最后获取步骤5生成的bean,并将其返回
        return context.getBean(dataSourceBean.getId());
    }

    //获取类属性和对应的值,放入Map中
    @SuppressWarnings("unused")
    private <T> Map<String, Object> getPropertyKeyValues(Class<T> clazz, Object object) throws IllegalAccessException {
        Field[] fields = clazz.getDeclaredFields();
        Map<String,Object> map = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(object));
        }
        map.remove("beanName");
        return map;
    }
    //通过反射获取AbstractRoutingDataSource的targetDataSources属性
    @SuppressWarnings("unchecked")
    public Map<Object, Object> getTargetSource() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = AbstractRoutingDataSource.class.getDeclaredField("targetDataSources");
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(this);
    }

}
