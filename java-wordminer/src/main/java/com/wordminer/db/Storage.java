package com.wordminer.db;

import com.wordminer.dictionary.DictionaryEntry;
import com.wordminer.model.Article;
import com.wordminer.model.ArticleSummary;
import com.wordminer.model.TokenStat;
import com.wordminer.model.VocabStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * JDBC-backed storage layer mirroring the original SQLite schema.
 */
public final class Storage implements AutoCloseable {

    private final Path dbPath;
    private final Connection connection;

    public Storage(Path dbPath) {
        try {
            Class.forName("org.sqlite.JDBC");
            this.dbPath = dbPath;
            Path parent = dbPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            this.connection.setAutoCommit(true);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA journal_mode = WAL;");
            }
            initSchema();
        } catch (Exception e) {
            throw new StorageException("Unable to initialize storage", e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS articles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        path TEXT,
                        content TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    );
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS article_tokens (
                        article_id INTEGER NOT NULL,
                        lemma TEXT NOT NULL,
                        word_count INTEGER NOT NULL,
                        difficulty TEXT NOT NULL,
                        PRIMARY KEY (article_id, lemma),
                        FOREIGN KEY (article_id) REFERENCES articles(id) ON DELETE CASCADE
                    );
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS vocab_status (
                        lemma TEXT PRIMARY KEY,
                        status TEXT NOT NULL CHECK (status IN ('mastered','learning','unfamiliar'))
                    );
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS preferences (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    );
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tokens_article ON article_tokens(article_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tokens_lemma ON article_tokens(lemma);");
        }
    }

    public int addArticle(String title, Path path, String content) {
        String sql = "INSERT INTO articles(title, path, content, created_at) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, path == null ? null : path.toString());
            ps.setString(3, content);
            ps.setString(4, OffsetDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new StorageException("Failed to create article");
        } catch (SQLException e) {
            throw new StorageException("Unable to add article", e);
        }
    }

    public List<ArticleSummary> listArticles() {
        String sql = "SELECT id, title, path, created_at FROM articles ORDER BY id DESC";
        List<ArticleSummary> articles = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                articles.add(new ArticleSummary(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("path"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to list articles", e);
        }
        return articles;
    }

    public Optional<Article> getArticle(int articleId) {
        String sql = "SELECT id, title, path, content, created_at FROM articles WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Article(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("path"),
                            rs.getString("content"),
                            rs.getString("created_at")
                    ));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new StorageException("Unable to load article", e);
        }
    }

    public void upsertArticleTokens(int articleId, Map<String, Integer> counts, Map<String, DictionaryEntry> dictionary) {
        String sql = """
                INSERT INTO article_tokens(article_id, lemma, word_count, difficulty)
                VALUES (?,?,?,?)
                ON CONFLICT(article_id, lemma)
                DO UPDATE SET word_count=excluded.word_count, difficulty=excluded.difficulty;
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String lemma = entry.getKey();
                int count = entry.getValue();
                DictionaryEntry info = dictionary == null ? null : dictionary.get(lemma);
                String difficulty = info == null ? "Unknown" : info.level();
                ps.setInt(1, articleId);
                ps.setString(2, lemma);
                ps.setInt(3, count);
                ps.setString(4, difficulty);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new StorageException("Unable to persist article tokens", e);
        }
    }

    public Map<String, Integer> getArticleStats(int articleId) {
        String sql = "SELECT difficulty, SUM(word_count) as total FROM article_tokens WHERE article_id=? GROUP BY difficulty";
        Map<String, Integer> stats = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("difficulty"), rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load article stats", e);
        }
        return stats;
    }

    public List<TokenStat> getArticleVocab(int articleId) {
        String sql = """
                SELECT lemma, word_count, difficulty
                FROM article_tokens
                WHERE article_id=?
                ORDER BY word_count DESC
                LIMIT 500
                """;
        List<TokenStat> tokens = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, articleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tokens.add(new TokenStat(
                            rs.getString("lemma"),
                            rs.getInt("word_count"),
                            rs.getString("difficulty")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load vocabulary list", e);
        }
        return tokens;
    }

    public Map<String, Integer> crossOverlap(List<Integer> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        String sql = "SELECT lemma FROM article_tokens WHERE article_id=?";
        List<Set<String>> sets = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Integer id : articleIds) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    Set<String> set = new HashSet<>();
                    while (rs.next()) {
                        set.add(rs.getString("lemma"));
                    }
                    sets.add(set);
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to compute overlap", e);
        }
        if (sets.isEmpty()) {
            return Map.of();
        }
        Set<String> union = new HashSet<>(sets.get(0));
        Set<String> intersection = new HashSet<>(sets.get(0));
        for (int i = 1; i < sets.size(); i++) {
            union.addAll(sets.get(i));
            intersection.retainAll(sets.get(i));
        }
        Map<String, Integer> result = new HashMap<>();
        result.put("unique", union.size());
        result.put("shared", intersection.size());
        return result;
    }

    public Optional<VocabStatus> getStatus(String lemma) {
        String sql = "SELECT status FROM vocab_status WHERE lemma=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lemma);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(VocabStatus.fromDb(rs.getString("status")));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new StorageException("Unable to lookup status", e);
        }
    }

    public void setStatus(String lemma, VocabStatus status) {
        String sql = "REPLACE INTO vocab_status(lemma, status) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, lemma);
            ps.setString(2, status.dbValue());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new StorageException("Unable to update status", e);
        }
    }

    public Map<String, VocabStatus> getAllStatuses() {
        String sql = "SELECT lemma, status FROM vocab_status";
        Map<String, VocabStatus> statuses = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String lemma = rs.getString("lemma");
                VocabStatus status = VocabStatus.fromDb(rs.getString("status"));
                if (status != null) {
                    statuses.put(lemma, status);
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to load vocabulary statuses", e);
        }
        return statuses;
    }

    public List<String> listVocab(VocabStatus status) {
        String sql = "SELECT lemma FROM vocab_status WHERE status=? ORDER BY lemma ASC";
        List<String> words = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.dbValue());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    words.add(rs.getString("lemma"));
                }
            }
        } catch (SQLException e) {
            throw new StorageException("Unable to list vocabulary", e);
        }
        return words;
    }

    public void backupTo(Path destination) {
        try {
            if (destination == null) {
                throw new StorageException("Destination path is required for backup");
            }
            Path parent = destination.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(dbPath, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Unable to backup database", e);
        }
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new StorageException("Unable to close database", e);
        }
    }
}
