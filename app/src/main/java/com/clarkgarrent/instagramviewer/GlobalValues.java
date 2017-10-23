package com.clarkgarrent.instagramviewer;


import com.clarkgarrent.instagramviewer.Models.LikesData;
import com.clarkgarrent.instagramviewer.Models.UserMediaData;

import java.util.ArrayList;

/**
 * Created by karlc on 8/10/2017.
 */

public class GlobalValues {

    private static final String TAG = "## My Info ##";

    public static String token;  // The access token

    // This is the list that is used by the RecyclerView adapters.
    public static ArrayList<UserMediaData> alUserMediaData = new ArrayList<>();
    // This is a list of the images the user likes.
    private static ArrayList<String> likedIds = new ArrayList<>();

    public static final String BASE_URL = "https://api.instagram.com/";
    public static final String PREFS_NAME = "prefs_name";
    public static final String PREFS_TOKEN = "prefs_token";

    public static void setLikedIds(ArrayList<LikesData> likedData){
        // Create an array list from the array list that was originally populated by Retrofit
        if (likedData == null){
            likedIds = null;
        } else {
            likedIds = new ArrayList<>();
            for (LikesData ld : likedData) {
                likedIds.add(ld.getId());
            }
        }
    }

    public static void setUserMediaData(ArrayList<UserMediaData> data){
        // Create and array list from the UserMediaData array created by Retrofit.
        // Throw out video files. Compare each item to the list of liked items and
        // set the liked field appropriately.
        alUserMediaData = new ArrayList<>();
        for (UserMediaData umd:data){
            if ( ! umd.getType().equals("video")){
                if (likedIds.indexOf(umd.getId()) != -1){
                    umd.setLiked(true);
                }
                alUserMediaData.add(umd);
            }
        }
    }
}
