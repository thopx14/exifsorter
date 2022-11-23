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
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final Map<Path, LocalDate> fileDateMap = new HashMap<>();

    @Override
    public Map<Path, LocalDate> parse( Path p ) throws IOException {
        SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                logger.debug( "Visiting file {}", file );
                // We use dummy values here, to ensure that we copy all files!
                putFileToMap( file, LocalDate.of( 1970, 1, 1 ) );
                Date date = null;

                try ( InputStream in = Files.newInputStream( file ) ) {
                    Metadata metadata = ImageMetadataReader.readMetadata( in );
                    if ( !metadata.hasErrors() ) {
                        // obtain the Exif directory
                        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType( ExifSubIFDDirectory.class );
                        // If directory is not null, there is something found!
                        if ( directory != null ) {
                            date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL );
                            if ( date == null ) {
                                logger.debug( "TAG_DATETIME_ORIGINAL == null trying different entry!" );
                                // Try a different date!
                                date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED );
                            }
                            if ( date == null ) {
                                logger.debug( "TAG_DATETIME_DIGITIZED == null trying different entry!" );
                                // Try a different date!
                                date = directory.getDate( ExifSubIFDDirectory.TAG_DATETIME );
                            }

                        } else {
                            putFileToMap( file, parseDateFromFileName( file ) );
                        }
                    } else {
                        putFileToMap( file, parseDateFromFileName( file ) );
                        logger.warn( "There are errors reading the file {}. Maybe it's not an image file?", file );
                    }

                    if ( date != null ) {
                        LocalDate parsedDate = LocalDate.parse( DATE_FORMAT.format( date ), DATE_TIME_FORMATTER );
                        logger.debug( "Parsed date {} of file {}", parsedDate, file.getFileName() );
                        // In case we found a correct date, we put it to the map and replace the existing value.
                        putFileToMap( file, parsedDate );

                    } else {
                        logger.warn( "Could not find date for file: {}", file.getFileName() );
                    }

                } catch ( ImageProcessingException e ) {
                    logger.warn( "Failed to process file: {} => {}", file.getFileName(), e );
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

    private void putFileToMap( Path file, LocalDate parsedDate ) {
        fileDateMap.put( file, parsedDate );
    }

    private LocalDate parseDateFromFileName( Path file ) {
         /*
            Trying to determine the date by filename. An example could look like this:
            IMG_20200929_202519.jpg
        */
        String fileString = file.getFileName().toString();
        // Setting dummy value here, in case we cannot find any date!
        LocalDate parsedDate = LocalDate.of( 1970, 1, 1 );
        logger.debug( "Try to determine the date based on the filename: {}", fileString );
        Pattern pattern = Pattern.compile( "\\d{8}" );
        Matcher matcher = pattern.matcher( fileString );
        while ( matcher.find() ) {
            try {
                LocalDate tempDate = LocalDate.parse( matcher.group(), DateTimeFormatter.ofPattern( "uuuuMMdd" ) );
                logger.debug( "Parsed date: {}", tempDate );
                parsedDate = tempDate;
                // We found something. Stop here!
                break;

            } catch ( DateTimeParseException e ) {
                logger.debug( "Could not extract date from String: {}", e.getParsedString() );
            }
        }

        return parsedDate;
    }

}
