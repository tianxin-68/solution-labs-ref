package com.bi.queryer.util.component.datagrid.alarm;

public abstract class YellowFlagAlarm extends Alarm{
	@Override
	public AlarmType getType() {
		return AlarmType.Yellow_Flag;
	}
}
