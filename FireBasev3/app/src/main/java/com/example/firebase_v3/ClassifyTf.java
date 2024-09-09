package com.example.firebase_v3;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.firebase_v3.ml.ModelTFlite;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
// Autor Jose Vasquez

public class ClassifyTf {
    // Tamaño de entrada requerido por el modelo TensorFlow Lite
    private static final int INPUT_SIZE = 224;

    // Instancia del modelo TensorFlow Lite
    private final ModelTFlite modelTfp;

    // Interfaz para comunicar los resultados de la clasificación
    private ReturnInterpreter returnInterpreter;

    /**
     * Constructor de la clase.
     * Inicializa el modelo TensorFlow Lite con el contexto proporcionado.
     */
    public ClassifyTf(Context context) {
        try {
            // Intenta crear una nueva instancia del modelo
            this.modelTfp = ModelTFlite.newInstance(context);
        } catch (IOException e) {
            // Si hay un error al cargar el modelo, lanza una excepción
            throw new RuntimeException("Error al cargar el modelo TensorFlow Lite", e);
        }
    }

    /**
     * Establece el listener para recibir los resultados de la clasificación.
     */
    public void listenerInterpreter(ReturnInterpreter returnInterpreter) {
        this.returnInterpreter = returnInterpreter;
    }

    /**
     * Método principal para clasificar una imagen.
     * Procesa el bitmap y devuelve los resultados a través del returnInterpreter.
     */
    public void classify(Bitmap bitmap) {
        // Verifica si se ha establecido el returnInterpreter
        if (returnInterpreter == null) {
            throw new IllegalStateException("ReturnInterpreter no ha sido establecido. Llama a setInterpreterListener primero.");
        }

        // Prepara la entrada, procesa la imagen y obtiene la salida
        TensorBuffer inputFeature = prepareInputFeature(bitmap);
        ModelTFlite.Outputs output = modelTfp.process(inputFeature);
        TensorBuffer outputFeature = output.getOutputFeature0AsTensorBuffer();

        // Obtiene el array de confianza y encuentra el índice con mayor confianza
        float[] confidence = outputFeature.getFloatArray();
        int maxPos = findMaxConfidenceIndex(confidence);

        // Comunica los resultados a través del returnInterpreter
        returnInterpreter.classify(confidence, maxPos);
    }

    /**
     * Prepara el TensorBuffer de entrada a partir de un bitmap.
     */
    private TensorBuffer prepareInputFeature(Bitmap bitmap) {
        // Crea un TensorBuffer con el tamaño y tipo de datos adecuados
        TensorBuffer inputFeature = TensorBuffer.createFixedSize(new int[]{1, INPUT_SIZE, INPUT_SIZE, 3}, DataType.FLOAT32);

        // Prepara un ByteBuffer para almacenar los datos de la imagen
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        // Obtiene los valores de los píxeles del bitmap
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        // Convierte los valores de los píxeles al formato requerido por el modelo
        for (int pixelValue : intValues) {
            float red = ((pixelValue >> 16) & 0xFF) / 255.0f;
            float green = ((pixelValue >> 8) & 0xFF) / 255.0f;
            float blue = (pixelValue & 0xFF) / 255.0f;

            byteBuffer.putFloat(red);
            byteBuffer.putFloat(green);
            byteBuffer.putFloat(blue);        // Formato color Azul
        }

        // Carga el ByteBuffer en el TensorBuffer
        inputFeature.loadBuffer(byteBuffer);
        return inputFeature;
    }

    /**
     * Encuentra el índice del valor máximo en el array de confianza.
     */
    private int findMaxConfidenceIndex(float[] confidence) {
        int maxPos = 0;
        for (int i = 1; i < confidence.length; i++) {
            if (confidence[i] > confidence[maxPos]) {
                maxPos = i;
            }
        }
        return maxPos;
    }
}