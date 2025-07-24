package com.logic.reconocimientodeformas;

import static com.logic.reconocimientodeformas.AreaDibujo.area;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.logic.reconocimientodeformas.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import android.util.Base64;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'reconocimientodeformas' library on application startup.
    static {
        System.loadLibrary("reconocimientodeformas");
    }

    private ActivityMainBinding binding;
    private Button button;
    private Button predecir;
    private Spinner spinner;
    private TextView clasificado;

    private boolean momento = false;
    List<String> items;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        items = Arrays.asList("Momentos de HU", "Momentos de Zernike");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        button = findViewById(R.id.limpiar);

        button.setOnClickListener(v ->{
           area.clean();
        });

        spinner = findViewById(R.id.seleccion);
        spinner.setAdapter(adapter);

        predecir = findViewById(R.id.clasificar);
        clasificado = findViewById(R.id.clasificado);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if(selected == "Momentos de HU"){
                    momento = false;
                }else{
                    momento = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        predecir.setOnClickListener(v ->{
            Bitmap bitmapEntrada = area.getBitmap();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmapEntrada.compress(Bitmap.CompressFormat.PNG, 100, output);
            byte[] bytes = output.toByteArray();

            String base = Base64.encodeToString(bytes, Base64.DEFAULT);


            String ruta = copyRawResourceToInternalStorage(this, R.raw.hu_moments, "hu_moments.csv");
            clasificado.setText(reconocimiento(bitmapEntrada, momento, ruta));
        });

        // Example of a call to a native method
        //TextView tv = binding.sampleText;
        //tv.setText(stringFromJNI());
    }


    public String copyRawResourceToInternalStorage(Context context, int rawResourceId, String outputFileName) {
        File outputFile = new File(context.getFilesDir(), outputFileName);

        try (InputStream inputStream = context.getResources().openRawResource(rawResourceId);
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return outputFile.getAbsolutePath(); // Retornar la ruta absoluta del archivo copiado
    }


    /**
     * A native method that is implemented by the 'reconocimientodeformas' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native String reconocimiento(Bitmap entrada, boolean momento, String ruta);
}

enum clasificador{
    HU,
    ZERNIKE
}