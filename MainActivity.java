package com.bushido.leon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private DojoDb db;
    private LinearLayout root, content;
    private static final int CREATE_BACKUP = 90;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new DojoDb(this);
        showHome();
    }

    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size);
        t.setTextColor(0xFF17263B); if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(dp(4),dp(5),dp(4),dp(5)); return t;
    }
    private Button button(String s, Runnable action) {
        Button b = new Button(this); b.setText(s); b.setOnClickListener(v -> action.run()); return b;
    }
    private EditText edit(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true);
        e.setPadding(dp(12),dp(8),dp(12),dp(8)); return e;
    }
    private void begin(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(0xFFF5F6F8);
        TextView header = text("BUSHIDO  •  " + title, 20, true);
        header.setTextColor(0xFFFFFFFF); header.setBackgroundColor(0xFF14243A);
        header.setPadding(dp(16),dp(18),dp(12),dp(18)); root.addView(header);
        LinearLayout nav = new LinearLayout(this); nav.setBackgroundColor(0xFFE5E9EF);
        nav.addView(button("Inicio", this::showHome), new LinearLayout.LayoutParams(0,dp(52),1));
        nav.addView(button("Alumnos", this::showStudents), new LinearLayout.LayoutParams(0,dp(52),1));
        nav.addView(button("Registrar", this::showStudentForm), new LinearLayout.LayoutParams(0,dp(52),1));
        nav.addView(button("Pagos", this::showPayments), new LinearLayout.LayoutParams(0,dp(52),1));
        root.addView(nav);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setPadding(dp(14),dp(12),dp(14),dp(24)); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    private void showHome() {
        begin("CONTROL DE DOJO");
        content.addView(text("Control local en el teléfono",22,true));
        content.addView(text("Los registros se guardan en SQLite dentro de esta aplicación. No requiere cuenta ni conexión a internet.",16,false));
        Cursor c = db.students(); int n = c.getCount(); c.close();
        content.addView(text("Alumnos registrados: " + n,18,true));
        String month = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        content.addView(text("Ingresos registrados en " + month + ": " + money(db.totalPaidForMonth(month)),18,true));
        content.addView(button("＋ Registrar alumno", this::showStudentForm));
        content.addView(button("Registrar pago", this::showPayments));
        content.addView(button("Crear respaldo JSON", this::backup));
        content.addView(text("IMPORTANTE: guarda copias de respaldo periódicas. Si desinstalas la aplicación o borras sus datos, la base local puede eliminarse.",14,false));
    }
    private String money(double n) { return String.format(Locale.getDefault(), "$%,.2f MXN", n); }

    private void showStudents() {
        begin("ALUMNOS");
        content.addView(button("＋ Nuevo alumno", this::showStudentForm));
        Cursor c = db.students();
        if (c.getCount()==0) content.addView(text("No hay alumnos registrados.",16,false));
        while (c.moveToNext()) {
            long id = c.getLong(0); String name = c.getString(1);
            String age = c.getInt(2)>0 ? String.valueOf(c.getInt(2))+" años" : "Edad sin capturar";
            String grade = c.getString(3)==null ? "" : c.getString(3);
            String school = c.getString(4)==null ? "" : c.getString(4);
            String disc = c.getString(5)==null ? "" : c.getString(5);
            double fee = c.getDouble(6), paid = c.getDouble(7);
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFFFFFFFF); card.setPadding(dp(10),dp(8),dp(10),dp(8));
            card.addView(text(name,18,true));
            card.addView(text(age + "  •  " + grade,14,false));
            card.addView(text(school + "  •  " + disc,14,false));
            card.addView(text("Mensualidad: " + money(fee) + "  |  Pagado este mes: " + money(paid),14,false));
            card.addView(button("Editar alumno", () -> showStudentForm(id)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(10); content.addView(card,p);
        }
        c.close();
    }

    private void showStudentForm() { showStudentForm(0); }
    private void showStudentForm(long studentId) {
        begin(studentId==0 ? "NUEVO ALUMNO" : "EDITAR ALUMNO");
        content.addView(text("Datos del alumno",20,true));
        EditText name=edit("Nombre completo *"); content.addView(name);
        EditText age=edit("Edad"); age.setInputType(2); content.addView(age);
        EditText grade=edit("Grado / cinta"); content.addView(grade);
        Spinner school = new Spinner(this); school.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Bushido León","Flow"})); content.addView(text("Escuela",14,true)); content.addView(school);
        CheckBox judo=new CheckBox(this); judo.setText("Judo Kodokan"); judo.setChecked(true); content.addView(judo);
        CheckBox jj=new CheckBox(this); jj.setText("Jiu-Jitsu"); content.addView(jj);
        CheckBox mma=new CheckBox(this); mma.setText("MMA"); content.addView(mma);
        EditText fee=edit("Mensualidad individual en pesos (ej. 800)"); fee.setInputType(8194); content.addView(fee);
        EditText notes=edit("Notas (opcional)"); content.addView(notes);
        if (studentId>0) {
            Cursor c=db.getReadableDatabase().rawQuery("SELECT name,age,grade,school,disciplines,monthly_fee,notes FROM students WHERE id=?",new String[]{String.valueOf(studentId)});
            if(c.moveToFirst()){
                name.setText(c.getString(0)); age.setText(c.getInt(1)==0?"":String.valueOf(c.getInt(1)));
                grade.setText(c.getString(2)); school.setSelection("Flow".equals(c.getString(3))?1:0);
                String ds=c.getString(4); judo.setChecked(ds!=null&&ds.contains("Judo Kodokan")); jj.setChecked(ds!=null&&ds.contains("Jiu-Jitsu"));
                mma.setChecked(ds!=null&&ds.contains("MMA")); fee.setText(c.getDouble(5)==0?"":String.valueOf(c.getDouble(5))); notes.setText(c.getString(6));
            } c.close();
        }
        Button save=button("GUARDAR ALUMNO", () -> {
            String nm=name.getText().toString().trim();
            if(nm.isEmpty()){ name.setError("Escribe el nombre del alumno"); name.requestFocus(); return; }
            int a=0; try { if(!age.getText().toString().trim().isEmpty()) a=Integer.parseInt(age.getText().toString().trim()); } catch(Exception ex){ age.setError("Edad no válida"); return; }
            double f=0; try { if(!fee.getText().toString().trim().isEmpty()) f=Double.parseDouble(fee.getText().toString().trim().replace(",",".")); } catch(Exception ex){ fee.setError("Mensualidad no válida"); return; }
            if(f<0){fee.setError("La mensualidad no puede ser negativa");return;}
            StringBuilder d=new StringBuilder(); if(judo.isChecked())d.append("Judo Kodokan, "); if(jj.isChecked())d.append("Jiu-Jitsu, "); if(mma.isChecked())d.append("MMA, ");
            if(d.length()>0)d.setLength(d.length()-2);
            long result=db.saveStudent(studentId,nm,a,grade.getText().toString().trim(),school.getSelectedItem().toString(),d.toString(),f,notes.getText().toString().trim());
            if(result>0){ Toast.makeText(this,"Alumno guardado correctamente",Toast.LENGTH_LONG).show(); showStudents(); }
            else Toast.makeText(this,"No se pudo guardar. Intenta otra vez.",Toast.LENGTH_LONG).show();
        });
        save.setTextSize(16); content.addView(save);
        content.addView(button("Cancelar", this::showStudents));
    }

    private void showPayments() {
        begin("REGISTRAR PAGO");
        Cursor students=db.getReadableDatabase().rawQuery("SELECT id,name,monthly_fee FROM students WHERE active=1 ORDER BY name COLLATE NOCASE",null);
        java.util.ArrayList<Long> ids=new java.util.ArrayList<>();
        java.util.ArrayList<String> labels=new java.util.ArrayList<>();
        while(students.moveToNext()){ ids.add(students.getLong(0)); labels.add(students.getString(1)+" — mensualidad "+money(students.getDouble(2))); }
        students.close();
        if(labels.isEmpty()){content.addView(text("Primero registra un alumno.",16,false));return;}
        Spinner who=new Spinner(this); who.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        content.addView(text("Alumno",14,true)); content.addView(who);
        String currentMonth=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
        EditText month=edit("Mes de pago (AAAA-MM)"); month.setText(currentMonth); content.addView(month);
        EditText date=edit("Fecha (AAAA-MM-DD)"); date.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())); content.addView(date);
        EditText amount=edit("Cantidad recibida en pesos"); amount.setInputType(8194); content.addView(amount);
        Spinner method=new Spinner(this); method.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Efectivo","Transferencia","Tarjeta","Otro"}));
        content.addView(text("Método de pago",14,true)); content.addView(method);
        EditText note=edit("Notas del pago (opcional)"); content.addView(note);
        content.addView(button("GUARDAR PAGO",()->{
            double value=0; try{value=Double.parseDouble(amount.getText().toString().trim().replace(",","."));}catch(Exception ex){amount.setError("Escribe una cantidad válida");return;}
            String m=month.getText().toString().trim(), dt=date.getText().toString().trim();
            if(!m.matches("\\d{4}-\\d{2}")){month.setError("Usa AAAA-MM");return;}
            if(value<=0){amount.setError("La cantidad debe ser mayor que cero");return;}
            long result=db.addPayment(ids.get(who.getSelectedItemPosition()),m,dt,value,method.getSelectedItem().toString(),note.getText().toString().trim());
            if(result>0){new AlertDialog.Builder(this).setTitle("Pago registrado").setMessage("Folio: "+result+"\nAlumno: "+labels.get(who.getSelectedItemPosition())+"\nMes: "+m+"\nCantidad: "+money(value)).setPositiveButton("Aceptar",(d,w)->showPaymentHistory()).show();}
            else Toast.makeText(this,"No se pudo registrar el pago",Toast.LENGTH_LONG).show();
        }));
        content.addView(button("Ver historial de pagos",this::showPaymentHistory));
    }

    private void showPaymentHistory() {
        begin("HISTORIAL DE PAGOS");
        Cursor c=db.paymentHistory();
        if(c.getCount()==0)content.addView(text("Todavía no hay pagos registrados.",16,false));
        while(c.moveToNext()){
            content.addView(text("Recibo #"+c.getInt(1)+" • "+c.getString(2),17,true));
            content.addView(text("Mes: "+c.getString(3)+" • Fecha: "+c.getString(4),14,false));
            content.addView(text(money(c.getDouble(5))+" • "+c.getString(6),16,true));
            if(c.getString(7)!=null&&!c.getString(7).isEmpty())content.addView(text(c.getString(7),14,false));
            View line=new View(this);line.setBackgroundColor(0xFFD5DCE5);content.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        } c.close();
    }

    private void backup() {
        try {
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/json"); i.putExtra(Intent.EXTRA_TITLE,"respaldo_bushido_dojo.json");
            startActivityForResult(i,CREATE_BACKUP);
        } catch(Exception e){Toast.makeText(this,"No se pudo abrir la opción de respaldo",Toast.LENGTH_LONG).show();}
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==CREATE_BACKUP && resultCode==RESULT_OK && data!=null){
            Uri uri=data.getData();
            try(OutputStream out=getContentResolver().openOutputStream(uri)){
                JSONArray json=db.exportJson();
                out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this,"Respaldo creado correctamente",Toast.LENGTH_LONG).show();
            } catch(Exception e){Toast.makeText(this,"Error al crear respaldo: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }
}
