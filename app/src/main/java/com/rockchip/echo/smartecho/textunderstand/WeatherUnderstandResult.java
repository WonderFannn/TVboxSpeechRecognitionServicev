package com.rockchip.echo.smartecho.textunderstand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by yhc on 16-10-23.
 */

public class WeatherUnderstandResult extends TextUnderstandResult {

    public WeatherUnderstandResult() {

    }

    public WeatherUnderstandResult(String text) {
        super(text);
    }

    @Override
    protected String parser(String understandText) {
        String ttstext = null;
        JSONObject textobj = null;
        try {
            textobj = new JSONObject(understandText);
            if(textobj == null) {
                return null;
            }
            JSONObject data = textobj.getJSONObject("data");
            JSONArray result = data.getJSONArray("result");
            JSONObject todayResult = result.getJSONObject(0);
            JSONObject semantic = textobj.getJSONObject("semantic");
            JSONObject slots = semantic.getJSONObject("slots");
            JSONObject location = slots.getJSONObject("location");
            JSONObject datetime = slots.getJSONObject("datetime");
            String weather = todayResult.getString("weather");
            String tempRange = todayResult.getString("tempRange");
            String wind = todayResult.getString("wind");
            String humidity = todayResult.getString("humidity");
            String airQuality = todayResult.getString("airQuality");
            String city = todayResult.getString("city");
            String date = parserDatetime(datetime);
//            ttstext = city + date + "天气" + weather + " " + tempRange + " " + wind + " 湿度" +
//                    humidity + " 空气质量" + airQuality;
            ttstext = city + date + "天气" + weather + " " + tempRange + " 空气质量" + airQuality;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(ttstext == null || "".equals(ttstext)) {
            ttstext = DEFAULT_TEXT_NO_FOUND_ANSWER;
        }
        return ttstext;
    }

    private String parserDatetime(JSONObject datetime) throws JSONException {
        String date = datetime.getString("date");
        String dateOrig = null;
        if(datetime.has("dateOrig")) {
            dateOrig = datetime.getString("dateOrig");
        }
        if("CURRENT_DAY".equals(date)) {
            return "今天";
        }
        if(dateOrig != null) {
            return dateOrig;
        }
        return "";
    }

    private static boolean isToday(String dateStr) {
        Date date = null;
        DateFormat fmt =new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = fmt.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(date != null) {
            isToday(date);
        }
        return false;
    }

    private static boolean isToday(Date date) {
        Date todayDate = new Date(System.currentTimeMillis());
        return isSameDate(todayDate, date);
    }

    private static boolean isSameDate(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        boolean isSameYear = cal1.get(Calendar.YEAR) == cal2
                .get(Calendar.YEAR);
        boolean isSameMonth = isSameYear
                && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
        boolean isSameDate = isSameMonth
                && cal1.get(Calendar.DAY_OF_MONTH) == cal2
                .get(Calendar.DAY_OF_MONTH);

        return isSameDate;
    }
}
