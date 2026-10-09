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
        return orderId == order.orderId;
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

// EKLENECEK
