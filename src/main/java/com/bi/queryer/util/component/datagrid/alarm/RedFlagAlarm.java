package com.bi.queryer.util.component.datagrid.alarm;

public abstract class RedFlagAlarm extends Alarm{
	@Override
	public AlarmType getType() {
		return AlarmType.Red_Flag;
	}
}
