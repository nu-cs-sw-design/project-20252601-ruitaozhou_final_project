package com.wordminer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.CoreLabel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Main WordMiner Application - All-in-one messy implementation
 */
public class WordMinerApp extends JFrame {
    
    // Database connection
    private Connection dbConnection;
    
    // Stanford CoreNLP pipeline for lemmatization
    private StanfordCoreNLP pipeline;
    
    // Dictionary cache: lemma -> {level, translations, phrases}
    private Map<String, DictEntry> dictionary = new HashMap<>();
    
    // User labeled words: lemma -> label
    private Map<String, String> userWords = new HashMap<>();
    
    // GUI components
    private JTable articleTable;
    private DefaultTableModel tableModel;
    private JButton importBtn, deleteBtn, reportBtn, readBtn, labelsBtn;
    
    // Current reading article and position
    private int currentArticleId = -1;
    private Map<Integer, Integer> readingProgress = new HashMap<>();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new WordMinerApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to start: " + e.getMessage());
            }
        });
    }
    
    public WordMinerApp() {
        System.out.println("Initializing database...");
        initDatabase();
        System.out.println("Database initialized.");
        
        System.out.println("Initializing NLP pipeline (this may take a minute)...");
        initNLP();
        System.out.println("NLP pipeline ready.");
        
        System.out.println("Loading dictionary...");
        loadDictionary();
        System.out.println("Dictionary loaded: " + dictionary.size() + " words");
        
        System.out.println("Loading user words...");
        loadUserWords();
        System.out.println("User words loaded.");
        
        System.out.println("Creating GUI...");
        initGUI();
        refreshArticleList();
        System.out.println("Application ready!");
    }
    
    private void initDatabase() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:wordminer.db");
            Statement stmt = dbConnection.createStatement();
            
            stmt.execute("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, content TEXT, file_path TEXT, total_words INTEGER, unique_words INTEGER, import_date TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS dictionary (word TEXT PRIMARY KEY, level TEXT, data TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS user_words (lemma TEXT PRIMARY KEY, label TEXT, date TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS reading_progress (article_id INTEGER PRIMARY KEY, position INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS dict_loaded (level TEXT PRIMARY KEY)");
            
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void initNLP() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        props.setProperty("tokenize.language", "en");
        pipeline = new StanfordCoreNLP(props);
    }
    
    private void loadDictionary() {
        try {
            String[] levels = {"1-middle-school", "2-high-school", "3-CET4", "4-CET6", "5-postgraduate", "6-TOEFL", "7-SAT"};
            
            for (String level : levels) {
                // Check if already loaded
                PreparedStatement check = dbConnection.prepareStatement("SELECT 1 FROM dict_loaded WHERE level=?");
                check.setString(1, level);
                ResultSet rs = check.executeQuery();
                if (rs.next()) continue;
                
                // Load from JSON
                String jsonPath = "data/dictionary/" + level + ".json";
                File jsonFile = new File(jsonPath);
                if (!jsonFile.exists()) continue;
                
                String jsonContent = Files.readString(jsonFile.toPath());
                Gson gson = new Gson();
                List<Map<String, Object>> words = gson.fromJson(jsonContent, new TypeToken<List<Map<String, Object>>>(){}.getType());
                
                PreparedStatement insert = dbConnection.prepareStatement("INSERT OR IGNORE INTO dictionary VALUES (?,?,?)");
                for (Map<String, Object> wordData : words) {
                    String word = (String) wordData.get("word");
                    insert.setString(1, word);
                    insert.setString(2, level);
                    insert.setString(3, gson.toJson(wordData));
                    insert.executeUpdate();
                }
                
                // Mark as loaded
                PreparedStatement markLoaded = dbConnection.prepareStatement("INSERT INTO dict_loaded VALUES (?)");
                markLoaded.setString(1, level);
                markLoaded.executeUpdate();
            }
            
            // Load into memory
            PreparedStatement ps = dbConnection.prepareStatement("SELECT * FROM dictionary");
            ResultSet rs = ps.executeQuery();
            Gson gson = new Gson();
            while (rs.next()) {
                String word = rs.getString("word");
                String level = rs.getString("level");
                String data = rs.getString("data");
                Map<String, Object> wordData = gson.fromJson(data, new TypeToken<Map<String, Object>>(){}.getType());
                
                if (!dictionary.containsKey(word)) {
                    dictionary.put(word, new DictEntry());
                }
                dictionary.get(word).levels.add(level);
                dictionary.get(word).data = wordData;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadUserWords() {
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT * FROM user_words");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                userWords.put(rs.getString("lemma"), rs.getString("label"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void initGUI() {
        setTitle("WordMiner - Vocabulary Learning Tool");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Ensure window appears on top
        setAlwaysOnTop(true);
        setAlwaysOnTop(false);
        toFront();
        requestFocus();
        
        JPanel topPanel = new JPanel();
        importBtn = new JButton("Import Article");
        deleteBtn = new JButton("Delete Article");
        reportBtn = new JButton("Vocabulary Report");
        readBtn = new JButton("Read Article");
        labelsBtn = new JButton("My Labeled Words");
        
        reportBtn.setEnabled(false);
        readBtn.setEnabled(false);
        
        importBtn.addActionListener(e -> importArticle());
        deleteBtn.addActionListener(e -> deleteArticle());
        reportBtn.addActionListener(e -> showReport());
        readBtn.addActionListener(e -> readArticle());
        labelsBtn.addActionListener(e -> showLabeledWords());
        
        topPanel.add(importBtn);
        topPanel.add(deleteBtn);
        topPanel.add(reportBtn);
        topPanel.add(readBtn);
        topPanel.add(labelsBtn);
        
        String[] columns = {"ID", "Title", "Total Words", "Unique Words", "Import Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        articleTable = new JTable(tableModel);
        articleTable.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = articleTable.getSelectedRow() >= 0;
            reportBtn.setEnabled(selected);
            readBtn.setEnabled(selected);
        });
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(articleTable), BorderLayout.CENTER);
    }
    
    private void refreshArticleList() {
        tableModel.setRowCount(0);
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT * FROM articles ORDER BY id DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getInt("total_words"),
                    rs.getInt("unique_words"),
                    rs.getString("import_date")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void importArticle() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".txt");
            }
            public String getDescription() { return "Text Files (*.txt)"; }
        });
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fc.getSelectedFile();
                String content = Files.readString(file.toPath());
                
                if (content.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "File is empty!");
                    return;
                }
                
                // Analyze with NLP
                CoreDocument doc = new CoreDocument(content);
                pipeline.annotate(doc);
                
                int totalWords = 0;
                Set<String> uniqueLemmas = new HashSet<>();
                Map<String, Integer> levelCounts = new HashMap<>();
                
                for (CoreLabel token : doc.tokens()) {
                    if (token.word().matches("[a-zA-Z]+")) {
                        totalWords++;
                        String lemma = token.lemma().toLowerCase();
                        uniqueLemmas.add(lemma);
                        
                        if (dictionary.containsKey(lemma)) {
                            for (String level : dictionary.get(lemma).levels) {
                                levelCounts.put(level, levelCounts.getOrDefault(level, 0) + 1);
                            }
                        }
                    }
                }
                
                int notInDict = uniqueLemmas.size();
                for (String lemma : uniqueLemmas) {
                    if (dictionary.containsKey(lemma)) notInDict--;
                }
                
                // Save to DB
                PreparedStatement ps = dbConnection.prepareStatement(
                    "INSERT INTO articles (title, content, file_path, total_words, unique_words, import_date) VALUES (?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, file.getName());
                ps.setString(2, content);
                ps.setString(3, file.getAbsolutePath());
                ps.setInt(4, totalWords);
                ps.setInt(5, uniqueLemmas.size());
                ps.setString(6, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                ps.executeUpdate();
                
                // Show summary
                StringBuilder summary = new StringBuilder();
                summary.append("Article imported successfully!\n\n");
                summary.append("Total words: " + totalWords + "\n");
                summary.append("Unique words: " + uniqueLemmas.size() + "\n\n");
                summary.append("Vocabulary Level Distribution:\n");
                for (Map.Entry<String, Integer> entry : levelCounts.entrySet()) {
                    summary.append(entry.getKey() + ": " + entry.getValue() + " words\n");
                }
                summary.append("Not in dictionary: " + notInDict + " words");
                
                JOptionPane.showMessageDialog(this, summary.toString());
                refreshArticleList();
                
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Import failed: " + e.getMessage());
            }
        }
    }
    
    private void deleteArticle() {
        int row = articleTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an article");
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Delete article: " + title + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                PreparedStatement ps = dbConnection.prepareStatement("DELETE FROM articles WHERE id=?");
                ps.setInt(1, id);
                ps.executeUpdate();
                
                ps = dbConnection.prepareStatement("DELETE FROM reading_progress WHERE article_id=?");
                ps.setInt(1, id);
                ps.executeUpdate();
                
                refreshArticleList();
                JOptionPane.showMessageDialog(this, "Article deleted");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void showReport() {
        int row = articleTable.getSelectedRow();
        if (row < 0) return;
        
        int id = (int) tableModel.getValueAt(row, 0);
        
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT * FROM articles WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");
                int totalWords = rs.getInt("total_words");
                int uniqueWords = rs.getInt("unique_words");
                
                // Analyze
                CoreDocument doc = new CoreDocument(content);
                pipeline.annotate(doc);
                
                Set<String> uniqueLemmas = new HashSet<>();
                Map<String, Integer> levelCounts = new HashMap<>();
                Map<String, Integer> labelCounts = new HashMap<>();
                labelCounts.put("known", 0);
                labelCounts.put("unknown", 0);
                labelCounts.put("uncertain", 0);
                labelCounts.put("unlabeled", 0);
                
                for (CoreLabel token : doc.tokens()) {
                    if (token.word().matches("[a-zA-Z]+")) {
                        String lemma = token.lemma().toLowerCase();
                        uniqueLemmas.add(lemma);
                    }
                }
                
                for (String lemma : uniqueLemmas) {
                    if (dictionary.containsKey(lemma)) {
                        for (String level : dictionary.get(lemma).levels) {
                            levelCounts.put(level, levelCounts.getOrDefault(level, 0) + 1);
                        }
                    }
                    
                    String label = userWords.getOrDefault(lemma, "unlabeled");
                    labelCounts.put(label, labelCounts.get(label) + 1);
                }
                
                int notInDict = uniqueLemmas.size();
                for (String lemma : uniqueLemmas) {
                    if (dictionary.containsKey(lemma)) notInDict--;
                }
                
                // Display report
                JFrame reportFrame = new JFrame("Vocabulary Report - " + title);
                reportFrame.setSize(600, 500);
                reportFrame.setLocationRelativeTo(this);
                
                StringBuilder report = new StringBuilder();
                report.append("Article: " + title + "\n\n");
                report.append("=== Basic Statistics ===\n");
                report.append("Total words: " + totalWords + "\n");
                report.append("Unique words: " + uniqueWords + "\n\n");
                
                report.append("=== Vocabulary Level Distribution ===\n");
                for (Map.Entry<String, Integer> entry : levelCounts.entrySet()) {
                    report.append(entry.getKey() + ": " + entry.getValue() + " words\n");
                }
                report.append("Not in dictionary: " + notInDict + " words\n\n");
                
                report.append("=== Your Label Status ===\n");
                report.append("Known: " + labelCounts.get("known") + " words\n");
                report.append("Unknown: " + labelCounts.get("unknown") + " words\n");
                report.append("Uncertain: " + labelCounts.get("uncertain") + " words\n");
                report.append("Unlabeled: " + labelCounts.get("unlabeled") + " words\n");
                
                JTextArea textArea = new JTextArea(report.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
                reportFrame.add(new JScrollPane(textArea));
                reportFrame.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void readArticle() {
        int row = articleTable.getSelectedRow();
        if (row < 0) return;
        
        int id = (int) tableModel.getValueAt(row, 0);
        
        try {
            PreparedStatement ps = dbConnection.prepareStatement("SELECT * FROM articles WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String title = rs.getString("title");
                String content = rs.getString("content");
                
                // Get saved position
                int savedPos = 0;
                PreparedStatement ps2 = dbConnection.prepareStatement("SELECT position FROM reading_progress WHERE article_id=?");
                ps2.setInt(1, id);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    savedPos = rs2.getInt("position");
                }
                
                new ReadingWindow(id, title, content, savedPos, dbConnection, pipeline, dictionary, userWords, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showLabeledWords() {
        JFrame frame = new JFrame("My Labeled Words");
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(this);
        
        JPanel topPanel = new JPanel();
        JComboBox<String> filterCombo = new JComboBox<>(new String[]{"All", "Known", "Unknown", "Uncertain"});
        JButton exportBtn = new JButton("Export to CSV");
        topPanel.add(new JLabel("Filter:"));
        topPanel.add(filterCombo);
        topPanel.add(exportBtn);
        
        String[] columns = {"Lemma", "Label"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable table = new JTable(model);
        
        Runnable refresh = () -> {
            model.setRowCount(0);
            String filter = (String) filterCombo.getSelectedItem();
            for (Map.Entry<String, String> entry : userWords.entrySet()) {
                if (filter.equals("All") || entry.getValue().equalsIgnoreCase(filter)) {
                    model.addRow(new Object[]{entry.getKey(), entry.getValue()});
                }
            }
        };
        
        filterCombo.addActionListener(e -> refresh.run());
        refresh.run();
        
        exportBtn.addActionListener(e -> exportToCSV());
        
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        frame.setVisible(true);
    }
    
    private void exportToCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File dir = fc.getSelectedFile();
                
                // Export three files
                String[] labels = {"known", "unknown", "uncertain"};
                for (String label : labels) {
                    File file = new File(dir, label + "_words.csv");
                    try (PrintWriter pw = new PrintWriter(file)) {
                        pw.println("lemma");
                        for (Map.Entry<String, String> entry : userWords.entrySet()) {
                            if (entry.getValue().equals(label)) {
                                pw.println(entry.getKey());
                            }
                        }
                    }
                }
                
                JOptionPane.showMessageDialog(this, "Exported to:\n" + dir.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
            }
        }
    }
}
