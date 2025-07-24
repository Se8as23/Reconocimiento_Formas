package com.logic.reconocimientodeformas;

import static com.logic.reconocimientodeformas.AreaDibujo.area;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.logic.reconocimientodeformas.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    // Cargar la biblioteca nativa 'reconocimientodeformas'
    static {
        System.loadLibrary("reconocimientodeformas");
    }

    private ActivityMainBinding binding;
    private Button buttonClear;
    private Button buttonPredict;
    private Spinner spinner;
    private TextView resultText;

    private boolean isHuMoments = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
    }

    private void setupUI() {
        buttonClear = findViewById(R.id.limpiar);
        buttonPredict = findViewById(R.id.clasificar);
        resultText = findViewById(R.id.clasificado);

        setupSpinner();
        setupClearButton();
        setupPredictButton();
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getClassifiersList());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.seleccion);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                isHuMoments = position == 0; // Hu Moments selected if position is 0
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClearButton() {
        buttonClear.setOnClickListener(v -> area.clean());
    }

    private void setupPredictButton() {
        buttonPredict.setOnClickListener(v -> {
            Bitmap inputBitmap = area.getBitmap();
            String encodedBitmap = encodeBitmapToBase64(inputBitmap);

            String filePath = copyRawResourceToInternalStorage(this, R.raw.hu_moments, "hu_moments.csv");
            resultText.setText(runRecognition(inputBitmap, filePath));
        });
    }

    private String runRecognition(Bitmap inputBitmap, String filePath) {
        return reconocimiento(inputBitmap, isHuMoments, filePath);
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] bytes = outputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private List<String> getClassifiersList() {
        return Arrays.asList("Momentos de HU", "Momentos de Zernike");
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

        return outputFile.getAbsolutePath();
    }

    // Métodos nativos de C++ que se comunican con la biblioteca nativa
    public native String stringFromJNI();
    public native String reconocimiento(Bitmap entrada, boolean momento, String ruta);
}
