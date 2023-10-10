module com.example.typer {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.typer to javafx.fxml;
    exports com.example.typer;
}