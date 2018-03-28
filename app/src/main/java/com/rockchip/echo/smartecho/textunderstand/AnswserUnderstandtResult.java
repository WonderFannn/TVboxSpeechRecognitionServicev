package com.rockchip.echo.smartecho.textunderstand;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yhc on 16-10-23.
 */

public class AnswserUnderstandtResult extends TextUnderstandResult {

    @Override
    protected String parser(String understandText) {
        JSONObject textobj = null;
        String answerText = null;
        try {
            textobj = new JSONObject(understandText);
            if(textobj == null) {
                return null;
            }
            JSONObject answer = textobj.getJSONObject("answer");
            String answerType = answer.getString("type");
            if(answerType.equals("T")) {
                answerText = answer.getString("text");
            }
            return answerText;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
