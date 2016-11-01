package com.development.mobiledevice.alphafitness;

import android.provider.BaseColumns;

public final class UserTable {

    private static final String TEXT_TYPE = " TEXT ";
    private static final String INTEGER_TYPE = " INTEGER ";
    private static final String COMMA_SEP = ",";

    public static final String TABLE_NAME = "USERS";
    public static final String COLUMN_USERID = "UserId";
    public static final String COLUMN_USERNAME = "UserName";
    public static final String COLUMN_SEX = "Sex";
    public static final String COLUMN_WEIGHT = "Weight";
    public static final String COLUMN_HEIGHT = "Height";
    public static final String CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    COLUMN_USERID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    COLUMN_USERNAME + TEXT_TYPE + COMMA_SEP +
                    COLUMN_SEX + TEXT_TYPE + COMMA_SEP+
                    COLUMN_WEIGHT + INTEGER_TYPE + COMMA_SEP +
                    COLUMN_HEIGHT + INTEGER_TYPE + " )";
}