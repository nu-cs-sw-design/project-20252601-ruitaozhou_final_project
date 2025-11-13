package datasource;

public class DBArticleDTO {
    public int id;
    public String title;
    public String content;
    
    public DBArticleDTO(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }
}
