package ftp.scan.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import ftp.scan.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

@Route(value = "columnMapping")
public class ColumnMappingView extends VerticalLayout {
    private List<TextField> textFields = new ArrayList<>();
    @Value("${column.mapping.key}")
    private String columnMappingKey;
    private ConfigService configService;

    public ColumnMappingView(ConfigService headerService){
        this.configService = headerService;
        Map<String, List<String>> headers = headerService.getHeaders();

        if(headers == null){
            headers = new HashMap<>();
            headers.put("Email", Collections.emptyList());
            headers.put("First name", Collections.emptyList());
            headers.put("Last name", Collections.emptyList());
        }

        setAlignItems(Alignment.CENTER);
        add(new Label("Column names mapping configuration"));
        headers.forEach((k, v) -> {
            FormLayout hl = new FormLayout();
            hl.setWidth("1200px");
            TextField aliases = new TextField();
            aliases.setValue(String.join(",", v));
            aliases.setWidth("800px");
            aliases.setPlaceholder(k);
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
            save.put(textField.getPlaceholder(), Arrays.asList(textField.getValue().split(",")));
        }

        configService.saveHeaders(save);
    }
}
