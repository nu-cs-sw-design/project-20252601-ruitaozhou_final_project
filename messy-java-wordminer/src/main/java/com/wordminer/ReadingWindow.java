package com.wordminer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Reading window for displaying and interacting with articles
 */
class ReadingWindow extends JFrame {
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private int articleId;
    private Connection dbConnection;
    private StanfordCoreNLP pipeline;
    private Map<String, DictEntry> dictionary;
    private Map<String, String> userWords;
    
    public ReadingWindow(int id, String title, String content, int savedPos, 
                         Connection dbConnection, StanfordCoreNLP pipeline,
                         Map<String, DictEntry> dictionary, Map<String, String> userWords,
                         JFrame parent) {
        this.articleId = id;
        this.dbConnection = dbConnection;
        this.pipeline = pipeline;
        this.dictionary = dictionary;
        this.userWords = userWords;
        
        setTitle("Reading: " + title);
        setSize(900, 700);
        setLocationRelativeTo(parent);
        
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Georgia", Font.PLAIN, 16));
        
        // Add click listener
        textPane.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int pos = textPane.viewToModel2D(e.getPoint());
                String word = getWordAt(pos);
                if (word != null && !word.isEmpty()) {
                    showWordPopup(word);
                }
            }
        });
        
        scrollPane = new JScrollPane(textPane);
        add(scrollPane);
        
        // Display with highlighting
        displayContent(content);
        
        // Restore position
        SwingUtilities.invokeLater(() -> {
            scrollPane.getVerticalScrollBar().setValue(savedPos);
        });
        
        // Save position on scroll
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!e.getValueIsAdjusting()) {
                savePosition(scrollPane.getVerticalScrollBar().getValue());
            }
        });
        
        setVisible(true);
    }
    
    private void displayContent(String content) {
        StyledDocument doc = textPane.getStyledDocument();
        
        try {
            doc.remove(0, doc.getLength());
            
            // Styles
            Style defaultStyle = textPane.addStyle("default", null);
            StyleConstants.setForeground(defaultStyle, Color.BLACK);
            
            Style knownStyle = textPane.addStyle("known", null);
            StyleConstants.setForeground(knownStyle, Color.GRAY);
            
            Style unknownStyle = textPane.addStyle("unknown", null);
            StyleConstants.setForeground(unknownStyle, new Color(200, 50, 50));
            
            Style uncertainStyle = textPane.addStyle("uncertain", null);
            StyleConstants.setForeground(uncertainStyle, new Color(200, 100, 0));
            
            // Process with NLP
            CoreDocument coreDoc = new CoreDocument(content);
            pipeline.annotate(coreDoc);
            
            int lastEnd = 0;
            for (CoreLabel token : coreDoc.tokens()) {
                // Add text between tokens
                int start = token.beginPosition();
                if (start > lastEnd) {
                    doc.insertString(doc.getLength(), content.substring(lastEnd, start), defaultStyle);
                }
                
                String word = token.word();
                String lemma = token.lemma().toLowerCase();
                
                Style style = defaultStyle;
                if (word.matches("[a-zA-Z]+")) {
                    String label = userWords.getOrDefault(lemma, "unlabeled");
                    switch (label) {
                        case "known": style = knownStyle; break;
                        case "unknown": style = unknownStyle; break;
                        case "uncertain": style = uncertainStyle; break;
                    }
                }
                
                doc.insertString(doc.getLength(), word, style);
                lastEnd = token.endPosition();
            }
            
            // Add remaining text
            if (lastEnd < content.length()) {
                doc.insertString(doc.getLength(), content.substring(lastEnd), defaultStyle);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getWordAt(int pos) {
        try {
            StyledDocument doc = textPane.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            
            int start = pos;
            int end = pos;
            
            while (start > 0 && Character.isLetter(text.charAt(start - 1))) start--;
            while (end < text.length() && Character.isLetter(text.charAt(end))) end++;
            
            if (start < end) {
                return text.substring(start, end);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void showWordPopup(String word) {
        // Get lemma
        CoreDocument doc = new CoreDocument(word);
        pipeline.annotate(doc);
        String lemma = doc.tokens().get(0).lemma().toLowerCase();
        
        // Create popup
        JDialog dialog = new JDialog(this, "Word: " + word, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Info area
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        StringBuilder info = new StringBuilder();
        info.append("Word: " + word + "\n");
        info.append("Lemma: " + lemma + "\n\n");
        
        if (dictionary.containsKey(lemma)) {
            DictEntry entry = dictionary.get(lemma);
            info.append("Levels: " + String.join(", ", entry.levels) + "\n\n");
            
            Map<String, Object> data = entry.data;
            if (data.containsKey("translations")) {
                info.append("Translations:\n");
                List<Map<String, String>> trans = (List<Map<String, String>>) data.get("translations");
                for (Map<String, String> t : trans) {
                    info.append("  " + t.get("type") + ": " + t.get("translation") + "\n");
                }
                info.append("\n");
            }
            
            if (data.containsKey("phrases")) {
                info.append("Example Phrases:\n");
                List<Map<String, String>> phrases = (List<Map<String, String>>) data.get("phrases");
                int count = 0;
                for (Map<String, String> p : phrases) {
                    if (count++ >= 5) break;
                    info.append("  " + p.get("phrase") + " - " + p.get("translation") + "\n");
                }
            }
        } else {
            info.append("Not found in dictionary\n");
        }
        
        String currentLabel = userWords.getOrDefault(lemma, "unlabeled");
        info.append("\nCurrent Label: " + currentLabel);
        
        infoArea.setText(info.toString());
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        // Buttons
        JPanel btnPanel = new JPanel();
        JButton knownBtn = new JButton("Mark Known");
        JButton unknownBtn = new JButton("Mark Unknown");
        JButton uncertainBtn = new JButton("Mark Uncertain");
        
        knownBtn.addActionListener(e -> { updateLabel(lemma, "known"); dialog.dispose(); });
        unknownBtn.addActionListener(e -> { updateLabel(lemma, "unknown"); dialog.dispose(); });
        uncertainBtn.addActionListener(e -> { updateLabel(lemma, "uncertain"); dialog.dispose(); });
        
        btnPanel.add(knownBtn);
        btnPanel.add(unknownBtn);
        btnPanel.add(uncertainBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }
    
    private void updateLabel(String lemma, String label) {
        try {
            PreparedStatement ps = dbConnection.prepareStatement(
                "INSERT OR REPLACE INTO user_words (lemma, label, date) VALUES (?,?,?)"
            );
            ps.setString(1, lemma);
            ps.setString(2, label);
            ps.setString(3, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ps.executeUpdate();
            
            userWords.put(lemma, label);
            
            // Re-display content
            try {
                PreparedStatement ps2 = dbConnection.prepareStatement("SELECT content FROM articles WHERE id=?");
                ps2.setInt(1, articleId);
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    int pos = scrollPane.getVerticalScrollBar().getValue();
                    displayContent(rs.getString("content"));
                    SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(pos));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void savePosition(int pos) {
        try {
            PreparedStatement ps = dbConnection.prepareStatement(
                "INSERT OR REPLACE INTO reading_progress (article_id, position) VALUES (?,?)"
            );
            ps.setInt(1, articleId);
            ps.setInt(2, pos);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
