# Omni Search

**Omni Search** is a flexible, extensible, and entity-based search abstraction for Java 21+. It allows developers to implement dynamic querying, filtering, sorting, and pagination on entities without boilerplate code.

This library consists of a **core module** defining the search contract and shared utilities, and **JPA implementation module** based on JPA Criteria API and RSQL.

---

## ✨ Features

- Generic interface for reusable search logic
- Querying via [RSQL](https://github.com/jirutka/rsql-parser)
- Sorting and pagination support
- Extensible with custom implementations (e.g., MongoDB, Elasticsearch, etc.)
- Java 21 compatible

---

## 🧠 How It Works

### Core Interface

```java
package com.peluware.omnisearch;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;

import java.util.List;

public interface OmniSearch {

    <E> List<E> list(Class<E> entityClass, OmniSearchOptions options);

    <E> long count(Class<E> entityClass, OmniSearchBaseOptions options);
}
```

You can implement this interface using your own data source. For example, with JPA, MongoDB, or custom storage engines.

For reactive use cases, there is also a reactive variant returning Uni<T> from Mutiny:

```java
package com.peluware.omnisearch.reactive.multiny;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface MutinyOmniSearch {

    <E> Uni<List<E>> list(Class<E> entityClass, OmniSearchOptions options);

    <E> Uni<Long> count(Class<E> entityClass, OmniSearchBaseOptions options);
}
```

Or native reactive with `Flow.Publisher<T>`:

```java
package com.peluware.omnisearch.reactive;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;

import java.util.List;
import java.util.concurrent.Flow;

public interface ReactiveOmniSearch {

    <E> Flow.Publisher<List<E>> list(Class<E> entityClass, OmniSearchOptions options);

    <E> Flow.Publisher<Long> count(Class<E> entityClass, OmniSearchBaseOptions options);
}

```

---

### Usage Example

#### Imperative Example with JPA

```java
import com.peluware.omnisearch.jpa.*;

EntityManager entityManager = ...;
OmniSearch search = new JpaOmniSearch(entityManager);

Node query = new cz.jirutka.rsql.parser.RSQLParser().parse("age>25;name==*john*");

List<User> users = search.list(User.class, opts -> {
    opts.setSort(Sort.by("lastName", Order.Direction.ASC));
    opts.setPagination(Pagination.of(0, 20));
    opts.setQuery(query);
});
```

#### Reactive Example with Reactive Hibernate

```java
import com.peluware.omnisearch.hibernate.reactive.*;
import org.hibernate.reactive.mutiny.Mutiny;
import io.smallrye.mutiny.Uni;

Mutiny.SessionFactory sessionFactory = ...;
MutinyOmniSearch search = new HibernateOmniSearch(sessionFactory);

Uni<List<User>> users = search.list(User.class, opts -> {
    opts.setSearch("john"); // Intelligent search across multiple fields
    opts.setSort(Sort.by("lastName", Order.Direction.ASC));
    opts.setPagination(Pagination.of(0, 20));
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
    <version>2.1.0</version>
</dependency>
```

### JPA Module

The `omni-search-jpa` module provides a ready-to-use implementation using JPA Criteria API and RSQL.

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>omni-search-jpa</artifactId>
    <version>2.1.0</version>
</dependency>
```

### MongoDB Module

The `omni-search-mongodb` module provides a ready-to-use implementation using MongoDB with POJO entities.

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <artifactId>omni-search-mongodb</artifactId>
    <version>2.1.0</version>
</dependency>
```

## Reactive Hibernate Module

The `omni-search-hibernate-reactive` module provides a ready-to-use implementation using Reactive Hibernate dependency.

```xml

<dependency>
    <groupId>com.peluware</groupId>
    <module>omni-search-hibernate-reactive</module>
    <version>2.1.0</version>
</dependency>
```

---

## 🧩 Custom Implementations

You can implement your own version of `OmniSearch` by providing logic for `search()` and `count()` operations against your data source.

Example:

```java
import com.peluware.omnisearch.OmniSearch;

public class MyCustomSearch implements OmniSearch {
    public <E> List<E> list(Class<E> clazz, OmniSearchOptions options) {
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
