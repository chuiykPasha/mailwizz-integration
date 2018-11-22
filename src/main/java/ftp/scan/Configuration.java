package ftp.scan;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Configuration {
    private HeaderService headerService;
    private Map<String, List<String>> fieldToAliases = new HashMap<>();

    public Configuration(HeaderService headerService){
        this.headerService = headerService;
        setFieldToAliases(this.headerService.getHeaders());
    }

    public Map<String, List<String>> getFieldToAliases() {
        return fieldToAliases;
    }

    public void setFieldToAliases(Map<String, List<String>> fieldToAliases) {
        this.fieldToAliases = fieldToAliases;
    }
}
