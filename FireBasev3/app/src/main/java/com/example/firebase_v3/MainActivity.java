package com.example.firebase_v3;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// Autor Jose Vasquez

public class MainActivity extends AppCompatActivity {
    // Variables de para la interfaz
    // Botones de la interfaz

    private Switch switchFirebase;
    private Switch switchVentilador;
    private SeekBar seekBar;
    private Button buttonBuzzer;
    private Button buttonC;
    private Button butonTemp;

    //Referencias a la base de datos FireBase

    private DatabaseReference myRef, refseek, refBuzz, refTemp, refVentilador;

    //Variables usadas para mostrar el Alert de Temperatura

    private AlertDialog temperatureDialog;
    private TextView temperatureTextView;
    private Handler handler = new Handler();
    private boolean isDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicializaciond de la Vista y referencia a la base de datos
        Log.d("MainActivity", "Inicializando vistas y referencias de Firebase.");
        inicializarView();
        inicializarReferenciasFB();

        //Inicializar metodos configuradores de los eventos listener de los botones
        configurarSwitchListener();
        configurarSeekBarListener();
        configurarBotonesListener();
    }
    private void inicializarView(){
        switchFirebase = findViewById(R.id.switchFirebase);
        seekBar = findViewById(R.id.seekBar);
        buttonBuzzer = findViewById(R.id.buttonBuzzer);
        buttonC = findViewById(R.id.buttonCamara);
        butonTemp = findViewById(R.id.buttonTemp);
        switchVentilador = findViewById(R.id.switchVentilador);
        Log.d("MainActivity", "Vistas inicializadas correctamente.");
    }
    private void inicializarReferenciasFB(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Led");
        refseek = database.getReference("Cervo");
        refBuzz = database.getReference("Buzzer");
        refTemp = database.getReference("Temperatura");
        refVentilador = database.getReference("Ventilador");
        Log.d("MainActivity", "Referencias de Firebase inicializadas correctamente.");
    }

    //Metodo para configurar la accion de la barra de progreso
    private void configurarSeekBarListener(){
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("MainActivity", "SeekBar progress changed: " + progress);
                refseek.setValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    //Metodo para configurar la accion de los interruptores
    private void configurarSwitchListener(){
        switchFirebase.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("MainActivity", "Switch Firebase changed: " + isChecked);
                myRef.setValue(isChecked ? 1 : 0);
            }
        });

        switchVentilador.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("MainActivity", "Switch Ventilador changed: " + isChecked);
                refVentilador.setValue(isChecked ? 1 : 0);
            }
        });
    }

    //metodo para configurar la accion de los botones
    private void configurarBotonesListener(){
        buttonBuzzer.setOnClickListener(v -> refBuzz.setValue(1));

        buttonC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainActivity", "Button Buzzer clicked.");
                Intent intent = new Intent(MainActivity.this, CamarAI.class);
                startActivity(intent);
            }
        });

        butonTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTemperatureDialog();
            }
        });
    }

    //Metodo para mostrar la ventana emergente de termperatura
    private void showTemperatureDialog() {
        Log.d("MainActivity", "Mostrando dialogo con temperatura.");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.temperature_dialog, null);
        temperatureTextView = dialogView.findViewById(R.id.temperatureTextView);

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
}