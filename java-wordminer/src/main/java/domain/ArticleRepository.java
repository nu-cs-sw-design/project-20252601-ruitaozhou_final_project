package domain;

import datasource.DBArticleDTO;
import datasource.DBLoader;
import datasource.DBSaver;
import datasource.FileLoader;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ArticleRepository - 管理所有 Article 实体的仓库
 * 
 * 职责：
 * 1. 初始化时从数据库载入数据 (DBLoader) → 通过 Mapper 转换为 Article 对象
 * 2. importArticle(path) 时：
 *    - FileLoader 读文件内容
 *    - DBSaver 存库（获得新 ID）
 *    - DBLoader 重新读取（确保 ID 正确）
 *    - Mapper 转换为 Article
 *    - 添加到 articles Map
 * 3. 删除文章时通知 datasource 删除数据库 (DBSaver)
 */
public class ArticleRepository {
    private final Map<Integer, Article> articles;
    private final DBLoader dbLoader;
    private final DBSaver dbSaver;
    private final FileLoader fileLoader;
    private final ArticleDataMapper mapper;
    
    public ArticleRepository(DBLoader dbLoader, DBSaver dbSaver, FileLoader fileLoader, ArticleDataMapper mapper) {
        this.dbLoader = dbLoader;
        this.dbSaver = dbSaver;
        this.fileLoader = fileLoader;
        this.mapper = mapper;
        this.articles = new HashMap<>();
        
        // 初始化：从数据库加载所有文章
        List<DBArticleDTO> dtos = dbLoader.loadAll();
        for (DBArticleDTO dto : dtos) {
            Article article = mapper.fromDTO(dto);
            articles.put(article.getId(), article);
        }
    }
    
    /**
     * 导入文章
     * @param path 文件路径
     * @return 是否导入成功
     */
    public boolean importArticle(Path path) {
        try {
            // 1. 读取文件内容
            String content = fileLoader.readFile(path);
            
            // 2. 提取标题（文件名去掉扩展名）
            String fileName = path.getFileName().toString();
            String title = fileName.contains(".") 
                ? fileName.substring(0, fileName.lastIndexOf('.'))
                : fileName;
            
            // 3. 保存到数据库，获得新 ID
            int newId = dbSaver.save(title, content);
            
            // 4. 从数据库重新读取（确保 ID 正确）
            DBArticleDTO dto = dbLoader.loadById(newId);
            
            // 5. 通过 Mapper 转换为 Article
            Article article = mapper.fromDTO(dto);
            
            // 6. 添加到内存管理
            articles.put(article.getId(), article);
            
            return true;
        } catch (Exception e) {
            System.err.println("Failed to import article: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Article> getAllArticles() {
        return new ArrayList<>(articles.values());
    }
    
    public Article findById(int id) {
        return articles.get(id);
    }
    
    public List<Article> findByTitle(String title) {
        return articles.values().stream()
            .filter(a -> a.getTitle().contains(title))
            .collect(Collectors.toList());
    }
    
    public boolean delete(int id) {
        if (articles.containsKey(id)) {
            boolean deleted = dbSaver.delete(id);
            if (deleted) {
                articles.remove(id);
                return true;
            }
        }
        return false;
    }
    
    public boolean exists(int id) {
        return articles.containsKey(id);
    }
    
    public int count() {
        return articles.size();
    }
}
