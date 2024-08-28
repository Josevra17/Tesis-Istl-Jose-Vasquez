package com.example.firebase_v3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.firebase_v3.databinding.ActivityCamaraiBinding;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ingenieriiajhr.jhrCameraX.BitmapResponse;
import com.ingenieriiajhr.jhrCameraX.CameraJhr;

public class CamarAI extends AppCompatActivity {

    // Ligadura o binding para lograr el acceso eficiente a las vistas
    private @NonNull ActivityCamaraiBinding binding;

    // Declaraciones para iniciar la camara y reconocimiento de las imagenes en tiempo real
    private CameraJhr cameraJhr;
    private ClassifyTf classifyTf;
    private Button activateCameraButton;

    // Constantes para usar en la configuracion de la camara y solicitud de los permisos
    public static final int INPUT_SIZE = 224;
    public static final int REQUEST_CAMERA_PERMISSION = 200;

    //Arreglo que contiene las clases con las que ha sido entrenado el modelo TFLite
    private String[] classes = {"Vacio", "Prender Luz", "Apagar Luz", "Abrir Cortinas", "Temperatura"};

    // Variables para la detección consistente y el control de comandos
    private String detectedClass = "";
    private long detectionStartTime = 0;
    private static final long DETECTION_PERIOD =2000; // 2 segundos para poder varificar gesto
    private boolean waitingForConfirmation = false;
    private long lastCommandTime = 0;

    // Referencias de Firebase para el control de los dispositivos
    private DatabaseReference myRef, refseek, refBuzz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCamaraiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d("CamarAI", "Iniciando componentes y activando cámara.");
        inciarComponentes();
        activarCamara();
        inicialiazarRefrenciasFirebase();
    }

    //Inicializa los componentes principales para la camara
    private void inciarComponentes(){
        classifyTf = new ClassifyTf(this);
        cameraJhr = new CameraJhr(this);
        activateCameraButton = findViewById(R.id.button_activate_camera);
        Log.d("CamarAI", "Componentes iniciados correctamente.");

    }

    //Darle funcionlidad al boton que activa la camara
    private void activarCamara(){
        activateCameraButton.setOnClickListener(v -> {
            Log.d("CamarAI", "Botón de activación de cámara presionado.");
            permisosCamara();
        });
    }

    //Inicializa la base de datos y las referencias de Firebase
    private void inicialiazarRefrenciasFirebase(){
        // Inicializar referencias de Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Led");
        refseek = database.getReference("Cervo");
        refBuzz = database.getReference("Buzzer");
    }

    //Metodo encargado de verificar los permisos de uso de la camara
    private void permisosCamara(){
        if (ContextCompat.checkSelfPermission(CamarAI.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCameraPreview();
        } else {
            ActivityCompat.requestPermissions(CamarAI.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    //Muestra una vista previa de la camara
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


    // Inicia la cámara y configura el procesamiento de imágenes
    private void startCameraJhr() {
        cameraJhr.addlistenerBitmap(this::processImage);
        cameraJhr.initBitmap();
        cameraJhr.initImageProxy();
        cameraJhr.start(0, 0, binding.cameraPreview, true, false, true);
    }

    // Procesa la imagen capturada
    private void processImage(@Nullable Bitmap bitmap) {
        if (bitmap != null) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            classifyTf.listenerInterpreter(this::handleClassificationResult);
            classifyTf.classify(scaledBitmap);
        }
    }

    // Maneja el resultado de la clasificación
    private void handleClassificationResult(float[] confidence, int maxConfidence) {
        runOnUiThread(() -> {
            updateResultText(confidence, maxConfidence);
            checkForGestureDetection(classes[maxConfidence], confidence[maxConfidence]);
        });
    }

    // Actualiza el texto de resultado en la UI
    private void updateResultText(float[] confidence, int maxConfidence) {
        StringBuilder resultText = new StringBuilder();
        for (int i = 0; i < classes.length; i++) {
            resultText.append(classes[i]).append(": ").append(confidence[i]).append("\n");
        }
        resultText.append("Mejor: ").append(classes[maxConfidence]);
        binding.txtResult.setText(resultText.toString());
    }

    // Verifica si se ha detectado un gesto válido
    private void checkForGestureDetection(String currentClass, float confidence) {
        long currentTime = System.currentTimeMillis();
        if (!waitingForConfirmation && confidence > 0.7) {
            if (currentClass.equals(detectedClass) && currentTime - detectionStartTime > DETECTION_PERIOD) {
                showConfirmationDialog(currentClass);
                waitingForConfirmation = true;
            } else {
                detectedClass = currentClass;
                detectionStartTime = currentTime;
            }
        }
    }

    // Muestra un diálogo de confirmación para la acción detectada
    private void showConfirmationDialog(String actionClass) {
        String message = getMessageForAction(actionClass);
        new AlertDialog.Builder(this)
                .setTitle("Confirmación")
                .setMessage(message)
                .setPositiveButton("Aceptar", (dialog, which) -> executeAction(actionClass))
                .setNegativeButton("Cancelar", (dialog, which) -> waitingForConfirmation = false)
                .show();
    }

    // Obtiene el mensaje correspondiente a la acción detectada
    private String getMessageForAction(String actionClass) {
        switch (actionClass) {
            case "Prender Luz": return "Se encenderá la luz.";
            case "Apagar Luz": return "Se apagará la luz.";
            case "Abrir Cortinas": return "Se abrirán las cortinas.";
            case "Temperatura": return "Se medirá la temperatura.";
            default: return "Acción no reconocida.";
        }
    }

    // Ejecuta la acción correspondiente al gesto detectado
    private void executeAction(String actionClass) {
        switch (actionClass) {
            case "Prender Luz":
                myRef.setValue(1);
                break;
            case "Apagar Luz":
                myRef.setValue(0);
                break;
            case "Abrir Cortinas":
                refseek.setValue(2);
                break;
            case "Temperatura":

                break;
        }
        waitingForConfirmation = false;
        lastCommandTime = System.currentTimeMillis();
    }

    // Método de utilidad para rotar bitmaps (si es necesario)
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}