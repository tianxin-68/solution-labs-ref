package com.bi.queryer.util.excelUtil;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

/**
 * @author contributor
 */
public class DateUtilLocal {

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int BAD_DATE = -1;
    private static final long DAY_MILLISECONDS = 86400000L;
    private static final Pattern TIME_SEPARATOR_PATTERN = Pattern.compile(":");
    private static final Pattern date_ptrn1 = Pattern.compile("^\\[\\$\\-.*?\\]");
    private static final Pattern date_ptrn2 = Pattern.compile("^\\[[a-zA-Z]+\\]");
    private static final Pattern date_ptrn3 = Pattern.compile("^[\\[\\]yYmMdDhHsS\\-/,. :\"\\\\]+0*[ampAMP/]*$");
    private static final Pattern date_ptrn4 = Pattern.compile("^\\[([hH]+|[mM]+|[sS]+)\\]");

    protected DateUtilLocal() {
    }

    public static double getExcelDate(Date date) {
        return getExcelDate(date, false);
    }

    public static double getExcelDate(Date date, boolean use1904windowing) {
        Calendar calStart = new GregorianCalendar();
        calStart.setTime(date);
        return internalGetExcelDate(calStart, use1904windowing);
    }

    public static double getExcelDate(Calendar date, boolean use1904windowing) {
        return internalGetExcelDate((Calendar)date.clone(), use1904windowing);
    }

    private static double internalGetExcelDate(Calendar date, boolean use1904windowing) {
        if (!use1904windowing && date.get(1) < 1900 || use1904windowing && date.get(1) < 1904) {
            return -1.0D;
        } else {
            double fraction = (double)(((date.get(11) * 60 + date.get(12)) * 60 + date.get(13)) * 1000 + date.get(14)) / 8.64E7D;
            Calendar calStart = dayStart(date);
            double value = fraction + (double)absoluteDay(calStart, use1904windowing);
            if (!use1904windowing && value >= 60.0D) {
                ++value;
            } else if (use1904windowing) {
                --value;
            }

            return value;
        }
    }

    public static Date getJavaDate(double date) {
        return getJavaDate(date, false);
    }

    public static Date getJavaDate(double date, boolean use1904windowing) {
        if (!isValidExcelDate(date)) {
            return null;
        } else {
            int wholeDays = (int)Math.floor(date);
            int millisecondsInDay = (int)((date - (double)wholeDays) * 8.64E7D + 0.5D);
            Calendar calendar = new GregorianCalendar();
            setCalendar(calendar, wholeDays, millisecondsInDay, use1904windowing);
            return calendar.getTime();
        }
    }

    public static void setCalendar(Calendar calendar, int wholeDays, int millisecondsInDay, boolean use1904windowing) {
        int startYear = 1900;
        int dayAdjust = -1;
        if (use1904windowing) {
            startYear = 1904;
            dayAdjust = 1;
        } else if (wholeDays < 61) {
            dayAdjust = 0;
        }

        calendar.set(startYear, 0, wholeDays + dayAdjust, 0, 0, 0);
        calendar.set(14, millisecondsInDay);
    }

    public static boolean isADateFormat(int formatIndex, String formatString) {
        if (isInternalDateFormat(formatIndex)) {
            return true;
        } else if (formatString != null && formatString.length() != 0) {
            String fs = formatString;
            StringBuilder sb = new StringBuilder(formatString.length());

            for(int i = 0; i < fs.length(); ++i) {
                char c = fs.charAt(i);
                if (i < fs.length() - 1) {
                    char nc = fs.charAt(i + 1);
                    if (c == '\\') {
                        switch(nc) {
                            case ' ':
                            case ',':
                            case '-':
                            case '.':
                            case '\\':
                                continue;
                        }
                    } else if (c == ';' && nc == '@') {
                        ++i;
                        continue;
                    }
                }

                sb.append(c);
            }

            fs = sb.toString();
            fs = fs.replaceAll("[\"|\']","").replaceAll("[年|月|日|时|分|秒|毫秒|微秒]", "");

            if (date_ptrn4.matcher(fs).matches()) {
                return true;
            } else {
                fs = date_ptrn1.matcher(fs).replaceAll("");
                fs = date_ptrn2.matcher(fs).replaceAll("");

                if (fs.indexOf(59) > 0 && fs.indexOf(59) < fs.length() - 1) {
                    fs = fs.substring(0, fs.indexOf(59));
                }

                return date_ptrn3.matcher(fs).matches();
            }
        } else {
            return false;
        }
    }

    public static boolean isInternalDateFormat(int format) {
        switch(format) {
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 45:
            case 46:
            case 47:
                return true;
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            default:
                return false;
        }
    }

    public static boolean isCellDateFormatted(Cell cell) {
        if (cell == null) {
            return false;
        } else {
            boolean bDate = false;
            double d = cell.getNumericCellValue();
            if (isValidExcelDate(d)) {
                CellStyle style = cell.getCellStyle();
                if (style == null) {
                    return false;
                }

                int i = style.getDataFormat();
                String f = style.getDataFormatString();
                bDate = isADateFormat(i, f);
            }

            return bDate;
        }
    }

    public static boolean isCellInternalDateFormatted(Cell cell) {
        if (cell == null) {
            return false;
        } else {
            boolean bDate = false;
            double d = cell.getNumericCellValue();
            if (isValidExcelDate(d)) {
                CellStyle style = cell.getCellStyle();
                int i = style.getDataFormat();
                bDate = isInternalDateFormat(i);
            }

            return bDate;
        }
    }

    public static boolean isValidExcelDate(double value) {
        return value > -4.9E-324D;
    }

    protected static int absoluteDay(Calendar cal, boolean use1904windowing) {
        return cal.get(6) + daysInPriorYears(cal.get(1), use1904windowing);
    }

    private static int daysInPriorYears(int yr, boolean use1904windowing) {
        if ((use1904windowing || yr >= 1900) && (!use1904windowing || yr >= 1900)) {
            int yr1 = yr - 1;
            int leapDays = yr1 / 4 - yr1 / 100 + yr1 / 400 - 460;
            return 365 * (yr - (use1904windowing ? 1904 : 1900)) + leapDays;
        } else {
            throw new IllegalArgumentException("'year' must be 1900 or greater");
        }
    }

    private static Calendar dayStart(Calendar cal) {
        cal.get(11);
        cal.set(11, 0);
        cal.set(12, 0);
        cal.set(13, 0);
        cal.set(14, 0);
        cal.get(11);
        return cal;
    }

    public static double convertTime(String timeStr) {
        try {
            return convertTimeInternal(timeStr);
        } catch (FormatException var3) {
            String msg = "Bad time format '" + timeStr + "' expected 'HH:MM' or 'HH:MM:SS' - " + var3.getMessage();
            throw new IllegalArgumentException(msg);
        }
    }

    private static double convertTimeInternal(String timeStr) throws FormatException {
        int len = timeStr.length();
        if (len >= 4 && len <= 8) {
            String[] parts = TIME_SEPARATOR_PATTERN.split(timeStr);
            String secStr;
            switch(parts.length) {
                case 2:
                    secStr = "00";
                    break;
                case 3:
                    secStr = parts[2];
                    break;
                default:
                    throw new FormatException("Expected 2 or 3 fields but got (" + parts.length + ")");
            }

            String hourStr = parts[0];
            String minStr = parts[1];
            int hours = parseInt(hourStr, "hour", 24);
            int minutes = parseInt(minStr, "minute", 60);
            int seconds = parseInt(secStr, "second", 60);
            double totalSeconds = (double)(seconds + (minutes + hours * 60) * 60);
            return totalSeconds / 86400.0D;
        } else {
            throw new FormatException("Bad length");
        }
    }

    public static Date parseYYYYMMDDDate(String dateStr) {
        try {
            return parseYYYYMMDDDateInternal(dateStr);
        } catch (FormatException var3) {
            String msg = "Bad time format " + dateStr + " expected 'YYYY/MM/DD' - " + var3.getMessage();
            throw new IllegalArgumentException(msg);
        }
    }

    private static Date parseYYYYMMDDDateInternal(String timeStr) throws FormatException {
        if (timeStr.length() != 10) {
            throw new FormatException("Bad length");
        } else {
            String yearStr = timeStr.substring(0, 4);
            String monthStr = timeStr.substring(5, 7);
            String dayStr = timeStr.substring(8, 10);
            int year = parseInt(yearStr, "year", -32768, 32767);
            int month = parseInt(monthStr, "month", 1, 12);
            int day = parseInt(dayStr, "day", 1, 31);
            Calendar cal = new GregorianCalendar(year, month - 1, day, 0, 0, 0);
            cal.set(14, 0);
            return cal.getTime();
        }
    }

    private static int parseInt(String strVal, String fieldName, int rangeMax) throws FormatException {
        return parseInt(strVal, fieldName, 0, rangeMax - 1);
    }

    private static int parseInt(String strVal, String fieldName, int lowerLimit, int upperLimit) throws FormatException {
        int result;
        try {
            result = Integer.parseInt(strVal);
        } catch (NumberFormatException var6) {
            throw new FormatException("Bad int format '" + strVal + "' for " + fieldName + " field");
        }

        if (result >= lowerLimit && result <= upperLimit) {
            return result;
        } else {
            throw new FormatException(fieldName + " value (" + result + ") is outside the allowable range(0.." + upperLimit + ")");
        }
    }

    private static final class FormatException extends Exception {
        public FormatException(String msg) {
            super(msg);
        }
    }
}
