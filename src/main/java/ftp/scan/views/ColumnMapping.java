package ftp.scan.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ftp.scan.Configuration;
import ftp.scan.HeaderService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Route(value = "columnMapping")
public class ColumnMapping extends VerticalLayout {
    private Configuration configuration;
    private List<TextField> textFields = new ArrayList<>();
    @Value("${column.mapping.key}")
    private String columnMappingKey;
    @Autowired
    private HeaderService headerService;


    public ColumnMapping(Configuration configuration){
        this.configuration = configuration;

        setAlignItems(Alignment.CENTER);
        add(new Label("Column names mapping configuration"));
        this.configuration.getFieldToAliases().forEach((k, v) -> {
            FormLayout hl = new FormLayout();
            hl.setWidth("1200px");
            TextField aliases = new TextField();
            aliases.setValue(String.join(",", v));
            aliases.setWidth("800px");
            hl.addFormItem(aliases, k);
            textFields.add(aliases);
            add(hl);
        });

        Button button = new Button("Save");
        button.addClickListener(this::onSave);
        add(button);
    }

    private void onSave(ClickEvent<Button> buttonClickEvent) {
        Map<String, List<String>> save = new HashMap<>();

        for(TextField textField : textFields){
            save.put(textField.getLabel(), Arrays.asList(textField.getValue().split(",")));
        }

        headerService.saveHeaders(save);
    }
}
