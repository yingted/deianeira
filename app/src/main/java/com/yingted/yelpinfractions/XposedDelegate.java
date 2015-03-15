package com.yingted.yelpinfractions;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by ted on 3/14/15.
 */
public class XposedDelegate implements IXposedHookLoadPackage {

    private Class<?> fragmentClass;
    private Method fragmentGetActivityMethod;
    private Method fragmentGetArgumentsMethod;
    private Class<?> businessClass;
    private Method businessGetId;

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
    protected final void addInfractionsView(RelativeLayout parent, View fixture, Object business) throws Throwable {
        final String id = (String)businessGetId.invoke(business);
        debug("addInfractionsView: " + id + ": " + parent + " " + fixture);
        ;
    }
    protected final boolean addInfractionsView(ListView listView, Object business) throws Throwable {
        if (listView.getChildCount() <= 0)
            return false;
        final RelativeLayout headerView = findStrictDescendant(listView, RelativeLayout.class);
        final RelativeLayout detailsView = findStrictDescendant(headerView, RelativeLayout.class);
        final RelativeLayout ratingsView = findStrictDescendant(detailsView, RelativeLayout.class);
        addInfractionsView(detailsView, ratingsView, business);
        return true;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam pkg) throws Throwable {
        if (!"com.yelp.android".equals(pkg.packageName))
            return;
        log("Finding methods");
        fragmentClass = pkg.classLoader.loadClass("android.support.v4.app.Fragment");
        fragmentGetActivityMethod = fragmentClass.getMethod("getActivity");
        fragmentGetArgumentsMethod = fragmentClass.getMethod("getArguments");
        businessClass = pkg.classLoader.loadClass("com.yelp.android.serializable.YelpBusiness");
        businessGetId = businessClass.getMethod("getId");
        log("Installing hooks");
        findAndHookMethod("com.yelp.android.ui.activities.businesspage.BusinessPageFragment", pkg.classLoader, "onActivityCreated", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Adding infractions to business page");
                final Object thiz = param.thisObject;
                final Bundle bundle = (Bundle) param.args[0];
                final Activity activity;
                final Bundle arguments;
                if (thiz instanceof Fragment) {
                    Fragment fragment = (Fragment) thiz;
                    activity = fragment.getActivity();
                    arguments = fragment.getArguments();
                } else if (fragmentClass.isInstance(thiz)) {
                    activity = (Activity) fragmentGetActivityMethod.invoke(thiz);
                    arguments = (Bundle) fragmentGetArgumentsMethod.invoke(thiz);
                } else
                    return;
                final Object business = arguments.getParcelable("extra.business");
                final View root = activity.getWindow().getDecorView().getRootView();
                final ListView listView = findDescendant(root, ListView.class);
                if (addInfractionsView(listView, business))
                    return;
                final Adapter adapter = listView.getAdapter();
                final AtomicReference<DataSetObserver> observerReference = new AtomicReference<>();
                final DataSetObserver observer = new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        try {
                            if (addInfractionsView(listView, business))
                                adapter.unregisterDataSetObserver(observerReference.get());
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onInvalidated() {
                        onChanged();
                    }
                };
                observerReference.set(observer);
                adapter.registerDataSetObserver(observer);
            }
        });
        findAndHookMethod("com.yelp.android.ui.panels.businesssearch.BusinessAdapter", pkg.classLoader, "getView", int.class, View.class, ViewGroup.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                log("Adding infractions to business search result");
                final Object result = param.getResult();
                final RelativeLayout group = (RelativeLayout)result;
                int position = (Integer)param.args[0];
                final Object thiz = param.thisObject;
                final Adapter adapter = (Adapter)thiz;
                final Object business = adapter.getItem(position);
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
                addInfractionsView(group, lowest, business);
            }
        });
    }

    protected static void log(String message) {
        XposedBridge.log("YelpInfractions: " + message);
    }
    protected static void debug(String message) { log(message); }
}
