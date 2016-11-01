package com.development.mobiledevice.alphafitness;

public final class StepCounterTable {

    private static final String TEXT_TYPE = " TEXT ";
    private static final String INTEGER_TYPE = " INTEGER ";
    private static final String COMMA_SEP = ",";
    private static final String DATE_TIME=" datetime ";

    public static final String TABLE_NAME = "STEP_COUNTER";
    public static final String C_ID = "Id";
    public static final String C_STARTTIME = "StartTime";
    public static final String C_ENDTIME = "EndTime";
    public static final String C_COUNT = "Count";

    public static final String CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    C_STARTTIME + DATE_TIME + COMMA_SEP +
                    C_ENDTIME + DATE_TIME + COMMA_SEP+
                    C_COUNT + INTEGER_TYPE + " )";
}