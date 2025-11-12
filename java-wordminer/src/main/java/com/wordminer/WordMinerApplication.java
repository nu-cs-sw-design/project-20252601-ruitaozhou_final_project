package com.wordminer;

import com.wordminer.analysis.AnalysisResult;
import com.wordminer.analysis.TextAnalyzer;
import com.wordminer.db.Storage;
import com.wordminer.db.StorageException;
import com.wordminer.dictionary.DictionaryEntry;
import com.wordminer.dictionary.DictionaryLoader;
import com.wordminer.model.Article;
import com.wordminer.model.ArticleSummary;
import com.wordminer.model.TokenStat;
import com.wordminer.model.VocabStatus;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JavaFX port of the WordMiner desktop app.
 */
public class WordMinerApplication extends Application {

    private static final List<String> LEVEL_ORDER = List.of(
            "Middle School",
            "High School",
            "CET-4",
            "CET-6",
            "Postgraduate",
            "TOEFL",
            "SAT",
            "Unknown"
    );

    private final TextAnalyzer analyzer = new TextAnalyzer();
    private Storage storage;
    private Map<String, DictionaryEntry> dictionary = Map.of();
    private final ObservableList<ArticleSummary> articleItems = FXCollections.observableArrayList();
    private final ObservableList<ArticleSummary> analysisItems = FXCollections.observableArrayList();
    private final Map<String, VocabStatus> statusCache = new HashMap<>();

    private int currentArticleId = -1;
    private Article currentArticle;

    private Stage stage;
    private TabPane tabs;
    private Tab reportsTab;
    private Tab readerTab;
    private Tab vocabTab;
    private Tab analysisTab;

    private ListView<ArticleSummary> dashboardList;
    private ListView<ArticleSummary> analysisList;
    private Label reportsTitle;
    private TextArea reportArea;
    private BarChart<String, Number> difficultyChart;
    private Label readerTitle;
    private TextFlow readerFlow;
    private ListView<String> masteredList;
    private ListView<String> learningList;
    private ListView<String> unfamiliarList;
    private TextArea analysisArea;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        try {
            storage = new Storage(Path.of("wordminer.db"));
            dictionary = DictionaryLoader.load(Path.of("data", "dictionary"));
            refreshStatusCache();
        } catch (IOException | StorageException e) {
            showError("Startup failed", "Unable to initialize storage or dictionary", e);
            Platform.exit();
            return;
        }

        stage.setTitle("WordMiner");
        stage.setScene(buildScene());
        stage.setWidth(1080);
        stage.setHeight(720);
        stage.show();
        stage.setOnCloseRequest(e -> safeClose());

        refreshArticleLists();
        if (dictionary.isEmpty()) {
            showWarning("Dictionary Missing", """
                    No dictionaries loaded from data/dictionary.
                    Difficulty and definitions will be limited.
                    """);
        }
    }

    private Scene buildScene() {
        tabs = new TabPane();
        tabs.getTabs().add(buildDashboardTab());
        reportsTab = buildReportsTab();
        readerTab = buildReaderTab();
        vocabTab = buildVocabTab();
        analysisTab = buildAnalysisTab();
        tabs.getTabs().addAll(reportsTab, readerTab, vocabTab, analysisTab);

        Scene scene = new Scene(tabs);
        String stylesheet = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
        return scene;
    }

    private Tab buildDashboardTab() {
        Tab tab = new Tab("Dashboard");
        tab.setClosable(false);

        BorderPane root = new BorderPane();
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox header = new HBox();
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Imported Articles");
        title.getStyleClass().add("heading");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button importBtn = new Button("Import Article");
        importBtn.setOnAction(e -> importArticle());
        header.getChildren().addAll(title, spacer, importBtn);

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.7);

        dashboardList = new ListView<>(articleItems);
        dashboardList.setPlaceholder(new Label("No articles yet. Import to begin."));
        dashboardList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                onArticleSelected(val.id());
            }
        });

        VBox actions = new VBox(8);
        actions.setPadding(new Insets(10));
        actions.setFillWidth(true);
        Button gotoReports = new Button("View Reports");
        gotoReports.setMaxWidth(Double.MAX_VALUE);
        gotoReports.setOnAction(e -> tabs.getSelectionModel().select(reportsTab));
        Button gotoReader = new Button("Read Article");
        gotoReader.setMaxWidth(Double.MAX_VALUE);
        gotoReader.setOnAction(e -> tabs.getSelectionModel().select(readerTab));
        Button gotoVocab = new Button("Review Vocabulary");
        gotoVocab.setMaxWidth(Double.MAX_VALUE);
        gotoVocab.setOnAction(e -> tabs.getSelectionModel().select(vocabTab));
        Button gotoAnalysis = new Button("Cross-Article Analysis");
        gotoAnalysis.setMaxWidth(Double.MAX_VALUE);
        gotoAnalysis.setOnAction(e -> tabs.getSelectionModel().select(analysisTab));
        Button backupBtn = new Button("Backup Data");
        backupBtn.setMaxWidth(Double.MAX_VALUE);
        backupBtn.setOnAction(e -> backupDatabase());

        actions.getChildren().addAll(gotoReports, gotoReader, gotoVocab, gotoAnalysis, backupBtn);

        split.getItems().addAll(dashboardList, actions);

        root.setTop(header);
        root.setCenter(split);
        tab.setContent(root);
        return tab;
    }

    private Tab buildReportsTab() {
        Tab tab = new Tab("Reports");
        tab.setClosable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        reportsTitle = new Label("Select an article from Dashboard");

        reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setPrefRowCount(8);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Difficulty");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Occurrences");
        difficultyChart = new BarChart<>(xAxis, yAxis);
        difficultyChart.setAnimated(false);
        difficultyChart.setLegendVisible(false);

        layout.getChildren().addAll(reportsTitle, reportArea, difficultyChart);
        VBox.setVgrow(difficultyChart, Priority.ALWAYS);

        tab.setContent(layout);
        return tab;
    }

    private Tab buildReaderTab() {
        Tab tab = new Tab("Reader");
        tab.setClosable(false);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        readerTitle = new Label("Select an article from Dashboard");
        readerFlow = new TextFlow();
        readerFlow.getStyleClass().add("reader-flow");
        ScrollPane scroller = new ScrollPane(readerFlow);
        scroller.setFitToWidth(true);
        layout.getChildren().addAll(readerTitle, scroller);
        VBox.setVgrow(scroller, Priority.ALWAYS);

        tab.setContent(layout);
        return tab;
    }

    private Tab buildVocabTab() {
        Tab tab = new Tab("Vocabulary");
        tab.setClosable(false);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        HBox lists = new HBox(10);
        masteredList = new ListView<>();
        learningList = new ListView<>();
        unfamiliarList = new ListView<>();
        Stream.of(masteredList, learningList, unfamiliarList).forEach(list -> {
            list.setPrefWidth(200);
            list.setPlaceholder(new Label("No words"));
        });
        lists.getChildren().addAll(
                vocabColumn("Mastered", masteredList),
                vocabColumn("Learning", learningList),
                vocabColumn("Unfamiliar", unfamiliarList)
        );

        VBox controls = new VBox(8);
        controls.setPadding(new Insets(0, 0, 0, 10));
        Button markMastered = new Button("Mark Mastered");
        markMastered.setMaxWidth(Double.MAX_VALUE);
        markMastered.setOnAction(e -> markSelected(VocabStatus.MASTERED));
        Button markLearning = new Button("Mark Learning");
        markLearning.setMaxWidth(Double.MAX_VALUE);
        markLearning.setOnAction(e -> markSelected(VocabStatus.LEARNING));
        Button markUnfamiliar = new Button("Mark Unfamiliar");
        markUnfamiliar.setMaxWidth(Double.MAX_VALUE);
        markUnfamiliar.setOnAction(e -> markSelected(VocabStatus.UNFAMILIAR));
        Button exportBtn = new Button("Export CSV");
        exportBtn.setMaxWidth(Double.MAX_VALUE);
        exportBtn.setOnAction(e -> exportVocabulary());
        controls.getChildren().addAll(markMastered, markLearning, markUnfamiliar, exportBtn);

        layout.setCenter(lists);
        layout.setRight(controls);
        tab.setContent(layout);
        return tab;
    }

    private VBox vocabColumn(String title, ListView<String> listView) {
        VBox wrapper = new VBox(5);
        Label label = new Label(title);
        wrapper.getChildren().addAll(label, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);
        return wrapper;
    }

    private Tab buildAnalysisTab() {
        Tab tab = new Tab("Cross-Article Analysis");
        tab.setClosable(false);

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(10));

        analysisList = new ListView<>(analysisItems);
        analysisList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        layout.setCenter(analysisList);

        VBox right = new VBox(8);
        Button computeBtn = new Button("Compute Overlap");
        computeBtn.setMaxWidth(Double.MAX_VALUE);
        computeBtn.setOnAction(e -> computeOverlap());
        Button refreshBtn = new Button("Refresh List");
        refreshBtn.setMaxWidth(Double.MAX_VALUE);
        refreshBtn.setOnAction(e -> refreshArticleLists());
        right.getChildren().addAll(computeBtn, refreshBtn);

        analysisArea = new TextArea();
        analysisArea.setEditable(false);
        analysisArea.setPrefRowCount(6);
        layout.setBottom(analysisArea);
        layout.setRight(right);
        tab.setContent(layout);
        return tab;
    }

    private void importArticle() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Article");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String title = file.getName();
            int articleId = storage.addArticle(title, file.toPath(), content);
            AnalysisResult result = analyzer.analyze(content, dictionary);
            storage.upsertArticleTokens(articleId, result.lemmaCounts(), dictionary);
            refreshArticleLists();
            dashboardList.getSelectionModel().selectFirst();
            showInfo("Import Complete", "Article \"" + title + "\" imported.");
        } catch (IOException | StorageException e) {
            showError("Import Failed", "Unable to import article", e);
        }
    }

    private void onArticleSelected(int articleId) {
        storage.getArticle(articleId).ifPresent(article -> {
            currentArticleId = articleId;
            currentArticle = article;
            renderReport(article);
            renderReader(article);
            refreshVocabLists();
            tabs.getSelectionModel().select(readerTab);
        });
    }

    private void renderReport(Article article) {
        reportsTitle.setText("Reports - " + article.title());
        Map<String, Integer> stats = storage.getArticleStats(article.id());
        List<TokenStat> vocab = storage.getArticleVocab(article.id());

        StringBuilder sb = new StringBuilder();
        sb.append("Article ID: ").append(article.id()).append("\n");
        sb.append("Created: ").append(article.createdAt()).append("\n");
        sb.append("Word count: ").append(stats.values().stream().mapToInt(Integer::intValue).sum()).append("\n\n");
        sb.append("Difficulty breakdown:\n");
        LEVEL_ORDER.forEach(level ->
                sb.append(" - ").append(level).append(": ").append(stats.getOrDefault(level, 0)).append("\n")
        );

        sb.append("\nTop vocabulary:\n");
        vocab.stream().limit(20).forEach(token ->
                sb.append(String.format(" - %s (%d) [%s]%n", token.lemma(), token.count(), token.difficulty()))
        );
        reportArea.setText(sb.toString());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        LEVEL_ORDER.forEach(level -> series.getData().add(new XYChart.Data<>(level, stats.getOrDefault(level, 0))));
        difficultyChart.getData().setAll(series);
    }

    private void renderReader(Article article) {
        readerTitle.setText("Reader - " + article.title());
        readerFlow.getChildren().clear();
        String content = article.content();
        var matcher = analyzer.matcher(content);
        int offset = 0;
        Map<String, VocabStatus> statuses = new HashMap<>(statusCache);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            if (start > offset) {
                String gap = content.substring(offset, start);
                readerFlow.getChildren().add(new Text(gap));
            }
            String word = content.substring(start, end);
            String lemma = analyzer.lemmatize(word);
            DictionaryEntry entry = dictionary.get(lemma);
            VocabStatus status = statuses.get(lemma);
            readerFlow.getChildren().add(createWordNode(word, lemma, entry, status));
            offset = end;
        }
        if (offset < content.length()) {
            String tail = content.substring(offset);
            readerFlow.getChildren().add(new Text(tail));
        }
    }

    private Region createWordNode(String word, String lemma, DictionaryEntry entry, VocabStatus status) {
        Label label = new Label(word);
        label.setPadding(new Insets(1, 2, 1, 2));
        label.getStyleClass().add("word-chip");
        label.getStyleClass().add("status-" + (status == null ? "unknown" : status.dbValue()));
        label.getStyleClass().add("level-" + cssSafe(entry == null ? "Unknown" : entry.level()));
        label.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                showWordDialog(lemma, entry, status);
            }
        });
        return label;
    }

    private void refreshArticleLists() {
        List<ArticleSummary> summaries = storage.listArticles();
        articleItems.setAll(summaries);
        analysisItems.setAll(summaries.stream()
                .sorted(Comparator.comparing(ArticleSummary::id))
                .collect(Collectors.toList()));
        if (!summaries.isEmpty() && currentArticleId == -1) {
            dashboardList.getSelectionModel().selectFirst();
        }
    }

    private void refreshVocabLists() {
        masteredList.setItems(FXCollections.observableArrayList(storage.listVocab(VocabStatus.MASTERED)));
        learningList.setItems(FXCollections.observableArrayList(storage.listVocab(VocabStatus.LEARNING)));
        unfamiliarList.setItems(FXCollections.observableArrayList(storage.listVocab(VocabStatus.UNFAMILIAR)));
    }

    private void refreshStatusCache() {
        statusCache.clear();
        statusCache.putAll(storage.getAllStatuses());
    }

    private void markSelected(VocabStatus status) {
        String word = getSelectedWord();
        if (word == null || word.isBlank()) {
            showInfo("Select Word", "Choose a word from any list.");
            return;
        }
        storage.setStatus(word, status);
        refreshStatusCache();
        refreshVocabLists();
        if (currentArticle != null) {
            renderReader(currentArticle);
        }
    }

    private String getSelectedWord() {
        String fromMastered = masteredList.getSelectionModel().getSelectedItem();
        if (fromMastered != null) {
            return fromMastered;
        }
        String fromLearning = learningList.getSelectionModel().getSelectedItem();
        if (fromLearning != null) {
            return fromLearning;
        }
        return unfamiliarList.getSelectionModel().getSelectedItem();
    }

    private void exportVocabulary() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Vocabulary CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        chooser.setInitialFileName("wordminer-vocabulary.csv");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            List<String> lines = new ArrayList<>();
            lines.add("lemma,status");
            for (VocabStatus status : VocabStatus.values()) {
                for (String lemma : storage.listVocab(status)) {
                    lines.add(lemma + "," + status.dbValue());
                }
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            showInfo("Exported", "Vocabulary saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Export Failed", "Unable to export vocabulary", e);
        }
    }

    private void computeOverlap() {
        List<ArticleSummary> selected = analysisList.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            analysisArea.setText("Select two or more articles.");
            return;
        }
        List<Integer> ids = selected.stream().map(ArticleSummary::id).toList();
        Map<String, Integer> stats = storage.crossOverlap(ids);
        if (stats.isEmpty()) {
            analysisArea.setText("No statistics available.");
            return;
        }
        analysisArea.setText("""
                Unique words total: %d
                Shared across selection: %d
                """.formatted(stats.getOrDefault("unique", 0), stats.getOrDefault("shared", 0)));
    }

    private void backupDatabase() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Backup Database");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite", "*.db"));
        chooser.setInitialFileName("wordminer-backup.db");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            storage.backupTo(file.toPath());
            showInfo("Backup Complete", "Database copied to " + file.getAbsolutePath());
        } catch (StorageException e) {
            showError("Backup Failed", "Unable to backup database", e);
        }
    }

    private void showWordDialog(String lemma, DictionaryEntry entry, VocabStatus status) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Word Details - " + lemma);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Lemma: " + lemma),
                new Label("Level: " + (entry == null ? "Unknown" : entry.level())),
                new Label("Status: " + (status == null ? "unknown" : status.dbValue()))
        );

        TextArea definitions = new TextArea();
        definitions.setEditable(false);
        definitions.setWrapText(true);
        definitions.setPrefRowCount(8);

        if (entry != null) {
            if (!entry.translations().isEmpty()) {
                definitions.appendText("Meaning(s):\n");
                entry.translations().stream().limit(10).forEach(tr ->
                        definitions.appendText(" - " + tr.translation() + " (" + tr.type() + ")\n"));
                definitions.appendText("\n");
            }
            if (!entry.phrases().isEmpty()) {
                definitions.appendText("Phrases:\n");
                entry.phrases().stream().limit(10).forEach(ph ->
                        definitions.appendText(" - " + ph.phrase() + ": " + ph.translation() + "\n"));
            }
        } else {
            definitions.setText("No dictionary entry available.");
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);
        for (VocabStatus target : VocabStatus.values()) {
            Button button = new Button(target.name().charAt(0) + target.name().substring(1).toLowerCase());
            button.setOnAction(e -> {
                storage.setStatus(lemma, target);
                refreshStatusCache();
                refreshVocabLists();
                if (currentArticle != null) {
                    renderReader(currentArticle);
                }
                dialog.close();
            });
            buttons.getChildren().add(button);
        }

        content.getChildren().addAll(definitions, buttons);
        dialog.getDialogPane().setContent(content);
        dialog.show();
    }

    private String cssSafe(String level) {
        return level.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }

    private void showError(String title, String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void safeClose() {
        try {
            if (storage != null) {
                storage.close();
            }
        } catch (Exception ex) {
            // ignore close errors
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
