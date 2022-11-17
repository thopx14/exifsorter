package de.thopx.myexifsorter.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

public class DateParser extends Parser<Path, LocalDate> {

    private static final Logger logger = LogManager.getLogger();

    /*
     * These static attributes are for converting java.util.Date to java.time.LocalDate.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern( "M/d/yy" ); // 'Month/DayOfMonth/Year'
    private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance( DateFormat.SHORT, Locale.US ); // '6/24/22'

    @Override
    public Map<Path, LocalDate> parse( Path p ) throws IOException {

        Map<Path, LocalDate> fileDateMap = new HashMap<>();

        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                logger.debug( "Visiting file {}", file );
                // We use dummy values here, to ensure that we copy all files!
                fileDateMap.put( file, LocalDate.of( 1970, 1, 1 ) );

                try ( InputStream in = Files.newInputStream( file ) ) {
                    Metadata metadata = ImageMetadataReader.readMetadata( in );
                    if(!metadata.hasErrors()) {
                        // obtain the Exif directory
                        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType( ExifSubIFDDirectory.class );
                         // If directory is not null, there is something found!
                        if ( directory != null ) {
                            Date date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL );
                            if(date == null) {
                                logger.debug("TAG_DATETIME_ORIGINAL == null trying different entry!");
                                // Try a different date!
                                date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED );
                            }
                            if(date == null) {
                                logger.debug("TAG_DATETIME_DIGITIZED == null trying different entry!");
                                // Try a different date!
                                date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME );
                            }
                            if ( date != null ) {
                                LocalDate parsedDate = LocalDate.parse( DATE_FORMAT.format( date ), DATE_TIME_FORMATTER );
                                logger.debug( "Parsed date {} of file {}", parsedDate, file.getFileName() );
                                // In case we found a correct date, we put it to the map and replace the existing value.
                                fileDateMap.put( file, parsedDate );
                            } else {
                                logger.warn("Could not find date for file: {}", file.getFileName());
                            }
                        }
                    }
                } catch ( ImageProcessingException e ) {
                    logger.warn("Failed to process file: {} => {}", file.getFileName(),  e );
                }

                return super.visitFile( file, attrs );
            }

            /*
             * In case we cannot open the file, skip it!
             */
            @Override
            public FileVisitResult visitFileFailed( Path file, IOException exc ) {
                logger.warn( "Skipping file {} because it cannot be read!", file );
                return FileVisitResult.CONTINUE;
            }
        };

        Files.walkFileTree( p, visitor );

        return fileDateMap;
    }

}
