#include <WiFi.h>
#include <FirebaseESP32.h>
#include <ESP32Servo.h>
#include <DHT.h>

// Configuración Wi-Fi
#define WIFI_SSID "Red17"
#define WIFI_PASSWORD "Jose123J"

// Configuración de Firebase
#define FIREBASE_HOST "app-firebase-v3-default-rtdb.firebaseio.com"
#define FIREBASE_AUTH "Zbwu61OvXFw47TgwDi7jehxIp5VOzSxA4tdTWHtA"

// Pines
#define LED_PIN 14
#define SERVO_PIN 26
#define BUZZER_PIN 25
#define DHT_PIN 27
#define DHT_TYPE DHT11
const int relayPin = 32;

// Inicialización de objetos
FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;
DHT dht(DHT_PIN, DHT_TYPE);
Servo myservo;

void setup() {
  Serial.begin(115200);

  // Conexión Wi-Fi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nConectado a WiFi");

  // Configuración de Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Inicialización de pines y sensores
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  myservo.attach(SERVO_PIN);
  dht.begin();
  pinMode(relayPin, OUTPUT);
  digitalWrite(relayPin, HIGH);
}

void loop() {
  // Leer el estado del LED desde Firebase
  if (Firebase.getInt(firebaseData, "/Led")) {
    if (firebaseData.intData() == 1) {
      Serial.println("Se encendió el LED");
      digitalWrite(LED_PIN, HIGH);
    } else {
      Serial.println("Se apagó el LED");
      digitalWrite(LED_PIN, LOW);
    }
  } else {
    Serial.println("Error al leer el estado del LED");
    Serial.println(firebaseData.errorReason());
  }

  // Leer el valor del servomotor desde Firebase
  if (Firebase.getInt(firebaseData, "/Cervo")) {
    int servoPosition = firebaseData.intData();
    int angle = 0;
    switch (servoPosition) {
      case 0:
        angle = 0;
        break;
      case 1:
        angle = 90;
        break;
      case 2:
        angle = 180;
        break;
    }
    Serial.print("Moviendo el servomotor a ");
    Serial.print(angle);
    Serial.println(" grados");
    myservo.write(angle);
  } else {
    Serial.println("Error al leer el valor del servomotor");
    Serial.println(firebaseData.errorReason());
  }

  // Leer el estado del buzzer desde Firebase
  if (Firebase.getInt(firebaseData, "/Buzzer")) {
    if (firebaseData.intData() == 1) {
      Serial.println("Activando el buzzer");
      digitalWrite(BUZZER_PIN, HIGH);
      delay(1000); // Hacer sonar el buzzer durante 1 segundo
      digitalWrite(BUZZER_PIN, LOW);
      Firebase.setInt(firebaseData, "/Buzzer", 0); // Apagar el buzzer en Firebase
    }
  } else {
    Serial.println("Error al leer el estado del buzzer");
    Serial.println(firebaseData.errorReason());
  }

  // Leer y enviar temperatura y humedad a Firebase
  float temperatura = dht.readTemperature();
  float humedad = dht.readHumidity();

  if (!isnan(temperatura) && !isnan(humedad)) {
    int tempInt = (int)temperatura;
    int humedadInt = (int)humedad;

    Serial.print("Temperatura a enviar: ");
    Serial.println(tempInt);
    Serial.print("Humedad a enviar: ");
    Serial.println(humedadInt);

    if (Firebase.setInt(firebaseData, "/Temperatura", tempInt)) {
      Serial.println("Temperatura enviada a Firebase con éxito");
    } else {
      Serial.println("Error al enviar Temperatura");
      Serial.println(firebaseData.errorReason());
    }

    if (Firebase.setInt(firebaseData, "/Humedad", humedadInt)) {
      Serial.println("Humedad enviada a Firebase con éxito");
    } else {
      Serial.println("Error al enviar Humedad");
      Serial.println(firebaseData.errorReason());
    }

    // Encender ventilador automáticamente si la temperatura supera los 40 grados
    if (tempInt > 40) {
      Serial.println("Temperatura alta detectada, encendiendo ventilador por 15 segundos.");
      digitalWrite(relayPin, HIGH);
      delay(15000); // Ventilador encendido por 15 segundos
      digitalWrite(relayPin, LOW);
    }

  } else {
    Serial.println("Error al leer del sensor DHT11");
  }

  // Leer el estado del ventilador desde Firebase
  if (Firebase.getInt(firebaseData, "/Ventilador")) {
    if (firebaseData.intData() == 1) {
      Serial.println("Se encendió el ventilador");
      digitalWrite(relayPin, LOW);
    } else {
      Serial.println("Se apagó el ventilador");
      digitalWrite(relayPin, HIGH);
    }
  } else {
    Serial.println("Error al leer el estado del ventilador");
    Serial.println(firebaseData.errorReason());
  }

  // Delay para la siguiente iteración
  delay(2000);
}
