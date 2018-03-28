package com.rockchip.echo.smartecho.textunderstand;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yhc on 16-10-23.
 */

public class TextUnderstandResult {

    public int rc = 0;
    public String text = "";
    public String history = "";
    public String service = "";
    public String operation = "";

    public static final String DEFAULT_TEXT_NO_FOUND_ANSWER = "没有为你查到";

    public TextUnderstandResult mTextUnderstandResult;

    public TextUnderstandResult() {

    }

    public TextUnderstandResult(String text) {
        setTextUnderstandText(text);
    }

    protected String mUnderstandText;
    protected String mTtsText = null;

    public String getTtsText() {
        return mTtsText;
    }

    public void setTextUnderstandText(String text) {
        mUnderstandText = text;
        parserFirst(mUnderstandText);
    }

    protected void parserFirst(String understandText) {
        JSONObject textobj = null;
        try {
            textobj = new JSONObject(understandText);
            if(textobj == null) {
                return;
            }
            rc = textobj.getInt("rc");
            if(rc == 0) {
                service = textobj.getString("service");
                operation = textobj.getString("operation");
                Log.d("rk", "TextUnderstandResult - service: " + service + " operation: " + operation);
                if("QUERY".equals(operation) && "weather".equals(service)) {
                    mTextUnderstandResult = new WeatherUnderstandResult();
                } else if("ANSWER".equals(operation)) {
                    mTextUnderstandResult = new AnswserUnderstandtResult();
                }
                mTtsText = mTextUnderstandResult.parser(understandText);
                Log.d("rk", "TextUnderstandResult - parser tts text: " + mTtsText);
            } else if(rc == 4) {
                Log.d("rk", "TextUnderstandResult - rc=4, can't understand text");
                String text = textobj.getString("text");
                if(text.equals("。")) {
                    return;
                }
                mTtsText =DEFAULT_TEXT_NO_FOUND_ANSWER;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected String parser(String understandText) {
        return  null;
    }

}
