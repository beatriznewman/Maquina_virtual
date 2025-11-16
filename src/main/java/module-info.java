module com.example.virtualmachine {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.virtualmachine to javafx.base, javafx.fxml;
    opens virtualMachine to javafx.base;  // Adicione esta linha
    exports com.example.virtualmachine;
}
