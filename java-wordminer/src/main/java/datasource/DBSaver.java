package datasource;

public interface DBSaver {
    int save(String title, String content);
    boolean delete(int id);
}
