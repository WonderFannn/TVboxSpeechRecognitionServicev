package com.rockchip.echo.smartecho;

import android.os.FileObserver;

import com.rockchip.echo.util.LogUtil;

import org.apache.http.util.EncodingUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * Created by yhc on 16-11-20.
 */

public class CaeWakeUpFileObserver extends FileObserver {

    CaeWakeupListener mCaeWakeupListener;
    static final String CAE_WAKEUP_FILE = "/data/cae_wakeup";
    int mAngle = -1;
    int mChanel = -1;

    public CaeWakeUpFileObserver(CaeWakeupListener caeWakeupListener) {
        super(CAE_WAKEUP_FILE);
        mCaeWakeupListener = caeWakeupListener;
    }

    public boolean getCAEWakeState() {
        boolean isWakeup = false;
        try {
            String cae_wakeup_file_str = readStringFromFile(CAE_WAKEUP_FILE);
            LogUtil.d("===== read " + CAE_WAKEUP_FILE +" : " + cae_wakeup_file_str);
            String[] temp_str = cae_wakeup_file_str.split(" ");
            if(temp_str != null && !cae_wakeup_file_str.equals("")) {
                if(temp_str[0] != null && !temp_str[1].equals("")) {
                    if("true".equals(temp_str[0])) {
                        isWakeup = true;
                    }
                }
                if(temp_str[1] != null && !temp_str[1].equals("")) {
                    mAngle = Integer.parseInt(temp_str[1]);
                }
                if(temp_str[2] != null && !temp_str[1].equals("")) {
                    mChanel = Integer.parseInt(temp_str[2]);
                }
                LogUtil.d("===== mIsWakeup: " + isWakeup + " mAngle: " + mAngle
                        + " mChanel: " + mChanel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isWakeup;
    }

    private String readStringFromFile(String fileName) throws IOException{
        String res = "";
        try {
            FileInputStream fin = new FileInputStream(new File(CAE_WAKEUP_FILE));
            int length = fin.available();
            byte [] buffer = new byte[length];
            fin.read(buffer);
            res = EncodingUtils.getString(buffer, "UTF-8");
            fin.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public void setCaeWakeupState(boolean isWakeup, int angle, int chanel) {
        String write_str = isWakeup + " " + angle + " " + chanel;
        LogUtil.d("====== update " + CAE_WAKEUP_FILE + " : " + write_str);
        try{
            FileOutputStream fout = new FileOutputStream(CAE_WAKEUP_FILE);
            byte [] bytes = write_str.getBytes();
            fout.write(bytes);
            fout.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setCaeWakeupState(boolean isWakeup) {
        setCaeWakeupState(isWakeup, mAngle, mChanel);
    }

    @Override
    public void onEvent(int i, String s) {
        if (i == FileObserver.MODIFY) {
            LogUtil.d("====== " + CAE_WAKEUP_FILE + " has been modify, read it go!");
            boolean isWakeup = getCAEWakeState();
            if(isWakeup) {
                mCaeWakeupListener.onWakeUp(mAngle, mChanel);
                setCaeWakeupState(false);
            }
        }
    }
}
