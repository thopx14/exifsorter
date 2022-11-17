package de.thopx.myexifsorter.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.ResourceBundle;


public class MainUI extends Application {

    private static final Logger logger = LogManager.getLogger();
    public static Stage primaryStage = null;

    @Override public void start( Stage stage ) throws Exception {
        logger.debug("loading : mainui.fxml");
        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation( getClass().getResource( "mainui.fxml"));
        fxmlLoader.setResources( ResourceBundle.getBundle( "texts" ) );

        Parent parent = fxmlLoader.load();
        Scene newScene = new Scene( parent );
        stage.setScene( newScene );
        stage.setResizable( false );
        primaryStage = stage;
        primaryStage.getIcons().add(new Image( Objects.requireNonNull( MainUI.class.getResourceAsStream( "icons/kamera.png" ) ) ));
        Platform.setImplicitExit( false );
        primaryStage.setOnCloseRequest( event -> {
            MainUiController controller = fxmlLoader.getController();
            if(controller.isCopyServiceRunning()) {
                event.consume();
            }
            else {
                Platform.exit();
            }
        } );
        stage.show();
    }

    public static void main( String[] args ) {
        launch( args );
    }
}
