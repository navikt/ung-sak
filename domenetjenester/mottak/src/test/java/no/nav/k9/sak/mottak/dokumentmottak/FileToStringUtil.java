package no.nav.k9.sak.mottak.dokumentmottak;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileToStringUtil {
    private FileToStringUtil() {
    }

    public static String readFile(String filename) throws URISyntaxException, IOException {
        Path path = Paths.get(FileToStringUtil.class.getClassLoader().getResource(filename).toURI());
        return Files.readString(path);
    }
}
