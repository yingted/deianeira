package com.yingted.yelpinfractions;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by ted on 3/14/15.
 */
public class XposedDelegate implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg) throws Throwable {
        if (!"com.yelp.android".equals(pkg.packageName))
            return;
        log("Installing hooks");
        findAndHookMethod("com.yelp.android.ui.activities.businesspage.BusinessPageFragment", pkg.classLoader, "onActivityCreated", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("onActivityCreated");
                final Object thiz = param.thisObject;
                log("thiz: " + thiz);
                final Bundle bundle = (Bundle) param.args[0];
                final Activity activity;
                if (true) {
                    for (Class<?> cls = thiz.getClass(); !Object.class.equals(cls); cls = cls.getSuperclass()) {
                        log("cls: " + cls);
                    }
                }
                if (thiz instanceof android.app.Fragment)
                    activity = ((android.app.Fragment) thiz).getActivity();
                else if (thiz instanceof android.support.v4.app.Fragment)
                    activity = ((android.support.v4.app.Fragment) thiz).getActivity();
                else
                    return;
                log("Created business page: " + activity);
                final View root = activity.getWindow().getDecorView().getRootView();
                log("Root view: " + root);
            }
        });
        findAndHookMethod("com.yelp.android.ui.panels.businesssearch.BusinessAdapter", pkg.classLoader, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("getView");
                final Object result = param.getResult();
                if (!(result instanceof View))
                    return;
                final View view = (View)result;
                log("Created business search item view: " + view);
            }
        });
    }

    protected static void log(String message) {
        XposedBridge.log("YelpInfractions: " + message);
    }
}
