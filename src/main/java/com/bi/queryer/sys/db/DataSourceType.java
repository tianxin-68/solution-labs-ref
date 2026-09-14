package com.bi.queryer.sys.db;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataSourceType {

	public static DataSourceType Default = new DataSourceType("default", "defaultDataSource", "默认数据源", "mysql", "Default");
	public static DataSourceType OLAP = new DataSourceType("olap", "olapDataSource", "olap数据源", "mysql", "OLAP");
	public static DataSourceType OLAP_UT = new DataSourceType("olap_ut", "olapUtDataSource", "olap_ut数据源", "mysql", "OLAP_UT");

	public static DataSourceType ETL = new DataSourceType("etl", "etlDataSource", "ETL数据源", "mysql", "ETL");
	public static DataSourceType Data_Studio = new DataSourceType("data_studio", "dataStudioDataSource", "白皮书指标数据源", "mysql", "Data_Studio");
//	public static DataSourceType Doris_Master = new DataSourceType("doris_master", "dorisMasterDataSource", "doris数据源", "doris", "Doris_Master", true);
//	public static DataSourceType Doris_Slave01 = new DataSourceType("doris_slave01", "dorisSlave01DataSource", "doris数据源(slave01-带熔断规则)", "doris", "Doris_Slave01", false);

	/**
	 * 强制切换为HW
	 */
	public static DataSourceType Doris_Master = new DataSourceType("hw_doris_master", "hwDorisMasterDataSource", "hw-doris数据源", "doris", "HW_Doris_Master", true);
	public static DataSourceType Doris_Slave01 = new DataSourceType("hw_doris_slave01", "hwDorisSlave01DataSource", "hw-doris数据源(slave01-带熔断规则)", "doris", "HW_Doris_Slave01", false);

	public static DataSourceType HW_Doris_Master = new DataSourceType("hw_doris_master", "hwDorisMasterDataSource", "hw-doris数据源", "doris", "HW_Doris_Master", true);
	public static DataSourceType HW_Doris_Slave01 = new DataSourceType("hw_doris_slave01", "hwDorisSlave01DataSource", "hw-doris数据源(slave01-带熔断规则)", "doris", "HW_Doris_Slave01", false);

	public static DataSourceType AGENT = new DataSourceType("agent", "agentDataSource", "agent数据源", "mysql", "AGENT");

	public static DataSourceType Trino_Master = new DataSourceType("trino_master", "trinoMasterDataSource", "trino-master04集群", "trino", "Trino_Master");
	public static DataSourceType Trino_Slave01 = new DataSourceType("trino_slave01", "trinoSlave01DataSource", "trino-master05集群", "trino", "Trino_Slave01");
	public static DataSourceType Trino_Slave02 = new DataSourceType("trino_slave02", "trinoSlave02DataSource", "trino-master06集群", "trino", "Trino_Slave02");


	protected static Map<String, DataSourceType> types = new LinkedHashMap<String, DataSourceType>();
	static {
		init();
	}

	// 用于切换
	private String id = "";

	// 用于获取数据源bean
	private String name = "";
	private String desc = "";
	private String dialect = "";

	// 用于存储和查找
	private String key = "";

	private boolean isMaster = true;

	public DataSourceType(String id, String name, String desc, String dialect, String key){
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.dialect = dialect;
		this.key = key;
		this.isMaster = true;
	}

	public DataSourceType(String id, String name, String desc, String dialect, String key, boolean isMaster){
		this(id, name, desc, dialect, key);
		this.isMaster = isMaster;
	}

	/**
	 * 初始化加载
	 */
	public static void init(){
		add(Default);
		add(OLAP);
		add(OLAP_UT);
		add(ETL);
		add(Data_Studio);
		add(Doris_Master);
		add(Doris_Slave01);
		add(HW_Doris_Master);
		add(HW_Doris_Slave01);
		add(Trino_Master);
		add(Trino_Slave01);
		add(Trino_Slave02);
		add(AGENT);
	}

	public static void add(DataSourceType type){
		types.put(type.getKey(), type);
	}

	public static void remove(String key){
		types.remove(key);
	}

	public static void clear(){
		types.clear();
		init();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static DataSourceType[] values(){
		DataSourceType[] result = new DataSourceType[types.size()];
		types.values().toArray(result);
		return result;
	}

	public static DataSourceType getType(String typeKey){
		for(DataSourceType type : values()){
//			if(type.toString().equalsIgnoreCase(typeStr)){
			if(type.getKey().equalsIgnoreCase(typeKey)){
				return type;
			}
		}
		return Default;
	}

	public static DataSourceType getTypeById(String id){
		for(DataSourceType type : values()){
			if(type.getId().equalsIgnoreCase(id)){
				return type;
			}
		}
		return Default;
	}

	public boolean isDoris(){
		return this == HW_Doris_Master || this == HW_Doris_Slave01 || this == Doris_Master || this == Doris_Slave01;
	}

	public boolean isTrino(){
		return this == Trino_Master || this == Trino_Slave01 || this == Trino_Slave02;
	}

	public boolean isHWDoris(){
		return this == HW_Doris_Master || this == HW_Doris_Slave01;
	}

	public boolean isIdcDoris(){
		return this == Doris_Master || this == Doris_Slave01;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getDialect() {
		return dialect;
	}

	public void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return this.key;
	}

}
