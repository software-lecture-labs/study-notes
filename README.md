<a id="top"></a>

# Ders Notları

Ders sırasında anlatılan konularla ilişkili haftalık notların yer aldığı repodur.

## Java Patterns and Practices

| **Bölüm** | **Açıklama** | **Etiketler** |
| --- | --- | --- |
| [Bölüm 01: Gitflow ve İlk İş Nesnesi](java-chapter-01.md) | Gitflow branch stratejisi ve hotfix senaryosu. Northwind Customers tablosundan yola çıkarak POJO'dan doğrulama yapan, immutable bir Customer sınıfına geçiş. | `git-flow` `hotfix` `pojo` `immutability` `validation` |
| [Bölüm 02: Git Pratikleri, Anemik Model ve Record'lar](java-chapter-02.md) | Temel git komutlarıyla uçtan uca bir gitflow senaryosu. Anemik Product sınıfının sorunları, Address value object'i, class ile record karşılaştırması ve record'ların bytecode seviyesinde `invokedynamic` ile incelenmesi. | `git` `anemic-model` `value-object` `record` `equals-hashcode` `bytecode` |
| [Bölüm 03: Money Value Object ve Rich Entity Tasarımı](java-chapter-03.md) | `double/float` yuvarlama hataları ve BigDecimal. Money value object'i, OrderStatus, OrderLine ve Order ile zengin entity tasarımı, anemik tasarımlarla karşılaştırma, value object ve entity ayrımı. | `bigdecimal` `money-pattern` `value-object` `rich-entity` `ddd` |
| [Bölüm 04: Aggregate Sınırları, State Machine ve Domain Exception'lar](java-chapter-04.md) | Customer'ın entity olarak yeniden tasarımı, aggregate sınırları ve kimlik ile referans. OrderStatus enum'ı ile state machine, domain exception hiyerarşisi, JUnit ve AssertJ ile ilk birim testler. | `aggregate-root` `state-machine` `enum` `domain-exception` `junit` `assertj` |
| [Bölüm 05: Optional, Stream API ve Sorgu Tasarımı](java-chapter-05.md) | Optional sözleşmesi, Stream API'nin lazy yapısı ve dikkat edilmesi gerekenler. Order'a sorgu metotları ve in-memory OrderBook ile gruplama, raporlama ve birim testler. | `optional` `stream-api` `collectors` `query` `unit-test` |
| [Bölüm 06: Yazılım Mimarileri Üzerine Yardımcı Notlar](java-chapter-06.md) | Coupling, cohesion, test edilebilirlik gibi ölçütler. Katmanlı, SOA, mikroservis, hexagonal, onion/clean, vertical slice ve modüler monolit mimarilerinin karşılaştırılması ve karar matrisi. | `software-architecture` `layered` `microservices` `hexagonal` `clean-architecture` `modular-monolith` |
| [Bölüm 07: Typed Identity, Static Factory, Factory ve Builder ile Nesne Üretimi](java-chapter-07.md) | Typed identity, static factory, factory ve builder pattern'leri ile nesne üretimi. | `typed-identity` `static-factory` `factory` `builder` `pattern` |
| [Bölüm 08: Strategy, Specification, Repository ve Mapper kavramları](java-chapter-08.md) | İndirim politikaları için strateji deseni, kuralları veri haline getirmek için specification deseni, veri erişimi için repository deseni ve nesne dönüşümleri için mapper kavramları. | `strategy-pattern` `discount-policy` `lambda` `specification-pattern` `repository-pattern` `mapper` |
| [Bölüm 09: Maven Modülleri ve ArchUnit Kullanımı](java-chapter-09.md) | Maven modüllerinin kullanımı ve ArchUnit ile mimari kuralların test edilmesi. | `maven` `modular-project` `archunit` |
| [Bölüm 10: Uygulama Servisleri ve Manuel Dependency Injection](java-chapter-10.md) | Yazma yönündeki kullanım senaryolarının `OrderService` sınıfına taşınması, constructor injection, composition root ve `Clock` enjeksiyonu. CDI *(Context and Dependency Injection)* sisteminin neyi otomatikleştirdiğini görmek için aynı yapının sıfırdan kurgulanması. | `application-service` `use-case` `dependency-injection` `composition-root` `clock` `cdi` |
| [Bölüm 11: Domain Events ve Observer Deseni](java-chapter-11.md) | Aggregate'te gerçekleşen değişikliklerin ilgili taraflara duyurulması, domain event kavramı ve observer deseni. | `domain-event` `observer-pattern` `event-driven` |
| [Bölüm 12: Persistence Boundary, JDBC ve Flayway](java-chapter-12.md) | Persistence boundary kavramı, JDBC ile veri erişimi ve Flayway ile veritabanı migration yönetimi. | `persistence-boundary` `jdbc` `flayway` |
