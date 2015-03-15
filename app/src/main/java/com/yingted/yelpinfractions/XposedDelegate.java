package com.yingted.yelpinfractions;

import android.app.Activity;
import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by ted on 3/14/15.
 */
public class XposedDelegate implements IXposedHookLoadPackage {

    private Class<?> fragmentClass;
    private Method fragmentGetActivityMethod;
    private Method fragmentGetArgumentsMethod;
    private Class<?> businessClass;
    private Method businessGetId;

    protected final <ViewClass extends View> ViewClass findDescendant(final View view, final Class<ViewClass> cls) {
        if (cls.isInstance(view))
            return cls.cast(view);
        return findStrictDescendant(view, cls);
    }

    protected final <ViewClass extends View> ViewClass findStrictDescendant(final View view, final Class<ViewClass> cls) {
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

    private final WeakHashMap<RelativeLayout, TextView> infractionsViewCache = new WeakHashMap<>();

    protected final void setInfractionsView(final RelativeLayout parent, final View fixture, final Object business) throws Throwable {
        final String id = (String) businessGetId.invoke(business);
        TextView view = infractionsViewCache.get(parent);
        if (view == null) {
            view = addInfractionsView(parent, fixture);
            infractionsViewCache.put(parent, view);
        }
        updateInfractionsView(view, id);
    }

    private final OkHttpClient client = new OkHttpClient();
    private final void updateInfractionsView(final TextView view, final String id) {
        view.setText("");
        view.setTextColor(0);
        view.post(new Runnable() {
            @Override
            public void run() {
                debug("Get colour for " + id);
                final String url;
                try {
                    url = "https://www.yingted.com/static/test.json?" + URLEncoder.encode(id, "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    XposedBridge.log(e);
                    return;
                }
                final Request request = new Request.Builder()
                        .url(url)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        XposedBridge.log(e);
                    }
                    @Override
                    public void onResponse(Response response) throws IOException {
                        final String data = response.body().string();
                        final JSONObject obj;
                        try {
                            obj = new JSONObject(data);
                        } catch (final JSONException e) {
                            XposedBridge.log(e);
                            return;
                        }
                        final String textString = obj.optString("text");
                        final int color = obj.optInt("color");
                        final boolean html = obj.optBoolean("html");
                        final CharSequence text;
                        if (html)
                            text = Html.fromHtml(textString);
                        else
                            text = textString;
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setText(text);
                                view.setTextColor(color);
                                if (html)
                                    view.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                        });
                    }
                });
            }
        });
    }
    private final TextView addInfractionsView(final RelativeLayout parent, final View fixture) {
        final TextView view = new TextView(parent.getContext());
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        final int fixtureId = fixture.getId();
        lp.addRule(RelativeLayout.BELOW, fixtureId);
        lp.addRule(RelativeLayout.ALIGN_RIGHT, fixtureId);
        int viewId = Integer.MAX_VALUE;
        for (int i = 0, len = parent.getChildCount(); i < len; ++i) {
            final View child = parent.getChildAt(i);
            viewId = Math.min(viewId, child.getId() - 1000000);
        }
        view.setId(viewId);
        parent.addView(view, lp);
        return view;
    }

    protected final boolean setInfractionsView(final ListView listView, final Object business) throws Throwable {
        if (listView.getChildCount() <= 0)
            return false;
        final RelativeLayout headerView = findStrictDescendant(listView, RelativeLayout.class);
        final RelativeLayout detailsView = findStrictDescendant(headerView, RelativeLayout.class);
        final RelativeLayout ratingsView = findStrictDescendant(detailsView, RelativeLayout.class);
        setInfractionsView(detailsView, ratingsView, business);
        return true;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam pkg) throws Throwable {
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
                debug("Adding infractions to business page");
                final Object thiz = param.thisObject;
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
                if (setInfractionsView(listView, business))
                    return;
                final Adapter adapter = listView.getAdapter();
                final AtomicReference<DataSetObserver> observerReference = new AtomicReference<>();
                final DataSetObserver observer = new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        try {
                            if (setInfractionsView(listView, business))
                                adapter.unregisterDataSetObserver(observerReference.get());
                        } catch (final Throwable e) {
                            XposedBridge.log(e);
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
                debug("Adding infractions to business search result");
                final RelativeLayout group = (RelativeLayout) param.getResult();
                final int position = (Integer) param.args[0];
                final Object thiz = param.thisObject;
                final Adapter adapter = (Adapter) thiz;
                // XXX preload adapter
                final Object business = adapter.getItem(position);
                group.post(new Runnable() {
                    @Override
                    public void run() {
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
                        try {
                            setInfractionsView(group, lowest, business);
                        } catch (final Throwable e) {
                            XposedBridge.log(e);
                        }
                    }
                });
            }
        });
    }

    protected static void log(final String message) {
        XposedBridge.log("YelpInfractions: " + message);
    }

    protected static void debug(final String message) {
        //log(message);
    }
}
