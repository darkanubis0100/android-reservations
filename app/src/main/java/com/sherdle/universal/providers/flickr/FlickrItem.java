package com.sherdle.universal.providers.flickr;

import android.os.Parcel;
import android.os.Parcelable;

import com.sherdle.universal.providers.tumblr.TumblrItem;

public class FlickrItem extends TumblrItem implements Parcelable {
    private String thumbUrl;
    
    public FlickrItem(){
        super();
    }
    
    public FlickrItem(String id, String link, String url, String thumbUrl) {
        super();
        this.id = id;
        this.link = link;
        this.url = url;
        this.thumbUrl = thumbUrl;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }
    
    public FlickrItem(Parcel source) {
        id = source.readString();
        link = source.readString();
        url = source.readString();
        thumbUrl = source.readString();
    }

    public int describeContents() {
	return this.hashCode();
    }

    public void writeToParcel(Parcel dest, int flags) {
	dest.writeString(id);
	dest.writeString(link);
	dest.writeString(url);
	dest.writeString(thumbUrl);
    }

    public static final Creator<FlickrItem> CREATOR
             = new Creator<FlickrItem>() {
         public FlickrItem createFromParcel(Parcel in) {
             return new FlickrItem(in);
         }

         public FlickrItem[] newArray(int size) {
             return new FlickrItem[size];
         }
    };
}