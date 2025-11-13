package datasource;

import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileLoader implements FileLoader {
    @Override
    public String readFile(Path path) throws Exception {
        return Files.readString(path);
    }
}
