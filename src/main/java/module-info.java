module blash10x.ocrtranslator {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.graphics;
  requires javafx.swing;
  requires java.desktop;
  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires com.google.common;
  requires com.google.genai;
  requires opencv;
  requires tess4j;
  requires static lombok;

  opens blash10x.ocrtranslator to javafx.fxml;
  exports blash10x.ocrtranslator;

  opens blash10x.ocrtranslator.controller to javafx.fxml;
  exports blash10x.ocrtranslator.controller;
}