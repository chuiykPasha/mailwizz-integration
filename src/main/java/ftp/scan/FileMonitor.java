package ftp.scan;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    @Autowired
    private RedissonClient redisson;
    private ExecutorService executor;
    @Value("${ftp.connection}")
    private String connectionFTP;
    @Value("${ftp.user.name}")
    private String ftpUserName;
    @Value("${ftp.user.password}")
    private String ftpUserPassword;
    @Autowired
    private ConfigService configService;
    private String fileName;

    @Scheduled(fixedDelayString = "${intervalForScan.in.milliseconds}")
    public void pollFiles() {
        try {
            if (!ftpClient.isConnected()) {
                try {
                    if (!connectToFTP()) {
                        return;
                    }
                } catch (IOException e) {
                    logger.error("Can't connect to ftp server", e);
                    return;
                }
            }

            executor = Executors.newFixedThreadPool(COUNT_THREADS);
            fileName = null;
            fileName = getFirstFile();

            if (fileName == null) {
                logger.info("Can't find file");
                return;
            }

            logger.info("Find file " + fileName);
            logger.info(fileName + " change directory to " + PROCESSING_PATH);
            ftpClient.rename(fileName, PROCESSING_PATH + "/" + fileName);

            ftpClient.changeWorkingDirectory(PROCESSING_PATH);

            processingFile(retrieveFile(fileName));

            executor.shutdown();

            while (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.info("Awaiting completion of threads.");
            }

            logger.info("File " + fileName + " csv reader complete");
            logger.info(fileName + " change directory to " + PROCESSED_PATH);
            ftpClient.rename(fileName, PROCESSED_PATH + "/" + fileName);
        } catch (Exception e) {
            logger.error("Can't process file", e);
            try {
                logger.info(fileName + " change directory to " + FAILED_PATH);
                ftpClient.rename(fileName, FAILED_PATH + "/" + fileName);
            } catch (IOException exc) {
                logger.error("Can't transfer file to " + FAILED_PATH, exc);
            }
        }
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

    private void processingFile(InputStream file) throws Exception {
        String emailAlias;
        String fNameAlias;
        String sNameAlias;
        Set<String> csvHeader;

        if(file == null){
            return;
        }

        final int NOT_EXISTS = 0;
        Reader in = new InputStreamReader(file);
        CSVParser records;

        records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);

        csvHeader = records.getHeaderMap().keySet();

        emailAlias = getEqualsColumn(csvHeader, configService.getHeaders().get("Email"));
        fNameAlias = getEqualsColumn(csvHeader, configService.getHeaders().get("First name"));
        sNameAlias = getEqualsColumn(csvHeader, configService.getHeaders().get("Last name"));

        if(emailAlias == null || fNameAlias == null || sNameAlias == null){
            throw new Exception("Wrong header, can't get values");
        }

        for (CSVRecord record : records) {
            if (redisson.getKeys().isExists(record.get(emailAlias)) == NOT_EXISTS) {
                executor.submit(() -> System.out.println(record.get(fNameAlias) + "," + record.get(sNameAlias) + "," + record.get(emailAlias)));
                RBucket<Boolean> bucket = redisson.getBucket(record.get(emailAlias));
                bucket.set(Boolean.TRUE);
            } else {
                logger.info("Email:" + record.get(emailAlias) + " already exists. Don't call api");
            }
        }
    }

    private String getEqualsColumn(Set<String> header, List<String> aliases){
        for(String alias : aliases){
            if(header.contains(alias)){
                return alias;
            }
        }

        return null;
    }

    private String getFirstFile() throws IOException {
        logger.info("Move to root directory");
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

    private InputStream retrieveFile(String name) throws IOException {
        InputStream inputStream = null;

        inputStream = ftpClient.retrieveFileStream(name);

        if (ftpClient.completePendingCommand()) {
            logger.info("Retrive file complete");
        }

        return inputStream;
    }
}
