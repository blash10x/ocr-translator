module blash10x.ocrtranslator {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.swing;
  requires java.desktop;
  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires tess4j;

  opens blash10x.ocrtranslator to javafx.fxml;
  exports blash10x.ocrtranslator;

  opens blash10x.ocrtranslator.controller to javafx.fxml;
  exports blash10x.ocrtranslator.controller;
}