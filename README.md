# Omni Search

**Omni Search** is a flexible, extensible, and entity-based search abstraction for Java 21+. It allows developers to implement dynamic querying, filtering, sorting, and pagination on entities without boilerplate code.

This library consists of a **core module** defining the search contract and shared utilities, and **JPA implementation module** based on JPA Criteria API and RSQL.

---

## ✨ Features

- Generic interface for reusable search logic
- Filtering via [RSQL](https://github.com/jirutka/rsql-parser)
- Sorting and pagination support
- Extensible with custom implementations (e.g., MongoDB, Elasticsearch, etc.) (future feature)
- Java 21 compatible

---

## 🧠 How It Works

### Core Interface

```java
public interface OmniSearch {
    <E> List<E> search(Class<E> entityClass, OmniSearchOptions options);

    <E> long count(Class<E> entityClass, OmniSearchBaseOptions options);
}
```

You can implement this interface using your own data source. For example, with JPA, MongoDB, or custom storage engines.

---

## 📦 JPA Implementation

The `omni-search-jpa` module provides a ready-to-use implementation using JPA Criteria API and RSQL.

### Usage Example

```java
import com.peluware.omnisearch.jpa.*;

EntityManager entityManager = ...;
OmniSearch search = new JpaOmniSearch(entityManager);

List<User> users = search.search(User.class, opts -> {
    opts.setSearch("john");
    opts.setSort(Sort.by("lastName", true));
    opts.setPagination(new Pagination(0, 20));
});
```

---

## 🔧 Installation

Make sure your `pom.xml` contains the following dependencies:

### Core Module

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>omni-search-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### JPA Module

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>omni-search-jpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 🧩 Custom Implementations

You can implement your own version of `OmniSearch` by providing logic for `search()` and `count()` operations against your data source.

Example:

```java
import com.peluware.omnisearch.core.*;

public class MyCustomSearch implements OmniSearch {
    public <E> List<E> search(Class<E> clazz, OmniSearchOptions options) {
        // implement search logic
    }

    public <E> long count(Class<E> clazz, OmniSearchBaseOptions options) {
        // implement count logic
    }
}
```

---

## 📜 License

Apache 2.0 License. See `LICENSE` file for details.

---

## 👥 Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.
