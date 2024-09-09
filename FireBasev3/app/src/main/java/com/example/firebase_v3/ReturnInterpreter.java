package com.example.firebase_v3;
// Autor Jose Vasquez

public interface ReturnInterpreter {

    /**
     * Método para procesar y devolver los resultados de la clasificación.
     *
     * @param confidence Un array de flotantes que representa las puntuaciones de confianza
     *                   para cada clase posible. Cada índice en este array corresponde a
     *                   una clase específica, y el valor en ese índice representa la
     *                   confianza del modelo en esa clasificación.
     *
     * @param maxConfidence El índice del valor de confianza más alto en el array 'confidence'.
     *                      Este parámetro indica la clase que el modelo considera más probable
     *                      para la entrada dada.

     * Las implementaciones de este método deberían manejar estos resultados de la manera
     * apropiada para la aplicación, como mostrar la clase predicha al usuario,
     * almacenar los resultados, o realizar acciones basadas en la clasificación.
     */
    void classify(float[] confidence, int maxConfidence);
}