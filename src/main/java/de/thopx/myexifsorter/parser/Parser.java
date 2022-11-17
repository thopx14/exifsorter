package de.thopx.myexifsorter.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * This class is a generic Parser, that should be subclassed.
 *
 * @author thopX14
 */
public abstract class Parser<T, U> {
    public abstract Map<T, U> parse( Path p ) throws IOException;
}
