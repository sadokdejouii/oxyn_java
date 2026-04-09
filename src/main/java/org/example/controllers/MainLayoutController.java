package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class MainLayoutController {

    @FXML
    private void handleFloatingButton(ActionEvent event) {
        System.out.println("Floating button clicked");
    }

    @FXML
    private void handleToggleMenu(ActionEvent event) {
        System.out.println("Toggle menu clicked");
    }

    @FXML
    private void handleHome(ActionEvent event) {
        System.out.println("Home clicked");
    }

    @FXML
    private void handleAnimals(ActionEvent event) {
        System.out.println("Animals clicked");
    }

    @FXML
    private void handleEquipment(ActionEvent event) {
        System.out.println("Equipment clicked");
    }

    @FXML
    private void handleStock(ActionEvent event) {
        System.out.println("Stock clicked");
    }

    @FXML
    private void handleBackofficeStock(ActionEvent event) {
        System.out.println("Backoffice Stock clicked");
    }

    @FXML
    private void handleCulture(ActionEvent event) {
        System.out.println("Culture clicked");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        System.out.println("Users clicked");
    }

    @FXML
    private void handleWorkers(ActionEvent event) {
        System.out.println("Workers clicked");
    }

    @FXML
    private void handleOuvrier(ActionEvent event) {
        System.out.println("Ouvrier clicked");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        System.out.println("Profile clicked");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.out.println("Logout clicked");
    }
}