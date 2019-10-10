package com.sherdle.universal.providers.instagram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.sherdle.universal.util.Log;
import com.sherdle.universal.util.ThemeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * This activity is used to display a list of instagram photos
 */

public class InstagramFragment extends Fragment  implements InfiniteRecyclerViewAdapter.LoadMoreListener {

	private RecyclerView photosListView = null;
	private ArrayList<InstagramPhoto> photosList;
	private InstagramPhotosAdapter photosListAdapter = null;

	private Activity mAct;
	private RelativeLayout ll;

	private String nextpageurl;
	String username;
	Boolean isLoading = false;

	private static String API_URL = "https://graph.facebook.com/v3.1/";
	private static String API_URL_END = "/media?fields=caption,id,ig_id,comments_count,timestamp,permalink,owner{profile_picture_url},media_url,media_type,thumbnail_url,like_count,comments{text,username},username,children&access_token=";

    private static final SimpleDateFormat INSTAGRAM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());

    @SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ll = (RelativeLayout) inflater.inflate(R.layout.fragment_list,
				container, false);
		setHasOptionsMenu(true);

		username = this.getArguments().getStringArray(MainActivity.FRAGMENT_DATA)[0];

		photosListView = ll.findViewById(R.id.list);
		photosList = new ArrayList<>();
		photosListAdapter = new InstagramPhotosAdapter(getContext(), photosList, this);
		photosListAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
		photosListView.setAdapter(photosListAdapter);
		photosListView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

		return ll;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAct = getActivity();

		refreshItems();
	}


	public void updateList(ArrayList<InstagramPhoto> photosList) {
		if (photosList.size() > 0) {
			this.photosList.addAll(photosList);
		}

		if (nextpageurl == null || photosList.size() == 0)
			photosListAdapter.setHasMore(false);
		photosListAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_LIST);

	}

	@Override
	public void onMoreRequested() {
		if (!isLoading && nextpageurl != null) {
			new DownloadFilesTask(false).execute();
		}
	}

	private class DownloadFilesTask extends AsyncTask<String, Integer, ArrayList<InstagramPhoto>> {

		boolean initialload;

		DownloadFilesTask(boolean firstload) {
			this.initialload = firstload;
		}

		@Override
		protected void onPreExecute() {
			if (isLoading) {
				this.cancel(true);
			} else {
				isLoading = true;
			}
			if (initialload) {
				nextpageurl = (API_URL + username + API_URL_END  + getResources().getString(R.string.instagram_access_token));
			}
		}

		@Override
		protected void onPostExecute(ArrayList<InstagramPhoto> result) {
			if (null != result && result.size() > 0) {
				updateList(result);
			} else {
				Helper.noConnection(mAct);
				photosListAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_EMPTY);
			}

			isLoading = false;
		}

		@Override
		protected ArrayList<InstagramPhoto> doInBackground(String... params) {
			//Getting data from url and parsing JSON
			JSONObject json = Helper.getJSONObjectFromUrl(nextpageurl);
			return parseJson(json);
		}
	}

	public ArrayList<InstagramPhoto> parseJson(JSONObject json) {
		ArrayList<InstagramPhoto> photosList = new ArrayList<>();

		try {
            if (json.has("paging") && json.getJSONObject("paging").has("next"))
                nextpageurl = json.getJSONObject("paging").getString("next");
            else
                nextpageurl = null;

			// parsing json object
			 JSONArray dataJsonArray = json.getJSONArray("data");
             for (int i = 0; i < dataJsonArray.length(); i++) {
                 JSONObject photoJson = dataJsonArray.getJSONObject(i);
                 InstagramPhoto photo = new InstagramPhoto();
                 photo.id = photoJson.getString("ig_id");
                 photo.type = photoJson.getString("media_type");
                 photo.username = photoJson.getString("username");
                 photo.profilePhotoUrl = photoJson.getJSONObject("owner").getString("profile_picture_url");
                 if (photoJson.has("caption") && !photoJson.isNull("caption")){
                	 photo.caption = photoJson.getString("caption");
                 }
                 photo.createdTime = INSTAGRAM_DATE_FORMAT.parse(photoJson.getString("timestamp"));
                 photo.likesCount = photoJson.getInt("like_count");
                 photo.link = photoJson.getString("permalink");
                 if (photoJson.has("comments"))
				 	photo.commentsJson = photoJson.getJSONObject("comments").toString();
                 
                 if (photo.type.equalsIgnoreCase("video")) {
					 photo.videoUrl = photoJson.getString("media_url");
					 photo.imageUrl = photoJson.getString("thumbnail_url");
				 } else {
                     photo.imageUrl = photoJson.getString("media_url");
                 }

                 photo.commentsCount = photoJson.getInt("comments_count");

                 // Add to array list
                 photosList.add(photo);
			}
		} catch (Exception e) {
			Log.printStackTrace(e);
		}

        return photosList;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.refresh_menu, menu);
		ThemeUtils.tintAllIcons(menu, mAct);
	}

	public void refreshItems(){
		photosList.clear();
		photosListAdapter.setHasMore(true);
		photosListAdapter.setModeAndNotify(InfiniteRecyclerViewAdapter.MODE_PROGRESS);
		new DownloadFilesTask(true).execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.refresh:
			if (!isLoading) {
				refreshItems();
			} else {
				Toast.makeText(mAct, getString(R.string.already_loading),
						Toast.LENGTH_LONG).show();
			}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
