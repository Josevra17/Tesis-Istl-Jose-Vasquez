package com.example.firebase_v3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.firebase_v3.databinding.ActivityCamaraiBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ingenieriiajhr.jhrCameraX.BitmapResponse;
import com.ingenieriiajhr.jhrCameraX.CameraJhr;
// Autor Jose Vasquez
public class CamarAI extends AppCompatActivity {
    private @NonNull ActivityCamaraiBinding binding;
    private CameraJhr cameraJhr;
    private ClassifyTf classifyTf;
    private Button activateCameraButton;
    private FloatingActionButton fabAyuda;
    private android.app.AlertDialog temperatureDialog;
    private TextView temperatureTextView;
    private Handler handler = new Handler();
    private boolean isDialogShowing = false;

    public static final int INPUT_SIZE = 224;
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    private String[] classes = {"Vacio","Prender Luz","Apagar Luz","Abrir Cortinas","Temperatura","Cerrar Cortinas","Timbre","Prender Ventilador","Apagar Ventilador"
    };

    // Variables para la detección consistente y el control de comandos
    private String detectedClass = "";
    private long detectionStartTime = 0;
    private static final long DETECTION_PERIOD = 3000; // 2 segundos
    private static final long COMMAND_DELAY = 5000; // 5 segundos de espera después de la confirmación
    private boolean waitingForConfirmation = false;
    private long lastCommandTime = 0;

    // Referencias de Firebase
    private DatabaseReference myRef, refseek, refBuzz, refTemp, refVent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCamaraiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        classifyTf = new ClassifyTf(this);
        cameraJhr = new CameraJhr(this);

        activateCameraButton = findViewById(R.id.button_activate_camera);
        activateCameraButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(CamarAI.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                showCameraPreview();
            } else {
                ActivityCompat.requestPermissions(CamarAI.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        });

        fabAyuda = findViewById(R.id.fab_help);
        fabAyuda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CamarAI.this, AyudaActivity.class);
                startActivity(i);
            }
        });

        // Inicializar referencias de Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Led");
        refseek = database.getReference("Cervo");
        refBuzz = database.getReference("Buzzer");
        refTemp = database.getReference("Temperatura");
        refVent = database.getReference("Ventilador");

    }

    private void showCameraPreview() {
        binding.cameraPreview.setVisibility(View.VISIBLE);
        activateCameraButton.setVisibility(View.GONE);
        startCameraJhr();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showCameraPreview();
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCameraJhr() {
        cameraJhr.addlistenerBitmap(new BitmapResponse() {
            @Override
            public void bitmapReturn(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    classifyImage(bitmap);
                }
            }
        });

        cameraJhr.initBitmap();
        cameraJhr.initImageProxy();
        cameraJhr.start(0, 0, binding.cameraPreview, true, false, true);
    }

    private void classifyImage(Bitmap bitmap) {
        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        classifyTf.listenerInterpreter(new ReturnInterpreter() {
            @Override
            public void classify(float[] confidence, int maxConfidence) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        binding.txtResult.setText(
                                //classes[0] + ": " + confidence[0] + "\n" +
                                  //      classes[1] + ": " + confidence[1] + "\n" +
                                    //    classes[2] + ": " + confidence[2] + "\n" +
                                      //  classes[3] + ": " + confidence[3] + "\n" +
                                        //classes[4] + ": " + confidence[4] + "\n" +
                                        "Acción: " + classes[maxConfidence]
                        );

                        String currentClass = classes[maxConfidence];
                        long currentTime = System.currentTimeMillis();

                        if (!waitingForConfirmation && confidence[maxConfidence] > 0.7) {
                            if (currentClass.equals(detectedClass)) {
                                // Si la clase es la misma, verifica si ha pasado el tiempo de detección
                                if (currentTime - detectionStartTime > DETECTION_PERIOD) {
                                    // Muestra un cuadro de diálogo para la confirmación
                                    showConfirmationDialog(currentClass);
                                    waitingForConfirmation = true;
                                }
                            } else {
                                // Si la clase es diferente, reinicia el temporizador de detección
                                detectedClass = currentClass;
                                detectionStartTime = currentTime;
                            }
                        }
                    }
                });
            }
        });

        classifyTf.classify(bitmapScale);
    }

    private void showConfirmationDialog(String actionClass) {
        String message = "";

        switch (actionClass) {
            case "Prender Luz":
                message = "Se encenderá la luz.";
                break;
            case "Apagar Luz":
                message = "Se apagará la luz.";
                break;
            case "Abrir Cortinas":
                message = "Se abrirán las cortinas.";
                break;
            case "Temperatura":
                showTemperatureDialog();
                return;  // No continuar con el flujo normal si es "Temperatura"
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage(message)
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    executeAction(actionClass);
                    startDetectionDelay();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    waitingForConfirmation = false;
                    startDetectionDelay();
                })
                .show();
    }

    // Método para iniciar un retraso antes de permitir una nueva detección
    private void startDetectionDelay() {
        waitingForConfirmation = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                waitingForConfirmation = false;
            }
        }, COMMAND_DELAY);  // Esperar 5 segundos antes de permitir una nueva detección
    }

    private void executeAction(String actionClass) {
        switch (actionClass) {
            case "Prender Luz":
                myRef.setValue(1);  // Enviar comando para encender la luz a Firebase
                break;
            case "Apagar Luz":
                myRef.setValue(0);  // Enviar comando para apagar la luz a Firebase
                break;
            case "Abrir Cortinas":
                refseek.setValue(1);  // Enviar comando para abrir cortinas a Firebase
                break;
            case "Temperatura":
                showTemperatureDialog();
                break;
            case "Cerrar Cortinas":
                refseek.setValue(0);  // Enviar comando para cerrar cortinas a Firebase
                break;
            case "Timbre":
                refBuzz.setValue(1);  // Enviar comando para tocar timbre a Firebase
                break;
            case "Prender Ventilador":
                refVent.setValue(1);  // Enviar comando para prender ventilador a Firebase
                break;
            case "Apagar Ventilador":
                refVent.setValue(0);  // Enviar comando para apagar ventilador a Firebase
                break;
        }

        // Restablecer el estado para la siguiente detección
        waitingForConfirmation = false;
        lastCommandTime = System.currentTimeMillis();
    }
    private void showTemperatureDialog() {
        Log.d("CamarAI", "Mostrando dialogo con temperatura.");
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.temperature, null);
        temperatureTextView = dialogView.findViewById(R.id.temperatureText);

        builder.setView(dialogView)
                .setTitle("Temperatura")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isDialogShowing = false;
                        Log.d("MainActivity", "Dialogo de temperatura cerrado");
                    }
                });
        temperatureDialog = builder.create();
        temperatureDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isDialogShowing = false;
            }
        });

        temperatureDialog.show();
        isDialogShowing = true;
        startTemperatureUpdates();
    }

    //Metoodo para iniciar la actualizacion de la temperatura cada segundo
    private void startTemperatureUpdates() {
        Log.d("MainActivity", "Iniciando actualizadcion de temperatura");
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isDialogShowing) {
                    updateTemperature();
                    handler.postDelayed(this, 1000); // Actualiza cada segundo
                }
            }
        });
    }

    //Metodo para actualizar la vista de la temperatura si esta cambia
    private void updateTemperature() {
        refTemp.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Double temperature = dataSnapshot.getValue(Double.class);
                    if (temperature != null) {
                        Log.d("MainActivity", "Temperature actualizada: " + temperature);

                        temperatureTextView.setText(String.format("%.1f °C", temperature));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Error en carga de temperatura: " + databaseError.getMessage());


            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (temperatureDialog != null && temperatureDialog.isShowing()) {
            temperatureDialog.dismiss();
        }
        handler.removeCallbacksAndMessages(null);
    }
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}