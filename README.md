# Explore-It: Enterprise Spring Boot Search Engine & GUI Client

A modern, high-performance client-server application built with **Spring Boot** and **Java Swing (FlatLaf)**. This project transforms a basic desktop application into a robust, secure, and optimized enterprise-ready backend system by leveraging REST APIs, database logging, in-memory caching, and build automation.

---

## Architecture & Data Flow

The project operates under a decoupled **Client-Server Architecture**. Below is the flow of how search requests are processed and optimized across the stack:

```
[ Swing GUI Client ]
       │
       ▼ (REST API HTTP GET /api/search)
[ Spring Boot Controller (SearchController) ]
       │
       ├─► [ JPA Database Logs (H2 Database) ] ──► (Persisted search log history)
       │
       ▼
[ Spring Cache Layer (executeSearch) ]
       │
       ├──► (Cache Hit)  ──► [ Return cached JSON response instantly ]
       │
       └──► (Cache Miss) ──► [ Call Google Custom Search API ] ──► [ Cache & Return Response ]
```

### Request Lifecycle:
1. **Search Initiation**: The user enters a search query in the Swing desktop client.
2. **REST API Invocation**: The client issues an asynchronous HTTP GET request to the local Spring Boot backend server (`http://localhost:8080/api/search`).
3. **Database Logging**: The server intercepts the request and asynchronously logs the search query, type, and timestamp into the **H2 Database** using **Spring Data JPA** for auditing.
4. **Cache Lookup**: The server checks the **Spring Cache** manager.
   * **Cache Hit**: If the query has been searched recently, the backend instantly returns the cached JSON results.
   * **Cache Miss**: If it's a new query, the backend securely signs the request with the Google Custom Search credentials and queries Google's APIs. The returned results are cached and sent back to the client.
5. **UI Rendering**: The Swing client receives the clean JSON, parses it, and renders Google-like styled search cards in modern HTML with interactive hyperlinks.

---

## Core Features

* 🔐 **Secure Backend Credentials**: Google Search API Keys and Engine IDs are configured securely in the backend (`application.properties`), protecting private keys from client exposure.
* ⚡ **Performance Optimization (Spring Cache)**: Prevents rate-limiting issues by caching search results, drastically reducing response latency for repeated queries.
* 🗄️ **Query Persistence**: Leverages **Spring Data JPA** and **H2 In-Memory Database** to automatically persist search records.
* 🖥️ **Modern Desktop Client**: Styled with the modern flat dark theme (**FlatLaf**), featuring a history sidebar, SafeSearch controls, Web/Image toggles, and dynamic Look-and-Feel switching.
* 📦 **Standard Build Tooling**: Uses **Maven** for complete dependency management and project build execution.

---

## Tech Stack

* **Java Baseline**: Java 17+ (JDK 25 compatible)
* **Backend Framework**: Spring Boot 3.3.0
* **Database & ORM**: Spring Data JPA / Hibernate
* **Database Engine**: H2 (In-memory, embedded)
* **Caching Engine**: Spring Cache (ConcurrentMapCacheManager)
* **GUI Look & Feel**: FlatLaf Dark & Light Theme
* **JSON Parser**: org.json library
* **Build System**: Apache Maven 3.9+

---

## REST API Documentation

### 1. Perform Search
* **Endpoint**: `GET /api/search`
* **Query Parameters**:
  * `q` (required): Search keyword
  * `type` (optional): `web` (default) or `image`
  * `safe` (optional): `active` or `off` (default)
* **Response**: Google Custom Search JSON string payload.

### 2. Fetch Search Logs
* **Endpoint**: `GET /api/history`
* **Response**: Returns a JSON Array containing the top 10 unique recent search terms ordered by timestamp descending.
  ```json
  ["Spring Boot", "Java Backend Developer", "Here Technologies"]
  ```

### 3. Clear Logs
* **Endpoint**: `DELETE /api/history`
* **Response**: `{"message":"History cleared successfully"}`

### 4. Dynamic Credentials configuration
* **Endpoint**: `POST /api/config`
* **Query Parameters**: `apiKey`, `searchId`
* **Response**: Returns the updated credentials map.

---

## Getting Started & Run Instructions

### Prerequisites
Make sure you have **Java 17+** and **Maven** installed on your system. 
*(If on macOS, you can install Maven via Homebrew: `brew install maven`)*

### Running the Application

1. Open your terminal in the directory `GUI-BASED-SEARCH-ENGINE/`.
2. Build and compile the project using Maven:
   ```bash
   mvn clean compile
   ```
3. Run the Spring Boot server (which automatically boots the backend and opens the Swing Desktop Client):
   ```bash
   mvn spring-boot:run
   ```

### Accessing the Database Web Console
You can view the live database tables and search history logs directly in your browser:
* **H2 Console URL**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
* **JDBC URL**: `jdbc:h2:mem:searchenginedb`
* **Username**: `sa`
* **Password**: *(Leave blank)*
