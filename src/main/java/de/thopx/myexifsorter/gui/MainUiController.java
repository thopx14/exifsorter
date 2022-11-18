package de.thopx.myexifsorter.gui;

import de.thopx.myexifsorter.parser.DateParser;
import de.thopx.myexifsorter.parser.Parser;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class MainUiController {

    // FXML
    @FXML
    private Button btnCopy;

    @FXML
    private TextField txtFieldDest;

    @FXML
    private TextField txtFieldSource;

    @FXML
    private Label lblDone;

    @FXML
    private Label lblFailed;

    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private Label lblProcessingFile;
    // end of FXML

    // Private class for handling copy service
    private final CopyService copyService;

    private final String unkownDateFolder = ResourceBundle.getBundle( "texts" ).getString( "folder.name.not.found" );
    private final String parsingText = ResourceBundle.getBundle( "texts" ).getString( "parsing.text" );
    private final String copyText = ResourceBundle.getBundle( "texts" ).getString( "copy.text" );

    // static
    private static final Logger logger = LogManager.getLogger();

    // non-static
    private String sourceDir;
    private String destDir;

    public MainUiController() {
        sourceDir = "";
        destDir = "";
        copyService = new CopyService( new DateParser() );
    }

    public void initialize() {
        progressBar.progressProperty().bind( copyService.progressProperty());
    }

    public void btnChooseSourceClicked() {
       chooseFolder( "Source" );
    }

    public void btnChooseDestClicked() {
        chooseFolder( "Destination" );
    }

    private void chooseFolder(String type) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle( "Bitte Zielverzeichnis wählen" );
        File file = directoryChooser.showDialog( MainUI.primaryStage );
        if ( file != null ) {

            switch ( type ) {
                case "Source" -> {
                    sourceDir = file.getAbsolutePath();
                    txtFieldSource.setText( sourceDir );
                    lblDone.setVisible( false );
                    enableButtonCopy();
                }
                case "Destination" -> {
                    destDir = file.getAbsolutePath();
                    txtFieldDest.setText( destDir );
                    lblDone.setVisible( false );
                    enableButtonCopy();
                }
            }
        }
    }

    public void btnCopyClicked() {
        lblDone.setVisible( false );
        lblFailed.setVisible( false );
        lblProcessingFile.setVisible( true );
        lblProcessingFile.setText( parsingText + " " + MainUiController.this.sourceDir + " . . ." );
            if ( !copyService.isRunning() ) {
                progressBar.setVisible( true );
                copyService.restart();
                disableButtonCopy();
            }
    }

    public void btnInfoClicked() {
        logger.debug("loading : about.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL aboutFXML = getClass().getResource( "about.fxml");
        if(aboutFXML != null) {
            fxmlLoader.setLocation(aboutFXML);
            fxmlLoader.setResources( ResourceBundle.getBundle( "texts" ) );

            try {
                Parent parent = fxmlLoader.load();
                Scene newScene = new Scene( parent );
                Stage stage = new Stage();
                stage.setScene( newScene );
                stage.setResizable( false );
                stage.getIcons().add(new Image( Objects.requireNonNull( MainUI.class.getResourceAsStream( "icons/kamera.png" ) ) ));
                stage.show();

            } catch ( IOException e ) {
                logger.error( "Unable to load about.fxml!" );
                logger.throwing( e );
            }
        } else {
            logger.error( "about.fxml file cannot be found in module - please check that this file exists!" );
        }

    }

    private void enableButtonCopy() {
        if(!sourceDir.equals( "" ) && !destDir.equals( "" )) {
            btnCopy.setDisable( false );
        }
    }

    private void disableButtonCopy() {
        btnCopy.setDisable( true );
    }

    public boolean isCopyServiceRunning() {
        return this.copyService.isRunning();
    }

    private void resetUiAfterCopying(boolean wasAllOkay) {
        btnCopy.setDisable( false );
        progressBar.setVisible( false );
        if(wasAllOkay) {
            lblDone.setVisible( true );
        } else {
            lblFailed.setVisible( true );
            logger.error( "Failing to copy, please check the logs!" );
        }
        lblProcessingFile.setText( "" );
        lblProcessingFile.setVisible( false );
    }

    private class CopyService extends Service<Void> {
        private final Parser<Path, LocalDate> parser;

        public CopyService( Parser<Path, LocalDate> parser ) {
            this.parser = parser;
        }

        @Override protected Task<Void> createTask() {
            return new Task<>() {
                @Override protected Void call() throws Exception {
                    Map<Path, LocalDate> dateMap = parser.parse( Path.of( sourceDir ) );
                    int dateMapSize = dateMap.size();
                    int cnt = 0;
                    /*
                     * Copy everything!
                     */
                    for ( Map.Entry<Path, LocalDate> entry : dateMap.entrySet() ) {

                        Path sourcePath = entry.getKey();
                        LocalDate val = entry.getValue();
                        Path whereToCopy;

                        if( val.isEqual(LocalDate.of( 1970, 1, 1 )) ) {

                            Path destDir = Path.of( MainUiController.this.destDir ).resolve( Path.of(unkownDateFolder));
                            if ( Files.notExists( destDir ) ) {
                                Files.createDirectories( destDir );
                            }

                            whereToCopy = destDir.resolve( sourcePath.getFileName() );

                        } else {

                            String localMonthName = val.format( DateTimeFormatter.ofPattern( "MMMM" ) );
                            String year = val.format( DateTimeFormatter.ofPattern( "yyyy" ) );

                            Path destDir = Path.of( MainUiController.this.destDir ).resolve( year ).resolve( localMonthName );
                            if ( Files.notExists( destDir ) ) {
                                Files.createDirectories( destDir );
                            }

                            whereToCopy = destDir.resolve( sourcePath.getFileName() );
                        }
                        logger.debug( "Count = {}, dateMapSize = {}", cnt, dateMapSize );
                        updateProgress( cnt++, dateMapSize );

                        try {
                            // We need to create the destination directory first!
                            logger.info( "Copy: {} -> {}", sourcePath, whereToCopy );
                            Files.copy( sourcePath, whereToCopy, LinkOption.NOFOLLOW_LINKS );

                        } catch ( IOException e) {
                            if(e instanceof FileAlreadyExistsException ) {
                                logger.info( "Skipping {}, because it already exists!", sourcePath );

                            } else {
                                throw  e;
                            }
                        } finally {
                            Platform.runLater(() ->
                                lblProcessingFile.setText( copyText + " " + sourcePath )
                            );
                        }
                    }
                    return null;
                }

                @Override protected void succeeded() {
                    resetUiAfterCopying(true);
                }

                @Override protected void failed() {
                    resetUiAfterCopying(false);
                    super.failed();
                }
            };
        }
    }
}
