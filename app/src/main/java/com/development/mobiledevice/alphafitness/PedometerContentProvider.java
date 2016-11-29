package com.development.mobiledevice.alphafitness;

/**
 * Created by PraveenSubramaniyam on 11/12/2016.
 */
import java.io.File;
import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class PedometerContentProvider extends ContentProvider {
    static final String PROVIDER_NAME = "com.development.mobiledevice.alphafitness.PedometerContentProvider";
    static final String USERTABLE_URL = "content://" + PROVIDER_NAME + "/"+UserTable.TABLE_NAME;
    static final Uri USERTABLE_CONTENT_URI = Uri.parse(USERTABLE_URL);
    static final String STEPTABLE_URL = "content://" + PROVIDER_NAME + "/"+StepCounterTable.TABLE_NAME;
    static final Uri STEPTABLE_CONTENT_URI = Uri.parse(STEPTABLE_URL);
    static final String id = "id";
    static final String name = "name";
    static final int userTableCode = 1;
    static final int stepCounterTableCode = 2;
    static final UriMatcher uriMatcher;
    private static HashMap<String, String> values;
    private final static String TAG = "Pedometer";
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, UserTable.TABLE_NAME, userTableCode);
        uriMatcher.addURI(PROVIDER_NAME, UserTable.TABLE_NAME+"/*", userTableCode);
        uriMatcher.addURI(PROVIDER_NAME, StepCounterTable.TABLE_NAME, stepCounterTableCode);
        uriMatcher.addURI(PROVIDER_NAME, StepCounterTable.TABLE_NAME+"/*", stepCounterTableCode);
    }
    private SQLiteDatabase db;
    private DataBaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        Log.i(TAG,"ContentProvider Oncreate called");
        Context context = getContext();
        dbHelper = new DataBaseHelper(context);
        db = dbHelper.getWritableDatabase();
        if (db != null) {
            Log.i(TAG,"DB Created");
            Log.i(TAG,"DB Path: "+db.getPath());
            File varTmpDir = new File(db.getPath());
            boolean exists = varTmpDir.exists();
            Log.i(TAG,"File exists: "+exists);
            //db.close();
            return true;
        }
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        db = dbHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case userTableCode:
                count = db.delete(UserTable.TABLE_NAME, selection, selectionArgs);
                break;
            case stepCounterTableCode:
                count = db.delete(StepCounterTable.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return count;
    }

    @Override
    public String getType(Uri uri) {
        Log.i(TAG,"Get type called");
        switch (uriMatcher.match(uri)) {
            case userTableCode:
                return "vnd.android.cursor.dir/"+UserTable.TABLE_NAME;
            case stepCounterTableCode:
                return "vnd.android.cursor.dir/"+StepCounterTable.TABLE_NAME;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri _uri = null;
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        db = dbHelper.getWritableDatabase();
        Log.i(TAG,"Inside Insert DB path: "+db.getPath());
        switch (uriMatcher.match(uri))
        {
            case userTableCode: {
                long rowID = db.insert(UserTable.TABLE_NAME, "", values);
                if (rowID > 0) {
                    _uri = ContentUris.withAppendedId(USERTABLE_CONTENT_URI, rowID);
                    getContext().getContentResolver().notifyChange(_uri, null);

                }
            }
            break;
            case stepCounterTableCode: {
                long rowID = db.insert(StepCounterTable.TABLE_NAME, "", values);
                if (rowID > 0) {
                    _uri = ContentUris.withAppendedId(STEPTABLE_CONTENT_URI, rowID);
                    getContext().getContentResolver().notifyChange(_uri, null);

                }
            }
            break;
            default:
                throw new SQLException("Failed to add a record into " + uri);
        }
        db.close();
        return _uri;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        Log.i(TAG,"Query called");
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        db = dbHelper.getReadableDatabase();
        switch (uriMatcher.match(uri)) {
            case userTableCode:
                qb.setTables(UserTable.TABLE_NAME);
                qb.setProjectionMap(values);
                break;
            case stepCounterTableCode:
                qb.setTables(StepCounterTable.TABLE_NAME);
                qb.setProjectionMap(values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        //if (sortOrder == null || sortOrder == "") {
          //  sortOrder = name;
        //}
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        //db.close();
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;
        Log.i(TAG,"Update called:");
        DataBaseHelper dbHelper = new DataBaseHelper(getContext());
        db = dbHelper.getWritableDatabase();
        switch (uriMatcher.match(uri)) {
            case userTableCode:
                count = db.update(UserTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            case stepCounterTableCode:
                count = db.update(StepCounterTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        db.close();
        return count;
    }
}
