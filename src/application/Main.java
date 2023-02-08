package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
 
public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("GUI.fxml"));
        
        stage.setMaximized(true);
        stage.setTitle("JFXOpenStreetMap Example");

        
        Scene scene = new Scene(root, stage.getWidth(), stage.getHeight());
        
        stage.setScene(scene);
        
        stage.show();
        
    }

}