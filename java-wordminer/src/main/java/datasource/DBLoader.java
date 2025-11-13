package datasource;

import java.util.List;

public interface DBLoader {
    List<DBArticleDTO> loadAll();
    DBArticleDTO loadById(int id);
}
