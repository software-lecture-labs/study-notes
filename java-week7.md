# Bölüm 07: Typed Identity, Static Factory, Factory ve Builder ile Nesne Üretimi

Şu ana kadar tasarladığımız nesneleri oluşturmak için çoğunlukla yapıcılardan *(constructors)* yararlandık. Örneğin yeni bir sipariş oluşturmak ya da adres bilgisi girmek için aşağıdaki kod parçasındakilere benzer şekilde ilerledik.

```java
var order = new Order(10248, "VINET", java.time.LocalDate.of(1996, 7, 4));

var shippingAddress = new Address("Lisbon Main", "Lisbon", "1000-001", "Portugal");
```

Order nesne örneğinin yapıcı metoduna konsantre olalım. İlk parametre ürün kimliğini ifade eden sayısal bir değer iken ikinci parametre müşteri kimliğini ifade eden string türden bir değer. Dolayısıyla sipariş oluştururken sipariş kimliği ile müşteri kimliğini karıştırmamız mümkün değil. Parametreleri hatalı sırada veremeyiz. Lakin Address türü için tüm parametreler string türündendir. İlk iki bilginin sırasını karıştırmak pekala mümkündür ;) Derleyici için bu bir sorun teşkil etmeyecektir fakat adres bilgisini aşağıdaki gibi yazarak yanlış kaydetme olasılığı vardır. Üstelik bunu sadece ilgili parametre yer değişikliğini kontrol eden test metotları varsa yakalayabiliriz.

```java
var shippingAddress = new Address("Lisbon", "Lisbon Main", "1000-001", "Portugal");
```

## Typed Identity ile Güvenli Kimlikler

Order nesnesi her ne kadar sorunsuz görünse de kimlik bilgileri her zaman probleme açıktır. Şöyle düşünelim; sistem içinde dolaşan String türünden bir customerId bilgisi pekala String türünden olduğu için yanlışlıkla productCode bilgisini taşır hale gelebilir. İşte bu gibi nedenler primitive türlerin sarmalanarak **typed identity** olarak kullanılması gerektiğini gösterir. Yani her kimlik bilgisi kendi türü ile temsil edilmelidir. Şimdi domain'imize müşteri kimliğini temsil edecek bir typed identity ekleyelim.

```java
package com.lectures.business.design.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record CustomerId(String value) implements Comparable<CustomerId> {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{5}$");

    public CustomerId {
        String raw = value;
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }

        value = value.strip().toUpperCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "customerId must be exactly five letters (A-Z): " + raw);
        }
    }

    public static CustomerId of(String value) {
        return new CustomerId(value);
    }

    @Override
    public int compareTo(CustomerId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
```

Kobay olarak kullandığımız Northwind sisteminde müşteri kimliklerinin 5 harften oluştuğunu biliyoruz. Bu o domain'in bir kuralı olarak geçiyor. Dolayısıyla CustomerId bilgisini ifade eden yukarıdaki record bu kuralları bir regex ifadesi ile kontrol ediyor. Compact constructor sayesinde değer nesnesi oluşturulurken gerekli doğrulama işlemleri de uygulanıyor. Şimdilik istisnalar IllegalArgumentException olarak fırlatılıyor ama daha önceden de belirttiğimiz gibi bunlar domain'e özgü özel istisnalar ile değiştirilebilir. Record türü minimalde ihtiyaç duyduğu tüm fonksiyonelliklere sahip. Karşılaştırmalar için generic Comparable arayüzünden gelen compareTo metodu override ediliyor.

Siparişlerin de bir kimliği var ve OrderId ile temsil ediliyor. Bunu da güçlendirilmiş bir kimlik tipi olarak tasarlayabiliriz.

```java
public record OrderId(int value) implements Comparable<OrderId> {

    public OrderId {
        if (value <= 0) {
            throw new IllegalArgumentException("orderId must be positive: " + value);
        }
    }

    public static OrderId of(int value) {
        return new OrderId(value);
    }

    @Override
    public int compareTo(OrderId other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
```

Artık bu iki güçlendirilmiş kimlik türünü Customer ve Order sınıflarına entegre edebiliriz. Daha önceki bölümlerde de belirttiğimiz üzere ihtiyaçları ortaya çıkartıp tasarımlarımızı değiştiriyoruz. İlk olarak Customer sınıfının yeni halini ele alalım.

```java
import java.util.Objects;
import java.util.Optional;

public final class Customer {

    private final CustomerId customerId;
    private String companyName;
    private String contactName;
    private Address address;

    public Customer(CustomerId customerId, String companyName, Address address) {
        this.customerId = Objects.requireNonNull(customerId, "Customer Id must not be null");
        this.companyName = requireText(companyName, "companyName");
        this.address = Objects.requireNonNull(address, "address must not be null");

    }

    // Behavior methods start
    public void relocateTo(Address newAddress) {
        address = Objects.requireNonNull(newAddress, "address must not be null");
    }

    public void renameTo(String newCompanyName) {
        companyName = requireText(newCompanyName, "companyName");
    }

    public void assignContact(String newContactName) {
        contactName = requireText(newContactName, "contactName");
    }

    public void clearContact() {
        contactName = null;
    }

    // Behavior methods end
    // State methods start
    public CustomerId customerId() {
        return customerId;
    }

    public String companyName() {
        return companyName;
    }

    public Optional<String> contactName() {
        return Optional.ofNullable(contactName);
    }

    public Address address() {
        return address;
    }

    // State methods end
    // Helper methods start
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Customer customer)) {
            return false;
        }

        return customerId.equals(customer.customerId);
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }

    @Override
    public String toString() {
        return "Customer[" + customerId + " " + companyName + "]";
    }
    // Helper methods end
}
```

Önceki sürümle karşılaştırdığımızda şu değişiklikleri yaptığımızı ifade edebiliriz:

- String türünden olan customerId alanı artık CustomerId türünden,
- normaliseId metodu artık yok
- regex pattern ile id doğrulama artık CustomerId sınıfının sorumluluğunda olduğundan onunla ilgili enstrümanlar *(Pattern, Locale)* da kaldırıldı

> Bu değişiklikle birlikte kimlik doğrulaması tek bir yerde ele alınırken, entity kendi işine odaklanabiliyor.

Dolayısıyla artık bir customer nesnesi örneklenirken customerId alanına bir productCode bilgisi atamamız pek de mümkün değil. Tahmin edileceği üzere OrderId bileşenini de Order sınıfında ele alarak benzer bir yaklaşım sergileyeceğiz. Order sınıfının yeni hali;

```java
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Order {

    private static final int MAX_LINES = 50;

    private final OrderId orderId;
    private final CustomerId customerId;
    private final LocalDate orderDate;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status = OrderStatus.DRAFT;
    private Address shippingAddress;
    private LocalDate shippedDate;

    public Order(OrderId orderId, CustomerId customerId, LocalDate orderDate) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "customerId must not be null");
        this.orderDate = Objects.requireNonNull(orderDate, "orderDate must not be null");
    }

    public void addLine(int productId, Money unitPrice, int quantity, BigDecimal discount) {
        requireModifiable("add a line");
        int existing = indexOfProduct(productId);
        if (existing >= 0) {
            lines.set(existing, lines.get(existing).withAdditionalQuantity(quantity));
            return;
        }
        if (lines.size() == MAX_LINES) {
            throw new IllegalStateException("an order cannot hold more than " + MAX_LINES + " lines");
        }
        lines.add(new OrderLine(productId, unitPrice, quantity, discount));
    }

    public void removeLine(int productId) {
        requireModifiable("remove a line");
        int index = indexOfProduct(productId);
        if (index < 0) {
            throw new IllegalArgumentException("product is not on this order: " + productId);
        }
        lines.remove(index);
    }

    public void confirm() {
        requireModifiable("confirm");
        if (lines.isEmpty()) {
            throw new IncompleteOrderException(orderId, IncompleteOrderException.MissingPart.LINES);
        }

        if (shippingAddress == null) {
            throw new IncompleteOrderException(
                    orderId, IncompleteOrderException.MissingPart.SHIPPING_ADDRESS);
        }
        transitionTo(OrderStatus.CONFIRMED);

    }

    public void shipTo(Address address) {
        requireModifiable("change the shipping address");
        shippingAddress = Objects.requireNonNull(address, "address must not be null");
    }

    public void ship(LocalDate date) {
        requireTransition(OrderStatus.SHIPPED);
        Objects.requireNonNull(date, "shipped date must not be null");
        if (date.isBefore(orderDate)) {
            throw new IllegalArgumentException("shipped date cannot precede the order date");
        }
        shippedDate = date;
        status = OrderStatus.SHIPPED;
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    // --- behaviour end ---    
    public Money total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money::plus)
                .orElse(Money.tl("0"));
    }

    // --- state begin ---
    public OrderId orderId() {
        return orderId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public LocalDate orderDate() {
        return orderDate;
    }

    public OrderStatus status() {
        return status;
    }

    public Optional<Address> shippingAddress() {
        return Optional.ofNullable(shippingAddress);
    }

    public Optional<LocalDate> shippedDate() {
        return Optional.ofNullable(shippedDate);
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }
    // --- state end ---

    // --- helpers begin ---
    private int indexOfProduct(int productId) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).productId() == productId) {
                return i;
            }
        }
        return -1;
    }

    private void requireModifiable(String action) {
        if (!status.isModifiable()) {
            throw new OrderNotModifiableException(orderId, status, action);
        }
    }

    private void requireTransition(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(orderId, status, target);
        }
    }

    private void transitionTo(OrderStatus target) {
        requireTransition(target);
        status = target;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Order order)) {
            return false;
        }
        return orderId.equals(order.orderId);
    }

    @Override
    public int hashCode() {
        return orderId.hashCode();
    }
    // --- helpers end ---

    // --- queries start ---
    
    // Same as before

    // --- queries end ---
}
```

Aslında String customerId ve Integer orderId alanlarının yeni güçlendirilmiş kimlik tipleri ile *(CustomerId ve OrderId)* değiştirdik. Tabii Order sınıfı bu haliyle derlenmeyecektir. Zira InvalidStateTransitionException, OrderNotFoundException, OrderNotModifiableException ve IncompleteOrderException gibi istisna türleri integer türünden orderId alanını kullanıyordu. Bu istisna türlerini de yeni kimlik tiplerini kullanacak şekilde güncellememiz gerekecek. Bir örneğini paylaşalım, gerisi sizde.

```java
public class IncompleteOrderException extends DomainException {

    public enum MissingPart {
        LINES,
        SHIPPING_ADDRESS
    }
    private final OrderId orderId;
    private final MissingPart missingPart;

    public IncompleteOrderException(OrderId orderId, MissingPart missingPart) {
        super("order " + orderId + " cannot be confirmed: " + missingPart + " missing");
        this.orderId = orderId;
        this.missingPart = missingPart;
    }

    public OrderId orderId() {
        return orderId;
    }

    public MissingPart missingPart() {
        return missingPart;
    }
}
```

Dikkat edilecek noktalardan birisi metinsel kullanımlarda orderId üzerinden toString çağrısı yapmıyor oluşumuz. Hatırlayacağınız gibi OrderId record türünde toString metodunu override etmiştik. Business Object tasarımlarını baştan çok iyi düşünmek gerekir. Örneğin OrderId ve CustomerId dönüşümleri Customer, Order ve bazı Exception sınıflarında değişiklik yapmamızı gerektirdi. Benzer şekilde OrderBook sınıfını, test metotlarını ve object user kodlarının da güncellememiz gerekiyor. İlk olarak OrderBook tarafını güncelleyelim.

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class OrderBook {

    private final List<Order> orders = new ArrayList<>();

    public void add(Order order) {
        Objects.requireNonNull(order, "order must not be null");

        if (findById(order.orderId()).isPresent()) {
            throw new IllegalArgumentException("order already in the book: " + order.orderId());
        }
        orders.add(order);
    }

    public int size() {
        return orders.size();
    }

    public Optional<Order> findById(OrderId orderId) {
        return orders.stream()
                .filter(order -> order.orderId().equals(orderId))
                .findFirst();
    }

    public Order getById(OrderId orderId) {
        return findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public List<Order> findByCustomer(CustomerId customerId) {
        return orders.stream()
                .filter(order -> order.customerId().equals(customerId))
                .sorted(Comparator.comparing(Order::orderDate))
                .toList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orders.stream()
                .filter(order -> order.status() == status)
                .toList();
    }

    public Map<OrderStatus, Long> countByStatus() {
        return orders.stream()
                .collect(Collectors.groupingBy(
                        Order::status,
                        () -> new EnumMap<>(OrderStatus.class),
                        Collectors.counting()));
    }

    public Map<CustomerId, Money> revenueByCustomer() {
        return orders.stream()
                .filter(order -> order.status() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        Order::customerId,
                        TreeMap::new,
                        Collectors.reducing(Money.tl("0"), Order::total, Money::plus)));
    }

    public List<CustomerId> topCustomers(int limit) {
        return revenueByCustomer().entrySet().stream()
                .sorted(Map.Entry.<CustomerId, Money>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    public Money totalRevenue() {
        return orders.stream()
                .filter(order -> order.status() != OrderStatus.CANCELLED)
                .map(Order::total)
                .reduce(Money.tl("0"), Money::plus);
    }

    public Map<Boolean, List<Order>> partitionByShipped() {
        return orders.stream()
                .collect(Collectors.partitioningBy(
                        order -> order.status() == OrderStatus.SHIPPED));
    }
}
```

Artık yeni bir sipariş nesnesi oluşturmak için aşağıdaki gibi bir çağrı yapılması gerekir.

```java
var order = new Order(new OrderId(10248), new CustomerId("VINET"), java.time.LocalDate.of(1996, 7, 4));
```

ve pek tabii ilgili test sınıfları da yeni düzenlemelere uygun şekilde güncellenmelidir.

## Static Factory Metotları

CustomerId ve OrderId record türlerinde static olarak tanımlanmış of isimli metotlar olduğunu fark etmişsinizdir. Statik metotlar tanımlandığı nesne türüne ait bir örneğe ihtiyaç duymadan çağrılabilir. Bunlar statik fabrika metotlar olarak adlandırılmaktadırlar. Zaten örneklerimizdeki bu fabrika metotları yine tanımlandıkları nesne türüne ait bir örnek oluşturup döndürürler ve bu sırada yapıcı metotlar içerisinde alınmış kuralları da işletirler. Dolayısıyla aşağıdaki gibi kullanımlar da pekala mümkündür.

```java
var order = new Order(OrderId.of(10248), CustomerId.of("VINET"), java.time.LocalDate.of(1996, 7, 4));
```

Aslında CustomerId ve OrderId tiplerine gelene kadar projede statik fabrika metotlarını çaktırmadan da olsa kullandık. Money türünün tl ve zero metotları da statik fabrika metotlarıdır. Örneğin:

```java
var price = Money.tl("100");
var free = Money.zero();
```

Bu örnekleri göz önüne aldığımızda statik fabrika metotlarının avantajlarını şöyle özetleyebiliriz;

- Nesne oluşturmak için anlamlı isimler kullanabiliriz. `Money.tl("100")` ifadesi, Türk Lirası türünden bir Money nesnesi oluşturduğumuzu açıkça ifade eder.
- Her çağrımda yeni bir nesne örneği üretmek zorunda kalmayız. `Optional.empty()`, `List.of()`, `Boolean.valueOf()` ve hatta `Money.zero()` gibi metotlar optimize edilmiş şekilde mevcut nesneleri döndürebilir.
- Fabrika metodu geriye alt tip döndürebilir. Object user asıl implementasyonunu bilmeden döndürülen nesneyi kullanabilir *(`Collectors.toList()` gibi)*

Tabii dikkat edilmesi gereken birkaç durum da söz konusu. Statik fabrika metotu ve constructor public olduklarında hangisinin kullanılacağı belirsizleşebilir. record türünde constructor'lar zorunlu olduğundan, statik fabrika metotları genellikle ek esneklik ve anlamlı isimlendirme sağlamak için tercih edilir. İsimlendirmelerde genellikle `of`, `from`, `valueOf`, `getInstance`, `newInstance`, `create` gibi anlamlı ve yaygın olarak kabul görmüş adlar kullanılır. Şunu da unutmayalım statik fabrika metotları yapıcı metot değildir.

## Factory Metotlarını Kullanmak

Bazı durumlarda nesne oluşturmak için farklı bileşenlere bağımlılık gerekebilir. Örneğin bir sipariş oluşturmak istediğimizi düşünelim. new operatörü yardımıyla Order nesnesinin bir örneğini oluştururuz. Ancak veritabanı seviyesinde baktığımızda bir sipariş oluşturulduğunda örneğin OrderId değeri otomatik olarak artan şekilde üretilir. Hatta sipariş tarihi bilgisi günün tarihi olarak atanır *(LocalDate.now())* ama bu şekilde test metotları patlayabilir. Bir iş kuralı olarak da varsayılan teslimat adresi mevzusunu işin içerisine katabiliriz. Müşterinin adresi siparişin varsayılan teslimat adresi olarak atanabilir ama şu anki tasarımımızı düşündüğümüzde Order'ın Customer'ı tanımadığını da biliyoruz. Sadece CustomerId ile kurulan bir ilişki söz konusu. Dolayısıyla Order nesnesi örneklenirken gerekli varsayılan adres bilgisi yok. Tüm bunları bir araya getirdiğimizde bir Order nesnesnin oluşturulması sırasında bağımlı olduğu başka nesneler gerekiyor. Herbiri kendi sorumluluğuna sahip olan nesneler.

İlk olarak OrderId üretimini ele alalım. Her ne kadar veritabanı seviyesinde otomatik artan bir değer olarak görülsede kod tarafında böyle bir bağımlılık inşa etmemiz doğru olmaz. İşte bir sözleşme *(contract)* ile karşılaşacağımız ilk yer. OrderId üretme davranışını bir interface olarak tanımlayıp bu interface'i implemente eden farklı sınıflar aracılığıyla OrderId üretimini soyutlayabiliriz. Bu sayede test ortamında sabit veya sahte OrderId üretebilen bir implementasyon kullanabilirken, üretim ortamında veritabanına bağımlı gerçek bir implementasyonu tercih edebiliriz. Buna göre aşağıdaki arayüz türünü tanımlayarak devam edelim.

```java
package com.lectures.business.design.domain;

@FunctionalInterface
public interface OrderIdGenerator {
    OrderId next();
}
```

Bu interface `@FunctionalInterface` anotasyonu ile işaretlendi. Buna göre yalnızca tek bir soyut metot *(abstract method)* içerebilir ve bunun garanti altına alınmasını sağlar. Java 8 ile gelen bu anotasyon aslında fonksiyonel programlamayı ve lambda ifadelerini desteklemek amacıyla dile entegre edilmiştir. Şimdi kod tarafında bu sınıfı implemente eden gerçek bir sınıf oluşturalım.

```java
package com.lectures.business.design.domain;

import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialOrderIdGenerator implements OrderIdGenerator {

    private final AtomicInteger counter;

    public SequentialOrderIdGenerator(int startingAt) {
        if (startingAt <= 0) {
            throw new IllegalArgumentException("startingAt must be positive: " + startingAt);
        }
        this.counter = new AtomicInteger(startingAt);
    }

    @Override
    public OrderId next() {
        return OrderId.of(counter.getAndIncrement());
    }
}
```

Bu sınıf aslında in-memory çalışan bir OrderId üreticisidir. Yani uygulama çalıştığı sürece artan bir sayaç üzerinden OrderId üretir ve uygulama kapandığında bu sayaç sıfırlanır. Bu tür bir implementasyon özellikle test senaryolarında veya veritabanına bağımlı olmayan geçici çözümlerde oldukça kullanışlıdır.

> Ekstra bilgi: Id artırma gibi operasyonlar çok kullanıcı ortamlarda dikkatlice ele alınmalıdır. Eş zamanlı olarak gelen bir çok talep olduğu düşünüldüğünde aynı Id değerinin üretimi söz konusu olabilir. Bu örnekte yer alan in-memory implementasyon da AtomicInteger tipi bu yüzden ele alınmıştır. Klasik bir int değerini artırmak üç ayrı adımdan oluşur; değerin bellekten okunması, 1 artırılması ve tekrardan belleğe yazılması. Multi-thread ortamlarda değerler üstüste yazabileceğinden thread senkronizasyonu yapılması gerekir (synchronized kullanımı). Ancak bunun da bir maliyeti vardır. AtomicInteger türü bu sorunu CPU seviyesinde CAS(compare-and-swap) operasyonu ile kilitleme yapmadan(none-blocking) çözer. Birçok dil buna benzer atomik tipler için destek sunar.

Bu hazırlıklar sonrasında aslında bir sipariş nesnesi üretmek için kullanabileceğimiz asıl fabrika sınıfını aşağıdaki gibi tasarlayabiliriz.

```java
package com.lectures.business.design.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class OrderFactory {

    private final OrderIdGenerator idGenerator;
    private final Clock clock;

    public OrderFactory(OrderIdGenerator idGenerator, Clock clock) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Order draftFor(Customer customer) {
        Objects.requireNonNull(customer, "customer must not be null");
        return draftFor(customer, customer.address());
    }

    public Order draftFor(Customer customer, Address destination) {
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Order order = new Order(idGenerator.next(), customer.customerId(), LocalDate.now(clock));
        order.shipTo(destination);
        return order;
    }
}
```

OrderFactory aslında bir domain hizmeti sunar *(bu nedenle Domain Service olarak da ifade edebiliriz)* bir aggregate değildir. Kendisine ait hiçbir state tutmaz ve bu senaryoda bir nesne üretimi için iki aggregate kullanır. Bu yaklaşım, domain mantığını aggregate'lar arasında dağıtmadan, nesne yaratma sorumluluğunu merkezi bir noktada toplamanın güzel bir örneğidir.

Dikkat edileceği üzere OrderId üretimi, zaman bilgisinin sorun çıkarmayacak şekilde oluşturulması ve varsayılan müşteri adresi bilgisi kullanımı gibi detaylar Order nesne üretimi için bu fabrika sınıfında ele alınır. Birde nasıl kullanabileceğimize bakalım.

```java
public static void main(String[] args) {
    OrderFactory orderFactory = new OrderFactory(new SequentialOrderIdGenerator(1), Clock.systemDefaultZone());
    Customer customer = new Customer(new CustomerId("ALFKI"), "Contoso Inc", new Address("Main Contoso St", "45001", "New York", "USA"));
    Order order = orderFactory.draftFor(customer);
    order.addLine(10001, Money.tl("9.40"), 10, BigDecimal.valueOf(0.1));
    order.addLine(10002, Money.tl("19.50"), 5, BigDecimal.valueOf(0.2));
    order.addLine(10003, Money.tl("5.00"), 20, BigDecimal.ZERO);

    System.out.println(order.total());
}
```

Daha önceden Order sınıfını tasarlarken Customer sınıfını referans olarak koymamıştık. Bu biraz kafa karıştırıcı olabilir ama OrderFactory'nin Customer nesnesini alması, Order nesnesinin yaratılmasında Customer bilgisinin gerekli olmasının bir sonucudur. Bu sayede Order nesnesi yaratılırken müşteri bilgisi eksik olamaz ve domain mantığı daha tutarlı bir şekilde uygulanır. Şunu unutmayalım ki; parametrele geçici alanlar ise kalıcıdır. Aggregate sınırları nesne grafiklerinde kalıcı bağlar kurmayı yasaklar ama bir metodun iki aggregate'i aynı anda okuması mümkündür ve bu, domain mantığını ihlal etmez.

Kodda değerlendirdiğimiz Clock tipi gerçekten bir bağımlılıktır. `LocalDate.now()` yazdığımızda bu tip kullanım içeren testler de o günkü tarihi ele alıp çalışır, bu da testlerin deterministik olmasını zorlaştırır. `Clock.fixed()` kullanarak sabit bir tarih belirleyebilir ve testlerin her zaman aynı sonucu vermesini sağlayabiliriz *(İlerleyen bölümlerde bu bağımlılığı `@Inject` anotasyonu ile nasıl sağlayabileceğimizi göreceğiz)*.

## Builder Kullanarak Nesne Yaratımı

Hatırlayacağınız üzere asıl repository nesneleri öncesi bir hazırlık mahiyetinde OrderBook sınıfı ile de çalışmıştık. Temelde dahili tuttuğu in-memory koleksiyona sipariş ekleme ve sorgulama işlemlerini gerçekleştiriyordu. Birde bu sınıfa ait fonksiyonellikler için yazdığımız birim test sınıfı vardı. Şimdi, OrderBookTest sınıfı içerisindeki yardımcı draft metoduna odaklanalım.

```java
private Order draft(OrderId orderId, CustomerId customerId, String unitPrice, int quantity) {
    Order order = new Order(orderId, customerId, LocalDate.of(1996, 7, 4));
    order.addLine(11, Money.tl(unitPrice), quantity, NO_DISCOUNT);
    order.shipTo(REIMS);
    return order;
}
```

Testler için kullanabileceğimiz şekilde bir Order nesnesi örnekliyor ve ona bir sipariş kalemi ekleyerek bir adrese gönderim bilgisi atıyor. Bu sayede testlerimizde her seferinde aynı başlangıç durumuna sahip Order nesneleri yaratabiliyoruz. Ancak yeni bir sipariş satırı daha eklemek istersek ya da sipariş iptal durumu için bir hazırlık yapmak istersek, draft metodunu her seferinde değiştirmek zorunda kalacağız ki bu da test kodunu bakımını zorlaştırır. Bu nedenle test tarafındaki operasyonu kolaylaştırmak için bir Builder sınıfı kullanabiliriz. Tasarlayacağımız bu sınıf sadece birim testlerimiz için kullanacağımız bir yardımcı olacağından onu test paketi altında tutacağız.

```java
import com.lectures.business.design.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class OrderBuilder {

    private static final Address DEFAULT_DESTINATION = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    private static final LocalDate DEFAULT_DATE = LocalDate.of(1996, 7, 4);

    private record LineSpec(int productId, Money unitPrice, int quantity, BigDecimal discount) {

    }
    private OrderId orderId = OrderId.of(10248);
    private CustomerId customerId = CustomerId.of("VINET");
    private LocalDate orderDate = DEFAULT_DATE;
    private Address destination = DEFAULT_DESTINATION;
    private OrderStatus target = OrderStatus.DRAFT;
    private final List<LineSpec> lines = new ArrayList<>();

    private OrderBuilder() {

    }

    public static OrderBuilder anOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withId(int value) {
        orderId = OrderId.of(value);
        return this;
    }

    public OrderBuilder forCustomer(String value) {
        customerId = CustomerId.of(value);
        return this;
    }

    public OrderBuilder orderedOn(LocalDate date) {
        orderDate = date;
        return this;
    }

    public OrderBuilder shippingTo(Address address) {
        destination = address;
        return this;
    }

    public OrderBuilder withLine(int productId, String unitPrice, int quantity) {
        lines.add(new LineSpec(productId, Money.tl(unitPrice), quantity, BigDecimal.ZERO));
        return this;
    }

    public OrderBuilder withDiscountedLine(int productId, String unitPrice, int quantity, String discount) {
        lines.add(new LineSpec(productId, Money.tl(unitPrice), quantity, new BigDecimal(discount)));
        return this;
    }

    public OrderBuilder confirmed() {
        target = OrderStatus.CONFIRMED;
        return this;
    }

    public OrderBuilder shipped() {
        target = OrderStatus.SHIPPED;
        return this;
    }

    public OrderBuilder cancelled() {
        target = OrderStatus.CANCELLED;
        return this;
    }

    public Order build() {
        Order order = new Order(orderId, customerId, orderDate);
        order.shipTo(destination);
        for (LineSpec line : lines) {
            order.addLine(line.productId(), line.unitPrice(), line.quantity(), line.discount());
        }
        return advance(order);
    }

    private Order advance(Order order) {
        switch (target) {
            case DRAFT -> {
            }
            case CANCELLED ->
                order.cancel();
            case CONFIRMED ->
                order.confirm();
            case SHIPPED -> {
                order.confirm();
                order.ship(orderDate.plusDays(12));
            }
        }
        return order;
    }
}
```

Bu sınıfı dikkatlice inceleyelim. Öncelikle varsayılan yapıcının private olduğunu ve doğrudan erişilemediğini görüyoruz. Bunun yerine, `anOrder()` adlı statik yapıcı metodunu kullanarak bir OrderBuilder örneği oluşturuyoruz. Sonrasında çağırılabilecek olan tüm public metotlar geriye yine OrderBuilder nesne örneğini döndürüyor. Bu sayede bir metot zinciri oluşturarak sipariş bilgilerini adım adım doldurmak mümkün. Örneğin;

```java
Order order = OrderBuilder.anOrder()
    .withId(10248)
    .forCustomer("VINET")
    .orderedOn(LocalDate.of(1996, 7, 4))
    .shippingTo(new Address("59 rue de l'Abbaye", "Reims", "51100", "France"))
    .withLine(11, "14.00", 12)
    .withDiscountedLine(42, "9.80", 10, "0.10")
    .confirmed()
    .shipped()
    .build();
```

Dolayısıya OrderBookTest sınıfında da OrderBuilder'ı kullanarak sipariş nesnelerini oluşturabiliriz. Bu sayede testlerimizde siparişlerin farklı durumlarını ve içeriklerini kolayca simüle edebiliriz.

```java
class OrderBookTest {

    private OrderBook book;

    @BeforeEach
    void setUp() {
        book = new OrderBook();
        book.add(OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET")
                .withLine(11, "14.00", 10)
                .shipped().build());                              
        book.add(OrderBuilder.anOrder()
                .withId(10249).forCustomer("VINET")
                .withLine(11, "10.00", 3)
                .confirmed().build());
        book.add(OrderBuilder.anOrder()
                .withId(10250).forCustomer("TOMSP")
                .withLine(11, "99.00", 5)
                .cancelled().build());
        book.add(OrderBuilder.anOrder()
                .withId(10251).forCustomer("TOMSP")
                .withLine(11, "20.00", 4)
                .build());
    }

    @Test
    @DisplayName("findById returns the order, or empty — it never returns null")
    void findByIdIsOptional() {
        assertThat(book.findById(OrderId.of(10248))).isPresent();
        assertThat(book.findById(OrderId.of(99999))).isEmpty();
    }

    @Test
    @DisplayName("getById throws a domain exception carrying the id")
    void getByIdThrowsWithTheId() {
        assertThat(book.getById(OrderId.of(10248)).customerId()).isEqualTo(CustomerId.of("VINET"));
        assertThatThrownBy(() -> book.getById(OrderId.of(99999)))
                .isInstanceOf(OrderNotFoundException.class)
                .extracting(e -> ((OrderNotFoundException) e).orderId())
                .isEqualTo(OrderId.of(99999));
    }

    @Test
    @DisplayName("the same order cannot be added twice")
    void duplicatesAreRejected() {
        assertThatThrownBy(() -> book.add(OrderBuilder.anOrder()
                .withId(10248).forCustomer("VINET")
                .withLine(11, "1.00", 1)
                .build()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(book.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("findByCustomer answers the question Customer refused to store")
    void findByCustomerReturnsOnlyThatCustomer() {
        assertThat(book.findByCustomer(CustomerId.of("VINET")))
                .extracting(Order::orderId)
                .containsExactly(OrderId.of(10248), OrderId.of(10249));
        assertThat(book.findByCustomer(CustomerId.of("ALFKI"))).isEmpty();
    }

    @Test
    @DisplayName("the returned collections are immutable")
    void returnedCollectionsAreImmutable() {
        assertThat(book.findByCustomer(CustomerId.of("VINET"))).isUnmodifiable();
        assertThat(book.findByStatus(OrderStatus.DRAFT)).isUnmodifiable();
    }

    @Test
    @DisplayName("countByStatus omits statuses that do not occur")
    void countByStatusOmitsMissingKeys() {
        assertThat(book.countByStatus())
                .containsEntry(OrderStatus.SHIPPED, 1L)
                .containsEntry(OrderStatus.CONFIRMED, 1L)
                .containsEntry(OrderStatus.CANCELLED, 1L)
                .containsEntry(OrderStatus.DRAFT, 1L);
        OrderBook empty = new OrderBook();

        assertThat(empty.countByStatus()).doesNotContainKey(OrderStatus.SHIPPED);
        assertThat(empty.countByStatus().getOrDefault(OrderStatus.SHIPPED, 0L)).isZero();
    }

    @Test
    @DisplayName("cancelled orders earn nothing")
    void revenueIgnoresCancelledOrders() {
        assertThat(book.revenueByCustomer())
                .containsEntry(CustomerId.of("VINET"), Money.tl("170.00"))
                .containsEntry(CustomerId.of("TOMSP"), Money.tl("80.00"));
        assertThat(book.totalRevenue()).isEqualTo(Money.tl("250.00"));
    }

    @Test
    @DisplayName("topCustomers ranks by revenue, highest first")
    void topCustomersAreRanked() {
        assertThat(book.topCustomers(1)).containsExactly(CustomerId.of("VINET"));
        assertThat(book.topCustomers(5)).containsExactly(CustomerId.of("VINET"), CustomerId.of("TOMSP"));
    }

    @Test
    @DisplayName("partitioningBy always produces both keys, even when one side is empty")
    void partitioningAlwaysHasBothKeys() {
        assertThat(book.partitionByShipped().get(true)).hasSize(1);
        assertThat(book.partitionByShipped().get(false)).hasSize(3);

        OrderBook empty = new OrderBook();
        assertThat(empty.partitionByShipped()).containsOnlyKeys(true, false);
        assertThat(empty.partitionByShipped().get(true)).isEmpty();
    }

    @Test
    @DisplayName("an empty book has zero revenue, not a missing one")
    void emptyBookHasZeroRevenue() {
        assertThat(new OrderBook().totalRevenue()).isEqualTo(Money.tl("0"));
    }
}
```

Builder sınıfımız ile ilgili birkaç noktayı vurgulayalım.

- Senaryomuzda kullandığımız builder sınıfı aggregate kurallarını atlamaz. Örneğin doğrudan bir sipariş durumu güncellemez. Bunun yerine `confirm()`, `ship()` veya `cancel()` gibi aggregate tarafından sağlanan metotları kullanır. Bu bir nevi garanti de sayılabilir. Örneğin sipariş kalemi olmayan bir siparişi confirm etmeye çalışmak istisna fırlatır. Bunu bir kusur değil istediğimiz bir özellik olarak düşünemliyiz. Eğer bu şekilde düşünmezsek testlerimiz aslında gerçek hayatta üretilmeyecek nesneler için yeşil bayrak kaldırır.
- Builder nesneleri her sınıf için gerekmez. Örneğin Address bileşenimizin zorunlu dört parametresi bulunuyor. Onun için bir builder kullanmak büyük ihtimalle satır sayısını artırır. Dolayısıyla isteğe bağlı parametre sayısı arttığında ve aynı nesnenin birden fazla varyasyonu gerektiğinde builder kullanmak daha anlamlıdır.

Buraya kadar anlattıklarımızda new yapıcı metodu, statik fabrika metodu, factory ve builder desenlerini ele aldık. Her birinin kullanım senaryoları ve avantajları farklıdır. Karar verme noktasında aşağıdaki tablodan yararlanabiliriz.

| **Enstrüman** | **Ne zaman kullanılır** | **Örnek** |
| --- | --- | --- |
| **Constructor (new)** | Az sayıda ve tamamı zorunlu parametreye sahip olduğunda ya da nesne oluşturma başka hiçbir şeye bağımlı olmadığında | `new Address()` , `new Order()` gibi |
| **Static Factory Method** | Nesne oluşturma sürecinde daha fazla kontrol gerektiğinde veya anlamlı isimlendirme yapmak istediğimizde | `CustomerId.of(...)`, `Money.tl(...)` gibi |
| **Factory** | Nesne oluşturma çeşitli bağımlılıklar gerektirdiğinde | `OrderFactory.draftFor(...)` gibi |
| **Builder** | Çok sayıda isteğe bağlı parametre olduğunda ya da aynı nesnenin farklı varyasyonlarını oluşturmak istediğimizde | `OrderBuilder.anOrder()...build()` gibi |

Bununla birlikte dikkat edilmesi gereken bazı durumlar vardır ve bunlar anti-pattern olarak değerlendirilebilir.

- OrderFactory sınıfı sipariş depolamaya başladığın anda aslında bir repository olur ve iki sorumluluk birden taşımaya başlar. Bu durum, SRP (Single Responsibility Principle) ihlali olarak değerlendirilir.
- Test tarafında kullandığımız builder sınıfı içerisinde reflection teknikleri kullanmaya kalktığımızda aggregate kuralarını atlayan test verileri oluşturabiliriz ve bu durum testlerin gerçek hayatta karşılaşılmayacak senaryoları yeşil bayrakla geçmesine neden olur.
- Sadece iki zorunlu parametresi olan bir sınıf için builder yazmak okuma ve bakım açısından gereksiz olabilir. Bu durumda doğrudan constructor veya static factory method kullanmak daha uygun olabilir.
- Bazen birçok yerde kullandığımız ortak fonksiyonları nereye koyacağımızı bilemez ve utility isimli bir paket açıp içerisine koyarız. Bu çok iyi bir pratik değildir ve buradaki örnekleri düşünürsek statik fabrika metotlarını utility sınıflarında toplamamalıyız. Nesne oluşturma işi çoğu zaman nesnenin kendi sorumluluğudur.

---

## Bu bölümün kodu

[`src/week07-object-creation`](src/week07-object-creation)

```powershell
cd src
mvn -pl week07-object-creation -am test
mvn -pl week07-step2-factory -am test
mvn -pl week07-step3-builder -am test
```
