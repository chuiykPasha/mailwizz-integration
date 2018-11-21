package ftp.scan;

import ch.qos.logback.core.util.FileUtil;
import com.opencsv.CSVReader;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FileMonitor {
    private Logger logger = LoggerFactory.getLogger(FileMonitor.class);
    private FTPClient ftpClient = new FTPClient();
    private final int COUNT_THREADS = 20;
    private final String ROOT_PATH = "/ftp";
    private final String PROCESSING_PATH = "/ftp/PROCESSING";
    private final String PROCESSED_PATH = "/ftp/PROCESSED";

    public FileMonitor() throws IOException {
        ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
        ftpClient.connect("192.168.0.101");
        ftpClient.login("test", "user");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    @Scheduled(fixedDelayString = "${intervalForScan.in.milliseconds}")
    public void pollFiles() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(COUNT_THREADS);

        FileDto file = null;
        try {
            file = getFirstFile();
        } catch (IOException e) {
            logger.error("Can't download file");
        }

        if(file == null){
            System.out.println(ftpClient.printWorkingDirectory());
            return;
        }

        CSVReader reader = new CSVReader(new InputStreamReader(file.getStream()));
        String[] nextRecord;
        while ((nextRecord = reader.readNext()) != null) {
            for(String line : nextRecord){
                System.out.println(line);
            }
        }

        System.out.println(ftpClient.getReplyString());
        System.out.println(ftpClient.printWorkingDirectory());
        ftpClient.changeWorkingDirectory(PROCESSING_PATH);
        System.out.println(ftpClient.printWorkingDirectory());
        ftpClient.rename(file.getFilePath(), PROCESSED_PATH + "/" + file.getName());

    }

    private FileDto getFirstFile() throws IOException {
        FTPFile[] files;
        ftpClient.changeWorkingDirectory(ROOT_PATH);

        try {
            files = ftpClient.listFiles("");
        } catch (IOException e) {
            logger.error("Wrong path", e);
            return null;
        }

        for(FTPFile file : files){
            if(file.isFile()){
                String filePath = PROCESSING_PATH + "/" + file.getName();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ftpClient.retrieveFile(file.getName(), outputStream);
                InputStream inputstream = new ByteArrayInputStream(outputStream.toByteArray());
                ftpClient.rename(file.getName(), PROCESSING_PATH + "/" + file.getName());
                return new FileDto(inputstream, filePath, file.getName());
            }
        }

        return null;
    }
}
