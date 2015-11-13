/**
 * PKVideoThumbnail
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package com.photokandy.PKVideoThumbnail;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.*;
import android.provider.MediaStore;
import android.util.Log;

import java.io.*;

/* For URI->path conversion */
import android.database.Cursor;
import android.net.Uri;
import android.content.Context;
import android.os.Build;
import android.provider.DocumentsContract;
import android.content.ContentUris;
import java.lang.Long;
import android.os.Environment;

/**
 * This class echoes a string called from JavaScript.
 */
public class PKVideoThumbnail extends CordovaPlugin {

    private static final String TAG = "PKVideoThumbnail";

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action        The action to execute.
     * @param args          JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            if (action.equals("createThumbnail")) {
                String sourceVideo = args.getString(0);
                String targetImage = args.getString(1);
                
                Context context = this.cordova.getActivity().getApplicationContext();
                Uri uri = Uri.parse(sourceVideo);
                
                // Convert content://... to /storage/...
                String sourceVideoPath = getPath(context, uri);
                Log.i(TAG, "Source video path: " + sourceVideoPath);

                // Source video path could start with /storage/... or file://...
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail ( sourceVideoPath, MediaStore.Images.Thumbnails.MINI_KIND);
                if (thumbnail == null) {
                    callbackContext.error("Video format not supported, or source path not file://...");
                    return true;
                }
                
                FileOutputStream theOutputStream;
                try
                {
                	File theOutputFile = new File (targetImage.substring(7));
                	if (!theOutputFile.exists())
                	{
                		if (!theOutputFile.createNewFile())
                		{
                                        callbackContext.error ( "Could not save thumbnail." );
                                        return true;
                		}
                	}
                	if (theOutputFile.canWrite())
                	{
                		theOutputStream = new FileOutputStream (theOutputFile);
                		if (theOutputStream != null)
                		{
                			thumbnail.compress(CompressFormat.JPEG, 75, theOutputStream);
                		}
                		else
                		{
                                        callbackContext.error ( "Could not save thumbnail; target not writeable");
                                        return true;
                		}
                	}
                }
                catch (IOException e)
                {
                	e.printStackTrace();
                        callbackContext.error ( "I/O exception saving thumbnail" );
                        return true; 
                }
                callbackContext.success ( targetImage );        
                return true; 
                
            } else {
                return false;
            }
        } catch (JSONException e) {
            callbackContext.error ( "JSON Exception" );
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            callbackContext.error("Unexpected error (" + e.getMessage() + ')');
            return true; 
        }
    }


    /* ###############################
     * The rest is JUST for converting from URI (e.g. content://...)
     * to file path (/storage/...) for creating thumbnail:
     *    http://stackoverflow.com/a/20559175/188926
     *
     * TODO: turn this into a cordova plugin like https://github.com/hiddentao/cordova-plugin-filepath
     * (that one just turns into native path, like file plugin resolveLocalFileSystemURL() method)
     * ###############################/

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
