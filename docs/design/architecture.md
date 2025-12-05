# Quality Attributes
1. **Performance**  
    - **Latency:** When the user imports a .txt article, the full import and analysis process should finish within 3 seconds.

2. **Usability**
    - **Feedback:** When the user scrolls through an article in the reading window, the system must render text and vocabulary highlights without lag.

    **Learnability:** The user interface and core functions should be simple and intuitive, allowing users to start reading, labeling words, and accessing reports with minimal learning effort.

3. **Reliability**  
    **Data Integrity:** When the user labels a word as known, unknown, or uncertain, the system must immediately persist the change to the local database to prevent any data loss.

    **Fault Tolerance:** If the application crashes unexpectedly, all previously imported articles and labeled vocabulary should remain intact and fully recoverable when the user restarts the application.

4. **Portibility:** 
    **Data Exportability:** The user should be able to export all labeled words as CSV files so the vocabulary data can be used outside the application.

# Ranking of Quality Attributes
1. **Feedback (H, M):** High importance to users, Medium technical risk. Because smooth scrolling and instant highlight updates directly determine reading comfort, but UI optimization is manageable with proper rendering techniques.

2. **Fault Tolerance (H, M):** High importance to users, Medium technical risk. Because preserving data after unexpected crashes is crucial for trust, while implementing safe recovery and durable storage requires attention but is feasible with local persistence mechanisms.

3. **Data Integrity (M, M):** Medium importance to users, Medium technical risk. Because users expect their labeled words to be saved correctly, and ensuring consistent, immediate database writes requires careful handling but no complex algorithms.

4. **Latency (M, M):** Medium importance to users, Medium technical risk. Because fast article import improves user flow but is not as critical as reading performance, and achieving acceptable speed depends on optimizing NLP steps without introducing heavy complexity.

5. **Learnability(M, L):** Medium importance to users, Low technical risk. Because an intuitive interface supports ease of adoption, and creating a simple, clear UI layout is relatively easy and requires minimal technical effort.

6. **Data Exportability (M, L):** Medium importance to users, Low technical risk. Because exporting labeled words is helpful for using vocabulary in other tools, and generating CSV files is straightforward with very low implementation complexity.


# Architectural Design Pattern: Three Principal Layer
WordMiner uses a **Three Principal Layer** architectural pattern:

- **Presentation Layer:**
Handles all UI elements such as the dashboard, reading window, and word popups. It displays information and captures user actions.

- **Application Layer:**
Contains the core logic for importing articles, generating reports, labeling words, and exporting data. It coordinates workflow between UI and data layers.

- **Data / Infrastructure Layer:**
Manages SQLite storage to store the imported articles, dictionaries, and labeled words.
