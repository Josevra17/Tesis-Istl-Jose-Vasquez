#include <WiFi.h>
#include <FirebaseESP32.h>
#include <ESP32Servo.h>
#include <DHT.h>

// Configuración Wi-Fi
#define WIFI_SSID "Red17"
#define WIFI_PASSWORD "Jose123123"

// Configuración de Firebase
#define FIREBASE_HOST "app-firebase-v3-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "Zbwu61OvXFw47TgwDi7jehxIp5VOzSxA4tdTWHtA"

// Definir los pines
#define LED_PIN 2
#define SERVO_PIN 5
#define BUZZER_PIN 16
#define DHT_PIN 4
#define DHT_TYPE DHT11  // Cambiar a DHT22 si usas ese sensor

// Inicializar Firebase y DHT
FirebaseData firebaseData;
DHT dht(DHT_PIN, DHT_TYPE);
Servo servoMotor;

void setup() {
  Serial.begin(115200);

  // Configuración de WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Conectado a WiFi");

  // Configuración de Firebase
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.reconnectWiFi(true);

  // Configuración de pines
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  myServo.attach(SERVO_PIN);
  dht.begin();

  // Iniciar flujo de datos
  if (!Firebase.beginStream(firebaseData, "/")) {
    Serial.println("No se pudo iniciar flujo de datos");
    Serial.println("Razón: " + firebaseData.errorReason());
  }

  // Configurar callback de flujo de datos
  Firebase.setStreamCallback(firebaseData, streamCallback, streamTimeoutCallback);
}

void loop() {
  // Mantén el código principal simple
}

// Callback cuando hay cambios en Firebase
void streamCallback(StreamData data) {
  String path = data.dataPath();
  int valor = data.intData();

  if (path == "/Led") {
    digitalWrite(LED_PIN, valor);
  } else if (path == "/Cervo") {
    myServo.write(map(valor, 0, 180, 0, 180)); // Ajusta el rango si es necesario
  } else if (path == "/Buzzer") {
    if (valor == 1) {
      tone(BUZZER_PIN, 1000); // Emitir sonido
      delay(500); // Duración del sonido
      noTone(BUZZER_PIN); // Detener sonido
    }
  }
}

// Callback en caso de error o tiempo de espera
void streamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println("Tiempo de espera excedido");
  }
}