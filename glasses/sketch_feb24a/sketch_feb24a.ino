#include <OLEDDisplay.h>
#include <OLEDDisplayFonts.h>
#include <OLEDDisplayUi.h>

#include <Wire.h>
#include <SSD1306.h>

// Initialize the OLED display using Wire library
SSD1306  display(0x3c, D3, D5);


void setup() {
  // put your setup code here, to run once:
  display.init();
  display.setFont(ArialMT_Plain_16);
  display.setTextAlignment(TEXT_ALIGN_LEFT);

}

void loop() {
    //Get UV
    int sensorValue = analogRead(A0);
    
    //Display
    display.clear();
    display.drawString(0, 0, "UV: " + String(sensorValue));
    display.display();
    delay(1000);
}
