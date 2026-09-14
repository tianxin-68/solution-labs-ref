package com.bi.queryer.util.component.datagrid.alarm;

public class AlarmFactory {

	public static Alarm create(String type, final String expression) {
		AlarmType alarmType = AlarmType.getType(type);
		Alarm alarm = null;
		switch (alarmType) {
		case Red_Font:
			alarm = new RedFontAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Yellow_Font:
			alarm = new YellowFontAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Green_Font:
			alarm = new GreenFontAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Red_Flag:
			alarm = new RedFlagAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Yellow_Flag:
			alarm = new YellowFlagAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Green_Flag:
			alarm = new GreenFlagAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Red_Arrow:
			alarm = new RedArrowAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Yellow_Arrow:
			alarm = new YellowArrowAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		case Green_Arrow:
			alarm = new GreenArrowAlarm() {
				@Override
				public String expression() {
					return expression;
				}
			};
			break;
		}

		return alarm;
	}

}
