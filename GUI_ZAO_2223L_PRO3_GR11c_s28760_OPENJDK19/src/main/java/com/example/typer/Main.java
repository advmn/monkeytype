package com.example.typer;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main extends Application {

    private final DoubleProperty fontSize = new SimpleDoubleProperty(10);

    private static final int MAX_WORDS = 30;
    private Stage primaryStage;
    private VBox centerBox;
    private TextFlow wordsTextFlow;
    private boolean isGameStarted = false;
    private int currentWordIndex = 0;
    private int currentLetterIndex = 0;
    private long startTime;
    private Label averageLabel;

    private int remainingTime;
    private Timeline timeline;
    private Label timeLabel;
    private Label characterCountLabel;
    private Label correctCountLabel;
    private Label missedCountLabel;
    private Label incorrectCountLabel;
    private int characterCount = 0;
    private int correctCount = 0;
    private int missedCount = 0;
    private int incorrectCount = 0;
    private List<Integer> charactersPerMinuteList;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: darkgrey;");

        Cursor cursor = Cursor.DEFAULT;

        HBox topBox = new HBox(35);
        topBox.setAlignment(Pos.CENTER);
        topBox.setCursor(cursor);

        ComboBox<String> languageBox = new ComboBox<>();
        languageBox.setCursor(cursor);
        File dictionary = new File("dictionary");
        File[] files = dictionary.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String filename = file.getName();
                    if (filename.endsWith(".txt")) {
                        filename = filename.substring(0, filename.length() - 4);
                    }
                    languageBox.getItems().add(filename);
                }
            }
        }

        languageBox.setValue("Jezyk");
        languageBox.setOnAction(e -> languageBox.setValue(languageBox.getSelectionModel().getSelectedItem()));
        languageBox.setCursor(cursor);
        languageBox.getStyleClass().add("my-combo-box");

        ComboBox<String> timeBox = new ComboBox<>();
        timeBox.setCursor(cursor);
        timeBox.getItems().addAll("15", "20", "45", "60", "90", "120", "300");
        timeBox.setValue("Sekundy");
        timeBox.setOnAction(e -> {
            String selectedTime = timeBox.getSelectionModel().getSelectedItem();
            if (selectedTime != null && selectedTime.matches("\\d+")) {
                int time = Integer.parseInt(selectedTime);
                startGameTimer(time);
            }
        });

        languageBox.setStyle("-fx-background-color: black; -fx-border-color: black;");
        timeBox.setStyle("-fx-background-color: black; -fx-border-color: black;");
        timeBox.getStyleClass().add("my-combo-box");

        topBox.getChildren().addAll(languageBox, timeBox);

        Button startGameButton = new Button("Start Typerka");
        startGameButton.setCursor(cursor);
        startGameButton.getStyleClass().add("my-button");
        startGameButton.setOnAction(e -> {
            if (!isGameStarted) {
                System.out.println("Jadą koty");
                String selectedLanguage = languageBox.getSelectionModel().getSelectedItem();
                if (selectedLanguage != null) {
                    List<String> words = getRandomWords(selectedLanguage);
                    showRandomWords(words);
                }
                isGameStarted = true;
                startGameButton.setVisible(false);
                startTime = System.currentTimeMillis();
                charactersPerMinuteList = new ArrayList<>();
            }
        });

        wordsTextFlow = new TextFlow();
        wordsTextFlow.setFocusTraversable(true);

        centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(startGameButton, wordsTextFlow);

        root.setTop(topBox);
        root.setCenter(centerBox);

        Label shortcutInfo1 = new Label("[ tab + enter ] - restart test");
        Label shortcutInfo2 = new Label("[ ctrl + shift + p ] - pause");
        Label shortcutInfo3 = new Label("[ esc ] - end test");
        shortcutInfo1.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: white;");
        shortcutInfo2.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: white;");
        shortcutInfo3.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: white;");

        VBox footer = new VBox(10, shortcutInfo1, shortcutInfo2, shortcutInfo3);
        footer.setAlignment(Pos.CENTER);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1200, 800);
        scene.setCursor(cursor);

        fontSize.bind(scene.heightProperty().divide(40));
        wordsTextFlow.styleProperty().bind(Bindings.concat("-fx-font-size: ", fontSize.asString()));

        scene.setOnKeyTyped(event -> {
            String inputChar = event.getCharacter();
            handleLetterInput(inputChar);
            moveToNextLetter();
        });

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
                restartTest();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                endTest();
            } else if (event.getCode() == KeyCode.P && event.isControlDown() && event.isShiftDown()) {
                togglePause();
            } else if (event.getCode() == KeyCode.SPACE) {
                handleSpace();
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                handleBackspace();
            }
        });

        primaryStage.setTitle("TyperekNaGUI");
        primaryStage.setResizable(false);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startGameTimer(int time) {
        if (timeline != null) {
            timeline.stop();
        }

        remainingTime = time;
        timeLabel = new Label();
        timeLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: yellow;");
        characterCountLabel = new Label();
        characterCountLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: blue;");
        correctCountLabel = new Label();
        correctCountLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: green;");
        missedCountLabel = new Label();
        missedCountLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: black;");
        incorrectCountLabel = new Label();
        incorrectCountLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: red;");
        averageLabel = new Label();
        averageLabel.setStyle("-fx-font-size: 40px; -fx-font-family: Arial; -fx-text-fill: green;");
        HBox timerBox = new HBox(10, timeLabel, characterCountLabel, correctCountLabel, missedCountLabel, incorrectCountLabel, averageLabel);
        timerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().add(timerBox);

        final Color[] colors = {Color.YELLOW, Color.RED, Color.GREEN, Color.PURPLE, Color.BLUE, Color.PINK, Color.BLACK, Color.WHITE};

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingTime--;
            int colorIndex = (remainingTime % colors.length);
            timeLabel.setTextFill(colors[colorIndex]);
            timeLabel.setText(formatTime(remainingTime));
            if (remainingTime <= 0) {
                endGame();
            }
        }
        ));
        timeline.setCycleCount(time);
        timeline.setOnFinished(event -> endGame());

        timeline.play();
    }
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
    private void endGame() {
        if (timeline != null) {
            timeline.stop();
        }
        centerBox.getChildren().removeAll(timeLabel, characterCountLabel, averageLabel);

        long endTime = System.currentTimeMillis();
        double elapsedTime = (endTime - startTime) / 60000.0;
        int totalCharacters = characterCount;
        int charactersPerMinute = (int) (totalCharacters / elapsedTime);

        primaryStage.close();

        Stage chartStage = new Stage();
        VBox chartBox = new VBox(10);
        chartBox.setAlignment(Pos.CENTER);
        Scene chartScene = new Scene(chartBox);
        chartStage.setTitle("Characters per Minute");
        chartStage.setScene(chartScene);

        LineChart<Number, Number> charactersPerMinuteChart = createChart();
        XYChart.Series<Number, Number> charactersPerMinuteSeries = new XYChart.Series<>();
        charactersPerMinuteChart.getData().add(charactersPerMinuteSeries);
        chartBox.getChildren().add(charactersPerMinuteChart);

        for (int i = 0; i < charactersPerMinuteList.size(); i++) {
            charactersPerMinuteSeries.getData().add(new XYChart.Data<>(i, charactersPerMinuteList.get(i)));
        }

        Label averageLabel = new Label("Średnia ilość znaków na minutę: " + charactersPerMinute);
        averageLabel.setStyle("-fx-font-size: 20px; -fx-font-family: Arial; -fx-text-fill: green;");
        chartBox.getChildren().add(averageLabel);

        Label percentageLabel = new Label("Procent poprawnych: " + calculatePercentage(correctCount, characterCount) + "%");
        Label missedPercentageLabel = new Label("Procent pominiętych: " + calculatePercentage(missedCount, characterCount) + "%");
        Label incorrectPercentageLabel = new Label("Procent błędnych: " + calculatePercentage(incorrectCount, characterCount) + "%");

        percentageLabel.setStyle("-fx-font-size: 20px; -fx-font-family: Arial; -fx-text-fill: green;");
        missedPercentageLabel.setStyle("-fx-font-size: 20px; -fx-font-family: Arial; -fx-text-fill: green;");
        incorrectPercentageLabel.setStyle("-fx-font-size: 20px; -fx-font-family: Arial; -fx-text-fill: green;");

        chartBox.getChildren().addAll(percentageLabel, missedPercentageLabel, incorrectPercentageLabel);

        chartStage.show();

        saveGameResults(charactersPerMinute, calculatePercentage(correctCount, characterCount), calculatePercentage(missedCount, characterCount), calculatePercentage(incorrectCount, characterCount));
    }

    private void saveGameResults(int charactersPerMinute, double correctPercentage, double missedPercentage, double incorrectPercentage) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        LocalDateTime now = LocalDateTime.now();
        String filename = dtf.format(now) + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("Data i godzina wykonania testu: " + dtf.format(now));
            writer.newLine();
            writer.write("Zestawienie kolejnych słów:");
            writer.newLine();
            List<Text> textNodes = getTextNodes();
            for (Text text : textNodes) {
                writer.write(text.getText());
                writer.newLine();
            }
            writer.write("Wyliczenie znaków na minutę:");
            writer.newLine();
            writer.write("Średnia ilość znaków na minutę: " + charactersPerMinute);
            writer.newLine();
            writer.write("Procent poprawnych: " + correctPercentage + "%");
            writer.newLine();
            writer.write("Procent pominiętych: " + missedPercentage + "%");
            writer.newLine();
            writer.write("Procent błędnych: " + incorrectPercentage + "%");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private double calculatePercentage(int count, int total) {
        if (total == 0) {
            return 0.0;
        }
        return ((double) count / total) * 100;
    }
    private LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (seconds)");
        yAxis.setLabel("Characters per Minute");
        return new LineChart<>(xAxis, yAxis);
    }

    private List<String> getRandomWords(String language) {
        List<String> words = new ArrayList<>();

        File dictionary = new File("dictionary/" + language + ".txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(dictionary))) {
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }

            Random random = new Random();
            int numLines = lines.size();
            for (int i = 0; i < MAX_WORDS; i++) {
                if (i < numLines) {
                    int randomIndex = random.nextInt(numLines);
                    String word = lines.get(randomIndex);
                    words.add(word);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return words;
    }
    private void showRandomWords(List<String> words) {
        if (wordsTextFlow != null && centerBox.getChildren().contains(wordsTextFlow)) {
            centerBox.getChildren().remove(wordsTextFlow);
            wordsTextFlow = null;
        }

        if (wordsTextFlow == null) {
            wordsTextFlow = new TextFlow();
        }

        for (String word : words) {
            for (char letter : word.toCharArray()) {
                Text textLetter = new Text(String.valueOf(letter));
                textLetter.setFill(Color.LIGHTGRAY);
                wordsTextFlow.getChildren().add(textLetter);
            }

            Text textSpace = new Text(" ");
            textSpace.setFill(Color.LIGHTGRAY);
            wordsTextFlow.getChildren().add(textSpace);
        }

        wordsTextFlow.setStyle("-fx-font-size: 48px;");

        centerBox.getChildren().add(wordsTextFlow);
        wordsTextFlow.requestFocus();
    }

    private void handleLetterInput(String inputChar) {
        if (wordsTextFlow != null) {
            List<Text> textNodes = getTextNodes();
            if (currentWordIndex < textNodes.size()) {
                Text currentText = textNodes.get(currentWordIndex);
                String currentWordText = currentText.getText();
                if (currentLetterIndex < currentWordText.length()) {
                    char currentLetter = currentWordText.charAt(currentLetterIndex);
                    if (currentLetter == inputChar.charAt(0)) {
                        currentText.setFill(Color.GREEN);
                        correctCount++;
                        correctCountLabel.setText("Poprawne: " + correctCount);

                        ScaleTransition st = new ScaleTransition(Duration.millis(200), currentText);
                        st.setByX(0.3);
                        st.setByY(0.3);
                        st.setCycleCount(2);
                        st.setAutoReverse(true);
                        st.play();
                    } else {
                        currentText.setFill(Color.RED);
                        incorrectCount++;
                        incorrectCountLabel.setText("Błędne: " + incorrectCount);

                        TranslateTransition tt = new TranslateTransition(Duration.millis(100), wordsTextFlow);
                        tt.setByX(10f);
                        tt.setByY(10f);
                        tt.setCycleCount(2);
                        tt.setAutoReverse(true);
                        tt.playFromStart();
                    }
                    currentLetterIndex++;
                    characterCount++;
                    characterCountLabel.setText(Integer.toString(characterCount));

                    long currentTime = System.currentTimeMillis();
                    double elapsedTime = (currentTime - startTime) / 60000.0;
                    int charactersPerMinute = (int) (characterCount / elapsedTime);
                    characterCountLabel.setTextFill(Color.BLUE);
                    characterCountLabel.setText(Integer.toString(charactersPerMinute));
                    charactersPerMinuteList.add(charactersPerMinute);
                } else {
                    Text extraText = new Text(inputChar);
                    extraText.setFill(Color.ORANGE);
                    wordsTextFlow.getChildren().add(extraText);
                    missedCount++;
                    missedCountLabel.setText("Pominięte: " + missedCount);
                }
            }
        }
    }

    private void moveToNextLetter() {
        if (wordsTextFlow != null) {
            List<Text> textNodes = getTextNodes();
            if (currentWordIndex < textNodes.size()) {
                Text currentText = textNodes.get(currentWordIndex);
                String currentWordText = currentText.getText();
                if (currentLetterIndex < currentWordText.length()) {
                    currentText.setFill(Color.DARKBLUE);
                } else if (currentLetterIndex == currentWordText.length()) {
                    currentWordIndex++;
                    currentLetterIndex = 0;
                    if (currentWordIndex < textNodes.size()) {
                        Text nextText = textNodes.get(currentWordIndex);
                        if (!nextText.getText().equals(" ")) {
                            nextText.setFill(Color.DARKBLUE);
                        }
                    }
                }
            }
        }
    }
    private List<Text> getTextNodes() {
        List<Text> textNodes = new ArrayList<>();
        for (javafx.scene.Node node : wordsTextFlow.getChildren()) {
            if (node instanceof Text) {
                textNodes.add((Text) node);
            }
        }
        return textNodes;
    }
    private void handleSpace() {
        if (wordsTextFlow != null) {
            List<Text> textNodes = getTextNodes();
            if (currentWordIndex < textNodes.size()) {
                Text currentText = textNodes.get(currentWordIndex);
                String currentWordText = currentText.getText();
                if (currentLetterIndex == currentWordText.length() && currentWordText.endsWith(" ")) {
                    // Skip over the space
                    currentWordIndex++;
                    currentLetterIndex = 0;
                    if (currentWordIndex < textNodes.size()) {
                        Text nextText = textNodes.get(currentWordIndex);
                        if (!nextText.getText().equals(" ")) {
                            nextText.setFill(Color.DARKBLUE);
                        }
                    }
                } else {
                    currentLetterIndex++;
                    if (currentLetterIndex < currentWordText.length()) {
                        currentText.setFill(Color.DARKBLUE);
                    } else {
                        currentText.setFill(Color.BLACK);
                    }
                }
            }
        }
    }
    private void handleBackspace() {
        if (wordsTextFlow != null) {
            List<Text> textNodes = getTextNodes();
            if (currentWordIndex < textNodes.size()) {
                Text currentText = textNodes.get(currentWordIndex);
                String currentWordText = currentText.getText();
                if (currentLetterIndex > 0) {
                    currentLetterIndex--;
                    String newText = currentWordText.substring(0, currentLetterIndex)
                            + currentWordText.substring(currentLetterIndex + 1);
                    currentText.setText(newText);
                    currentText.setFill(Color.LIGHTGRAY);
                }
            }
        }
    }
    private void restartTest() {
        if (wordsTextFlow != null) {
            wordsTextFlow.getChildren().clear();
            currentWordIndex = 0;
            currentLetterIndex = 0;
            characterCount = 0;
            correctCount = 0;
            missedCount = 0;
            incorrectCount = 0;
            characterCountLabel.setText("0");
            correctCountLabel.setText("Poprawne: 0");
            missedCountLabel.setText("Pominięte: 0");
            incorrectCountLabel.setText("Błędne: 0");
            averageLabel.setText("");
            isGameStarted = false;
            centerBox.getChildren().get(0).setVisible(true);
        }
    }
    private void togglePause() {
        if (isGameStarted) {
            if (timeline != null) {
                if (timeline.getStatus() == Animation.Status.PAUSED) {
                    timeline.play();
                } else if (timeline.getStatus() == Animation.Status.RUNNING) {
                    timeline.pause();
                }
            }
        }
    }
    private void endTest() {
        if (isGameStarted) {
            restartTest();
            primaryStage.close();
        }
    }
}