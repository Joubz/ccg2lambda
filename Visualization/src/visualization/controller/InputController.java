package visualization.controller;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import visualization.Main;
import visualization.utils.Tools;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Controller for the input view.
 *
 * @author Nathan Joubert
 * @author Thomas Guesdon
 * @author Gaétan Basile
 * @see visualization.view
 */
public class InputController implements Stageable {
    /**
     * Button for adding a sentence to the list.
     */
    @FXML
    public Button addSentenceButton;
    /**
     * Button for starting the processing of the sentences in the list.
     */
    @FXML
    public Button startProcessingButton;
    /**
     * MenuItem for setting the location of ccg2lambda.
     */
    @FXML
    public MenuItem setccg2lambdaLocationItem;
    /**
     * MenuItem for showing information about the software.
     */
    @FXML
    public Menu menuTemplate;
    /**
     * Menu for choosing the template
     */
    @FXML
    public RadioMenuItem radioTemplateEvent;
    /**
     * Radio menu template for the template event
     */
    @FXML
    public RadioMenuItem radioTemplateClassic;
    /**
     * Radio menu template for the template classic
     */
    @FXML
    public MenuItem showInformationItem;
    /**
     * MenuItem for showing the readme for ccg2lambda (on the web)
     */
    @FXML
    public MenuItem showReadMeItem;
    /**
     * TextField enabling the input of sentences from the user.
     */
    @FXML
    private TextField sentenceField;
    /**
     * Progress bar indicating the completion status of the task.
     */
    @FXML
    private ProgressBar visualizationProgressBar;
    /**
     * Progress of the conversion process.
     */
    private SimpleDoubleProperty progress = new SimpleDoubleProperty(0.0);
    /**
     * View of all the sentences the user has input.
     */
    @FXML
    private ListView<String> listSentences;
    /**
     * List of all the sentences.
     */
    private ObservableList<String> listSentencesItems = FXCollections.observableArrayList();

    /**
     * Enables knowing if the host OS is windows.
     */
    private final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    /**
     * The view this controller manages.
     */
    private Stage view;

    /**
     * For first time program is launch, install the virtual environment for python
     */
    private boolean firstTime;

    /**
     * Initializes the view.
     */
    @FXML
    public void initialize() {
        try {
            checkConfigAndInitializeEnvironment();
            initListView();
            initMenuTemplate();
            visualizationProgressBar.progressProperty().bindBidirectional(progress);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * initialize the template selection
     */
    private void initMenuTemplate() {
        radioTemplateClassic.setSelected(true);
        radioTemplateEvent.setSelected(false);
    }


    /**
     * Initializes the context menus for each listView item.
     */
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


    /**
     * Adds the sentence in the textField to the list.
     */
    public void addSentence() {
        if (sentenceField.getText() != null && !Objects.equals(sentenceField.getText(), "")) {
            listSentencesItems.add(sentenceField.getText());
            listSentences.setItems(listSentencesItems);
            sentenceField.setText("");
        }
    }

    /**
     * Opens a window for consulting results.
     */
    private void openResultsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/output.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle(Tools.windowTitleBase);
            stage.setScene(new Scene(root));
            Stageable controller = loader.getController();
            stage.setMinWidth(Tools.windowSize[0].doubleValue());
            stage.setMinHeight(Tools.windowSize[1].doubleValue());
            stage.show();
            controller.initStage(stage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches processing of the sentences in the listView.
     */
    public void visualize() {
        writeTxt();
        progress.set(0.25);
        if (!isWindows) {
            launchScript();
            switch (Main.selectedTemplateType) {
                case CLASSIC:
                    Main.xmlSemanticsFile = new File("../parsed/sentences.txt.sem.xml");
                    break;
                case EVENT:
                    Main.xmlSemanticsFile = new File("../parsed/sentences.txt.sem.xml");
                    break;
            }
            openResultsWindow();
        } else {
            progress.set(0.0);
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setContentText("Windows isn't available yet");
            a.showAndWait();
        }
    }

    /**
     * Writes the sentences to the sentences.txt file.
     */
    private void writeTxt() {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Launches the python scripts, using ccg2lambda.
     */
    private void launchScript() {
        //script
        System.out.println(System.getProperty("os.name"));

        System.out.println("  template type : " + Main.selectedTemplateType);

        String ccg2lambdaPath = Main.ccg2lambdaLocation.getAbsolutePath();
        Process process;

        File parsedDirectory = new File("../parsed");
        File resultDirectory = new File("../results");

        if (Main.selectedTemplateType == Tools.TemplateType.CLASSIC) {
            try {
                if (parsedDirectory.exists() && resultDirectory.exists()) {
                    /**
                     * Check if their is file in the directory, if yes, delete them
                     */
                    try {
                        for (File file : parsedDirectory.listFiles()) {
                            Files.deleteIfExists(file.toPath());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        for (File file : resultDirectory.listFiles()) {
                            Files.deleteIfExists(file.toPath());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /**
                     * If the file already exist, delete them
                     */
                    Files.deleteIfExists(parsedDirectory.toPath());
                    Files.deleteIfExists(resultDirectory.toPath());
                }

                System.out.println("python parser classic script");
                process = new ProcessBuilder("./src/visualization/scripts/scriptParserClassic_EMNLP2015.sh", ccg2lambdaPath).start();
                progress.set(1.00);
                process.waitFor();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } else if (Main.selectedTemplateType == Tools.TemplateType.EVENT) {
            try {
                if (parsedDirectory.exists() && resultDirectory.exists()) {
                    /**
                     * Check if their is file in the directory, if yes, delete them
                     */
                    try {
                        for (File file : parsedDirectory.listFiles()) {
                            Files.deleteIfExists(file.toPath());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        for (File file : resultDirectory.listFiles()) {
                            Files.deleteIfExists(file.toPath());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    /**
                     * If the file already exist, delete them
                     */
                    Files.deleteIfExists(parsedDirectory.toPath());
                    Files.deleteIfExists(resultDirectory.toPath());
                }


                System.out.println("python parser event script");
                process = new ProcessBuilder("./src/visualization/scripts/scriptParserEvent.sh", ccg2lambdaPath).start();
                progress.set(1.00);
                process.waitFor();

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if the user already got the python virtual environment, if not, the software install it
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void checkConfigAndInitializeEnvironment() throws IOException, InterruptedException {
        File py3Directory = new File("py3");
        firstTime = (!py3Directory.exists() && !py3Directory.isDirectory()) || (!Tools.configFile.exists() || (!Tools.configCandC.exists()) || (!Tools.configEasyCCG.exists()));
        Process process;
        if (firstTime) {
            //boolean ok = Tools.configFile.mkdirs();
            System.out.println("------------------------First Time ----------------------------");


            /**
             * If the file already exist, delete them
             */
            File ConfigDirectory = new File("config");
            try {
                for (File file : ConfigDirectory.listFiles()) {
                    Files.deleteIfExists(file.toPath());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Files.deleteIfExists(Tools.configFile.toPath());
            Files.deleteIfExists(Tools.configCandC.toPath());
            Files.deleteIfExists(Tools.configEasyCCG.toPath());


            System.out.println("ccg2lambda location");
            new File(ConfigDirectory.toPath().toString()).mkdir();
            boolean ok = true;
            try {
                ok = Tools.configFile.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!ok)
                throw new IOException();

            Alert firstTimeAlertccg2lambda = new Alert(Alert.AlertType.WARNING);
            firstTimeAlertccg2lambda.setTitle("First time configuration needed");
            firstTimeAlertccg2lambda.setHeaderText("First time configuration ccglambda");
            firstTimeAlertccg2lambda.setContentText(
                    "Configuration file is missing and/or corrupted." + '\n' +
                            "Please redo the configuration."
            );
            firstTimeAlertccg2lambda.showAndWait();
            File f1 = setccg2lambdaLocation();

            FileWriter fw = new FileWriter(Tools.configFile);

            fw.write(f1.getAbsolutePath());
            fw.close();


            System.out.println("C&C location");
            boolean okCandC = true;
            try {
                okCandC = Tools.configCandC.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!okCandC)
                throw new IOException();

            Alert firstTimeAlertCandC = new Alert(Alert.AlertType.WARNING);
            firstTimeAlertCandC.setTitle("First time configuration needed");
            firstTimeAlertCandC.setHeaderText("First time configuration C&C");
            firstTimeAlertCandC.setContentText(
                    "Configuration file is missing and/or corrupted." + '\n' +
                            "Please redo the configuration."
            );
            firstTimeAlertCandC.showAndWait();
            File f2 = setCandCLocation();

            FileWriter fwCandC = new FileWriter(Tools.configCandC);

            fwCandC.write(f2.getAbsolutePath());
            fwCandC.close();


            System.out.println("easyCCG location");
            boolean okEasyCCG = true;
            try {
                okEasyCCG = Tools.configEasyCCG.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!okEasyCCG)
                throw new IOException();

            Alert firstTimeAlertEasyCCG = new Alert(Alert.AlertType.WARNING);
            firstTimeAlertEasyCCG.setTitle("First time configuration needed");
            firstTimeAlertEasyCCG.setHeaderText("First time configuration easyCCG ");
            firstTimeAlertEasyCCG.setContentText(
                    "Configuration file is missing and/or corrupted." + '\n' +
                            "Please redo the configuration."
            );
            firstTimeAlertEasyCCG.showAndWait();
            File f3 = setEasyCCGLocation();

            FileWriter fwEasyCCG = new FileWriter(Tools.configEasyCCG);

            fwEasyCCG.write(f3.getAbsolutePath());
            fwEasyCCG.close();


            System.out.println("python virtual");
            process = new ProcessBuilder("./src/visualization/scripts/pythonVirtual.sh").start();
            process.waitFor();
            Alert py3InstallEnded = new Alert(Alert.AlertType.CONFIRMATION);
            py3InstallEnded.setTitle("First time configuration success");
            py3InstallEnded.setContentText(
                    "Configuration is now complete." + '\n' +
                            "ccg2lambda location registered & python 3 virtual environment installed in :" + '\n' +
                            py3Directory.getAbsolutePath());
            firstTime = false;

            System.out.println("------------------------First Time END ----------------------------");
        } else {
            BufferedReader br = new BufferedReader(new FileReader(Tools.configFile));
            String ccg2lambdaPath = br.readLine();
            Main.ccg2lambdaLocation = ccg2lambdaPath != null ? new File(ccg2lambdaPath) : null;
        }
    }


    /**
     * Fires when the return key is pressed.
     *
     * @param ae the event triggered by this action
     */
    public void enterPressed(ActionEvent ae) {
        addSentence();
    }

    /**
     * See the information about the software
     */
    public void displayInformation() {
        Alert popupInfo = new Alert(Alert.AlertType.INFORMATION);
        popupInfo.setTitle("About");
        popupInfo.setHeaderText("About this software ");
        popupInfo.setContentText("This software has been created by Gaétan BASILE, Thomas GUESDON and Nathan JOUBERT for the Bekki Lab at the Ochanomizu University" + "\n"
                + "Using ccg2lambda created by Pascual MARTINEZ-GOMEZ, Koji MINESHIMA, Yusuke MIYAO and Daisuke BEKKI, " + "\n"
                + "a tool to derive formal semantic representations of natural language sentences given CCG derivation trees and semantic templates.");
        popupInfo.getDialogPane().setMinWidth(1000);
        popupInfo.getDialogPane().setMinHeight(100);
        popupInfo.showAndWait();
    }

    /**
     * Redirect to the displayReadme
     */
    public void displayReadme() {
        String url = "https://github.com/BlackNat5937/ccg2lambda/tree/master/Visualization#ccg2lambda-visualize--composing-semantic-representations-guided-by-ccg-derivations";
        Main.openLink(url);
    }

    /**
     * for choosing the ccg2lambda location file
     *
     * @return
     */
    @FXML
    public File setccg2lambdaLocation() {
        DirectoryChooser locationChooser = new DirectoryChooser();
        locationChooser.setTitle("select ccg2lambda installation directory");
        File selected = null;
        while (selected == null)
            selected = locationChooser.showDialog(view);
        if (selected.isDirectory()) {
            if (selected.canRead() && selected.canExecute() && selected.canWrite())
                Main.ccg2lambdaLocation = selected;
        }
        System.out.println(Main.ccg2lambdaLocation);
        return Main.ccg2lambdaLocation;
    }

    /**
     * for choosing the C&C location file
     *
     * @return
     */
    @FXML
    public File setCandCLocation() {
        DirectoryChooser locationChooser = new DirectoryChooser();
        locationChooser.setTitle("select CCG Parser Cand directory");
        File selected = null;
        while (selected == null)
            selected = locationChooser.showDialog(view);
        if (selected.isDirectory()) {
            if (selected.canRead() && selected.canExecute() && selected.canWrite())
                Main.ccgCandCLocation = selected;
        }
        System.out.println(Main.ccgCandCLocation);
        return Main.ccgCandCLocation;
    }

    /**
     * choosing the easyCCG location file
     *
     * @return
     */
    @FXML
    private File setEasyCCGLocation() {
        DirectoryChooser locationChooser = new DirectoryChooser();
        locationChooser.setTitle("select easyCCG Parser directory");
        File selected = null;
        while (selected == null)
            selected = locationChooser.showDialog(view);
        if (selected.isDirectory()) {
            if (selected.canRead() && selected.canExecute() && selected.canWrite())
                Main.easyCCGLocation = selected;
        }
        System.out.println(Main.easyCCGLocation);
        return Main.easyCCGLocation;
    }

    @Override
    public void initStage(Stage primaryStage) {
        this.view = primaryStage;
    }

    /**
     * For setting the template
     */
    public void setTemplate() {
        if (radioTemplateEvent.isSelected()) {
            System.out.println("||||||||||||||||| template event");
            Main.selectedTemplateType = Tools.TemplateType.EVENT;
        } else if (radioTemplateClassic.isSelected()) {
            System.out.println("||||||||||||||||| template classic");
            Main.selectedTemplateType = Tools.TemplateType.CLASSIC;
        }
    }

    /**
     * set the event template
     */
    public void setTemplateEvent() {
        radioTemplateEvent.setSelected(true);
        radioTemplateClassic.setSelected(false);
        setTemplate();
    }

    /**
     * set the classic template
     */
    public void setTemplateClassic() {
        radioTemplateClassic.setSelected(true);
        radioTemplateEvent.setSelected(false);
        setTemplate();
    }

}
