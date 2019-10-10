package com.sherdle.universal.providers.soundcloud.api;

import com.sherdle.universal.attachmentviewer.model.MediaAttachment;
import com.sherdle.universal.providers.soundcloud.api.object.CommentObject;
import com.sherdle.universal.providers.soundcloud.api.object.TrackObject;
import com.sherdle.universal.providers.wordpress.PostItem;
import com.sherdle.universal.providers.wordpress.api.JsonApiPostLoader;
import com.sherdle.universal.providers.wordpress.api.RestApiPostLoader;
import com.sherdle.universal.providers.wordpress.api.WordpressGetTaskInfo;
import com.sherdle.universal.providers.wordpress.api.providers.RestApiProvider;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;

public class WordpressClient {
    private String apiUrl;
    private int maxPages;

    public WordpressClient(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public ArrayList<TrackObject> getListTrackObjectsByQuery(String query, int offset, int limit) {
        return null;
    }

    public ArrayList<TrackObject> getRecentTracks(int page) {
        return getTracksInCategory("", page);
    }

    public ArrayList<TrackObject> getTracksInCategory(String category, int page) {
        WordpressGetTaskInfo info = new WordpressGetTaskInfo(null, null, apiUrl, false);
        ArrayList<PostItem> posts = info.provider.parsePostsFromUrl(info, (StringUtil.isBlank(category) ?
                info.provider.getRecentPosts(info) :
                info.provider.getCategoryPosts(info, category)) + page);

        if (info.pages == null || posts == null) return null;
        maxPages = info.pages;

        final ArrayList<TrackObject> results = new ArrayList<>();
        for (final PostItem post : posts) {

            if (info.provider instanceof RestApiProvider)
                new RestApiPostLoader(post, apiUrl, new JsonApiPostLoader.BackgroundPostCompleterListener() {
                    @Override
                    public void completed(PostItem item) {
                        if (post.getAttachments().size() > 0) {
                            MediaAttachment audio = null;
                            for (MediaAttachment attachment : post.getAttachments()) {
                                if (attachment.getMime().contains(MediaAttachment.MIME_PATTERN_AUDIO)) {
                                    audio = attachment;
                                    break;
                                }
                            }
                            if (audio != null) {
                                TrackObject mTrackObject = new TrackObject(post.getId(), post.getDate(), 0, audio.getDuration(), null,
                                        null, null, post.getTitle(), post.getContent(), audio.getArtist(), null, post.getUrl(), post.getThumbnailCandidate(), null,
                                        0, 0, post.getCommentCount(), audio.getUrl());
                                mTrackObject.setStreamAble(true);

                                results.add(mTrackObject);
                            }
                        }
                    }
                }).run();
            else {
                MediaAttachment audio = null;
                for (MediaAttachment attachment : post.getAttachments()) {
                    if (attachment.getMime().contains(MediaAttachment.MIME_PATTERN_AUDIO)) {
                        audio = attachment;
                        break;
                    }
                }
                if (audio != null) {
                    TrackObject mTrackObject = new TrackObject(post.getId(), post.getDate(), 0, 0, null,
                            null, null, post.getTitle(), post.getContent(), post.getAuthor(), null, post.getUrl(), post.getThumbnailCandidate(), null,
                            0, 0, post.getCommentCount(), audio.getUrl());
                    mTrackObject.setStreamAble(true);

                    results.add(mTrackObject);
                }
            }


        }

        return results;
    }

    public ArrayList<CommentObject> getListCommentObject(long trackId) {
        return null;

    }

    public int getMaxPages() {
        return maxPages;
    }


}
