package presentation;

import domain.Article;
import domain.ArticleRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * MainWindow - Main UI for WordMiner
 * 表现层：负责用户界面和交互
 */
public class MainWindow extends JFrame {

    private JTable articleTable;
    private DefaultTableModel tableModel;
    private ArticleRepository articleRepository;

    public MainWindow(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("WordMiner MVP");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Top panel with buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton importBtn = new JButton("Import Article");
        importBtn.addActionListener(e -> onImportArticle());
        topPanel.add(importBtn);
        
        JButton deleteBtn = new JButton("Delete Article");
        deleteBtn.addActionListener(e -> onDeleteArticle());
        topPanel.add(deleteBtn);
        
        add(topPanel, BorderLayout.NORTH);

        // Center panel with article table
        tableModel = new DefaultTableModel(new Object[]{"ID", "Title", "Word Count", "Unique Words"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        articleTable = new JTable(tableModel);
        articleTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        articleTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        articleTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        articleTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        add(new JScrollPane(articleTable), BorderLayout.CENTER);

        // Status bar
        JLabel statusBar = new JLabel("Ready | Total articles: " + articleRepository.count());
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);

        refreshArticleList();
    }

    public void display() {
        setVisible(true);
    }

    private void onImportArticle() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }
            
            @Override
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });
        
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            Path path = file.toPath();
            
            boolean success = articleRepository.importArticle(path);
            
            if (success) {
                refreshArticleList();
                
                // 获取最新导入的文章并显示统计信息
                List<Article> articles = articleRepository.getAllArticles();
                if (!articles.isEmpty()) {
                    Article lastArticle = articles.get(articles.size() - 1);
                    JOptionPane.showMessageDialog(this, 
                        "Successfully imported: " + lastArticle.getTitle() + "\n" +
                        "Total words: " + lastArticle.countWords() + "\n" +
                        "Unique words: " + lastArticle.countUniqueWords() + "\n" +
                        "Richness: " + String.format("%.2f%%", lastArticle.calculateVocabularyRichness() * 100),
                        "Import Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to import article: " + file.getName(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onDeleteArticle() {
        int selectedRow = articleTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select an article to delete",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int articleId = (int) tableModel.getValueAt(selectedRow, 0);
        String title = (String) tableModel.getValueAt(selectedRow, 1);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete:\n" + title + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = articleRepository.delete(articleId);
            if (success) {
                refreshArticleList();
                JOptionPane.showMessageDialog(this, 
                    "Article deleted successfully",
                    "Delete Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to delete article",
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshArticleList() {
        List<Article> articles = articleRepository.getAllArticles();
        tableModel.setRowCount(0);

        for (Article article : articles) {
            tableModel.addRow(new Object[]{
                article.getId(), 
                article.getTitle(), 
                article.countWords(),
                article.countUniqueWords()
            });
        }
    }
}
