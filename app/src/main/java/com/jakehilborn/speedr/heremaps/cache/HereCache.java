package com.jakehilborn.speedr.heremaps.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jakehilborn.speedr.heremaps.deserial.pde.HerePDEResponse;
import com.jakehilborn.speedr.heremaps.deserial.pde.Rows;

import java.util.ArrayList;
import java.util.List;

public class HereCache {

    private CacheHelper dbHelper;

    public HereCache(Context context) {
        dbHelper = new CacheHelper(context);
    }

    public void putResponse(HerePDEResponse herePDEResponse, Integer tileX, Integer tileY, Integer level) {
        List<ContentValues> valuesList = new ArrayList<>(herePDEResponse.getRows().length);

        int time = (int) System.currentTimeMillis() / 1000;
        for (Rows row : herePDEResponse.getRows()) {
            ContentValues values = new ContentValues();
            values.put(CacheHelper.REF_ID, Long.parseLong(row.getLINK_ID()));
            values.put(CacheHelper.SPEED_LIMIT, parseLimit(row.getFROM_REF_SPEED_LIMIT(), row.getTO_REF_SPEED_LIMIT()));
            values.put(CacheHelper.TILE, tileX.toString() + ":" + tileY.toString() + ":" + level.toString());
            values.put(CacheHelper.INSERT_TIME, time);
            valuesList.add(values);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // TODO - wrap this is a transaction if there are performance issues
        for (ContentValues values : valuesList) {
            db.insert(CacheHelper.TABLE_NAME, null, values);
        }
    }

    private int parseLimit(String fromRef, String toRef) {
        int limit = -1;

        if (fromRef != null) {
            try {
                limit = Integer.parseInt(fromRef);
            } catch (NumberFormatException e) {
                //log whatever the format exception is
            }
        }

        if (limit == -1 && toRef != null) {
            try {
                limit = Integer.parseInt(toRef);
            } catch (NumberFormatException e) {
                //log whatever the format exception is
            }
        }

        return limit;
    }

    public Integer get(long refId) {
        String queryString = "SELECT " + CacheHelper.SPEED_LIMIT + " FROM " + CacheHelper.TABLE_NAME + " where " + CacheHelper.REF_ID + " = ?";
        String[] queryArgs = { String.valueOf(refId) };

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(queryString, queryArgs);

        Integer limit = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                limit = cursor.getInt(cursor.getColumnIndex(CacheHelper.SPEED_LIMIT));
            }
            cursor.close();
        }

        return limit;
    }

    public void close() {
        try {
            dbHelper.close();
        } catch (Exception e) {
            //do nothing
        }
    }
}

/*

private List<AbstractMap.SimpleEntry<Long, Integer>> parsePDEResponse(HerePDEResponse herePDEResponse) {
        List<AbstractMap.SimpleEntry<Long, Integer>> tileData = new ArrayList<>();

        for (Rows row : herePDEResponse.getRows()) {
            long refId = Long.parseLong(row.getLINK_ID());

            int limit = -1;

            if (row.getFROM_REF_SPEED_LIMIT() != null) {
                try {
                    limit = Integer.parseInt(row.getFROM_REF_SPEED_LIMIT());
                } catch (NumberFormatException e) {
                    //log whatever the format exception is
                }
            }

            if (limit == -1 && row.getTO_REF_SPEED_LIMIT() != null) {
                try {
                    limit = Integer.parseInt(row.getTO_REF_SPEED_LIMIT());
                } catch (NumberFormatException e) {
                    //log whatever the format exception is
                }
            }

            tileData.add(new AbstractMap.SimpleEntry<>(refId, limit));
        }

        return tileData;
    }

 */