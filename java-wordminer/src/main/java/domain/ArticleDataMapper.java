package domain;

import datasource.DBArticleDTO;

public class ArticleDataMapper {
    public Article fromDTO(DBArticleDTO dto) {
        return new Article(dto.id, dto.title, dto.content);
    }
}
