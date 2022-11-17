package de.thopx.myexifsorter.gui;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutController {

    @FXML
    private WebView webview;

    /**
     * This method will be called when all FXML elements are initialized. So it prevents NullPointerExceptions.
     */
    public void initialize() {
        String content = """
                <!DOCTYPE html>
                                       <html lang="en">
                                         <head>
                                           <title>About</title>
                                         </head>
                                         <body>
                                           <p style="font-size: 1.5rem"><b>Libraries:</b></p>
                                           <ul style="padding-inline-start: 20px; list-style: none">
                                             <li
                                               style="
                                                 padding-left: 10px;
                                                 border-left: 5px solid transparent;
                                                 border-color: #9eeeb3;
                                                 border-radius: 2px;
                                               "
                                             >
                                               <p>metadata-extractor by Drew Noakes:</p>
                                               <a href="https://github.com/drewnoakes/metadata-extractor"
                                                 >https://github.com/drewnoakes/metadata-extractor</a
                                               >
                                             </li>
                                             <li
                                               style="
                                                 padding-left: 10px;
                                                 border-left: 5px solid transparent;
                                                 border-color: #9eeeb3;
                                                 border-radius: 2px;
                                               "
                                             >
                                               <p>Apache Log4j 2:</p>
                                               <a href="https://logging.apache.org/log4j/2.x/"
                                                 >https://logging.apache.org/log4j/2.x/</a
                                               >
                                             </li>
                                           </ul>
                                           <p style="font-size: 1.5rem"><b>Icons:</b></p>
                                           <ul style="padding-inline-start: 20px; list-style: none">
                                               <li
                                                 style="
                                                   padding-left: 10px;
                                                   border-left: 5px solid transparent;
                                                   border-color: #9eeeb3;
                                                   border-radius: 2px;
                                                 "
                                               >
                                                 <p>About icon created by Ilham Fitrotul Hayat - Flaticon:</p>
                                                 <a href="https://www.flaticon.com/free-icons/about"
                                                   >https://www.flaticon.com/free-icons/about</a
                                                 >
                                               </li>
                                               <li
                                                 style="
                                                   padding-left: 10px;
                                                   border-left: 5px solid transparent;
                                                   border-color: #9eeeb3;
                                                   border-radius: 2px;
                                                 "
                                               >
                                                 <p>Exif icon created by Iconshop - Flaticon:</p>
                                                 <a href="https://www.flaticon.com/free-icons/exif"
                                                   >https://www.flaticon.com/free-icons/exif</a
                                                 >
                                               </li>
                                               <li
                                                 style="
                                                   padding-left: 10px;
                                                   border-left: 5px solid transparent;
                                                   border-color: #9eeeb3;
                                                   border-radius: 2px;
                                                 "
                                               >
                                                 <p>Copy icon created by Catalin Fertu - Flaticon:</p>
                                                 <a href="https://www.flaticon.com/free-icons/copy"
                                                   >https://www.flaticon.com/free-icons/copy</a
                                                 >
                                               </li>
                                             </ul>
                                         </body>
                                       </html>
                        """;

        /*
        The below code prevents WebEngine from loading URLs in the WebEnginge instance, but opens the
        default browser and open it there. Special thanks to:
        https://stackoverflow.com/questions/15555510/javafx-stop-opening-url-in-webview-open-in-browser-instead
         */
        final WebEngine engine = webview.getEngine();
        engine.getLoadWorker().stateProperty().addListener( ( observable, oldValue, newValue ) -> {
            if ( newValue == Worker.State.SUCCEEDED ) {
                Document doc = engine.getDocument();
                Element el = doc.getDocumentElement();
                NodeList nodeList = el.getElementsByTagName( "a" );
                for ( int i = 0; i < nodeList.getLength(); i++ ) {
                    Node node = nodeList.item( i );
                    EventTarget eventTarget = ( EventTarget ) node;
                    eventTarget.addEventListener( "click", evt -> {
                        EventTarget target = evt.getCurrentTarget();
                        HTMLAnchorElement anchorElement = ( HTMLAnchorElement ) target;
                        String href = anchorElement.getHref();
                        //handle opening URL outside JavaFX WebView
                        System.out.println( href );
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.browse( new URI( href ) );
                        } catch ( IOException | URISyntaxException e ) {
                            throw new RuntimeException( e );
                        }
                        evt.preventDefault();
                    }, false );
                }
            }
        } );
        engine.loadContent( content );
    }
}

