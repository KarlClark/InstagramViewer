package com.clarkgarrent.instagramviewer;

import android.util.Log;

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

    public static void setLikedIds(LikesData[] likedData){
        // Create an array list from the array that was originally populated by Retrofit
        likedIds = new ArrayList<>();
        for (int i = 0; i < likedData.length; i++){
            likedIds.add(likedData[i].getId());
        }
    }

    public static void setUserMediaData(UserMediaData[] data){
        // Create and array list from the UserMediaData array created by Retrofit.
        // Throw out video files. Compare each item to the list of liked items and
        // set the liked field appropriately.
        alUserMediaData = new ArrayList<>();
        for (int i = 0; i < data.length; i++){
            if ( ! data[i].getType().equals("video")){
                if (likedIds.indexOf(data[i].getId()) != -1){
                    data[i].setLiked(true);
                }
                alUserMediaData.add(data[i]);
            }
        }
    }
}
