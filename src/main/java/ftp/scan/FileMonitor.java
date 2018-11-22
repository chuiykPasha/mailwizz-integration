package ftp.scan;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.net.ftp.*;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class FileMonitor {
    private Logger logger = LoggerFactory.getLogger(FileMonitor.class);
    private FTPClient ftpClient = new FTPClient();
    private final int COUNT_THREADS = 2;
    private final String ROOT_PATH = "/ftp";
    private final String PROCESSING_PATH = "/ftp/PROCESSING";
    private final String PROCESSED_PATH = "/ftp/PROCESSED";
    private final String FAILED_PATH = "/ftp/FAILED";
    private RedissonClient redisson;
    private ExecutorService executor;
    @Value("${ftp.connection}")
    private String connectionFTP;
    @Value("${ftp.user.name}")
    private String ftpUserName;
    @Value("${ftp.user.password}")
    private String ftpUserPassword;

    public FileMonitor(@Value("${redis.connection}") String connection) {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(connection);
        redisson = Redisson.create(config);
    }

    @Scheduled(fixedDelayString = "${intervalForScan.in.milliseconds}")
    public void pollFiles() throws InterruptedException {
        if(!ftpClient.isConnected()) {
            try {
                if(!connectToFTP()){
                    return;
                }
            } catch (IOException e) {
                logger.error("Can't connect to ftp server", e);
                return;
            }
        }

        executor = Executors.newFixedThreadPool(COUNT_THREADS);
        String fileName = null;
        fileName = getFirstFile();

        if(fileName == null){
            logger.info("Can't find file");
            return;
        }

        logger.info("Find file " + fileName);
        logger.info(fileName + " change directory to " + PROCESSING_PATH);
        if(!changeFileDirectory(fileName, PROCESSING_PATH + "/" + fileName)){
            return;
        }

        if(!changeDirectory(PROCESSING_PATH)){
            return;
        }

        if(!processingFile(retrieveFile(fileName))){
            logger.info(fileName + " change directory to " + FAILED_PATH);
            changeFileDirectory(fileName, FAILED_PATH + "/" + fileName);
            return;
        }

        executor.shutdown();

        while (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
            logger.info("Awaiting completion of threads.");
        }

        logger.info("File " + fileName + " csv reader complete");
        logger.info(fileName + " change directory to " + PROCESSED_PATH);
        changeFileDirectory(fileName, PROCESSED_PATH + "/" + fileName);
    }

    private boolean connectToFTP() throws IOException {
        ftpClient.configure(new FTPClientConfig(FTPClientConfig.SYST_UNIX));
        ftpClient.connect(connectionFTP);
        ftpClient.login(ftpUserName, ftpUserPassword);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

        int reply = ftpClient.getReplyCode();

        if(!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            System.err.println("FTP server refused connection.");
            return false;
        }

        return true;
    }

    private boolean changeDirectory(String path){
        try {
            ftpClient.changeWorkingDirectory(path);
        } catch (IOException e) {
            logger.error("Can't change directory to " + path);
            return false;
        }

        return true;
    }

    private boolean changeFileDirectory(String from, String to){
        try {
            ftpClient.rename(from, to);
        } catch (IOException e) {
            logger.error("Can't change file directory");
            return false;
        }

        return true;
    }

    private boolean processingFile(InputStream file) {
        if(file == null){
            return false;
        }

        final int NOT_EXISTS = 0;
        Reader in = new InputStreamReader(file);
        Iterable<CSVRecord> records;

        try {
            records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
        } catch (IOException e) {
            logger.error("Can't parse file", e);
            return false;
        }

        for (CSVRecord record : records) {
            try {
                if (redisson.getKeys().isExists(record.get("email")) == NOT_EXISTS) {
                    executor.submit(() -> System.out.println(record.get("fname") + "," + record.get("lname") + "," + record.get("email")));
                    RBucket<Boolean> bucket = redisson.getBucket(record.get("email"));
                    bucket.set(Boolean.TRUE);
                } else {
                    logger.info("Email:" + record.get("email") + " already exists. Don't call api");
                }
            }catch (Exception e){
                logger.error("Failed parse string", e);
            }
        }

        return true;
    }

    private String getFirstFile() {
        logger.info("Move to root directory");
        if(!changeDirectory(ROOT_PATH)){
            return null;
        }

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
