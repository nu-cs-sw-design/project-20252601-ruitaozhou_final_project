# CS396 Software Design Principles and Practices  
## Project: Milestone 1 Report  

**People**  
Ruitao Zhou  

**Option**  
Option 1  

**Link to Project GitHub Repository**  
https://github.com/nu-cs-sw-design/project-20252601-ruitaozhou_final_project.git  

---

## Project Planning  

### Product Name  
**WordMiner**

### Target User  
Chinese learners of English who wish to expand their vocabulary through contextual reading rather than rote memorization.

### Purpose of the Product  
The purpose of **WordMiner** is to help Chinese learners overcome the common difficulties encountered when reading English articles—such as repeatedly forgetting unfamiliar words—and to gradually expand their vocabulary through continuous, contextual learning.  
The ultimate goal is to help users achieve fluent, barrier-free English reading comprehension.

When Chinese readers study English materials, they often encounter many unfamiliar words. Even after looking them up, these words are easily forgotten, and when the same words appear again in a new article, readers may still fail to recognize them.  
**WordMiner** addresses this challenge by recording all unfamiliar words that appear in the articles a user has read. When these words reoccur in future readings, the system automatically highlights them, allowing repeated exposure and reinforcement of memory.

In addition, the system identifies and summarizes words that appear frequently across multiple articles, helping users focus on the vocabulary that matters most.  
It also analyzes the overall vocabulary difficulty of each article, enabling users to select reading materials that best match their proficiency level—making the learning process more efficient, targeted, and personalized.

---

## Brief Description of Product Features  

**WordMiner** is a local desktop application (no internet required) that includes the following core functions:

1. **Article Import & Analysis**  
   - Users can import `.txt` English articles.  
   - The system automatically performs word segmentation, lemmatization, and difficulty classification based on built-in vocabulary databases (Junior / Senior / CET-4 / CET-6 / Postgraduate / TOEFL / SAT).

2. **Vocabulary Reports**  
   - For each imported article, users can view detailed reports showing total words, word frequency, and distribution across difficulty levels.

3. **Reading Mode with Word Highlighting**  
   - Articles are displayed with color-coded highlighting for known, learning, and unfamiliar words.  
   - Clicking a word displays its meaning, example phrases, and allows marking the word as *mastered*, *learning*, or *unfamiliar*.

4. **Cross-Article Analysis**  
   - Users can select multiple articles to compare vocabulary overlap, unique words, and shared expressions across readings.

5. **Personal Vocabulary Libraries**  
   - The system maintains three libraries (*mastered*, *learning*, *unfamiliar*), where users can browse, edit, or export their personalized word lists.

6. **Statistics & Visualization**  
   - Provides global learning analytics: total words read, vocabulary growth, difficulty distribution, and comprehension rate, displayed with charts and word clouds.

---

## Use Cases  

### UC1: Start the System  
**Actor:** User  
**Precondition:** The vocabulary database and user preference files exist in local storage.  

**Basic Flow:**  
1. The user launches the WordMiner application.  
2. The system displays the message “Welcome to WordMiner!”.  
3. The system loads the main dashboard, showing:  
   - The list of imported articles  
   - Buttons: “Import Article”, “View Vocabulary Reports”, “Read Article”, “Cross-Article Analysis”, “Review Vocabulary”, and “Backup Data”.  

**Postcondition:**  
The system is initialized and ready to accept user operations.  

**Special Requirements:**  
1. The database should automatically recover or rebuild indexes if the last session ended unexpectedly.  
2. The system must auto-save all configuration and user preferences at shutdown.

---

### UC2: Import a New Article  
**Actor:** User  
**Precondition:** The system is on the main dashboard.  

**Basic Flow:**  
1. The user selects “Import Article”.  
2. The system prompts the user to select a `.txt` file or paste text manually.  
3. The user provides the article text and an optional title or category tag.  
4. The system reads the content and performs:  
   - Tokenization and lemmatization using the NLP engine (e.g., spaCy).  
   - Stop-word removal and frequency analysis.  
   - Difficulty classification of each word (Junior / Senior / CET-4 / CET-6 / Postgraduate / TOEFL / SAT).  
5. The system saves the article and its vocabulary statistics to the database.  
6. The system displays a summary report, e.g.:  
   “Article imported successfully — 865 words processed, 95 new words identified.”  

**Exception Flow A:**  
4.a If the text is empty or unreadable, the system prompts the user to provide valid text.  

**Postcondition:**  
A new article and its associated vocabulary data are stored in the database.  

**Special Requirements:**  
- The import process should complete within 5 seconds for an average-length article.  
- If interrupted, partial results must not corrupt existing data.

---

### UC3: View Vocabulary Report for a Single Article  
**Actor:** User  
**Precondition:** At least one article has been imported.  

**Basic Flow:**  
1. The user selects “View Vocabulary Reports” on the main dashboard.  
2. The system displays a list of imported articles with word-count summaries.  
3. The user chooses an article.  
4. The system generates a detailed vocabulary report including:  
   - Total word count  
   - Number of unique words  
   - Distribution by difficulty level (Junior, Senior, CET-4, CET-6, Postgraduate, TOEFL, SAT)  
   - Percentage of mastered / unmastered / unfamiliar words.  

**Exception Flow:**  
3.a If the selected article no longer exists, the system notifies the user and refreshes the list.  

**Postcondition:**  
The user views a detailed vocabulary analysis of a specific article.  

**Special Requirements:**  
The report generation must not exceed 3 seconds.

---

### UC4: Read an Article and Mark Words  
**Actor:** User  
**Precondition:** The article has been analyzed and stored.  

**Basic Flow:**  
1. The user opens the “Read Article” module and selects an article.  
2. The system displays the article text in the reading interface with color highlighting:  
   - **Blue:** unfamiliar words  
   - **Yellow:** learning in progress  
   - **Gray:** mastered words  
3. The user clicks on a word.  
4. The system shows:  
   - The word’s definition and part of speech (from local JSON dictionary)  
   - Example phrases and their translations  
   - Buttons: “Mark as Mastered”, “Mark as Unfamiliar”, “Mark as Learning”.  
5. The user chooses one of the status options.  
6. The system updates the word’s status in the user vocabulary database.  
7. The reading progress (scroll position, statistics) is automatically saved.  

**Exception Flow:**  
4.a If the clicked token is punctuation or not found in the dictionary, show “No data available.”  

**Postcondition:**  
The user’s word-status database is updated, and reading progress is saved.  

**Special Requirements:**  
- The UI should support keyboard shortcuts for marking words quickly.  
- The dictionary lookup must occur locally to ensure offline usability.

---

### UC5: Cross-Article Vocabulary Analysis  
**Actor:** User  
**Precondition:** At least two articles exist in the database.  

**Basic Flow:**  
1. The user selects “Cross-Article Analysis”.  
2. The system prompts the user to choose two or more articles.  
3. The system performs comparison operations:  
   - Identify common words (intersection).  
   - Identify unique words (difference).  
   - Generate shared-word frequency tables.  
4. The system displays:  
   - A Venn diagram showing overlap.  
   - Statistical summaries of shared vs. unique vocabulary.  
   - Optional export of intersection/difference lists.  

**Alternate Flow:**  
3.a If articles have no shared vocabulary, system displays “No overlapping words found.”  

**Postcondition:**  
A comparison report of selected articles is displayed or exported.  

**Special Requirements:**  
- All operations must run entirely offline.  
- Computation time should remain under 3 seconds for typical article lengths (<5,000 tokens each).

---

### UC6: Manage Vocabulary Libraries  
**Actor:** User  
**Precondition:** User has interacted with at least one article and marked words.  

**Basic Flow:**  
1. The user selects “Vocabulary Library” from the main dashboard.  
2. The system displays three categorized tabs:  
   - “Mastered Words”  
   - “Learning Words”  
   - “Unfamiliar Words”  
3. Each tab lists words with attributes:  
   - Word  
   - Difficulty level  
   - Total occurrences  
   - Last appearance (article name)  
4. The user can:  
   - Search for a specific word.  
   - Move a word between categories.  
   - Delete or export word lists.  
5. The system immediately reflects updates in the database.  

**Alternate Flow:**  
2.a If a category is empty, display “No words in this list yet.”  

**Postcondition:**  
User’s vocabulary libraries are updated and consistent with database records.  

**Special Requirements:**  
- Changes must persist instantly to prevent data loss.  
- Export format options: CSV / JSON.

---

**GitHub Repository:**  
https://github.com/nu-cs-sw-design/project-20252601-ruitaozhou_final_project.git  
