package com.bi.queryer.util.component.datagrid.alarm;

public abstract class GreenFontAlarm extends Alarm{
	@Override
	public AlarmType getType() {
		return AlarmType.Green_Font;
	}
}
