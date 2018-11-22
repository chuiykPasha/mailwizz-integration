package ftp.scan.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ftp.scan.Configuration;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Route(value = "columnMapping")
public class ColumnMapping extends VerticalLayout {
    private Configuration configuration;
    private RedissonClient redisson;
    private List<TextField> textFields = new ArrayList<>();
    @Value("${column.mapping.key}")
    private String columnMappingKey;


    public ColumnMapping(Configuration configuration, RedissonClient redisson){
        this.configuration = configuration;
        this.redisson = redisson;

        add(new Label("Column names mapping configuration"));
        configuration.getFieldToAliases().forEach((k, v) -> {
            HorizontalLayout hl = new HorizontalLayout();
            hl.setWidth("100%");
            TextField aliases = new TextField(k);
            aliases.setValue(String.join(",", v));
            aliases.setWidth("500px");
            hl.add(aliases);
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

        RBucket<Map<String, List<String>>> bucket = redisson.getBucket(columnMappingKey);
        bucket.set(save);
    }
}
