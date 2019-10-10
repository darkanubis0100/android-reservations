package com.sherdle.universal.providers.tumblr.ui;

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
import com.sherdle.universal.providers.tumblr.ImageAdapter;
import com.sherdle.universal.providers.tumblr.TumblrItem;
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
 *  This activity is used to display a list of tumblr imagess
 */

public class TumblrFragment extends Fragment implements PermissionsFragment, InfiniteRecyclerViewAdapter.LoadMoreListener {

    private RecyclerView listView;
    ArrayList<TumblrItem> tumblrItems;
	private ImageAdapter imageAdapter = null;
	
	private Activity mAct;
	private RelativeLayout ll;

	String perpage = "25";
	Integer curpage = 0;
	Integer total_posts;
	
	String baseurl;
	
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

		String username = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];
		baseurl = "https://api.tumblr.com/v2/blog/"+username+".tumblr.com/posts?api_key="
				+ getString(R.string.tumblr_key) +"&type=photo&limit=" + perpage + "&offset=";

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
	
	public void updateList(ArrayList<TumblrItem> result) {
        if (result.size() > 0)
            tumblrItems.addAll(result);

        if ((curpage * Integer.parseInt(perpage)) > total_posts || result.size() == 0)
            imageAdapter.setHasMore(false);

        imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);
    }

	@Override
	public String[] requiredPermissions() {
		return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
	}

    @Override
    public void onMoreRequested() {
        if (!isLoading && (curpage * Integer.parseInt(perpage)) <= total_posts) {
            // It is time to add new data. We call the listener
            isLoading = true;
            new InitialLoadGridView().execute(baseurl);
        }
    }

    void refreshItems(){
        isLoading = true;
        curpage = 0;
        tumblrItems.clear();
        imageAdapter.setHasMore(true);
        imageAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
        new InitialLoadGridView().execute(baseurl);
    }

    private class InitialLoadGridView extends AsyncTask<String, Void, ArrayList<TumblrItem>> {

		@Override
		protected ArrayList<TumblrItem> doInBackground(String... params) {
			String geturl = params[0];
			geturl = geturl + Integer.toString((curpage) *  Integer.parseInt(perpage));
            curpage = curpage + 1;

			String jsonString = Helper.getDataFromUrl(geturl);

			Log.v("INFO", "Tumblr JSON: " + jsonString);
			JSONObject json= null;
			// try parse the string to a JSON object
			try {
				json = new JSONObject(jsonString).getJSONObject("response");
			} catch (JSONException e) {
				Log.printStackTrace(e);
			}
			
			ArrayList<TumblrItem> images = null;

			try {
				// Checking for SUCCESS TAG
				total_posts = json.getInt("total_posts");

				if (0 < total_posts) {
					// products found
					// Getting Array of Products
					JSONArray products;
					
					products = json.getJSONArray("posts");
                    images = new ArrayList<TumblrItem>();

					// looping through All Products
					for (int i = 0; i < products.length(); i++) {
						JSONObject c = products.getJSONObject(i);

						// Storing each json item in variable
						String id = c.getString("id");
						String link = c.getString("post_url");
						JSONArray photos = c.getJSONArray("photos");

						String url = null;
						if (photos.length() > 0)
					        url = ((JSONObject) photos.get(0)).getJSONObject("original_size").getString("url");

						// adding items to arraylist
						if (url != null){
							TumblrItem item = new TumblrItem(id, link, url);
							images.add(item);
						}
					}
				} else {
					Log.v("INFO", "No items found");
				}
			} catch (JSONException e) {
				Log.printStackTrace(e);
			} catch (NullPointerException e) {
				Log.printStackTrace(e);
			}
			
			return images;
		}
		
        @Override
		protected void onPostExecute(ArrayList<TumblrItem> results) {
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
        	if (!isLoading){
        		refreshItems();
	    	} else {
	    		Toast.makeText(mAct, getString(R.string.already_loading), Toast.LENGTH_LONG).show();
	    	}
        default:
            return super.onOptionsItemSelected(item);
        }
    }
	
}