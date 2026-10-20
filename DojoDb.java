package com.bushido.leon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class DojoDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "bushido_dojo.db";
    private static final int DB_VERSION = 1;

    public DojoDb(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, age INTEGER DEFAULT 0, " +
                "grade TEXT, school TEXT, disciplines TEXT, monthly_fee REAL DEFAULT 0, notes TEXT, active INTEGER DEFAULT 1, " +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP)");
        db.execSQL("CREATE TABLE payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, student_id INTEGER NOT NULL, month TEXT NOT NULL, " +
                "paid_at TEXT NOT NULL, amount REAL NOT NULL, method TEXT, notes TEXT, receipt_no INTEGER, " +
                "FOREIGN KEY(student_id) REFERENCES students(id))");
        db.execSQL("CREATE INDEX idx_payments_student_month ON payments(student_id, month)");
        String[] names = {"LEO","ALEJANDRA","DIEGO","MELI","ANGELIN","CHAMPI","OSSURU","VIC","KING JULIAN",
                "BRIAN","EMI","KIKE","FER","SAULIN","MARIJO","JULY","LALITO","CHRISS"};
        for (String name : names) {
            ContentValues v = new ContentValues();
            v.put("name", name); v.put("age", 0); v.put("grade", "");
            v.put("school", "Bushido León"); v.put("disciplines", "Judo Kodokan");
            v.put("monthly_fee", 0); v.put("notes", "");
            db.insert("students", null, v);
        }
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    public long saveStudent(long id, String name, int age, String grade, String school,
                            String disciplines, double fee, String notes) {
        ContentValues v = new ContentValues();
        v.put("name", name.trim()); v.put("age", age); v.put("grade", grade);
        v.put("school", school); v.put("disciplines", disciplines);
        v.put("monthly_fee", fee); v.put("notes", notes);
        if (id > 0) return getWritableDatabase().update("students", v, "id=?", new String[]{String.valueOf(id)});
        return getWritableDatabase().insert("students", null, v);
    }

    public Cursor students() {
        return getReadableDatabase().rawQuery(
                "SELECT s.id,s.name,s.age,s.grade,s.school,s.disciplines,s.monthly_fee," +
                "COALESCE((SELECT SUM(p.amount) FROM payments p WHERE p.student_id=s.id AND p.month=strftime('%Y-%m','now','localtime')),0) AS paid " +
                "FROM students s WHERE s.active=1 ORDER BY s.name COLLATE NOCASE", null);
    }

    public Cursor studentById(long id) {
        return getReadableDatabase().rawQuery("SELECT id,name,monthly_fee FROM students WHERE id=?", new String[]{String.valueOf(id)});
    }

    public long addPayment(long studentId, String month, String date, double amount, String method, String notes) {
        ContentValues v = new ContentValues();
        v.put("student_id", studentId); v.put("month", month); v.put("paid_at", date);
        v.put("amount", amount); v.put("method", method); v.put("notes", notes);
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(receipt_no),0)+1 FROM payments", null);
        int receipt = 1; if (c.moveToFirst()) receipt = c.getInt(0); c.close();
        v.put("receipt_no", receipt);
        return getWritableDatabase().insert("payments", null, v);
    }

    public Cursor paymentHistory() {
        return getReadableDatabase().rawQuery(
                "SELECT p.id,p.receipt_no,s.name,p.month,p.paid_at,p.amount,p.method,p.notes " +
                "FROM payments p JOIN students s ON s.id=p.student_id ORDER BY p.id DESC", null);
    }

    public double totalPaidForMonth(String month) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(amount),0) FROM payments WHERE month=?", new String[]{month});
        double n = 0; if (c.moveToFirst()) n = c.getDouble(0); c.close(); return n;
    }

    public JSONArray exportJson() throws Exception {
        SQLiteDatabase db = getReadableDatabase();
        JSONArray out = new JSONArray();
        Cursor c = db.rawQuery("SELECT id,name,age,grade,school,disciplines,monthly_fee,notes,active,created_at FROM students ORDER BY id", null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("type", "student");
            String[] cols = {"id","name","age","grade","school","disciplines","monthly_fee","notes","active","created_at"};
            for (int i=0;i<cols.length;i++) o.put(cols[i], c.isNull(i) ? JSONObject.NULL : c.getString(i));
            out.put(o);
        }
        c.close();
        c = db.rawQuery("SELECT id,student_id,month,paid_at,amount,method,notes,receipt_no FROM payments ORDER BY id", null);
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            o.put("type", "payment");
            String[] cols = {"id","student_id","month","paid_at","amount","method","notes","receipt_no"};
            for (int i=0;i<cols.length;i++) o.put(cols[i], c.isNull(i) ? JSONObject.NULL : c.getString(i));
            out.put(o);
        }
        c.close();
        return out;
    }
}
