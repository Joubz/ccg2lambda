package visualization.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import visualization.Tools;

import java.io.*;
import java.util.Objects;


public class inputController {

    @FXML
    private TextField sentenceField;

    @FXML
    private ProgressBar visualizationProgressBar;

    @FXML
    private ListView<String> listSentences;

    private ObservableList<String> listSentencesItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initListView();
    }

    private void initListView() {
        listSentences.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem deleteItem = new MenuItem("Delete sentence");
            deleteItem.setOnAction(event -> listSentencesItems.remove(
                    listSentences.getSelectionModel().getSelectedIndex()
            ));

            contextMenu.getItems().add(deleteItem);

            cell.textProperty().bind(cell.itemProperty());

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });
    }

    public void addSentence() {
        if (sentenceField.getText() != null && !Objects.equals(sentenceField.getText(), "")) {
            listSentencesItems.add(sentenceField.getText());
            listSentences.setItems(listSentencesItems);
            sentenceField.setText("");
        }
    }

    public void visualize() {
        writeTxt();
        launchScript();
    }

    private void openResultsWindow() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("../view/output.fxml")));
            Stage stage = new Stage();
            stage.setTitle(Tools.windowTitleBase);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void launchScript() {
        //script
    }

    public void writeTxt() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("../sentences.txt"), "utf-8"))) {

            //Browse the list and write each items to "sentences.txt"
            for (String s : listSentencesItems) {
                writer.write(s);

                //add a dot if there isn't
                char[] sentenceChar = s.toCharArray();
                if (sentenceChar[sentenceChar.length - 1] != '.') {
                    writer.write(".");
                }

                //next line
                ((BufferedWriter) writer).newLine();


            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}