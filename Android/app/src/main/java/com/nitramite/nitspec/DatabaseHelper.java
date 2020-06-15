package com.nitramite.nitspec;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.nitramite.adapters.HardwareItem;
import com.nitramite.adapters.HardwareType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Logging
    private static final String TAG = "DatabaseHelper";

    // Variables
    private Context context;

    // For database updating
    private boolean upgrade = false;
    private List<String> columnsGun;
    private List<String> columnsAmmunition;

    // DATABASE VERSION
    private static final int DATABASE_VERSION = 1;
    // 1  = v1.0.0 Initial version


    // DATABASE NAME
    private static final String DATABASE_NAME = "NITSPEC.db";

    // TABLE NAME'S
    private static final String TABLE_GUN = "table_gun"; // Guns
    private static final String TABLE_AMMUNITION = "table_ammunition"; // Ammunitions

    // -------------------------------------------------------------------

    // Table gun rows
    private static final String gun_id = "gun_id";
    private static final String gun_letter = "gun_letter";
    private static final String gun_name = "gun_name";
    private static final String gun_description = "gun_description";

    // Table ammunition rows
    private static final String ammunition_id = "ammunition_id";
    private static final String ammunition_letter = "ammunition_letter";
    private static final String ammunition_name = "ammunition_name";
    private static final String ammunition_description = "ammunition_description";
    private static final String ammunition_speed = "ammunition_speed";
    private static final String ammunition_weight = "ammunition_weight";
    private static final String ammunition_size_x = "ammunition_size_x";
    private static final String ammunition_size_y = "ammunition_size_y";
    private static final String ammunition_size_z = "ammunition_size_z";
    private static final String ammunition_drag_coefficient_x = "ammunition_drag_coefficient_x";
    private static final String ammunition_drag_coefficient_y = "ammunition_drag_coefficient_y";

    // -------------------------------------------------------------------

    // Constructor
    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // ***BACKUP*** Backup old database stuff
        if (upgrade) {
            // Backup gun
            columnsGun = GetColumns(db, TABLE_GUN);
            db.execSQL("ALTER TABLE " + TABLE_GUN + " RENAME TO TEMP_" + TABLE_GUN);
            // Backup ammunition
            columnsAmmunition = GetColumns(db, TABLE_AMMUNITION);
            db.execSQL("ALTER TABLE " + TABLE_AMMUNITION + " RENAME TO TEMP_" + TABLE_AMMUNITION);
        }

        // *** CREATE TABLES ***
        // Create gun table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_GUN + "(gun_id INTEGER PRIMARY KEY AUTOINCREMENT, gun_letter TEXT, gun_name TEXT, gun_description TEXT)");
        // Create ammunition table
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AMMUNITION + "(ammunition_id INTEGER PRIMARY KEY AUTOINCREMENT, ammunition_letter TEXT, ammunition_name TEXT, " +
                "ammunition_description TEXT, ammunition_speed TEXT, ammunition_weight TEXT, ammunition_size_x TEXT, ammunition_size_y TEXT, ammunition_size_z TEXT, " +
                "ammunition_drag_coefficient_x TEXT, ammunition_drag_coefficient_y TEXT)");

        // ***RESTORE***  Restore from old
        if (upgrade) {
            // Restore gun
            columnsGun.retainAll(GetColumns(db, TABLE_GUN));
            String parcelCols = join(columnsGun, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", TABLE_GUN, parcelCols, parcelCols, TABLE_GUN));
            db.execSQL("DROP TABLE TEMP_" + TABLE_GUN);
            // Restore ammunition
            columnsAmmunition.retainAll(GetColumns(db, TABLE_AMMUNITION));
            String trackingCols = join(columnsAmmunition, ",");
            db.execSQL(String.format("INSERT INTO %s (%s) SELECT %s FROM TEMP_%s", TABLE_AMMUNITION, trackingCols, trackingCols, TABLE_AMMUNITION));
            db.execSQL("DROP TABLE TEMP_" + TABLE_AMMUNITION);
        }
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgrade = true;
        onCreate(db);
    }


    // ---------------------------------------------------------------------------------------------
    /* C R U D */


    // Get guns
    public ArrayList<HardwareItem> getGuns() {
        ArrayList<HardwareItem> arrayList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + gun_id + ", " + gun_letter + ", " + gun_name + ", " + gun_description +
                " FROM " + TABLE_GUN + " ORDER BY " + gun_id + " ASC", null);
        while (cursor.moveToNext()) {
            arrayList.add(
                    new HardwareItem(HardwareType.GUN, cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3)
            ));
        }
        cursor.close();
        return arrayList;
    }


    /**
     * Insert gun
     * @param hardwareItem gun item
     * @return result
     */
    boolean insertGun(final HardwareItem hardwareItem){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(gun_letter, hardwareItem.getItemLetter());
        contentValues.put(gun_name, hardwareItem.getItemName());
        contentValues.put(gun_description, hardwareItem.getItemDescription());
        long result =  db.insert(TABLE_GUN, null, contentValues);
        Log.i(TAG, contentValues.toString());
        return result != -1;
    }


    // Delete gun
    boolean deleteGun(final String gun_id_) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+ TABLE_GUN + " WHERE " + gun_id + " = ?", new String[]{gun_id_});
        db.close();
        return true;
    }



    // Get ammunitions
    public ArrayList<HardwareItem> getAmmunitions() {
        ArrayList<HardwareItem> arrayList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + ammunition_id + ", " + ammunition_letter + ", " + ammunition_name + ", " + ammunition_description +
                ", " + ammunition_speed + ", " + ammunition_weight + ", " + ammunition_size_x + ", " + ammunition_size_y + ", " + ammunition_size_z +
                ", " + ammunition_drag_coefficient_x + ", " + ammunition_drag_coefficient_y +
                " FROM " + TABLE_AMMUNITION + " ORDER BY " + ammunition_id + " ASC", null);
        while (cursor.moveToNext()) {
            HardwareItem hardwareItem = new HardwareItem(HardwareType.AMMUNITION, cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
            hardwareItem.setAmmunitionSpeed(cursor.getDouble(4));
            hardwareItem.setAmmunitionWeight(cursor.getDouble(5));
            hardwareItem.setAmmunitionSizeMillisX(cursor.getDouble(6));
            hardwareItem.setAmmunitionSizeMillisY(cursor.getDouble(7));
            hardwareItem.setAmmunitionSizeMillisZ(cursor.getDouble(8));
            hardwareItem.setAmmunitionDragCoefficientXValue(cursor.getDouble(9));
            hardwareItem.setAmmunitionDragCoefficientYValue(cursor.getDouble(10));
            arrayList.add(hardwareItem);
        }
        cursor.close();
        return arrayList;
    }


    /**
     * Get ammunition params as hardware item
     * @param selectedAmmunitionId selected one
     * @return hardwareItem
     */
    public HardwareItem getSelectedAmmunition(final String selectedAmmunitionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + ammunition_id + ", " + ammunition_letter + ", " + ammunition_name + ", " + ammunition_description +
                ", " + ammunition_speed + ", " + ammunition_weight + ", " + ammunition_size_x + ", " + ammunition_size_y + ", " + ammunition_size_z +
                ", " + ammunition_drag_coefficient_x + ", " + ammunition_drag_coefficient_y +
                " FROM " + TABLE_AMMUNITION + " WHERE " + ammunition_id + " = " + selectedAmmunitionId + " LIMIT 1", null);
        cursor.moveToFirst();
        HardwareItem hardwareItem = new HardwareItem(HardwareType.AMMUNITION, cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
        hardwareItem.setAmmunitionSpeed(cursor.getDouble(4));
        hardwareItem.setAmmunitionWeight(cursor.getDouble(5));
        hardwareItem.setAmmunitionSizeMillisX(cursor.getDouble(6));
        hardwareItem.setAmmunitionSizeMillisY(cursor.getDouble(7));
        hardwareItem.setAmmunitionSizeMillisZ(cursor.getDouble(8));
        hardwareItem.setAmmunitionDragCoefficientXValue(cursor.getDouble(9));
        hardwareItem.setAmmunitionDragCoefficientYValue(cursor.getDouble(10));
        cursor.close();
        return hardwareItem;
    }


    /**
     * Insert ammunition
     * @param hardwareItem ammunition item
     * @return result
     */
    boolean insertAmmunition(final HardwareItem hardwareItem){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ammunition_letter, hardwareItem.getItemLetter());
        contentValues.put(ammunition_name, hardwareItem.getItemName());
        contentValues.put(ammunition_description, hardwareItem.getItemDescription());
        contentValues.put(ammunition_speed, hardwareItem.getAmmunitionSpeed());
        contentValues.put(ammunition_weight, hardwareItem.getAmmunitionWeight());
        contentValues.put(ammunition_size_x, hardwareItem.getAmmunitionSizeMillisY());
        contentValues.put(ammunition_size_y, hardwareItem.getAmmunitionSizeMillisY());
        contentValues.put(ammunition_size_z, hardwareItem.getAmmunitionSizeMillisZ());
        contentValues.put(ammunition_drag_coefficient_x, hardwareItem.getAmmunitionDragCoefficientYValue());
        contentValues.put(ammunition_drag_coefficient_y, hardwareItem.getAmmunitionDragCoefficientYValue());
        long result =  db.insert(TABLE_AMMUNITION, null, contentValues);
        Log.i(TAG, contentValues.toString());
        return result != -1;
    }


    // Update ammunition
    boolean updateAmmunition(final HardwareItem hardwareItem){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ammunition_letter, hardwareItem.getItemLetter());
        contentValues.put(ammunition_name, hardwareItem.getItemName());
        contentValues.put(ammunition_description, hardwareItem.getItemDescription());
        contentValues.put(ammunition_speed, hardwareItem.getAmmunitionSpeed());
        contentValues.put(ammunition_weight, hardwareItem.getAmmunitionWeight());
        contentValues.put(ammunition_size_x, hardwareItem.getAmmunitionSizeMillisY());
        contentValues.put(ammunition_size_y, hardwareItem.getAmmunitionSizeMillisY());
        contentValues.put(ammunition_size_z, hardwareItem.getAmmunitionSizeMillisZ());
        contentValues.put(ammunition_drag_coefficient_x, hardwareItem.getAmmunitionDragCoefficientYValue());
        contentValues.put(ammunition_drag_coefficient_y, hardwareItem.getAmmunitionDragCoefficientYValue());
        db.update(TABLE_AMMUNITION, contentValues, ammunition_id + " = ?",new String[] { hardwareItem.getHardwareIdToString() });
        db.close();
        return true;
    }


    // Delete ammunition
    boolean deleteAmmunition(final String ammunition_id_) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+ TABLE_AMMUNITION + " WHERE " + ammunition_id + " = ?", new String[]{ammunition_id_});
        db.close();
        return true;
    }


    // ---------------------------------------------------------------------------------------------
    /* Database upgrade script */

    private static List<String> GetColumns(SQLiteDatabase db, String tableName) {
        List<String> ar = null;
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 1", null);
            if (c != null) {
                ar = new ArrayList<>(Arrays.asList(c.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
        return ar;
    }

    private static String join(List<String> list, String delim) {
        StringBuilder buf = new StringBuilder();
        int num = list.size();
        for (int i = 0; i < num; i++) {
            if (i != 0)
                buf.append(delim);
            buf.append((String) list.get(i));
        }
        return buf.toString();
    }

    // ---------------------------------------------------------------------------------------------

} // End of class