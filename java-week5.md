# Hafta 05

Önceki derslerimizde bir müşterinin siparişlerini Customer sınıfında `List<Order>` olarak tutmayacağımızdan bahsetmiştik. Zira bunun bir sorgu *(query)* operasyonu ama alan *(field)* olmadığını savunmuştuk. Bu derste sorguyu yazmaya çalışacağız ve bunu yaparken Optional ile Stream API enstrümanlarını nasıl kullanabileceğimizi öğreneceğiz.

## Optional Sözleşmesi

Optional enstrümanı, bir değerin var olup olmadığını ifade etmek için kullanılır. Null referanslardan kaynaklanan hataları önlemeye yardımcı olur ve daha anlamlı API'ler tasarlamamızı sağlar. Optional, özellikle bir metodun her zaman bir değer döndürmeyebileceği durumlarda tercih edilir. Örneğin:

```java
Optional<Order> findLatestOrder(Customer customer) {
    // müşteri siparişlerini tarihe göre sıralayıp en yenisini döndür
}
```

Bu metodun döndürdüğü Optional, çağıran tarafa değerin var olup olmadığını kontrol etme sorumluluğunu verir. Optional ile çalışırken `isPresent()`, `ifPresent()`, `orElse()`, `orElseGet()` ve `orElseThrow()` gibi metodlar kullanılarak değerin varlığına göre işlem yapılabilir. Bu sayede null kontrolleri daha güvenli ve okunabilir bir şekilde gerçekleştirilebilir.

Şu ana kadarki nesne tasarımlarımızda Order sınıfının shippedDate ve Customer sınıfının contactName metotlarında Optional kullanmıştık. Aslında bir metodun null yerine dönüş tipi olarak Optional kullanması, "bu metot boş dönebilir" bilgisini metot imzasına açıkça yansıtmak anlamına gelir. Bu sayede çağıran taraf, değerin varlığını kontrol etmeden metodu kullanamaz ve null referans hatalarından korunmuş olur. Hatta şunu kesinlikle söyleyebiliriz de; *Optional dönen metot asla null döndürmez.*

Optional kullanımını birkaç örnekle ele alalım.

```java
public static void main(String[] args) {
    var reims = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    var customer = new Customer("VINET", "Vins et alcools Chevalier", reims);

    if (customer.contactName().isPresent()) {
        System.out.println(customer.contactName().get());
    }

    // Optional'ın daha zengin yöntemleri vardır

    customer.contactName().orElse("there is no recorded contact");
    customer.contactName().map(String::toUpperCase).orElse("-");
    customer.contactName().ifPresent(System.out::println);
    customer.contactName().ifPresentOrElse(
            name -> System.out.println("authorized: " + name),
            () -> System.out.println("unauthorized"));
    customer.contactName().orElseThrow(() -> new IllegalStateException("contact required"));
}
```

## Stream API ve Optional ile Çalışmak

Order üzerinden yapabileceğimiz sorgularda kullanmak üzere Optional enstrümanına kısaca değindik. Bir diğer konu ise Stream API'den yararlanmak. Stream API ile ilgili en büyük yanılgılardan birisi onu basit bir for döngüsü sanmaktır. Temelde bir stream üç parçadan oluşur; kaynak veri *(source)*, ara işlemler *(intermediate operations)* ve terminal işlemler *(terminal operations)*. Ara işlemler lazy (tembel) olarak değerlendirilir ve terminal işlem gerçekleşene kadar çalıştırılmaz. Büyük veri kümeleri üzerinde verimli bir şekilde işlemler yapılabilmesini sağlar.

```text
Kaynak --------------> Ara İşlemler ------------------> Terminal İşlemler
lines.stream()-------> filter(...).map(...)-----------> toList() veya reduce() veya count()
```

Basit bir kod parçası ile durumu değerlendirelim;

```java
public static void main(String[] args) {
    var names = List.of("Chai", "Chang", "Aniseed Syrup");
    System.out.println("Order list 1");
    names.stream().peek(System.out::println).filter(n -> n.length() > 4);
    System.out.println("Order list 2");
    names.stream().peek(System.out::println).filter(n -> n.length() > 4).toList();
}
```

![Order list result](./images/week_05_00.png)

Dikkat edileceği üzere ilk stream işlemi ekrana bir çıktı vermemiştir zira herhangi bir terminal işlem *(`toList()`, `reduce()`, `count()` vb.)* çağrılmamıştır. Stream işlemleri lazy *(tembel)* olarak değerlendirildiğinden terminal işlem gerçekleşene kadar ara işlemler çalıştırılmaz.

## Order Sınıfına Sorguları Ekliyoruz

Önceki derslerde de belirttiğimiz üzere konular ilerledikçe ve ekipmanlarımız güçlendikçe var olan tasarımlarımızı değiştirebiliriz. Bir siparişle ilgili sorgulamalar için `Order` sınıfına yeni metotlar ekleyebiliriz. Aşağıdaki metotları göz önüne alalım.

```java
 public Optional<OrderLine> lineFor(int productId) {
    return lines.stream()
            .filter(line -> line.productId() == productId)
            .findFirst();
}

public int totalQuantity() {
    return lines.stream()
            .mapToInt(OrderLine::quantity)
            .sum();
}

public boolean hasDiscountedLine() {
    return lines.stream()
            .anyMatch(line -> line.discount().signum() > 0);
}

public Optional<OrderLine> mostValuableLine() {
    return lines.stream()
            .max(Comparator.comparing(OrderLine::lineTotal));
}

public List<Integer> productIds() {
    return lines.stream()
            .map(OrderLine::productId)
            .sorted()
            .toList();
}
```

lineFor ve mostValuableLine metotları `Optional` döndürdüğünden, çağıran tarafın bu durumu ele alması gerekir. totalQuantity ve hasDiscountedLine metotları ise doğrudan değer döndürür. productIds metodu ise siparişteki ürünlerin ID'lerini sıralı bir liste olarak döndürür. Tüm metotlar Stream API'den yararlanır. Bu haliyle Order nesnesini test edelim.

```java
public static void main(String[] args) {
    var order = new Order(10248, "VINET", java.time.LocalDate.of(1996, 7, 4));

    order.addLine(11, Money.tl("14.00"), 12, java.math.BigDecimal.ZERO);
    order.addLine(42, Money.tl("9.80"), 10, new java.math.BigDecimal("0.15"));

    System.out.println("Total quantity is " + order.totalQuantity());
    System.out.println("Has discounted line " + order.hasDiscountedLine());

    // System.out.println("Most valuable line is " + order.mostValuableLine().get());
    // Yukarıdaki kullanım yerine aşağıdaki kullanımı tercih edelim.
    System.out.println("Most valuable line is " + order.mostValuableLine().map(OrderLine::productId).orElse(-1));
    System.out.println("is line 99 present " + order.lineFor(99).isPresent());
    System.out.println("Product IDs: " + order.productIds());
}
```

![Runtime 01](./images/week_05_01.png)

## OrderBook ile Sipariş Yönetimi *(Repository Değil)*

// DEVAM EDECEK
