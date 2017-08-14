package com.clarkgarrent.instagramviewer;

import com.clarkgarrent.instagramviewer.Models.LikesResponse;
import com.clarkgarrent.instagramviewer.Models.PostDeleteLikeResponse;
import com.clarkgarrent.instagramviewer.Models.UserMediaResponse;
import com.clarkgarrent.instagramviewer.Models.UserSearchResponse;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by karlc on 8/9/2017.
 * The use of this interface is described in the comments in GridViewActivity.  It is used
 * by Retrofit to generate a service class that has methods that can be used download json data
 * and deserialize it into java model classes.  Basically each two lines describe a URI to an
 * Instagram endpoint. For each REST call (GET, POST, DELETE) there i * an annotation. It
 * describe the path to be used in the URI.  Braces in this descriptions are
 * replaced at runtime by corresponding parameter with @Path annotation in the method signature.
 * The only other annotation used in this interface is the @Query which is used to construct
 * the query portion of the URI.
 */

public interface InstagramEndpointsInterface {

    @GET("v1/users/self/media/liked")
    Call<LikesResponse> getLiked(@Query("access_token") String token);

    @GET("v1/users/self/media/recent")
    Call<UserMediaResponse> getSelfMedia(@Query("access_token") String token);

    @GET("v1/users/{id}/media/recent")
    Call<UserMediaResponse> getUserMedia(@Path("id") String id, @Query("access_token") String token);

    @GET("v1/users/search")
    Call<UserSearchResponse> getMatchingUsers(@Query("q") String username, @Query("access_token") String token);

    @POST("v1/media/{id}/likes")
    Call<PostDeleteLikeResponse> postLike(@Path("id") String id, @Query("access_token") String token);

    @DELETE("v1/media/{id}/likes")
    Call<PostDeleteLikeResponse> deleteLike(@Path("id") String id, @Query("access_token") String token);
}
