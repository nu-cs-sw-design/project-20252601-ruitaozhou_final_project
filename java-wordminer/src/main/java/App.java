import datasource.*;
import domain.ArticleDataMapper;
import domain.ArticleRepository;
import presentation.MainWindow;

import javax.swing.SwingUtilities;

/**
 * WordMiner Application Entry Point
 * 
 * 架构说明：
 * 1. DataSource Layer: 数据访问层（DTO、接口、实现）
 * 2. Domain Layer: 领域层（Article、Mapper、Repository）
 * 3. Presentation Layer: 表现层（MainWindow）
 * 
 * 依赖注入流程：
 * SQLiteDataSource → SQLiteArticleDAO → ArticleDataMapper → ArticleRepository → MainWindow
 */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. 初始化 DataSource 层
                SQLiteDataSource dataSource = SQLiteDataSource.getInstance();
                SQLiteArticleDAO dao = new SQLiteArticleDAO(dataSource);
                LocalFileLoader fileLoader = new LocalFileLoader();
                
                // 2. 初始化 Domain 层
                ArticleDataMapper mapper = new ArticleDataMapper();
                ArticleRepository repository = new ArticleRepository(
                    dao,        // DBLoader
                    dao,        // DBSaver
                    fileLoader, // FileLoader
                    mapper      // ArticleDataMapper
                );
                
                // 3. 初始化 Presentation 层
                MainWindow mainWindow = new MainWindow(repository);
                mainWindow.display();
                
                System.out.println("WordMiner MVP started successfully!");
                System.out.println("Loaded " + repository.count() + " articles from database.");
                
            } catch (Exception e) {
                System.err.println("Failed to start application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
