package datasource;

import java.nio.file.Path;

public interface FileLoader {
    String readFile(Path path) throws Exception;
}
