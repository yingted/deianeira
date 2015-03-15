package com.yingted.yelpinfractions;

import android.app.Activity;
import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private final class Infraction {
        List<Runnable> callbacks = new ArrayList<>();
        String id;
        CharSequence text;
        int color;
        boolean html;
        volatile boolean finished;
        protected void fetch() {
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
                        obj = new JSONArray(data).getJSONObject(0);
                    } catch (final JSONException e) {
                        XposedBridge.log(e);
                        return;
                    }
                    final String textString = obj.optString("text");
                    color = obj.optInt("color");
                    html = obj.optBoolean("html");
                    if (html)
                        text = Html.fromHtml(textString);
                    else
                        text = textString;
                    complete();
                }
            });
        }
        protected void complete() {
            finished = true;
            for (final Runnable callback : callbacks)
                callback.run();
            callbacks.clear();
        }
        protected void when(Runnable runnable) {
            if (finished)
                runnable.run();
            else
                callbacks.add(runnable);
        }
    }
    final LruCache<String, Infraction> infractionsCache = new LruCache<>(1024);
    private Infraction getInfraction(final String id) {
        Infraction infraction = infractionsCache.get(id);
        if (infraction == null) {
            infraction = new Infraction();
            infraction.id = id;
            infraction.fetch();
            infractionsCache.put(id, infraction);
        }
        return infraction;
    }
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    final String endpoint = "https://www.yingted.com/static/test.json";
    private List<Infraction> getInfractions(final List<String> ids) throws Throwable {
        final List<Infraction> infractions = new ArrayList<>(ids.size());
        final JSONArray query = new JSONArray();
        final int[] infractionIndex = new int[ids.size()]; // big enough
        {
            for (int i = 0, j = 0, len = ids.size(); i < len; ++i) {
                final String id = ids.get(i);
                Infraction infraction = infractionsCache.get(id);
                if (infraction == null) {
                    infraction = new Infraction();
                    infraction.id = id;
                    infractionsCache.put(id, infraction);
                    infractionIndex[j] = i;
                    query.put(j++, id);
                }
                infractions.set(i, infraction);
            }
        }
        final String json = query.toString();
        final RequestBody body = RequestBody.create(JSON, json);
        final Request request = new Request.Builder()
                .url(endpoint)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                XposedBridge.log(e);
            }
            @Override
            public void onResponse(Response response) throws IOException {
                final String data = response.body().string();
                try {
                    final JSONArray array = new JSONArray(data);
                    // assert query.length() == array.length();
                    for (int i = 0, len = query.length(); i < len; ++i) {
                        final JSONObject obj = array.getJSONObject(i);
                        final Infraction infraction = infractions.get(infractionIndex[i]);
                        final String textString = obj.optString("text");
                        infraction.color = obj.optInt("color");
                        if (infraction.html = obj.optBoolean("html"))
                            infraction.text = Html.fromHtml(textString);
                        else
                            infraction.text = textString;
                        infraction.complete();
                    }
                } catch (final JSONException e) {
                    XposedBridge.log(e);
                }
            }
        });
        return infractions;
    }
    private void updateInfractionsView(final TextView view, final String id) {
        view.setText("");
        view.setTextColor(0);
        final Infraction infraction = getInfraction(id);
        infraction.when(new Runnable() {
            @Override
            public void run() {
                post(view, new Runnable() {
                    @Override
                    public void run() {
                        view.setText(infraction.text);
                        view.setTextColor(infraction.color);
                        if (infraction.html)
                            view.setMovementMethod(LinkMovementMethod.getInstance());
                    }
                });
            }
        });
    }
    private TextView addInfractionsView(final RelativeLayout parent, final View fixture) {
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

    protected final void setInfractionsView(final ListView listView, final Object business) throws Throwable {
        if (listView.getChildCount() <= 0) {
            post(listView, new Runnable() {
                @Override
                public void run() {
                    try {
                        setInfractionsView(listView, business);
                    } catch (final Throwable e) {
                        XposedBridge.log(e);
                    }
                }
            });
            return;
        }
        final RelativeLayout headerView = findStrictDescendant(listView, RelativeLayout.class);
        final RelativeLayout detailsView = findStrictDescendant(headerView, RelativeLayout.class);
        final RelativeLayout ratingsView = findStrictDescendant(detailsView, RelativeLayout.class);
        setInfractionsView(detailsView, ratingsView, business);
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
                setInfractionsView(listView, business);
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
                preloadAdapter(adapter, (View) param.args[2]);
                final Object business = adapter.getItem(position);
                post(group, new Runnable() {
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
    WeakHashMap<View, Boolean> seenViews = new WeakHashMap<>();
    protected void preloadAdapter(final Adapter adapter, final View view) throws Throwable {
        if (seenViews.get(view) != null)
            return;
        seenViews.put(view, true);
        final List<String> ids = new ArrayList<>();
        for (int i = 0, len = adapter.getCount(); i < len; ++i) {
            final String id = (String) businessGetId.invoke(adapter.getItem(i));
            ids.add(id);
        }
        getInfractions(ids);
        post(view, new Runnable() {
            @Override
            public void run() {
                seenViews.remove(view);
            }
        });
    }

    protected static void post(final View view, final Runnable runnable) {
        if (!view.post(runnable))
            log("Warning: view post failed");
    }

    protected static void log(final String message) {
        XposedBridge.log("YelpInfractions: " + message);
    }

    protected static void debug(final String message) {
        //log(message);
    }
}
