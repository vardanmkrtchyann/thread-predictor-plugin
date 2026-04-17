# AI Thread Predictor - IntelliJ IDEA Plugin

An AI-powered IntelliJ IDEA plugin that acts as a deterministic JVM execution engine. It analyzes multithreaded Java code directly within the IDE, identifies race conditions, and maps thread interleaving to predict exact STDOUT outputs or final variable states.

## 🚀 Features

* **Context-Aware UI (Zero Clutter):** Instead of spamming the user's right-click context menu, the plugin uses native JetBrains Gutter Icons (Lightning Bolt). The icon only appears next to valid concurrency triggers (e.g., `.start()`, `.submit()`, `synchronized`).
* **Deep Context Extraction:** When triggered, the plugin extracts not just the isolated lambda or method, but the entire enclosing `PsiClass`. This ensures the AI has full visibility into shared state (like class-level `volatile` fields) required for accurate concurrency analysis.
* **Asynchronous Processing:** Network calls to the LLM are strictly dispatched to background threads, guaranteeing zero IDE UI freezes.

## 🧠 Technical Decisions & Architecture

To align with JetBrains' plugin development best practices, several specific architectural choices were made:

### 1. AST/PSI Traversal > String Matching
To detect multithreaded code and place the Gutter Icon, the plugin does *not* rely on brittle Regex or string matching. Instead, it utilizes the Program Structure Interface (PSI).
* It uses `JavaRecursiveElementWalkingVisitor` to traverse the AST.
* It specifically targets `PsiMethodCallExpression` nodes matching threading signatures.
* **Deep Resolution:** It resolves `PsiReferenceExpression` nodes to check if any variables accessed inside a method belong to fields declared with the `PsiModifier.VOLATILE` keyword. 

### 2. Event Dispatch Thread (EDT) Safety
Large Language Model inference introduces high latency.
* All network requests are wrapped in `Task.Backgroundable` to push the heavy lifting off the main UI thread.
* Once the HTTP response is parsed, the plugin uses `ApplicationManager.getApplication().invokeLater()` to safely synchronize back to the EDT to render the `NotificationGroupManager` balloon.

### 3. Model Selection & Prompt Engineering
* **Model:** Powered by Google's **Gemini 2.5 Flash** API. This specific model was chosen over heavier models (like Pro) to optimize for low latency, which is critical for a responsive IDE developer experience.
* **Prompting:** The system prompt forces the LLM out of a "conversational" state and strictly bounds it to act as a deterministic execution engine, returning heavily constrained, HTML-formatted thread interleaving traces (limited to 15 words) rather than verbose explanations.

## 🛠️ Setup & Installation

### Prerequisites
* IntelliJ IDEA (Community or Ultimate) 2023.2+
* Java 17+
* A Google Gemini API Key

### Configuration
1. Clone this repository.
2. Open the project in IntelliJ IDEA and allow Gradle to sync.
3. Open `src/main/java/com/github/vardanmkrtchyann/threadpredictorplugin/services/GeminiService.java`.
4. Locate the `API_KEY` constant and paste your Gemini API key (Note: In a production environment, this would be migrated to the JetBrains `PasswordSafe` secure credential store).

### Running the Plugin
1. Open the Gradle tool window.
2. Navigate to `Tasks` -> `intellij` -> `runIde`.
3. Double-click to launch the Sandbox IDE.

## 💡 Usage

1. Inside the Sandbox IDE, open or create a Java file.
2. Write a multithreaded execution block (e.g., `new Thread(() -> System.out.println("Hello")).start();`).
3. An icon will automatically appear in the left-hand gutter next to the `.start()` call.
4. Click the icon. 
5. A progress bar will appear at the bottom of the IDE, followed by a notification balloon detailing the Thread Safety status, precise output predictions, and the execution trace.
