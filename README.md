# Ders Notları

Ders sırasında anlatılan konularla ilişkili haftalık notların yer aldığı repodur.

## Java Patterns and Practices

| **Hafta** | **Açıklama** | **Etiketler** |
| --- | --- | --- |
| [Hafta 01: Gitflow ve İlk İş Nesnesi](java-week1.md) | Gitflow branch stratejisi ve hotfix senaryosu. Northwind Customers tablosundan yola çıkarak POJO'dan doğrulama yapan, immutable bir Customer sınıfına geçiş. | `git-flow` `hotfix` `pojo` `immutability` `validation` |
| [Hafta 02: Git Pratikleri, Anemik Model ve Record'lar](java-week2.md) | Temel git komutlarıyla uçtan uca bir gitflow senaryosu. Anemik Product sınıfının sorunları, Address value object'i, class ile record karşılaştırması ve record'ların bytecode seviyesinde `invokedynamic` ile incelenmesi. | `git` `anemic-model` `value-object` `record` `equals-hashcode` `bytecode` |
| [Hafta 03: Money Value Object ve Rich Entity Tasarımı](java-week3.md) | `double/float` yuvarlama hataları ve BigDecimal. Money value object'i, OrderStatus, OrderLine ve Order ile zengin entity tasarımı, anemik tasarımlarla karşılaştırma, value object ve entity ayrımı. | `bigdecimal` `money-pattern` `value-object` `rich-entity` `ddd` |
| [Hafta 04: Aggregate Sınırları, State Machine ve Domain Exception'lar](java-week4.md) | Customer'ın entity olarak yeniden tasarımı, aggregate sınırları ve kimlik ile referans. OrderStatus enum'ı ile state machine, domain exception hiyerarşisi, JUnit ve AssertJ ile ilk birim testler. | `aggregate-root` `state-machine` `enum` `domain-exception` `junit` `assertj` |
| [Hafta 05: Optional, Stream API ve Sorgu Tasarımı](java-week5.md) | Optional sözleşmesi, Stream API'nin lazy yapısı ve dikkat edilmesi gerekenler. Order'a sorgu metotları ve in-memory OrderBook ile gruplama, raporlama ve birim testler. | `optional` `stream-api` `collectors` `query` `unit-test` |
| [Hafta 06: Yazılım Mimarileri Üzerine Yardımcı Notlar](java-week6.md) | Coupling, cohesion, test edilebilirlik gibi ölçütler. Katmanlı, SOA, mikroservis, hexagonal, onion/clean, vertical slice ve modüler monolit mimarilerinin karşılaştırılması ve karar matrisi. | `software-architecture` `layered` `microservices` `hexagonal` `clean-architecture` `modular-monolith` |
