package com.logic.reconocimientodeformas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AreaDibujo extends View {

    private float posX = 0, posY = 0;
    private Path currentPath;
    private Paint currentPaint;
    private List<Path> drawnPaths;
    private List<Paint> drawnPaints;

    public static AreaDibujo areaInstance;

    public AreaDibujo(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        drawnPaths = new ArrayList<>();
        drawnPaints = new ArrayList<>();
        areaInstance = this;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Dibujar todos los trazos guardados
        for (int i = 0; i < drawnPaths.size(); i++) {
            canvas.drawPath(drawnPaths.get(i), drawnPaints.get(i));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        posX = event.getX();
        posY = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Iniciar un nuevo trazo cuando se presiona la pantalla
                currentPaint = createRandomPaint();
                currentPath = new Path();
                currentPath.moveTo(posX, posY);
                drawnPaths.add(currentPath);
                drawnPaints.add(currentPaint);
                break;
                
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                // Continuar el trazo a medida que el dedo se mueve
                int historicalPoints = event.getHistorySize();
                for (int i = 0; i < historicalPoints; i++) {
                    currentPath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                break;
        }

        invalidate(); // Redibujar la vista
        return true;
    }

    private Paint createRandomPaint() {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);

        Paint paint = new Paint();
        paint.setStrokeWidth(15);
        paint.setARGB(255, red, green, blue); // Color aleatorio
        paint.setStyle(Paint.Style.STROKE);   // Solo contorno
        return paint;
    }

    public Bitmap getBitmap() {
        // Crear un bitmap del área de dibujo
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);  // Fondo blanco
        draw(canvas);                   // Dibujar los trazos en el canvas
        return bitmap;
    }

    public void clean() {
        // Limpiar los trazos y redibujar
        drawnPaths.clear();
        drawnPaints.clear();
        invalidate();
    }
}
