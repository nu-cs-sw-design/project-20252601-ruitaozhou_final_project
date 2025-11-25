### Product Name  
**WordMiner**

### Target User  
Chinese learners of English who wish to expand their vocabulary through contextual reading rather than rote memorization.

### Purpose of the Product  
**WordMiner** is designed to help Chinese English learners read English articles more effectively and confidently.

First, learners commonly encounter many unfamiliar words during reading. Manually keeping track of these words is time-consuming, and even after looking them up, they are often forgotten and unrecognized the next time they appear. **WordMiner** addresses this by allowing users to mark words directly during reading—such as known, unknown, or uncertain. These annotations are automatically saved, and when the user reads a new article, previously marked words are highlighted again to reinforce recognition.

Second, learners often cannot accurately judge the difficulty of an English article, especially the number of unfamiliar words it contains. WordMiner provides a reading report that analyzes vocabulary difficulty and highlights the proportion of new or challenging words, helping users choose materials that match their proficiency.

Ultimately, WordMiner aims to support Chinese learners in achieving smooth, barrier-free English reading through continuous exposure, intelligent word tracking, and personalized difficulty analysis.

---

## Brief Description of Product Features  

**WordMiner** is a local desktop application (no internet required) that includes the following core functions:

1. **Article Import & Analysis**  
    - Users can import `.txt` English articles.  
    - The system analyzes each article by calculating:
        - Total word count
        - Unique word count
        - Vocabulary-level distribution based on built-in word lists (Junior, Senior, CET-4, CET-6, Postgraduate, TOEFL, SAT)
    - A single word may belong to multiple vocabulary lists (e.g., both Senior and CET-4), and some words may not appear in any list.
    Therefore, the sum of words across all vocabulary categories may not equal the total number of unique words in the article.

2. **Article Delete**
    - Users can delete previously imported articles from the system.

3. **Vocabulary Reports**  
    - For each imported article, users can access a detailed vocabulary report showing:
        - Total word count
        - Unique word count (lemma)
        - Distribution of words across built-in vocabulary levels (Junior, Senior, CET-4, CET-6, Postgraduate, TOEFL, SAT, and words not appearing in any list)
    - The number of words that the user has previously labeled as known, unknown, uncertain, or unlabeled in their personal vocabulary database

4. **Reading Mode with Word Highlighting**  
    - Articles are displayed with color-coded highlights indicating whether each word is labeled as known, unknown, uncertain, or unlabeled.

5. **Word Lookup and Labeling While Reading**
    - When reading an article, users can click on any word to view its stored meaning (if available).
    - Users can update the word’s status by labeling it as known, unknown, or uncertain.
    - All labeled words are stored persistently and shared across all articles, allowing consistent tracking of vocabulary knowledge.

6. **View Personal Labeled Words**
    - Users can view all words they have labeled across articles.
    - Words can be filtered and viewed by category, such as known, unknown, and uncertain.

7. **Back up Labeled words**
    - Users can export their labeled vocabulary into three separate .csv files corresponding to the known, unknown, and uncertain categories.
---

## Use Cases  

### UC1: Start the System  
**Actor:** User  
**Precondition:** Required JSON files for built-in vocabulary lists exist in local storage.

**Basic Flow:**  
1. The user launches the WordMiner application.
2. The system checks whether the built-in vocabulary lists (Junior, Senior, CET-4, CET-6, Postgraduate, TOEFL, SAT) are already stored in the local database.
3. If any vocabulary list is missing (e.g., on first launch), the system loads the corresponding JSON files from local storage and inserts them into the database.
4. The system then loads the main dashboard, displaying:
    - The list of imported articles
    - Buttons: "Import Article", "Article Report", "Read Article", "Labeled Words" (“Article Report” and “Read Article” become available only after the user selects an article from the list.)

Postcondition:

**Postcondition:**  
The system is fully initialized, with built-in vocabulary lists loaded into the database and the dashboard ready for user operation. 

---

### UC2: Import a New Article
**Actor:** User  
**Precondition:** The system is on the main dashboard.  

**Basic Flow:**  
1. The user selects "Import Article."
2. The system prompts the user to choose a .txt file.
3. The system reads the file content and performs the following processing steps:
    - Tokenization and lemmatization using the local NLP engine (e.g. Stanford CoreNLP).
    - Stop-word removal and frequency analysis.
    - Difficulty classification for each word based on built-in vocabulary lists (Junior, Senior, CET-4, CET-6, Postgraduate, TOEFL, SAT, and words not appearing in any list).
4. The system saves the processed article and its computed vocabulary statistics to the database.
5. After successful import, the system displays a summary message including:
    - Total word count
    - Unique word count (lemma)
    - Vocabulary-level distribution (including "not appearing in any list")
    - **Important note:**
        - A single word may belong to multiple vocabulary lists (e.g., both Senior and CET-4).
        - Some words may not appear in any list.
        - Therefore, the sum of category counts may not equal the total number of unique words.

**Exception Flow A:**  
4.a If the text is empty or unreadable, the system prompts the user to provide valid text.  

**Postcondition:**  
A new article and all associated vocabulary analysis data are stored in the local database.

---

### UC3: Delete an Existing Article
**Actor:** User  
**Precondition:**
- The system is on the main dashboard.
- At least one article exists in the article list.

**Basic Flow:**
1. The user selects an article from the article list on the main dashboard.
2. The user clicks the "Delete Article" button.
3. The system displays a confirmation dialog:
"Are you sure you want to delete this article? This action cannot be undone."
4. The user confirms the deletion.
5. The system removes the selected article and its associated metadata from the database.
6. The system refreshes the article list to reflect the deletion.
7. The system displays a success message, e.g.:
"Article deleted successfully."

**Alternate Flow A — User Cancels Deletion:**  
4.a The user clicks "Cancel" in the confirmation dialog.  
5 The system returns to the main dashboard with no changes.  

**Alternate Flow B — User Close the Confirmation Dialog:**  
4.b The user close the confirmation dialog.  
5. The system returns to the main dashboard with no changes. 

**Postcondition:**  
- If confirmed, the selected article is fully removed from the database and no longer appears in the article list.
- If canceled, no changes are made to the system.

---

### UC4: View Vocabulary Report for a Single Article  
**Actor:** User  
**Precondition:** At least one article has been imported.  

**Basic Flow:**  
1. The system select an article on main dashboard.  
2. The user click "View Vocabulary Reports" on the main dashboard.  
3. The system opens a separate report window and generates a detailed vocabulary report for the selected article, including: 
    - Total word count
    - Unique word count (lemma)
    - Vocabulary-level distribution (including "not appearing in any list")
    - Number of words labeled as known
    - Number of words labeled as uncertain
    - Number of words labeled as unknown

**Postcondition:**  
The user views a detailed vocabulary analysis of a specific article.  

---

### UC5: Read an Article (Viewing with Word Highlighting)
**Actor:** User  
**Precondition:** The selected article has been imported.  

**Basic Flow:**  
1. The user selects an article from the article list on the main dashboard.
2. The user clicks "Read Article".
3. The system opens a separate reading window, designed for comfortable reading (proper font, spacing, and layout).
4. The system displays the article text in the reading window with color-coded word highlighting:
    - Soft Red: unknown words
    - Orange: uncertain words
    - Gray: known words
    - Black: unlabeled words
5. The user scrolls through and reads the article.
6. The system automatically saves reading progress (e.g., scroll position).

**Postcondition:**  
The article is displayed in a dedicated reading window with appropriate vocabulary highlighting, and the user’s reading progress is saved.

**Special Requirements:**
- When the user opens the same article again, the system should automatically jump to the last saved reading position.

---

### UC6: Label a Word While Reading
**Actor:** User  
**Precondition:** The user is currently reading an article in the “Read Article” window.  

**Basic Flow:**  
1. The user clicks on a word in the reading interface.
2. The system identifies the lemma of the clicked surface word (e.g., went → go, children → child).
3. The system displays a popup containing:
    - The lemma (base form) as the main entry
    - Example phrases and translations (if available)
    - Current label status: known, unknown, uncertain, or unlabeled
    - Buttons for labeling: "Mark as Known", "Mark as Unknown", "Mark as Uncertain"
4. The user selects a label for the word.
5. The system updates the word's status in the user vocabulary database under the word’s lemma.
6. The system refreshes the highlight color of all occurrences of that lemma in the article.
7. The updated label is stored persistently and will be consistently applied to all future articles containing the same lemma.

**Exception Flow:**  
2.a If the lemma cannot be found in the local dictionary:  
    - The system still displays the popup  
    - No definition or example sentences are   
    - The user may still label the word using the available label buttons

**Postcondition:**   
The selected word’s lemma is updated in the user’s vocabulary database, and the new label is successfully saved.

Special Requirements:
- Label updates must be stored instantly to avoid data loss.
- The system must always display the lemma as the main dictionary entry, even when the user clicks an inflected form (e.g., clicking "went" shows "go").

---

### UC7: Manage Labeled Words  
**Actor:** User  
**Precondition:** User has interacted with at least one article and marked words.  

**Basic Flow:**  
1. The user selects "Labeled Words" from the main dashboard.
2. The system opens a separate window showing all labeled words across articles.
3. The system displays the labeled words in a structured list/table, showing only:
    - The lemma (base form)
4. The system provides a filter panel that allows the user to view words by category:
    - Known words only
    - Unknown words only
    - Uncertain words only
    - All labeled words
5. The user selects a filter option, and the system updates the list accordingly. 

**Postcondition:**
The user can view and filter all labeled words stored in the database.

---
### UC8: Export Labeled Words

**Actor:** User

**Precondition:**  
- The user is viewing the “Labeled Words” window.
- At least one labeled word exists in the vocabulary database.

**Basic Flow:**  
1. The user clicks the “Export” button in the Labeled Words window.
2. The system generates three separate .csv files, corresponding to:
    - known_words.csv
    - unknown_words.csv
    - uncertain_words.csv
3. Each CSV file contains only one column:  
    lemma (base form) for each word in that category
4. The system prompts the user to select a directory to save the files.
5. The user selects a save location.  
6. The system exports the three CSV files to the chosen directory and displays a success message such as:
"Export completed successfully."

**Alternate Flow A — No Labeled Words:**  
1.a If no words exist in a category, the corresponding CSV file is still created but remains empty.

**Postcondition:**
Three CSV files representing known, unknown, and uncertain labeled words are generated and saved to the user-selected location.

