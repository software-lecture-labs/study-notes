<a id="top"></a>

# Bölüm 12: Persistence Boundary, JDBC ve Flayway

<!-- nav:top:start -->
[← Bölüm 11](java-chapter-11.md#top) · [İçindekiler](README.md#top)
<!-- nav:top:end -->

Geldiğimiz nokta itibariyle epeyce ilerledik. Ancak şu ana kadarki tüm siparişler bellekte yönetildi *(in-memory)* ve kalıcı olarak saklanmıyor. Uygulamamız kapandığında veriler kayboluyor ve **SequentialOrderIdGenerator** her açılışta 10248 gibi sabit rakamdan saymaya başlıyor. Diğer yandan birde sorunumuz var; **InMemoryOrderRepository** sınıfı nesneleri referans yoluyla sakladığı için servis içinden yapılan **save** çağrısını silsek bile bütün testler geçecektir *(Bunu deneyelim)* Yani elimizdeki in-memory repository gerçek bir veritabanının en temel davranışlarından birini taklit etmiyor.

İşte bu bölümde repository sözleşmelerine dokunmadan, onların arkasına JDBC ile çalışan implementasyonlar yerleştireceğiz. Genelde bu gibi çözümlerde JPA *(Java Persistence API)* kullanılıyor ve kullanmayı tercih edeceğiz ancak bu bölümde bilinçli olarak daha alt seviyede, saf JDBC *(Java Database Connectivity)* ile çalışacağız. Amacımız JPA'nın bizim yerimize neleri yaptığını neleri kolaylaştırdığını fark edebilmek. Ağırlıklı olarak aşağıdaki sorulara cevap bulmaya çalışacağız.

- Bir aggregate ilişkisel tablolarla nasıl eşlenir? Aggregate ve transaction sınırları *(boundary)* arasında nasıl bir ilişki vardır?
- Veritabanından okunan bir siparişi, durum geçiş kurallarını tekrar çalıştırmadan ve olay üretmeden nasıl yeniden oluşturabiliriz?
- Veritabanı şemasını kim, nasıl ve hangi sırayla oluşturur? *(Flyway)*
- Aynı repository sözleşmesine uyan üç farklı implementasyonun gerçekten aynı şekilde davrandığından nasıl emin olabiliriz?

## Schema Kavramı ve Flyway Aracı

Genel bir felsefe olarak işe ilk önce tabloları tasarlayarak başlayacağız. **Northwind** veritabanındaki `Orders` ve `Order Details` tablolarını hatırlayalım. Bizim modelimizde de bir sipariş *(aggregate root)* ve ona ait sipariş kalemleri *(OrderLine)* yer alıyor. Bu yapıyı üç tablo ile ifade edebiliriz, sipariş veren müşteriler için `customers`,siparişler için `orders` ve sipariş kalemleri için `order_lines`. Kolon adlarını kobay **Northwind** veritabanımızdan, tipleriyse domain modellerinden alıyoruz.

Veritabanı şemasını elle oluşturmak yerine **Flyway** aracını kullanacağız. **Flyway**, veritabanı şemasını versiyonlanmış SQL script'leri ile yöneten bir araçt. Her betik *(script)* bir versiyon numarası taşır ve hangi betiğin hangi veritabanında çalıştırıldığı bilgisi de `flyway_schema_history` isimli tabloda tutulur. Uygulama açıldığında henüz çalıştırılmamış betikler sırasıyla çalıştırılır. Bunu `C#` tarafındaki Entity Framework Migrations ya da DbUp'a benzetebiliriz. Fark şu ki, EF Core migration'ları genellikle model sınıflarından üretilir *(Code First veya Model First yaklaşımını araştıralım)* Java tarafında yaygın olan kültürse SQL'in elle yazılması yönündedir. Başlangıçtaki SQL betiklerini db şemasının sahibi olan modülün kaynaklarına ekleyerek ilerleyelim. Yani sales modülündeki resources klasörüne aşağıdaki gibi ekleyeceğiz.

> Apache NetBeans IDE'sinde çalışıyorsak `Weel 12 / sales` modülünü açtıktan sonra modül adına sağ tıklayıp yeni Folder eklememiz gerekiyor. Folder adını `db/migration/sales`, Parent klasörünü ise `src/main/resources` olarak belirleyebiliriz. Bu işlem sonrasında proje ağacında `sales/src/main/resources/db/migration/sales` yolunu görmüş oluruz. İçerisine `Empty File` ekleyerek `V1__create_sales_tables.sql` ve `V2__create_order_id_sequence.sql` dosyalarını oluşturabiliriz.

```text
sales/src/main/resources/db/migration/sales/
├── V1__create_sales_tables.sql
└── V2__create_order_id_sequence.sql
```

![Week 12_00 - Flyway Migration Folder](./images/week_12_00.png)

Buradaki isimlendirmeler tesadüfi değildir. Dosya adındaki `V1__` öneki versiyonu, iki alt çizgiden sonraki kısım ise açıklamayı ifade eder. Path bilgisinde yer alan `sales` ifadesi bilinçli bir tercihtir. Birden fazla modül olduğunda her modülün kendi migration'larını kendi klasör sisteminde tutmasında yarar var. Bu sayede modüller bağımsız olarak evrilebilir ve migration'lar birbirini etkilemez.

```sql
CREATE TABLE customers (
    customer_id   CHAR(5)      NOT NULL,
    company_name  VARCHAR(40)  NOT NULL,
    contact_name  VARCHAR(30),
    street        VARCHAR(60)  NOT NULL,
    city          VARCHAR(15)  NOT NULL,
    postal_code   VARCHAR(10)  NOT NULL,
    country       VARCHAR(15)  NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
);

CREATE TABLE order_statuses (
    status  VARCHAR(16) NOT NULL,
    CONSTRAINT pk_order_statuses PRIMARY KEY (status)
);

INSERT INTO order_statuses (status) VALUES ('DRAFT'), ('CONFIRMED'), ('SHIPPED'), ('CANCELLED');

CREATE TABLE orders (
    order_id          INTEGER      NOT NULL,
    customer_id       CHAR(5)      NOT NULL,
    order_date        DATE         NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    ship_street       VARCHAR(60),
    ship_city         VARCHAR(15),
    ship_postal_code  VARCHAR(10),
    ship_country      VARCHAR(15),
    shipped_date      DATE,
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_orders_status FOREIGN KEY (status) REFERENCES order_statuses (status)
);

CREATE TABLE order_lines (
    order_id    INTEGER        NOT NULL,
    line_no     SMALLINT       NOT NULL,
    product_id  INTEGER        NOT NULL,
    unit_price  NUMERIC(10, 2) NOT NULL,
    currency    CHAR(3)        NOT NULL,
    quantity    SMALLINT       NOT NULL,
    discount    NUMERIC(3, 2)  NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_lines_discount CHECK (discount BETWEEN 0 AND 1)
);
```

Buradaki şemada dikkat edilmesi gereken birkaç nokta var.

- **Teslimat adresi `orders` tablosunun içinde.** `Address` bir value object ve kendi kimliği yok. Bu yüzden ayrı bir tabloya değil doğrudan sahibinin satırına gömülüyor. Bunun JPA tarafındaki karşılığı ise `@Embedded` / `@Embeddable` ikilisi olacaktır.
- **`Money` iki kolona bölünüyor:** Tasarımımıza göre money nesnemiz `unit_price` ve `currency` bilgilerinden oluşuyor. Önceden `double` ile para tutmanın neden hatalı olduğu üzerinde durmuştuk. Veritabanında da aynı kural geçerlidir. `NUMERIC` alan kullanıyoruz, REAL ya da FLOAT değil *(Orijinal Northwind'de `Discount` kolonunun `REAL` olduğuna dikkat edelim)*.
- **`order_lines` tablosunun primary key alanı `(order_id, product_id)`.** Çünkü `Order.addLine` aynı ürün eklendiğinde yeni bir kalem açmak yerine mevcut kalemin miktarını artırıyor. Yani domain'deki bir kural veritabanı tarafında da bir kısıtlama *(constraint)* şeklinde korunuyor. `line_no` ise kalemlerin sırasını korumak için konulmuş bir alan.
- **Domain kuralları veritabanında ikinci kez korunuyor.** `quantity > 0` ve `discount BETWEEN 0 AND 1` kuralları zaten `OrderLine` record'unda var. Veritabanındaki kısıtlamalar *(constraints)* ise uygulamamız dışındaki bir istemcinin *(bir rapor aracı, elle çalıştırılan bir SQL)* hatalı veri yazmasını engellemek için. *(Bunun artı ve eksilerini tartışabiliriz)*

İkinci script dosyamız *(`V2__create_order_id_sequence.sql`)* ise sequence üretimini veritabanına tarafına taşıyor.

```sql
-- Order ids now come from the database: an in-process counter restarts at
-- 10248 on every boot and collides with the rows of the previous run.
CREATE SEQUENCE orders_seq START WITH 10248 INCREMENT BY 1;
```

> Flyway'in en önemli kuralı şu; **Uygulanmış bir migration dosyası asla değiştirilmez.** Flyway her script için bir checksum değeri saklar ve dosya değişmişse uygulama açılırken hata verir. Bir kolon eklemek istiyorsak `V1` dosyası üzerinde düzenleme yapmak yerine, `V3__add_freight_to_orders.sql` adında yeni bir script hazırlarız. Flyway'in güncel sürümleri ile birlikte PostgreSQL desteği ayrı bir modüle taşınmış olabilir. `flyway-core` bağımlılığının yanına `flyway-database-postgresql` bağımlılığı eklememiz gerekebilir. Bunu kontrol edelim.

