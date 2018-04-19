package visualization.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class BoxController implements Parametrable<String> {
    @FXML
    public VBox contentContainer;
    @FXML
    private TitledPane box;

    private static final Pattern boxTokenMatcher = Pattern.compile("_\\w*\\(\\w\\)");

    private String formula;
    private String header;
    private List<String> tokens = new ArrayList<>();

    public void initData(String formula) {
        this.formula = formula;
        box.setText(formula);
        parseFormula();

        for (String token : tokens) {
            if (token.length() > 1)
                token = token.substring(1);
            Text display = new Text(token);
            contentContainer.getChildren().add(display);
        }
    }

    private List<String> parseFormula() {
        Scanner sc = new Scanner(formula);
        sc.useDelimiter("&");
        do {
            String token;
            token = sc.findInLine(boxTokenMatcher);
            if (token != null)
                tokens.add(token);
            sc.next();
        } while (sc.hasNext());
        return tokens;
    }

    private String createHeader() {
        Pattern variableName = Pattern.compile("\\(\\w+\\)");
        List<String> used = new ArrayList<>();
        for (String token : tokens) {
        }
        return header;
    }
}