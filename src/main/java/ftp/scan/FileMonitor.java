package ftp.scan;

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
        ftpClient.connect("192.168.0.102");
        ftpClient.login("test", "user");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    @Scheduled(fixedDelayString = "${intervalForScan.in.milliseconds}")
    public void pollFiles() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(COUNT_THREADS);

        String fileName = null;
        try {
            fileName = getFirstFile();
        } catch (IOException e) {
            logger.error("Error get file", e);
        }

        if(fileName == null){
            logger.info("Can't find fileName");
            return;
        }

        logger.info("Get file " + fileName);
        ftpClient.rename(fileName, PROCESSING_PATH + "/" + fileName);
        logger.info(fileName + " change directory to " + PROCESSING_PATH);

        ftpClient.changeWorkingDirectory(PROCESSING_PATH);
        CSVReader reader = new CSVReader(new InputStreamReader(retrieveFile(fileName)));

        String[] nextRecord;
        while ((nextRecord = reader.readNext()) != null) {
            for(String line : nextRecord){
                System.out.println(line);
            }
        }

        logger.info("File " + fileName + " csv reader complete");
        ftpClient.rename(fileName, PROCESSED_PATH + "/" + fileName);
        logger.info(fileName + " change directory to " + PROCESSED_PATH);
    }

    private String getFirstFile() throws IOException {
        ftpClient.changeWorkingDirectory(ROOT_PATH);
        FTPFile[] files;

        try {
            files = ftpClient.listFiles("");
        } catch (IOException e) {
            logger.error("Wrong current directory", e);
            return null;
        }

        for(FTPFile file : files){
            if(file.isFile()){
                return file.getName();
            }
        }

        return null;
    }

    private InputStream retrieveFile(String name){
        InputStream inputStream = null;

        try {
            inputStream = ftpClient.retrieveFileStream(name);

            if(ftpClient.completePendingCommand()){
                logger.info("Retrive file complete");
            }
        } catch (IOException e) {
            logger.error("Can't retrive file " + name, e);
        }

        return inputStream;
    }
}
