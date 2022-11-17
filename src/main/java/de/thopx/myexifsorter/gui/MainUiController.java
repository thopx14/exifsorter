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
import java.util.ResourceBundle;

public class MainUiController {

    // FXML
    @FXML
    private Button btnChooseDest;

    @FXML
    private Button btnChooseSource;

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

    private class CopyService extends Service<Void> {
        private final Parser<Path, LocalDate> parser;

        public CopyService( Parser<Path, LocalDate> parser ) {
            this.parser = parser;
        }

        @Override protected Task<Void> createTask() {
            return new Task<>() {
                @Override protected Void call() throws Exception {
                    progressBar.setVisible( true );
                    progressBar.setProgress( 0.1 );
                    lblProcessingFile.setVisible( true );
                    Map<Path, LocalDate> dateMap = parser.parse( Path.of( sourceDir ) );
                    float progressStep = (float) 1 / dateMap.entrySet().size();
                    /*
                     * Copy everything!
                     */
                    for ( Map.Entry<Path, LocalDate> entry : dateMap.entrySet() ) {

                        Path sourcePath = entry.getKey();
                        LocalDate val = entry.getValue();

                        String localMonthName = val.format( DateTimeFormatter.ofPattern( "MMMM" ) );
                        String year = val.format( DateTimeFormatter.ofPattern( "yyyy" ) );

                        // TODO: What to do with dates equal 1.1.1970??

                        Path destDir = Path.of(MainUiController.this.destDir).resolve( year ).resolve( localMonthName );
                        if( Files.notExists( destDir )) {
                            Files.createDirectories( destDir );
                        }

                        Path whereToCopy = destDir.resolve( sourcePath.getFileName() );

                        try {
                            // We need to create the destination directory first!
                            logger.info( "Copy: {} -> {}", sourcePath, whereToCopy );
                            Files.copy( sourcePath, whereToCopy, LinkOption.NOFOLLOW_LINKS );

                        } catch ( IOException e) {
                            // TODO: Maybe print something, in case we need to skip!
                            if(e instanceof FileAlreadyExistsException ) {
                                logger.info( "Skipping {}, because it already exists!", sourcePath );

                            } else {
                                throw  e;
                            }
                        } finally {
                            Platform.runLater(() -> {
                                progressBar.setProgress( progressBar.getProgress() + progressStep );
                                lblProcessingFile.setText( sourcePath.toString() );
                            });
                        }
                    }
                    return null;
                }

                @Override protected void succeeded() {
                    lblDone.setVisible( true );
                    btnCopy.setDisable( false );
                    progressBar.setProgress( 0.0 );
                    progressBar.setVisible( false );
                    lblProcessingFile.setVisible( false );
                }

                @Override protected void failed() {
                    logger.error( "Failing to copy everything!" );
                    lblFailed.setVisible( true );
                    super.failed();
                }
            };
        }
    }
}
