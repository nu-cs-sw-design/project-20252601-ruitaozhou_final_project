1. Encapsulation
Complies.
Classes across Presentation, Domain, and Datasource keep fields private and expose only necessary getters/setters. Repositories also encapsulate data-source details and hide persistence mechanisms.

2. Delegation
Complies.
Presentation delegates business logic to Services, and Services delegate data access to Repositories. Repositories further delegate data access to Loaders/Savers. ViewFactory centralizes view creation.

3. Information hiding
Complies.
Presentation does not know DTO structures or database details; Repositories hide data-source logic. Domain entities is package private and expose getters for legitimate domain behavior.

4. encapsulate what varies
Complies.
Different data loaders and savers (article, dictionary, user words) are encapsulated behind the DBLoader/DBSaver/FileLoader interfaces. DTO–entity conversions (article, dictionary, user words) are encapsulated in Mapper interface, isolating change.

5. Favor Composition over Inheritance
Complies.
The architecture uses interfaces and “has-a” relationships rather than inheritance. Functionality is built through composition (services → repositories → loaders).

6. Program to Interface, not Implementation
Partially complies.
Loaders, Savers, and Mappers are accessed via interfaces.
Repositories themselves are concrete classes rather than interfaces, so Services depend on implementations. And Services are also concrete classes. Using concrete Service and Repository classes reduces extensibility but keeps the design simpler and avoids unnecessary abstraction.

7. Loosely Coupled Designs
Partially complies.
Services depend on multiple repositories because article analysis requires article data, dictionary data and user word data. This increases coupling but reflects real domain requirements.

8. Hollywood Principle
Complies.
Presentation calls Services, Services call Repositories, but lower layers never call back upward. There are no reverse dependencies. For the "service -> repository -> entity", there is also no reverse dependencies.

9. Principle of Least Knowledge
Mostly complies.
Presentation interacts only with Services, not with Repositories or data-source objects. Views occasionally access Article objects directly for simple design.

10. Single Responsibility Principle（SRP）
Mostly Complies.
Entities store domain data, Repositories manage collections, Services coordinate business logic, and Loaders/Savers handle persistence. Although Services are somewhat "large", their responsibilities but reflects real domain requirements

11. Open/Closed Principle（OCP）
Partially complies.
New Loaders, Savers, Mappers, and Views can be added without modifying existing code due to interface-based design. Services and Repositories may require modification when business logic expands, but this for simple design now.

12. Liskov Substitution Principle（LSP）
Complies.
All Loader/Saver implementations follow their interfaces consistently, and Mapper implementations can replace each other without breaking behavior.

13. Interface Segregation Principle（ISP）
The Mapper interface only handles conversion between each DTO and its respective entity. Instead of creating one massive DataAccessor, the design separates responsibilities into Loader and Saver, with Loader further split into DBLoader and FileLoader—DBLoader handles database I/O, while FileLoader is responsible for reading files.

14. Dependency Inversion Principle（DIP）
Partially complies.
Repositories depend on abstractions (Loaders/Savers/Mappers interfaces) rather than concrete data-source classes.
Services depend on concrete repository classes rather than repository interfaces, and the presentation also depend on concrete service, so DIP is only partially met. This is to achieve a simpler design.

15. Low Coupling and High Cohesion
Partially complies.
The Services expose many public methods and depend on multiple repositories, which increases coupling and decreases chohesion. Separating Service, Repository, and Entity into distinct classes introduces more classes, but it improves organization and supports clearer business logic. Repository methods are kept package-private, which helps limit the public API surface and reduce unnecessary coupling.


---
# Software Requirement Changes in the Future

1. Introduce tagging or categorization for articles and vocabulary.

    **How the design handles it:**  
    New fields can be added to the corresponding entities, and Repositories can persist them without major structural changes. UI only requires additional filters or displays. This change is easily supported by the current architecture.

2. Support exporting data in multiple output formats instead of only CSV.

    **How the design handles it:**  
    Additional FileSaver implementations (e.g., JSONSaver, PDFSaver) can be added as needed. The Presentation layer can introduce new onExportTo handlers, and VocabularyService can provide an exportLabeledWordsTo(format) function to support multiple export formats.

3. Add a cross-article comparison interface.

    **How the design handles it:**  
    A new View and a new MultiArticleService can be added to perform multi-article analysis. However, the current ArticleService already mixes CRUD and analysis logic, so adding cross-article features may require refactoring. The change is partially supported.

4. Add user accounts and per-user article libraries.

    **How the design handles it:** 
    The current model is single-user and does not attach user identity to Articles or Vocabulary states. Supporting multi-user functionality would require modifying domain entities, Repositories, and Services. This change is not naturally supported by the existing design.

5. Convert the application into a browser extension that analyzes articles directly on web pages.

    **How the design handles it:**  
    The core domain logic and Services could be reused, but the Presentation layer and FileLoader mechanisms would require substantial redesign. The current desktop-oriented UI and file-based import model do not translate directly to a browser extension environment. This change is poorly supported without major architectural adjustments.