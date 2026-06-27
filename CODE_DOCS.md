# Project SwiftLite Code Documentation

## InputLogicHandler.java
This class handles core input logic for the keyboard. It manages character insertion, space handling, enter key actions, and deletions. It also coordinates with correction and suggestion logic, and includes a "googly eyes" easter egg triggered by specific text.

## SwiftLiteIME.java
The main Input Method Service for SwiftLite. It initializes the keyboard view, theme manager, suggestion engine, and other components. It handles keyboard events, shift/caps lock states, and manages different panels like emoji and clipboard. It also implements idle detection and interactions with the input connection.

## KeyboardView.java
The primary view container for the keyboard. It manages the suggestion bar, different input panels (keys, emoji, clipboard, numbers), and the "googly eyes" easter egg overlay. It handles layout construction, theme application, and coordinates panel switching and suggestion updates.

## SuggestionEngine.java
The engine responsible for providing text suggestions and predictions. It integrates with the Android Spell Checker service, manages a custom dictionary using memory-mapped files, and applies usage-based scoring and bigram models to rank suggestions. It also handles word corrections and learns from user input to improve future results.

## PredictionEngine.java
Provides next-word predictions based on the previous word. It prioritizes learned bigrams from user history, followed by logical predictions from static data, grammar-based fallbacks, and global usage patterns. It also ensures appropriate casing for the predicted words.

## CorrectionManager.java
Manages text corrections, including grammar fixes and spelling adjustments. it uses a proximity map of the keyboard layout to calculate spatial distances between typed characters and candidate words. It also handles common contractions and contextual corrections for ambiguous words like "were" versus "we're".

## ThemeManager.java
Handles the keyboard's visual themes and user preferences. It loads theme and accent color definitions from assets, manages shared preferences for settings like auto-correct and auto-capitalization, and constructs the current KeyboardTheme based on user selection.

## UsageManager.java
Tracks and persists user typing patterns to improve suggestions. It records category usage, individual word frequencies, and bigram relationships in shared preferences. It also manages word rejections to identify potential new user-defined words for the dictionary.

## DictionaryLoader.java
Manages the extraction and loading of the primary dictionary file from assets to internal storage. It categorizes words into various semantic groups and populates grammar sets (articles, pronouns, etc.) from static text files. It also integrates user-defined words from preferences into the active dictionary.

## MmapDictionary.java
Provides efficient read-only access to a binary dictionary file using memory-mapped buffers. It supports binary search for exact word matches and prefix searching for autocomplete. The class handles the dictionary's custom binary format, including header information and offset tables for word entries.

## InputCorrectionLogic.java
Handles the automatic correction of typed text. It detects when a word should be corrected based on the suggestion engine's output, manages the reverting of corrections if the user deletes them, and handles automatic apostrophe insertion for contractions. It also filters out fields where auto-correction should be disabled, such as URI or search fields.

## InputSuggestionLogic.java
Manages the retrieval and committing of word suggestions and predictions. It analyzes the text around the cursor to determine the current word being typed and the preceding word, then requests appropriate suggestions or next-word predictions from the engine. It also handles the logic for committing a selected suggestion and learning new bigrams from user selections.

## SuggestionUtils.java
Utility class for suggestion-related operations. It includes methods for calculating edit distances between words, checking keyboard adjacency of characters, matching the casing of replacement words to their originals, and identifying punctuation. It also provides helper functions for extracting words from text and filtering suggestions to fit within a specific layout width.

## SetupActivity.java
The main entry activity for configuring the keyboard. It provides a tabbed interface for setting up the keyboard service, customizing appearance and behavior through the ThemeManager, and testing the keyboard in a safe environment. It handles dynamic theme reloading and UI construction for the setup pages.

## ClipboardDao.java
Data Access Object for the clipboard database. It defines SQL queries for inserting, retrieving, and deleting clipboard items, including support for pinning items and pruning old unpinned entries.

## ClipboardItem.java
Represents a single entry in the clipboard history, supporting both text and image content. It includes fields for content, image URIs, pin status, and timestamps.

## ClipboardAdapter.java
RecyclerView adapter for displaying clipboard items in a list. It handles different view types for text and images, manages item clicks for committing content or pinning/deleting items, and includes logic for loading image thumbnails asynchronously.

## ClipboardDatabase.java
Room database configuration for clipboard storage. It initializes the database instance and manages schema migrations, such as adding support for pinned items and image URIs.

## ClipboardRepository.java
Abstraction layer for clipboard data operations. It coordinates between the DAO and the file system to manage clipboard items, perform deduplication based on content or file hashes, and clean up orphaned image files when entries are pruned.

## ClipboardShareActivity.java
Handles incoming share intents to add text or images directly to the clipboard history. It extracts content from the intent and updates the system clipboard, which is then captured by the keyboard's monitor.

## EmojiData.java
Manages the loading and filtering of emoji data from assets. It initializes emoji categories and tab icons from a JSON file and provides a filtering mechanism to exclude emojis that the system font cannot render.

## EmojiPanel.java
The primary UI component for selecting emojis. It features a tabbed interface for different emoji categories, including a "recents" tab. It handles tab switching, grid rendering, and coordinates with the skin tone popup manager for emojis that support variations.

## EmojiAdapter.java
GridView adapter for rendering individual emoji cells. It applies skin tone modifiers to supported emojis and handles touch events to trigger either a direct emoji commitment or the skin tone selection popup.

## EmojiSkinToneHelper.java
Utility class for managing emoji skin tone variations. It identifies emojis that support skin tones and applies the appropriate Unicode modifiers, including handling for Zero Width Joiner (ZWJ) sequences in complex human emojis.

## SkinTonePopupManager.java
Manages the display and interaction of the skin tone selection popup. It calculates the popup's position relative to the anchor emoji, handles sliding touch highlights, and notifies the adapter when a specific skin tone is selected.

## BaseKeyCanvas.java
An abstract base class for custom views that render keyboard layouts. It handles key measurement, hit testing, drawing of key backgrounds, labels, and icons, and provides common touch event handling including long-press and repeat-delete logic.

## Key.java
A simple data model representing a single key in a keyboard layout. It contains information about the key's code, labels, icons, position, and visual style flags.

## KeyIcons.java
Utility class for drawing various keyboard icons using vector paths. It includes logic for resolving context-sensitive icons like the Enter key and provides a centralized drawing method for all standard keyboard icons.

## ExtraIcons.java
Contains specialized drawing logic for more complex keyboard icons such as the settings gear, the app logo, and action icons like undo and search. It uses Canvas and Path APIs for precise rendering.

## IconButton.java
A simple custom view that displays a single icon from the KeyIcons set. It is primarily used for utility buttons in the suggestion bar.

## IconView.java
An extension of IconButton that includes an eye-tracking animation for the app logo icon. It uses a handler-based animation loop to move the pupils towards random targets.

## KeyVibrator.java
Centralizes haptic feedback logic for keyboard interactions. It defines different vibration durations based on the type of key pressed (action, utility, or normal).

## UndoManager.java
Implements a basic undo function by sending a system-level Ctrl+Z key combination. This allows the keyboard to trigger undo actions in applications that support standard shortcuts.

## PanelManager.java
Manages the switching and transition animations between different keyboard panels (alphabetical keys, numbers, emoji, and clipboard). It handles view creation, theme application, and memory management for secondary panels.

## NumbersCanvas.java
A specialized BaseKeyCanvas for rendering number and symbol layouts. It supports multiple pages of symbols and a dedicated numeric keypad mode, loading its layout definitions from a JSON asset.

## KeysCanvas.java
The primary BaseKeyCanvas for the main alphabetical keyboard. It handles dynamic layout adjustments for different field types (e.g., email, search) and manages complex long-press alternatives for letters and symbols.

## GooglyEyesView.java
A playful visual overlay that renders animated "googly eyes" over the suggestion bar. It uses AnimatorSet and POSES data to create smooth pupil movements and transitions.

## KeyPopupManager.java
Coordinates the display of long-press popups for keys with alternative characters or options. It manages popup positioning, highlight states during touch movement, and supports both standard row-based and scrollable multi-row popups.

## ClipboardMonitor.java
Background monitor that captures text and image copies from the system clipboard and MediaStore. It handles deduplication, image downscaling, and persistence via the ClipboardRepository.

## ClipboardPanelView.java
The UI container for the clipboard history panel. It features a list of recent clips with options to pin or delete them, and integrates with the keyboard's input logic to commit clips to the current field.

## ContractionHelper.java
Helper class for expanding common English contractions (e.g., "im" to "I'm"). It loads a contraction list from assets and provides matching logic that preserves the original casing.

## EmojiHistoryManager.java
Manages the persistence of recently used emojis in shared preferences. It keeps a fixed-size history and provides methods to track new usages and retrieve the list of recents.

## KeyPreviewManager.java
Handles the display of transient key previews (magnified bubbles) that appear above keys as they are pressed. It manages the popup's lifecycle and entrance/exit animations.

## PrivacyHandler.java
Utility for analyzing input field metadata (EditorInfo) to identify sensitive contexts. It helps the keyboard decide when to disable features like learning, suggestions, and clipboard monitoring for privacy protection.

## SuggestionBarView.java
The UI component located at the top of the keyboard that displays word suggestions, predictions, and clipboard clips. It also provides quick-access buttons for settings, emoji, and undo.

## SuggestionChipBuilder.java
A layout utility that populates the suggestion bar with individual suggestion "chips". It manages the creation and recycling of TextViews and applies appropriate styling based on the importance of each suggestion.

## PopupViewFactory.java
A factory for creating consistent UI layouts for keyboard popups. it constructs both single-row standard popups and multi-row scrollable containers for larger sets of options.

## SetupView.java
Provides the UI for the initial keyboard setup process. It guides the user through enabling the keyboard in system settings, selecting it as the default input method, and granting necessary storage permissions.

## TesterView.java
A utility view that allows users to test the keyboard's input and image-pasting capabilities. It includes a word-per-minute (WPM) counter and a preview area for images pasted from the clipboard.

## CustomizeView.java
The primary settings UI for the keyboard. It allows users to select visual themes, choose accent colors, toggle features like auto-correction and vibration, and adjust the keyboard's font size.

## ThemePickerView.java
Contains static methods to build theme and accent color selection grids. It provides a visual preview for each theme and handles the activity recreation needed to apply deep visual changes.

## UserDictionaryView.java
Provides a simple interface for managing the user's custom dictionary. it allows users to add new words that the suggestion engine should recognize and delete existing entries from their personal list.

## KeyboardTheme.java
An immutable data model representing a visual theme for the keyboard. It contains a full set of colors for keys, backgrounds, accents, and borders, along with a flag to identify if the theme is dark.

## UIUtils.java
A collection of static helper methods for UI-related tasks. It includes utilities for converting DP to pixels, creating rounded backgrounds, generating vertical spacers, and building stylized text labels.

## VibrationUtils.java
A utility class for managing device haptics. It defines standardized vibration durations for various keyboard events and provides a simple method to trigger a one-shot vibration effect.

## CompactDictionary.java
An in-memory representation of a dictionary designed for minimal memory footprint. It packs all words into a single string and uses offset arrays to locate individual entries, supporting binary search for fast lookups.

## DictWord.java
A simple container for a dictionary entry, consisting of the original word and its semantic category. it stores a lowercase version of the word only if it differs from the original to save memory.

## PredictionData.java
Loads and provides access to logical next-word predictions from a JSON asset. it maps common words to an array of likely following words to assist the prediction engine.

## SuggestionSearcher.java
Implements the core search logic for word suggestions. It performs prefix searches for autocomplete and fuzzy searches for error correction, ranking results using a combination of edit distance and usage-based scoring. It also integrates user-defined words into the search results.
