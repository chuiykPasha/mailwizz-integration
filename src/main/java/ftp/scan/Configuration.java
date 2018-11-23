package ftp.scan;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Configuration {
    private HeaderService headerService;
    private Map<String, List<String>> fieldToAliases = new HashMap<>();

    public Configuration(HeaderService headerService){
        this.headerService = headerService;
    }

    public Map<String, List<String>> getFieldToAliases() {
        setFieldToAliases(this.headerService.getHeaders());
        return fieldToAliases;
    }

    public void setFieldToAliases(Map<String, List<String>> fieldToAliases) {
        if(fieldToAliases == null){     //returns default list for first startup
            fieldToAliases = new HashMap<>();
            fieldToAliases.put("Email", Collections.emptyList());
            fieldToAliases.put("First name", Collections.emptyList());
            fieldToAliases.put("Last name", Collections.emptyList());
        }
        this.fieldToAliases = fieldToAliases;
    }
}
