package com.yingted.yelpinfractions;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by ted on 3/14/15.
 */
public class XposedDelegate implements IXposedHookLoadPackage {
    protected final <ViewClass extends View> ViewClass findDescendant(View view, Class<ViewClass> cls) {
        if (cls.isInstance(view))
            return cls.cast(view);
        return findStrictDescendant(view, cls);
    }
    protected final <ViewClass extends View> ViewClass findStrictDescendant(View view, Class<ViewClass> cls) {
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0, len = group.getChildCount(); i < len; ++i) {
                final ViewClass res = findDescendant(group.getChildAt(i), cls);
                if (res != null)
                    return res;
            }
        }
        return null;
    }
    protected final void addInfractionsView(RelativeLayout parent, View fixture) {
        debug("addInfractionsView: " + parent + " " + fixture);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg) throws Throwable {
        if (!"com.yelp.android".equals(pkg.packageName))
            return;
        log("Finding Fragment#getActivity");
        final Class<?> fragmentClass = pkg.classLoader.loadClass("android.support.v4.app.Fragment");
        final Method fragmentGetActivityMethod = fragmentClass.getDeclaredMethod("getActivity");
        log("Installing hooks");
        findAndHookMethod("com.yelp.android.ui.activities.businesspage.BusinessPageFragment", pkg.classLoader, "onActivityCreated", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Adding infractions to business page");
                final Object thiz = param.thisObject;
                debug("thiz: " + thiz);
                final Bundle bundle = (Bundle) param.args[0];
                final Activity activity;
                if (thiz instanceof Fragment)
                    activity = ((Fragment) thiz).getActivity();
                else if (fragmentClass.isInstance(thiz))
                    activity = (Activity)fragmentGetActivityMethod.invoke(thiz);
                else
                    return;
                final View root = activity.getWindow().getDecorView().getRootView();
                debug("Root view: " + root);
                final ListView listView = findDescendant(root, ListView.class);
                debug("List view: " + listView);
                final RelativeLayout headerView = findStrictDescendant(listView, RelativeLayout.class);
                debug("Header view: " + headerView);
                final RelativeLayout detailsView = findStrictDescendant(headerView, RelativeLayout.class);
                debug("Details view: " + detailsView);
                final RelativeLayout ratingsView = findStrictDescendant(detailsView, RelativeLayout.class);
                debug("Ratings view: " + ratingsView);
                addInfractionsView(detailsView, ratingsView);
            }
        });
        findAndHookMethod("com.yelp.android.ui.panels.businesssearch.BusinessAdapter", pkg.classLoader, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Adding infractions to business search result");
                final Object result = param.getResult();
                if (!(result instanceof RelativeLayout)) {
                    debug("Not a relative layout");
                    return;
                }
                final RelativeLayout group = (RelativeLayout)result;
                debug("Created business search item view: " + group);
                final int width = group.getWidth();
                int maxBottom = 0;
                View lowest = null;
                for (int i = 0, len = group.getChildCount(); i < len; ++i) {
                    final View child = group.getChildAt(i);
                    if (child.getLeft() * 2 < width)
                        continue;
                    final int childBottom = child.getBottom();
                    if (childBottom >= maxBottom) {
                        maxBottom = childBottom;
                        lowest = child;
                    }
                }
                if (lowest == null) {
                    debug("No fixture to hang view from");
                    return;
                }
                addInfractionsView(group, lowest);
            }
        });
    }

    protected static void log(String message) {
        XposedBridge.log("YelpInfractions: " + message);
    }
    protected static void debug(String message) { log(message); }
}
