package domain;

// import java.sql.Timestamp;
// import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Article - Rich Domain Model (富领域模型)
 * 
 * 领域对象，包含：
 * 1. 数据（属性）
 * 2. 业务逻辑（行为方法）
 * 
 * 遵循 DDD 原则：领域模型应该是"充血模型"而非"贫血模型"
 */
public class Article {
    private int id;
    private String title;
    private String content;
    // private Timestamp importDate;

    // Constructors
    public Article() {}

    public Article(String title, String content) {
        this.title = title;
        this.content = content;
    }
    
    // Package-private constructor for Mapper use
    Article(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    // ============ Getters and Setters ============
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // public Timestamp getImportDate() {
    //     return importDate;
    // }

    // public void setImportDate(Timestamp importDate) {
    //     this.importDate = importDate;
    // }

    // ============ Business Logic Methods (领域行为) ============

    /**
     * 统计文章总词数
     */
    public int countWords() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    /**
     * 统计文章独立词数（去重）
     */
    public int countUniqueWords() {
        if (content == null || content.trim().isEmpty()) {
            return 0;
        }
        
        String[] words = content.toLowerCase().trim().split("\\s+");
        Set<String> uniqueWords = new HashSet<>();
        
        for (String word : words) {
            // Remove punctuation
            String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (!cleanWord.isEmpty()) {
                uniqueWords.add(cleanWord);
            }
        }
        
        return uniqueWords.size();
    }

    /**
     * 计算词汇丰富度
     * Vocabulary Richness = unique words / total words
     */
    public double calculateVocabularyRichness() {
        int total = countWords();
        if (total == 0) return 0.0;
        return (double) countUniqueWords() / total;
    }

    /**
     * 获取内容预览
     */
    public String getContentPreview(int length) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.length() > length 
            ? content.substring(0, length) + "..." 
            : content;
    }

    /**
     * 检查文章是否包含某个词
     */
    public boolean containsWord(String word) {
        if (content == null || word == null) {
            return false;
        }
        return content.toLowerCase().contains(word.toLowerCase());
    }

    /**
     * 统计某个词在文章中出现的频率
     */
    public int countWordFrequency(String targetWord) {
        if (content == null || targetWord == null) {
            return 0;
        }
        
        String[] words = content.toLowerCase().split("\\s+");
        String target = targetWord.toLowerCase();
        int count = 0;
        
        for (String word : words) {
            String cleanWord = word.replaceAll("[^a-zA-Z]", "");
            if (cleanWord.equals(target)) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * 验证文章数据的有效性
     */
    public boolean isValid() {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        if (title.length() > 255) {
            return false;
        }
        return true;
    }

    /**
     * 获取文章难度等级（基于词数）
     */
    public String getDifficultyLevel() {
        int wordCount = countWords();
        if (wordCount < 100) return "Easy";
        if (wordCount < 500) return "Medium";
        return "Hard";
    }

    @Override
    public String toString() {
        return "Article{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", words=" + countWords() +
                ", uniqueWords=" + countUniqueWords() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Article article = (Article) o;
        return id == article.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
