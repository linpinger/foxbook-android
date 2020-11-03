package com.linpinger.tool;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

// 作用: Android 下使用 SharedPreferences 来保存数据
public class FoxCFG {
    Context ctx;
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    String cfgName ;

    public FoxCFG(Context iContext) {
        this(iContext, iContext.getPackageName() + "_preferences"); // getDefaultSharedPreferencesName 使用默认
    }
    public FoxCFG(Context iContext, String iConfigName) {
        ctx = iContext;
        settings = ctx.getSharedPreferences(iConfigName, ctx.MODE_PRIVATE);
        editor = settings.edit();
        cfgName = iConfigName;
    }

    public void exportFile(String oPath) {
        File cfgFile = new File(ctx.getFilesDir().getParentFile(), "shared_prefs/" + cfgName + ".xml");
        File bakFile = new File(oPath);
        ToolJava.renameIfExist(bakFile);
        ToolJava.copyFile(cfgFile, bakFile);
    }
    public void importFile(String iPath) {
        File cfgFile = new File(ctx.getFilesDir().getParentFile(), "shared_prefs/" + cfgName + ".xml");
        File bakFile = new File(iPath);
        ToolJava.renameIfExist(cfgFile);
        ToolJava.copyFile(bakFile, cfgFile);
    }

    public boolean contains(String iKey) {
        return settings.contains(iKey);
    }
/*
    public Object get(String iKey, Object defValue) {
        if (defValue instanceof String) {
            return settings.getString(iKey, (String) defValue);
        } else if (defValue instanceof Boolean) {
            return settings.getBoolean(iKey, (Boolean) defValue);
        } else if (defValue instanceof Float) {
            return settings.getFloat(iKey, (Float) defValue);
        } else if (defValue instanceof Integer) {
            return settings.getInt(iKey, (Integer) defValue);
        } else if (defValue instanceof Long) {
            return settings.getLong(iKey, (Long) defValue);
        }
        return null;
    }
*/
    public String getString(String iKey) { // 默认: ""
        return getString(iKey, "");
    }
    public String getString(String iKey, String defValue) {
        return settings.getString(iKey, defValue);
    }

    public boolean getBoolean(String iKey) { // 默认: false
        return getBoolean(iKey, false);
    }
    public boolean getBoolean(String iKey, boolean defValue) {
        return settings.getBoolean(iKey, defValue);
    }

    public float getFloat(String iKey) {
        return getFloat(iKey, 1.0f);
    }
    public float getFloat(String iKey, float defValue ) {
        return settings.getFloat(iKey, defValue);
    }

/*
    public FoxCFG put(String iKey, Object iValue) {
        if (iValue instanceof String) {
            editor.putString(iKey, (String) iValue);
        } else if (iValue instanceof Boolean) {
            editor.putBoolean(iKey, (Boolean) iValue);
        } else if (iValue instanceof Float) {
            editor.putFloat(iKey, (Float) iValue);
        } else if (iValue instanceof Integer) {
            editor.putInt(iKey, (Integer) iValue);
        } else if (iValue instanceof Long) {
            editor.putLong(iKey, (Long) iValue);
        }
//        editor.apply();
        editor.commit();
        return this;
    }
*/
    public FoxCFG putString(String iKey, String iValue) {
        editor.putString(iKey, iValue);
        editor.commit();
        return this;
    }
    public FoxCFG putBoolean(String iKey, boolean iValue) {
        editor.putBoolean(iKey, iValue);
        editor.commit();
        return this;
    }
    public FoxCFG putFloat(String iKey, float iValue) {
        editor.putFloat(iKey, iValue);
        editor.commit();
        return this;
    }

}
