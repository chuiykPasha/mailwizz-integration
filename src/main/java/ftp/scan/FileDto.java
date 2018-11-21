package ftp.scan;

import java.io.InputStream;

public class FileDto {
    private InputStream stream;
    private String filePath;
    private String name;

    public FileDto(InputStream stream, String filePath, String name) {
        this.stream = stream;
        this.filePath = filePath;
        this.name = name;
    }

    public InputStream getStream() {
        return stream;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }
}
