package com.sherdle.universal.providers.flickr.ui;

import android.Manifest;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.sherdle.universal.MainActivity;
import com.sherdle.universal.R;
import com.sherdle.universal.inherit.PermissionsFragment;
import com.sherdle.universal.providers.flickr.ImageAdapter;
import com.sherdle.universal.providers.flickr.FlickrItem;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.sherdle.universal.util.Log;
import com.sherdle.universal.util.ThemeUtils;
import com.sherdle.universal.util.layout.StaggeredGridSpacingItemDecoration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This activity is used to display a list of tumblr imagess
 */

public class FlickrFragment extends Fragment implements PermissionsFragment, InfiniteRecyclerViewAdapter.LoadMoreListener {

    private RecyclerView listView;
    ArrayList<FlickrItem> tumblrItems;
    private ImageAdapter imageAdapter = null;

    private Activity mAct;
    private RelativeLayout ll;

    Integer curpage = 0;
    Integer total_pages;

    String baseurl;
    String method;

    Boolean isLoading = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ll = (RelativeLayout) inflater.inflate(R.layout.fragment_list, container, false);
        return ll;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        String apiKey = getString(R.string.flickr_key);
        String galleryId = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];
        method = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[1];

        String pathMethod = !method.equals("gallery") ? "photosets" : "galleries";
        String idMethod = !method.equals("gallery") ? "photoset_id" : "gallery_id";

        baseurl = "https://api.flickr.com/services/rest/?method=flickr." + pathMethod +
                ".getPhotos&api_key=" + apiKey +
                "&"+idMethod+"=" +
                galleryId + "&format=json" +
                "&extras=path_alias,url_o,url_c,url_b,url_z" +
                "&page=";

        listView = ll.findViewById(R.id.list);
        tumblrItems = new ArrayList<>();
        imageAdapter = new ImageAdapter(getContext(), tumblrItems, this);
        imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
        listView.setAdapter(imageAdapter);

        //TODO dynamically change grid span count
        listView.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addItemDecoration(new StaggeredGridSpacingItemDecoration((int) getResources().getDimension(R.dimen.woocommerce_padding), true));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAct = getActivity();

        refreshItems();
    }

    public void updateList(ArrayList<FlickrItem> result) {
        if (result.size() > 0)
            tumblrItems.addAll(result);

        if (curpage >= total_pages || result.size() == 0)
            imageAdapter.setHasMore(false);

        imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
    }

    @Override
    public String[] requiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    @Override
    public void onMoreRequested() {
        if (!isLoading && curpage < total_pages) {
            // It is time to add new data. We call the listener
            isLoading = true;
            new InitialLoadGridView().execute(baseurl);
        }
    }

    void refreshItems() {
        isLoading = true;
        curpage = 1;
        tumblrItems.clear();
        imageAdapter.setHasMore(true);
        imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
        new InitialLoadGridView().execute(baseurl);
    }

    private class InitialLoadGridView extends AsyncTask<String, Void, ArrayList<FlickrItem>> {

        @Override
        protected ArrayList<FlickrItem> doInBackground(String... params) {
            String geturl = params[0];
            geturl = geturl + Integer.toString(curpage);
            curpage = curpage + 1;

            String jsonString = Helper.getDataFromUrl(geturl);

            Log.v("INFO", "Tumblr JSON: " + jsonString);
            if (jsonString.isEmpty()) return null;
            JSONObject json = null;
            // try parse the string to a JSON object
            try {
                jsonString = jsonString.replace("jsonFlickrApi(", "");
                jsonString = jsonString.substring(0, jsonString.length() - 1);
                json = new JSONObject(jsonString);
            } catch (JSONException e) {
                Log.printStackTrace(e);
            }

            ArrayList<FlickrItem> images = null;

            try {
                // Checking for SUCCESS TAG
                String parentMethod = !method.equals("gallery") ? "photoset" : "photos";
                total_pages = json.getJSONObject(parentMethod).getInt("pages");

                // products found
                // Getting Array of Products
                JSONArray products;

                products = json.getJSONObject(parentMethod).getJSONArray("photo");
                images = new ArrayList<FlickrItem>();

                // looping through All Products
                for (int i = 0; i < products.length(); i++) {
                    JSONObject c = products.getJSONObject(i);

                    // Storing each json item in variable
                    String id = c.getString("id");
                    String title = c.getString("title");
                    String link = "https://www.flickr.com/photos/" + c.getString("pathalias") + "/" + id;

                    String url = null;
                    if (c.has("url_o"))
                        url = c.getString("url_o");
                    else if (c.has("url_b"))
                        url = c.getString("url_b");
                    else if (c.has("url_c"))
                        url = c.getString("url_c");


                    String thumbUrl = null;
                    if (c.has("url_z"))
                        thumbUrl = c.getString("url_z");
                    else
                        thumbUrl = url;

                    // adding items to arraylist
                    if (url != null) {
                        FlickrItem item = new FlickrItem(id, link, url, thumbUrl);
                        images.add(item);
                    }
                }

            } catch (JSONException e) {
                Log.printStackTrace(e);
            } catch (NullPointerException e) {
                Log.printStackTrace(e);
            }

            return images;
        }

        @Override
        protected void onPostExecute(ArrayList<FlickrItem> results) {
            if (null != results) {
                updateList(results);
            } else {
                Helper.noConnection(mAct);
                imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);
            }
            isLoading = false;
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh_menu, menu);
        ThemeUtils.tintAllIcons(menu, mAct);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh:
                if (!isLoading) {
                    refreshItems();
                } else {
                    Toast.makeText(mAct, getString(R.string.already_loading), Toast.LENGTH_LONG).show();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}