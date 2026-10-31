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

## JDBC Tabanlı Order Repository Sınıfının Geliştirilmesi

Artık sipariş işlemlerinin veritabanı tarafındaki yönetimi için `persistence` paketine yeni bir repository sınıfı ekleyebiliriz. Bu sınıfın kodunu aşağıdaki gibi yazabiliriz. Biraz uzun olmakla birlikte en önemli kısımları üzerinde duracağız. Öncesinde ihtiyacımız olan bir Exception genişletmesini de ekleyelim. Özellikle `JdbcOrderRepository` sınıfında karşılaşabileceğimiz hataları temsil edecek bir exception sınıfı oluşturacağız.

```java
package com.lectures.business.design.sales.persistence;

public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

Şimdi `JdbcOrderRepository` sınıfını oluşturabiliriz. 

```java
package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.domain.OrderLine;
import com.lectures.business.design.sales.domain.OrderStatus;
import com.lectures.business.design.shared.kernel.Address;
import com.lectures.business.design.shared.kernel.Money;
import com.lectures.business.design.shared.kernel.Specification;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

public final class JdbcOrderRepository implements OrderRepository {

    private static final String ORDER_COLUMNS = """
            order_id, customer_id, order_date, status,
            ship_street, ship_city, ship_postal_code, ship_country, shipped_date
            """;
    private static final String LINE_COLUMNS
            = "order_id, line_no, product_id, unit_price, currency, quantity, discount";

    private static final String UPDATE_ORDER = """
            UPDATE orders
               SET customer_id = ?, order_date = ?, status = ?,
                   ship_street = ?, ship_city = ?, ship_postal_code = ?, ship_country = ?,
                   shipped_date = ?
             WHERE order_id = ?
            """;
    private static final String INSERT_ORDER = """
            INSERT INTO orders (customer_id, order_date, status,
                                ship_street, ship_city, ship_postal_code, ship_country,
                                shipped_date, order_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String DELETE_LINES = "DELETE FROM order_lines WHERE order_id = ?";
    private static final String INSERT_LINE
            = "INSERT INTO order_lines (" + LINE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";

    private final DataSource dataSource;

    public JdbcOrderRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    @Override
    public void save(Order order) {
        Objects.requireNonNull(order, "order must not be null");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (writeOrder(connection, UPDATE_ORDER, order) == 0) {
                    writeOrder(connection, INSERT_ORDER, order);
                }
                replaceLines(connection, order);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositoryException("could not save order " + order.orderId(), e);
        }
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        List<Order> found = query(" WHERE order_id = ?", orderId.value());
        return found.stream().findFirst();
    }

    @Override
    public List<Order> findAll() {
        return query("", null);
    }

    @Override
    public List<Order> findAll(Specification<Order> specification) {
        Objects.requireNonNull(specification, "specification must not be null");
        // Honest but expensive: every order is loaded, then filtered in memory.
        // Turning a Specification into a WHERE clause is what JPA Criteria does for us.
        return findAll().stream()
                .filter(specification::isSatisfiedBy)
                .toList();
    }

    @Override
    public long count() {
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM orders"); ResultSet rs = statement.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RepositoryException("could not count orders", e);
        }
    }

    // --- writing ---
    private static int writeOrder(Connection connection, String sql, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Optional<Address> address = order.shippingAddress();
            statement.setString(1, order.customerId().value());
            statement.setObject(2, order.orderDate());
            statement.setString(3, order.status().name());
            statement.setString(4, address.map(Address::street).orElse(null));
            statement.setString(5, address.map(Address::city).orElse(null));
            statement.setString(6, address.map(Address::postalCode).orElse(null));
            statement.setString(7, address.map(Address::country).orElse(null));
            statement.setObject(8, order.shippedDate().orElse(null));
            statement.setInt(9, order.orderId().value());
            return statement.executeUpdate();
        }
    }

    private static void replaceLines(Connection connection, Order order) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_LINES)) {
            delete.setInt(1, order.orderId().value());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_LINE)) {
            int lineNo = 1;
            for (OrderLine line : order.lines()) {
                insert.setInt(1, order.orderId().value());
                insert.setInt(2, lineNo++);
                insert.setInt(3, line.productId());
                insert.setBigDecimal(4, line.unitPrice().amount());
                insert.setString(5, line.unitPrice().currency().getCurrencyCode());
                insert.setInt(6, line.quantity());
                insert.setBigDecimal(7, line.discount());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private List<Order> query(String where, Integer orderId) {
        String orderSql = "SELECT " + ORDER_COLUMNS + " FROM orders" + where + " ORDER BY order_id";
        String lineSql = "SELECT " + LINE_COLUMNS + " FROM order_lines" + where + " ORDER BY order_id, line_no";

        try (Connection connection = dataSource.getConnection()) {
            Map<Integer, List<OrderLine>> lines = readLines(connection, lineSql, orderId);
            List<Order> orders = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
                if (orderId != null) {
                    statement.setInt(1, orderId);
                }
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        orders.add(toOrder(rs, lines.getOrDefault(rs.getInt("order_id"), List.of())));
                    }
                }
            }
            return List.copyOf(orders);
        } catch (SQLException e) {
            throw new RepositoryException("could not load orders", e);
        }
    }

    private static Map<Integer, List<OrderLine>> readLines(Connection connection, String sql, Integer orderId)
            throws SQLException {
        Map<Integer, List<OrderLine>> lines = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (orderId != null) {
                statement.setInt(1, orderId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    lines.computeIfAbsent(rs.getInt("order_id"), id -> new ArrayList<>())
                            .add(new OrderLine(
                                    rs.getInt("product_id"),
                                    new Money(rs.getBigDecimal("unit_price"),
                                            Currency.getInstance(rs.getString("currency"))),
                                    rs.getInt("quantity"),
                                    rs.getBigDecimal("discount")));
                }
            }
        }
        return lines;
    }

    private static Order toOrder(ResultSet rs, List<OrderLine> lines) throws SQLException {
        String street = rs.getString("ship_street");
        Address address = street == null ? null : new Address(street, rs.getString("ship_city"),
                rs.getString("ship_postal_code"), rs.getString("ship_country"));

        return Order.reconstitute(
                OrderId.of(rs.getInt("order_id")),
                CustomerId.of(rs.getString("customer_id")),
                rs.getObject("order_date", LocalDate.class),
                OrderStatus.valueOf(rs.getString("status")),
                address,
                rs.getObject("shipped_date", LocalDate.class),
                lines);
    }
}
```

Öncelikle bu sınıfın bir aggregate'i bütün olarak ele aldığını ifade ederek başlayalım. Yani, sipariş ve kalemleri hiçbir zaman birbirinden bağımsız olarak kaydedilmez ya da okunmaz. Repository'ye verdiğimiz ve ondan geri aldığımız şey her zaman eksiksiz bir `Order` nesnes örneğidir. `order_lines` tablosu için ayrı bir repository yazmamamızın sebebi de aslında bu. Kalemler siparişin bir parçasıdır ve kendi başlarına bir yaşam döngüleri yoktur, anlamlı değildir. Kod biraz uzun görünse de aslında birkaç temel karar üzerine kurulu. Bu kararları sırasıyla ele alarak devam edelim.

### Aggregate Sınırı = Transaction Sınırı

Sınıfın en önemli metodu `save` üzerinde duralım. Aşağıdaki kod parçasından ibaret.

```java
@Override
public void save(Order order) {
    Objects.requireNonNull(order, "order must not be null");
    try (Connection connection = dataSource.getConnection()) {
        connection.setAutoCommit(false);
        try {
            if (writeOrder(connection, UPDATE_ORDER, order) == 0) {
                writeOrder(connection, INSERT_ORDER, order);
            }
            replaceLines(connection, order);
            connection.commit();
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        }
    } catch (SQLException e) {
        throw new RepositoryException("could not save order " + order.orderId(), e);
    }
}
```

Aggregate'leri *tutarlılığın sınırı (boundary of consistency)* olarak düşünmek gerekiyor. Bir siparişin `orders` tablosuna yazıldığını ama kalemlerinin `order_lines` tablosuna yazılamadığını düşünelim. Veritabanında kalemleri eksik yarım kalmış bir sipariş oluşur ve domain modelimiz böyle bir durumu asla kabul etmez. Bu yüzden sipariş satırını ve tüm kalemleri tek bir transaction içerisinde yazıyoruz. `setAutoCommit(false)` çağrısı ile transaction'ı başlatıyor ve her şey yolunda giderse **commit**, bir hata oluşursa **rollback** çağrılarını icra ediyoruz.

İçteki catch bloğunun sadece **SQLException** değil **RuntimeException** türünü de yakaladığına dikkat edelim. Örneğin kalemlerden birini yazarken beklenmedik bir `NullPointerException` oluşursa, o ana kadar yazılmış olan sipariş satırının da geri alınması gerekir.

Peki ya **rollback** metodunu çağırmasak ne olur? `try-with-resources` bloğu bağlantıyı zaten kapatıyor. Ancak JDBC spesifikasyonu, açık bir transaction varken `close` çağrıldığında ne olacağını driver tarafına bırakmakta *(implementation-defined konusunu araştıralım)*. Bir veritabanı sürücüsü işi geri alabilir, bir diğeri commit edebilir. Bağlantı havuzu *(connection pool)* kullanıyorsak bağlantı aslında kapanmaz, havuza geri döner. Dolayısıyla bu kararı sürücüye bırakmak yerine açıkça kendimiz veriyoruz. JDBC ile çalışırken bu güvenceleri de düşünmek ve gerekiyorsa hazırlamak zorundayız.

> `save` metodundaki `setAutoCommit(false)`, `commit` ve `rollback` satırlarını yorum satırına alıp projedeki testleri çalıştırmayı deneyelim. Kuvvetle muhtemel hiçbir test patlamayacaktır. Çünkü henüz hiçbir test, kalemlerin yazılamadığı bir durumu denemiyor. Bu açığı sözleşme testlerinde ele alıp kapatacağız.

### Önce Güncelle, Olmazsa Ekle *(Update-Then-Insert)*

Dikkat edileceği üzere **OrderRepository** arayüzünde **insert** ve **update** isimli iki ayrı metot yerine sadece **save** tanımı var. Bu kararı In-Memory implementasyonda pek düşünmedik desek yeridir. Zira bir **Map** veri yapısı için ekleme ile güncelleme arasında bir fark yoktur. Ancak veritabanında tarafında ekleme ve güncelleme farklı işlemlerdir. Biz de önce **UPDATE** sorgusunu çalıştırıyoruz. `executeUpdate` metodu etkilenen satır sayısını döndürür. Bu değer sıfırsa sipariş veritabanında henüz yok demektir ve **INSERT** sorgusunu çalıştırırız.

Burada küçük bir de hile var. `INSERT_ORDER` sorgusundaki kolon sırasına dikkat edelim, `order_id` en sonda yer alıyor. `UPDATE_ORDER` sorgusunda da `order_id` değeri `WHERE` ifadesindeki dokuzuncu parametre olarak kullanılıyor. Buna göre `writeOrder` metodu her iki sorgunun parametrelerini aynı sırayla bağlayabilir. Tek bir metot iki farklı sorguya hizmet eder. Kolon sırasını değiştirdiğimiz andaysa bu metot çaktırmadan yanlış kolonlara yanlış değerler yazmaya başlar. Dolayısıyla bu bağımlılığın farkında olmamız önemlidir.

Aynı işi tek sorguda yapmanın yolları da var. PostgreSQL'in `INSERT ... ON CONFLICT (order_id) DO UPDATE` söz dizimi ya da standart SQL'deki `MERGE` ifadesi bunlardan ikisi. Ancak bu ifadelerin söz dizimi ve davranışı veritabanından veritabanına farklılık gösterebilir. Bizim kodumuz hem **H2** hem de **PostgreSQL** gibi ortamlar üzerinde çalışacak şekilde düşünelim. Bu nedenle her iki veritabanında da aynı şekilde davranan basit yöntemleri tercih etmek yerinde bir karar.

Tabii bu yöntemin iki zayıf noktası var. Şimdilik çözmeyeceğiz ama bilmek yarar var.

- **Yarış durumu *(race condition)*:** Aynı yeni siparişi iki istek aynı anda kaydetmeye çalışırsa ikisinin `UPDATE` sorgusu da sıfır satır etkiler ve ikisi de `INSERT` yapmaya çalışır. Birincil anahtar kısıtı sayesinde veri bozulmaz ama ikinci istek bir `RepositoryException` alır.
- **Kayıp güncelleme *(lost update)*:** İki kullanıcı aynı siparişi okuyup farklı değişiklikler yaparsa, son kaydeden ilkinin değişikliğini fark etmeden ezer. JPA bu sorunu `@Version` alanı ile, EF Core ise concurrency token *(örneğin `RowVersion`)* ile çözer. Bu yaklaşıma **optimistic locking** adı verilir.

> Bu durumlarla ilgili olarak veritabanı seviyesinde izolasyon seviyeleri ve kilitleme mekanizmaları da kullanılabilir. Ancak bu konular, uygulama kodundan bağımsız olarak veritabanının nasıl davrandığını belirler. Yine de araştırmakta yarar vardır.

### Sipariş Kalemleri Silinip Yeniden Yazılıyor

**replaceLines** metodu, hangi kalemin eklendiğini, hangisinin silindiğini ya da değiştiğini takip etmez. Siparişin tüm kalemlerini siler ve güncel listeyi yeniden yazar. Bir siparişin en fazla 50 kalemi olabildiğini belirlediğimiz için *(MAX_LINES sabiti)* bu yöntem oldukça pratik. Kalemleri yazarken **addBatch** ve **executeBatch** metotları kullanılıyor. Bu sayede elli kalem için veritabanına elli ayrı istek göndermek yerine toplu olarak göndermelerine izin veriyoruz. JPA ise **dirty checking** adı verilen bir mekanizma ile sadece değişen satırları yazabiliyor. Bunun için yüklediği her nesnenin bir kopyasını saklar ve transaction sonunda karşılaştırma yapar.

Tablo tasarımında `order_lines` için birincil anahtarın `(order_id, product_id)` ikilisi olduğunu hatırlayalım. Bu tesadüfi bir karar değil. `Order.addLine` metodu aynı ürün ikinci kez eklendiğinde yeni bir kalem açmak yerine mevcut kalemin miktarını artırıyor *(indexOfProduct)*. `line_no` kolonu ise kimlik değil, sadece sıra bilgisi taşıyor. Okuma sırasında `ORDER BY order_id, line_no` kullanıldığı için sipariş kalemleri veritabanından kaydedildikleri sırayla geri geliyor. Kalemler her kayıtta yeniden yazıldığı için `line_no` değerleri de her zaman 1'den başlayıp kesintisiz devam ediyor.

### Kullanılan Exception'lar Hakkında

**SQLException** checked türünden bir exception. Onu doğrudan fırlatsaydık **OrderRepository** arayüzündeki metot imzalarına `throws SQLException` ifadesinin de eklenmesi gerekirdi. Bu durumda JDBC alt yapsına ait bir detay sözleşmenin parçası haline gelir ve **InMemoryOrderRepository** hiç ihtiyacı olmadığı halde bu imzayı taşımak zorunda kalırdı. Bunun yerine **SQLException** nesnesini unchecked bir **RepositoryException** ile çevreliyoruz. Asıl hatayı **cause** olarak sakladığımız için hiçbir bilgi kaybolmuyor ve mesaja eklediğimiz sipariş numarası sayesinde log kayıtlarında hangi siparişin kaydedilemediğini de görebiliyoruz.

**RepositoryException** sınıfının **DomainException** sınıfından değil **RuntimeException** sınıfından türediğine de dikkat edelim. Veritabanı bağlantısının kopması bir iş kuralı ihlali değil tam tersi bir altyapı hatası. Kullanıcı isteğini düzelterek bu hatayı ortadan kaldıramayız. HTTP boundary konusunu incelediğimizde bu ayrımın neden önemli olduğunu göreceğiz.

## Veritabanından Yeniden Oluşturmak *(Reconstitution)*

Veritabanından okunan satırların tekrar bir **Order** nesnesine dönüştürülmesi gerekiyor. İlk akla gelen yöntem, siparişi oluşturup metotlarını sırasıyla çağırmak olabilir. Aynen aşağıdaki kod parçasında görüldüğü gibi.

```java
Order order = new Order(id, customerId, orderDate);
order.shipTo(address);
lines.forEach(line -> order.addLine(...));
order.confirm();
order.ship(shippedDate);
```

İlk bakışta normal görünse de birkaç sebepten dolayı hatalıdır.

- `confirm()` ve `ship()` metotları **OrderConfirmed**, **OrderShipped** olaylarını üretir. Siparişi her okuduğumuzda yeni olaylar oluşur ve müşteriye tekrar tekrar *"siparişiniz kargoya verildi"* bildirimleri gönderilir.
- İptal edilmiş bir siparişin hangi durumdan iptale çekildiğini bilemeyiz. Hangi metotları hangi sırayla çağıracağımıza karar verebilmek için durum geçişlerini tersine mühendislikle çözmemiz gerekir.
- Kurallar zamanla değişebilir büyük sistemlerde bu son derece yaygındır. Bugün bir siparişte en fazla elli kalem olabiliyorken yarın bu sınır otuz kaleme düşerse, geçmişte kırk kalemle kaydedilmiş bir sipariş artık hiç okunamaz.

Kayıtlı bir sipariş esasında yeni bir business işlem değildir. Kurallardan zaten bir kez geçmiş bir durumun geri yüklenmesidir. Bu nedenle **Order** sınıfına ayrı bir statik üretim metodu ekliyoruz.

```java
public static Order reconstitute(OrderId orderId, CustomerId customerId, LocalDate orderDate,
        OrderStatus status, Address shippingAddress, LocalDate shippedDate, List<OrderLine> lines) {
    Order order = new Order(orderId, customerId, orderDate);
    order.status = Objects.requireNonNull(status, "status must not be null");
    order.shippingAddress = shippingAddress;
    order.shippedDate = shippedDate;
    order.lines.addAll(Objects.requireNonNull(lines, "lines must not be null"));
    if ((status == OrderStatus.SHIPPED) != (shippedDate != null)) {
        throw new IllegalStateException(
                "stored order " + orderId + " is corrupt: status " + status + ", shipped " + shippedDate);
    }
    return order;
}
```

Metot statik olduğu ve **Order** sınıfının içinde yer aldığı için **private** alanlara doğrudan erişebilir. **pendingEvents** listesine dokunmadığı için de hiçbir olay üretmez. Durum geçiş kurallarını çalıştırmasa da tamamen savunmasız değildir. Kargolanmış bir siparişin bir kargo tarihi olmalıyken, kargolanmamış bir siparişin ise olmamalıdır. Bu tek kontrol, veritabanında bozulmuş bir satırı ilk okumada yakalamamızı sağlıyor. Dolayısıyla **DomainException** yerine **IllegalStateException** kullanmamızın sebebi de aynı. Bozuk bir kayıt kullanıcının ihlal ettiği bir iş kuralından ziyade sistemin kendi tutarsızlığının bir göstergesidir.

**toOrder** metodu da bu metodu kullanıyor. Kargo adresi için küçük bir karar daha veriyoruz. `ship_street` kolonu boşsa adresin hiç girilmediğini varsayıp **null** değer geçiyoruz. Adresin dört kolonu ya hep birlikte doludur ya da hep birlikte boştur.

Peki bu metodu kim çağırabilir? Metot **public** olduğu için derleyici herhangi bir paketin onu çağırmasını engellemez. Bir servis geliştiricisinin *"siparişin durumunu doğrudan SHIPPED yapayım"* niyetiyle **reconstitute** metodunu kullanması, aggregate'in koruduğu tüm kuralların atlanması anlamına gelir. Bu kuralı derleyici koruyamadığına göre **ArchUnit** ile koruma altına almak çok daha mantıklı olacaktır. Bunu ilerleyen kısımlarda tekrardan ele alacağız. Pek tabii bu tek seçenek değil. Diğer alternatifleri ve bedellerini şöyle özetleyebiliriz.

| **Yöntem** | **Nasıl çalışır?** | **Bedeli** |
| --- | --- | --- |
| Statik **reconstitute** metodu *(Tercih ettiğimiz yöntem)* | **Aggregate** kendi alanlarını doğrudan doldurur | **public** bir kapıdır ve sadece **ArchUnit** ile korunabilir |
| Package-private constructor | Repository'nin **aggregate** ile aynı pakette olmasını gerektirir | **domain** ve **persistence** paketlerini ayırdığımız yapımızla *(ADR-0003)* çelişir |
| Snapshot / Memento | **Aggregate** kendi durumunu bir record'a dönüştürür ve aynı record'dan geri yüklenir | Ek tipler gerektirir ama sınırı çok net çizer, event sourcing'e geçişi kolaylaştırır |
| Reflection *(JPA'nın yöntemi)* | Argümansız constructor ile nesneyi oluşturur, alanları reflection ile doldurur | **final** alanlar kullanılamaz ve **domain** sınıfına annotation'lar girer |

## Okuma: İki Sorgu ve N+1 Problemi

Siparişleri okurken hem **orders** hem de `order_lines` tablosuna gidilmesi gerekiyor. En basit yöntem önce siparişleri okumak, ardından her sipariş için kalemlerini ayrı bir sorgu ile getirmek gibi duruyor. Yüz sipariş için `1 + 100 = 101` sorgu çalışacağını düşünebiliriz. Bu durum **N+1 problemi** olarak da ifade edilir ve ORM *(Object Relational Mapping)* kullanan uygulamalardaki performans sorunlarının en yaygın sebeplerinden biridir. JPA çatısında `@OneToMany` ilişkisinin varsayılan olarak **LAZY** yüklenmesi ve bir döngü içinde `order.getLines()` çağrılması bunun klasik bir örneğidir.

**JdbcOrderRepository** ise sipariş sayısından bağımsız olarak her zaman iki sorgu çalıştırmakta. **query** metodu önce ilgili tüm kalemleri okuyup **readLines** içinde `order_id` değerine göre bir `Map` üzerinde gruplama yapıyor. Ardından siparişleri okurken her siparişin kalemlerini bu **Map** nesnesi üzerinden alıyor. Henüz hiç kalemi olmayan taslak bir sipariş için **getOrDefault** ile boş bir liste veriliyor. Sonuç olarak dönen liste `List.copyOf` ile değiştirilemez hale getiriliyor.

**where** parametresinin dışarıdan gelen bir değer değil de sınıf içindeki sabit bir metin olduğuna dikkat edelim. Kullanıcıdan gelebilecek her değer *(burada `orderId`)* `?` parametreleri ile bağlanıyor. **SQL injection** saldırılarına karşı temel kural bu. *(SQL metni asla kullanıcı girdisiyle birleştirilmez)* Bununla birlikte **query** metodunun **where** metni ile **orderId** parametresinin birbiriyle uyumlu olmasına güvendiğini de kabul edelim. Yeni filtreler eklemeye başladığımızda bu yaklaşım hızla kırılganlaşacaktır. Bu noktada küçük bir sorgu oluşturucu ya da jOOQ *(Java Object Oriented Querying)* gibi bir kütüphane devreye girer.

> İki sorgu aynı bağlantıyı kullanıyor ama bir transaction içinde çalışmıyor. Peki ya kalemleri okuduktan hemen sonra, siparişleri okumadan önce başka bir istek aynı siparişi güncellerse ne olur? Okunan sipariş ile kalemleri farklı anlara ait olabilir mi? Bunu tartışmaya açalım.

## Specification Sorguları: Dürüst ama Pahalı

Daha önceden hazırladığımız ve specification kalıbını ele aldığımız örneklerde aşağıdaki gibi oldukça basit bir yöntem kullanıyoruz.

```java
@Override
public List<Order> findAll(Specification<Order> specification) {
    Objects.requireNonNull(specification, "specification must not be null");
    return findAll().stream()
            .filter(specification::isSatisfiedBy)
            .toList();
}
```

`Specification<Order>` bir Java predicate'idir ve çok doğal olarak veritabanı tarafı onu anlayamaz. Bu nedenle tüm siparişleri belleğe yükleyip filtrelemeyi orada yapıyoruz. Doğru sonuç veriyor olsa da bir milyon siparişlik bir tabloda **VINET** müşterisinin siparişlerini bulmak için bir milyon siparişi belleğe yüklemek kabul edilebilir bir yaklaşım değil. Bu sorunu çözmenin yolu specification'ı bir **WHERE** ifadesine çevirebilmekle mümkün olur. JPA Criteria API ve Spring Data'daki `Specification<T>` arayüzü tam olarak bu işi yapar.

> C# tarafında bu problem çok daha zarif bir şekilde çözülür. `Expression<Func<Order, bool>>` tipindeki bir lambda, derleyici tarafından bir ifade ağacına *(expression tree)* dönüştürülür. EF Core bu ağacı çalışma zamanında inceleyip SQL ifadesine çevirebilir, `IQueryable<T>` de bu mekanizmanın üzerine kuruludur. Java'da ise lambda'lar çalışma zamanında bir ifade ağacı olarak incelenemez. Bu nedenle aynı çeviriyi bir API üzerinden *(Criteria, QueryDSL, jOOQ)* yapmak gerekir.

## Küçük Ama Önemli Bazı Detaylar

Sınıfın geri kalanında ilk bakışta gözden kaçabilecek ama üzerinde durmaya değer birkaç detay daha var. Bunları aşağıdaki gibi sıralayabiliriz;

- **DataSource constructor üzerinden geliyor:** Repository bağlantıyı **DriverManager** nesnesi ile kendisi açmaz. Hangi veritabanına, hangi bağlantı havuzu ile bağlanılacağına composition root nesnesi karar verecek. Testlerde H2, gerçek ortamda PostgreSQL kullanmamızı sağlayan şey de tam olarak bu.
- **`try-with-resources`:** **Connection**, **PreparedStatement** ve **ResultSet** nesneleri açıldıkları sıranın tersine otomatik olarak kapatılıyorlar. Bağlantı sızıntısı *(connection leak)* oluşmasını engellemiş oluyor.
- **Para değerleri:** `unit_price` kolonunu `NUMERIC(10, 2)` olarak tanımlamış ve **getBigDecimal** ile okuyoruz. Para birimini de ayrı bir **currency** kolonunda saklayıp **Money** nesnesini yeniden oluşturuyoruz. Önceden **double** kullanmama kararını verirken korumaya çalıştığımız hassasiyet veritabanında da korunmaya devam ediyor.
- **SQL için text block'lar:** Çok satırlı sorguları Java 15 ile gelen text block'lar *(`"""`)* ile yazdık. Okunabilirlik açısından epey avantajlı.

Böylece repository sözleşmesinin arkasına ikinci bir implementasyon yerleştirmiş olduk. Peki **JdbcOrderRepository** sınıfının gerçekten **InMemoryOrderRepository** ile aynı şekilde davrandığından nasıl emin olacağız? Hatırlarsak **save** çağrısını silsek bile testlerin geçtiğinden bahsetmişti. Bir sonraki adımda her iki implementasyona karşı aynı testleri çalıştıran sözleşme testlerini *(contract tests)* yazacağız.

// DEVAM EDECEK...
