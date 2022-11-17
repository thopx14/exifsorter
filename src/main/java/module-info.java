module myexifsorter {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.apache.logging.log4j;
    requires metadata.extractor;
    requires jdk.xml.dom;
    requires java.desktop;

    exports de.thopx.myexifsorter.gui;
    exports de.thopx.myexifsorter.parser;

    opens de.thopx.myexifsorter.gui;
    opens de.thopx.myexifsorter.parser;
}