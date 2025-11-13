package datasource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SQLiteArticleDAO implements DBLoader, DBSaver {
    private final SQLiteDataSource dataSource;
    
    public SQLiteArticleDAO(SQLiteDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public List<DBArticleDTO> loadAll() {
        List<DBArticleDTO> articles = new ArrayList<>();
        String sql = "SELECT id, title, content FROM articles";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                articles.add(new DBArticleDTO(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("content")
                ));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load articles", e);
        }
        
        return articles;
    }
    
    @Override
    public DBArticleDTO loadById(int id) {
        String sql = "SELECT id, title, content FROM articles WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new DBArticleDTO(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("content")
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load article by id: " + id, e);
        }
        
        return null;
    }
    
    @Override
    public int save(String title, String content) {
        String sql = "INSERT INTO articles (title, content) VALUES (?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save article", e);
        }
        
        throw new RuntimeException("Failed to get generated ID");
    }
    
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM articles WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete article", e);
        }
    }
}
