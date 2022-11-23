package de.thopx.myexifsorter.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class CopyTests {

    private static Map<Path, LocalDate> fileDateMap;
    private static final String PATH_TO_TEST_PICTURES_FOLDER = "/TestOrdnerBilder";
    private static Path PATH_TO_SRC_FOLDER;

    public CopyTests() throws URISyntaxException {
        URL urlToTestFolder = getClass().getResource( PATH_TO_TEST_PICTURES_FOLDER );
        if ( urlToTestFolder != null ) {
            PATH_TO_SRC_FOLDER = Path.of( urlToTestFolder.toURI() );
        }
    }

    @BeforeEach
    void initFileDateMap() throws IOException {
        fileDateMap = new DateParser().parse( PATH_TO_SRC_FOLDER );
    }

    @Test
    void testIfDateMapContainsAllPaths() {
        List<Path> fileNamesOfSrcDir;
        try ( Stream<Path> pathStream = Files.walk( PATH_TO_SRC_FOLDER ) ) {
            fileNamesOfSrcDir = pathStream
                    .filter( Files::isRegularFile )
                    .collect( Collectors.toList() );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        assertNotNull( fileNamesOfSrcDir );
        assertTrue( fileNamesOfSrcDir.containsAll( fileDateMap.keySet() ) );

    }

    @Test
    void copySourceToDestination() {
        try {
            Path destinationDir = Files.createTempDirectory( "myExifSorter" );
            System.out.println(destinationDir);
            /*
            Copy everything!
             */
            copyToDestination(destinationDir);

            /*
            Check if files were copied correctly
             */
            List<Path> srcList;
            try ( Stream<Path> pathsSrc = Files.walk( PATH_TO_SRC_FOLDER ) ) {
                srcList = pathsSrc
                        .filter( Files::isRegularFile )
                        .map( Path::getFileName )
                        .collect( Collectors.toList() );
            }

            List<Path> destList;
            try ( Stream<Path> pathsDest = Files.walk( destinationDir ) ) {
                destList = pathsDest
                        .filter( Files::isRegularFile )
                        .map( Path::getFileName )
                        .collect( Collectors.toList() );
            }

            srcList.removeAll( destList );
            assertEquals( 0, srcList.size(), "There are items not copied: " + srcList );

            // Cleanup temp dir!
            cleanUpTempDir( destinationDir );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void copyToDestination(Path dest) throws IOException {
        for ( Map.Entry<Path, LocalDate> entry : fileDateMap.entrySet() ) {

            Path sourcePath = entry.getKey();
            LocalDate val = entry.getValue();

            String localMonthName = val.format( DateTimeFormatter.ofPattern( "MMMM" ) );
            String year = val.format( DateTimeFormatter.ofPattern( "yyyy" ) );

            Path destDir = dest.resolve( year ).resolve( localMonthName );
            if(Files.notExists( destDir )) {
                Files.createDirectories( destDir );
            }

            Path whereToCopy = destDir.resolve( sourcePath.getFileName() );
            Files.copy( sourcePath, whereToCopy, LinkOption.NOFOLLOW_LINKS );
        }
    }

    private void cleanUpTempDir(Path p) {

        SimpleFileVisitor<Path> fileVisitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
                Files.deleteIfExists( file );
                return super.visitFile( file, attrs );
            }

            @Override
            public FileVisitResult visitFileFailed( Path file, IOException exc ) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
                Files.deleteIfExists( dir );
                return super.postVisitDirectory( dir, exc );
            }
        };

        try {
            Files.walkFileTree( p, fileVisitor );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
