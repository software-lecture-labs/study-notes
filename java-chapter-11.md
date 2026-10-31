<a id="top"></a>

# Bölüm 11: Domain Events ve Observer Deseni

<!-- nav:top:start -->
[← Bölüm 10](java-chapter-10.md#top) · [İçindekiler](README.md#top) · [→ Bölüm 12](java-chapter-12.md#top)
<!-- nav:top:end -->

Bir önceki bölümde yazma yönündeki kullanım senaryolarını **OrderService** sınıfına taşıdık ve tüm bağımlılıkları **SalesModule** adındaki composition root nesnesi üzerinden bağladık. İş biriminden yeni bir talep geldiğini düşünelim; *"Sipariş kargoya verildiğinde müşteriye bir bildirim gitsin."* Bunu yapmanın en hızlı ve kolay yolu `OrderService.ship` metoduna bir satır eklemek olabilir. Fakat sonrasında *"kargolanan siparişler için stok düşülsün"*, ardından *"iptal edilen siparişler muhasebeye bildirilsin"* gibi taleplerin de geldiğini düşünelim. Bu talepleri de **OrderService** sınıfında karşılarsak onu biraz daha büyütür ve servisin zamanla sistemdeki her şeyi bilen bir sınıfa haline gelmesine neden oluruz. İşte bu bölümde cevaplarını arayacağımız sorular;

- Bir aggregate'te gerçekleşen bir değişikliği, o aggregate'in söz konusu değişiklikle ilgilenenlerin kim olduğunu bilmesine gerek kalmadan nasıl duyurabiliriz?
- Bir olay *(event)* hangi bilgileri taşımalıdır, hangi bilgileri taşımaMAlıdır?
- Olayları kim, ne zaman yayınlamalıdır? *(Kayıttan önce mi, sonra mı?)*
- Senkron yayınlamanın bedeli nedir, bu bedeli hafifletmek için ne gibi stratejiler kullanılabilir?

## Observer Deseni ve Domain Event Kavramı

Observer, GoF *(Gang of Four)* desenlerinden biridir ve bir nesnenin *(subject)* durumu değiştiğinde ona bağımlı olan nesnelerin *(observers)* otomatik olarak haberdar edilmesini sağlamak için tasarlanmıştır. Buradaki kritik nokta, subject nesnesinin gözlemcilerin kim olduğunu ve ne yaptığını bilmemesidir. Sadece bir olayın gerçekleştiğini duyurur. **Domain Driven Design** yaklaşımında bu fikir **Domain Event** adıyla karşımıza çıkar. Bir domain event, iş alanında *gerçekleşmiş* ve iş birimi açısından anlamlı olan bir olgudur. Bu tanımın üç önemli sonucu vardır.

1. **Geçmiş zaman kipiyle adlandırılır:** *ShipOrder* bir komuttur *(henüz olmamış bir istek olarak düşünülmelidir)*, **OrderShipped** ise bir olaydır *(olmuş bir şeyi ifade eder)*. Komut reddedilebilir ama olay reddedilemez çünkü zaten olmuştur.
2. **Değişmezdir *(immutable)*:** Geçmiş değiştirilemez ve bu yüzden olaylar için **record** kullanmak oldukça doğaldır.
3. **İş dilini *(ubiquitous language)* kullanır:** `OrderStatusChanged(from, to)` gibi teknik bir olay yerine **OrderShipped**, **OrderCancelled** gibi iş dilinde karşılığı olan olayları tercih ederiz. Şöyle düşünelim; Depoda görevli bir personelden *"Sipariş durumu değişti"* gibi bir cümle duymayız, bunun yerine *"sipariş kargolandı"* cümlesini duyarız.

> DDD oldukça geniş bir konudur ve birçok yazılım dili prensibini, tasarım desenini ve mimari yaklaşımı içerir. Bu dönem dersimizde DDD'nin çok temel yönlerini ele almaktayız.

## Olayları *(Events)* Tanımlayalım

İşe ilk olarak `shared-kernel` modülüne bir işaretleyici *(marker)* arayüz ekleyerek başlayalım.

```java
package com.lectures.business.design.shared.kernel;

public interface DomainEvent {
}
```

**DomainEvent** arayüzü *Bir tipin hangi iş modülüne ait olduğunu soramıyorsak o tip shared-kernel'e girer.* ilkesine göre `shared-kernel` modülünde yer alır. Sonuç itibariyle **DomainEvent** kavramı herhangi bir iş modülüne ait değildir ancak **OrderShipped** tamamen **sales** modülüne aittir. Bu arayüz tanımına `occurredAt()` veya `eventId()` gibi birkaç varsayılan davranış eklemeyi düşünebiliriz. Ancak almama sebebibimiz bilinçli bir karardır. **Order** sınıfı zamanı bilmiyor *(ADR-0006, no_hidden_clock)*. Olayın ne zaman gerçekleştiği, olayın kimliği, hangi kullanıcının tetiklediği gibi bilgiler olayı taşıyan altyapının sorumluluğunda. Genellikle gerçek hayat senaryolarında bir mesaj kuyruğu kullanılıyorsa bu bilgiler mesajın zarfına *(envelope)* eklenir. Sipariş olaylarını `sales.domain` paketinde **sealed** bir arayüz altında toplayabiliriz. Dolayısıyla bu modülün sunduğu asıl olaylar *(OrderConfirmed, OrderShipped, OrderCancelled gibi)* OrderEvent arayüzünü implement eder. Aşağıdaki kodlarla devam edelim.

```java
package com.lectures.business.design.sales.domain;

public sealed interface OrderEvent extends DomainEvent
        permits OrderConfirmed, OrderShipped, OrderCancelled {

    OrderId orderId();

    CustomerId customerId();
}
```

```java
public record OrderConfirmed(OrderId orderId, CustomerId customerId, Money total) implements OrderEvent {
}

public record OrderShipped(OrderId orderId, CustomerId customerId, LocalDate shippedDate, Address destination)
        implements OrderEvent {
}

public record OrderCancelled(OrderId orderId, CustomerId customerId, OrderStatus previousStatus)
        implements OrderEvent {
}
```

Her olayın *(record türleri)* onu dinleyecek gözlemcilerin *(observer)* işini görecek kadar bilgi taşıdığına dikkat edelim. **OrderShipped** teslimat adresini, **OrderCancelled** ise iptal edilmeden önceki durumu taşıyor. Olay nesnelerin ne taşıması gerektiğine karar vermek de önemli bir mevzu. Söz gelimi neden **OrderCanceled** olayı iptal edilmeden önceki durumu taşıyor; zira onaylanmış bir siparişin iptali ile taslak bir siparişin iptali business anlamda farklı sonuçlar doğurabilir. Örneğin onaylanmış bir siparişte stok ayrılmıştır ve bu stoğun serbest bırakılması gerekebilir.

> Bir olaya ne kadar veri koymalıyız sorusunu cevaplamak kolay değildir. Literatürde iki uç yaklaşım olduğu görülüyor. **Event Notification** yaklaşımında olay sadece kimlik bilgisini taşır *(`OrderShipped(orderId)`)* ve ilgilenen taraf geri kalan bilgiyi bu değeri kullanarak kendisi okur. **Event-Carried State Transfer** yaklaşımında ise olay gözlemcinin ihtiyaç duyacağı tüm bilgiyi kendi üzerinde taşır. İlk yöntemde daha az bağlantı *(coupling)* yaratır ama gözlemciyi kaynağa sorgu atmaya zorlar. İkinci teknikse gözlemciyi bağımsız kılar ancak olayın şeması bir sözleşmeye dönüşür. Biz dersimiz boyunca orta noktada kalmaya çalışacağız.

## Aggregate Nesnesinin Görevi

**Order** sınıfının yukarıda tanımladığımız olayları üretmesi gerekiyor. Ancak burada bahsedeceğimiz şey sınıfın olayları yayınlaması değil biriktirmesi ve gerektiğinde bu olayları dışarıya sunmasıdır. Aggregate nesnesi, kendi iç durumunu değiştiren olayları kaydeder ve bu olaylar daha sonra sistemin diğer parçaları tarafından işlenebilir. Buna göre **Order** sınıfındaki `confirm()`, `ship()` ve `cancel()` metodlarında bazı değişiklikler yapacağız ve bu metodlar artık ilgili olayları `pendingEvents` listesine ekleyecek.

```java
private final List<OrderEvent> pendingEvents = new ArrayList<>();

public void confirm() {
    requireModifiable("confirm");
    // Diğer kontroller ve işlemler
    transitionTo(OrderStatus.CONFIRMED);
    pendingEvents.add(new OrderConfirmed(orderId, customerId, total()));
}

public void ship(LocalDate date) {
    requireTransition(OrderStatus.SHIPPED);
    // Diğer kontroller ve işlemler
    shippedDate = date;
    status = OrderStatus.SHIPPED;
    pendingEvents.add(new OrderShipped(orderId, customerId, date, shippingAddress));
}

public void cancel() {
    OrderStatus previous = status;
    transitionTo(OrderStatus.CANCELLED);
    pendingEvents.add(new OrderCancelled(orderId, customerId, previous));
}

public List<OrderEvent> pullEvents() {
    List<OrderEvent> pulled = List.copyOf(pendingEvents);
    pendingEvents.clear();
    return pulled;
}
```

Neler eklediğimizi dikkatlice inceleyelim. Domain olayları her zaman kural kontrollerinden sonra ekleniyor. Böylece reddedilen bir geçiş *(örneğin taslak bir siparişin kargolanması)* exception fırlattığında hiçbir olay kaydedilmemiş oluyor. Diğer yandan `addLine` ve `removeLine` gibi metotların olay üretmediğini görüyoruz. Bunun en büyük sebebi iş birimi açısından henüz dış dünyaya duyurulacak olguların oluşmamış olması. Ama tabii ki bunlar kesin kurallar değil, iş biriminin ihtiyaçlarına göre değişebilir. Şimdi bu senaryoda **Order** sınıfından neden doğrudan olay yayınlamadığımızı tartışalım. Muhtemelen bunun için `ship(LocalDate)` metodunu aşağıdaki gibi değiştirecektik.

```java
public void ship(LocalDate date) {
    ...
    status = OrderStatus.SHIPPED;
    DomainEvents.raise(new OrderShipped(...));   // statik, global bir yayıncı olduğunu düşünelim.
}
```

Bu tür bir kullanımının bazı sorunları vardır.

- **Order** nesnesi global bir duruma bağlanır. Testler her çalıştırmadan önce bu yayıncıyı temizlemek zorunda kalır ve paralel çalışan testler birbirinin olaylarını görebilir.
- Olay, sipariş henüz kaydedilmeden dış dünyaya çıkar. Gözlemci tarafı müşteriye *"siparişiniz kargolandı"* e-postasını gönderir. Bir milisaniye sonra `orders.save(order)` çağrısı başarısız olursa, müşteri hiç gerçekleşmemiş bir kargo için bildirim almış olur.
- Domain katmanı olay yayınlama *(event publisher)* mekanizmasını tanımak zorunda kalır. Karara bağladığımız ADR-0003 belgesine göre domain hiçbir katmanı göremez.

Ancak bizim tercih ettiğimiz ve olayları bir listede topladığımız yaklaşımında, yayınlama zamanına aggregate yerine onu yükleyip kaydeden uygulama servisi karar verir.

## Yayıncının *(Publisher)* Tasarımı

Şu anda bu iş uygulama katmanının sorumluluğunda. Yayıncı nesneleri varsayılan davranışı olan bir interface olarak tasarlayabiliriz.

```java
package com.lectures.business.design.sales.application;

import com.lectures.business.design.shared.kernel.DomainEvent;
import java.util.List;

@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);

    default void publishAll(List<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
```

`publish` metodu tek bir domain olayını yayınlamak için kullanılırken, `publishAll` metodu bir liste halinde gelen tüm domain olaylarını yayınlamak için kullanılır. Bu sayede uygulama servisi, aggregate'ten çektiği olayları topluca yayınlayabilir. `publishAll` metodu parametre olarak **DomainEvent** türevlerinden oluşan bir liste alır. Şimdi bu arayüzü implemente eden `in-process` çalışan bir publisher sınıfı yazalım.

```java
package com.lectures.business.design.sales.application;

import com.lectures.business.design.shared.kernel.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class InProcessEventPublisher implements DomainEventPublisher {

    private record Subscription<E extends DomainEvent>(Class<E> type, Consumer<? super E> handler) {

        void deliver(DomainEvent event) {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }
    }

    private final List<Subscription<?>> subscriptions = new ArrayList<>();

    public <E extends DomainEvent> InProcessEventPublisher subscribe(Class<E> type, Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        subscriptions.add(new Subscription<>(type, handler));
        return this;
    }

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (Subscription<?> subscription : subscriptions) {
            subscription.deliver(event);
        }
    }
}
```

InProcessEventPublisher, yukarıda tanımladığımız `DomainEventPublisher` arayüzünü implemente eden bir sınıftır. Bu sınıf, uygulama içinde olayları senkron bir şekilde dağıtmak için kullanılır. `subscribe` metodu ile belirli bir türdeki olaylara abone olunabilir ve `publish` metodu ile olaylar ilgili abonelere iletilir. Bunun dışında;

- `Consumer<? super E>` ifadesi sayesinde **OrderEvent** tipindeki tüm olayları dinleyen bir gözlemci *(observer)*, **OrderShipped** aboneliği için de kullanılabilir. Bu PECS *(Producer Extends, Consumer Super)* kuralının ders kitabı örneklerinden de biridir. Gözlemci olayı tüketir ve bu yüzden `super` kullanılır.
- Abonelik `Class<E>` nesnesini de saklar. Java'da generic tipler çalışma zamanında silindiği *(type erasure)* için `Consumer<OrderShipped>` nesnesinin hangi tipi beklediğini çalışma zamanında öğrenemeyiz. Tip bilgisini `Class` nesnesi olarak yanımızda taşımak zorundayız.
- **isInstance** kontrolü sayesinde bir gözlemci kendi tipini ve onun tüm alt tiplerini görebilir. Yani bir gözlemci, sadece tam olarak beklediği tipteki olayları değil, aynı zamanda o tipin alt tiplerini de alabilir. Bir başka deyişle `DomainEvent.class` ile abone olan bir gözlemci sistemdeki bütün olayları alır.

## Gözlemciler*(Observers)*

İki gözlemci yazarak devam edelim. Bunlardan birisi müşteriye bildirim gönderme işini üstlenirken, diğeri tüm sipariş olaylarını okuyup loglama işini *(şimdilik sadece log kaydı döndürmeyi)* üstlenecek. İlk olarak **ShipmentNotificationHandler** sınıfı ile müşteri bildirimlerini ele alalım. Tabii öncesinde bildirim işini soyutlamamız lazım. Yeni bir interface ile bunu aşağıdaki gibi tanımlayabiliriz.

```java
package com.lectures.business.design.sales.application;

@FunctionalInterface
public interface CustomerNotifier {

    void notify(String customerId, String message);
}
```

Bu sınıf bir nevi **hexagonal mimari** terminolojisinden gelme diyebiliriz. Aslında uygulamamız için bir çıkış portu *(outgoing port)* görevi üstleniyor. **sales** modülü bildirimin e-posta ile mi, SMSM ile mi ya da terminale log bırakarak mı yapılacağını bilmek zorunda değil. İlerleyen kısımlarda **app** modülüne ekleyeceğimiz bir implementasyon ile bu bildirimi deneyebiliriz.

```java
package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderShipped;
import java.util.Objects;
import java.util.function.Consumer;

public final class ShipmentNotificationHandler implements Consumer<OrderShipped> {

    private final CustomerNotifier notifier;

    public ShipmentNotificationHandler(CustomerNotifier notifier) {
        this.notifier = Objects.requireNonNull(notifier, "notifier must not be null");
    }

    @Override
    public void accept(OrderShipped event) {
        notifier.notify(event.customerId().value(),
                "Order " + event.orderId() + " shipped on " + event.shippedDate()
                + " to " + event.destination().city());
    }
}
```

Dikkat edileceği üzere **CustomerNotifier** bileşeni **ShipmentNotificationHandler** sınıfına yapıcı üzerinden enjekte ediliyor. Bu sayede bildirim mekanizması değiştiğinde sadece ilgili implementasyonu değiştirerek uygulamanın geri kalanını etkilemeden yeni bir bildirim yöntemini sisteme dahil edebiliriz. `java.util.function` paketinden gelen **Consumer** arayüzü sayesinde de **ShipmentNotificationHandler** sınıfı bir olay dinleyici gibi davranabiliyor ve **OrderShipped** olaylarını işleyebiliyor. Olayı biraz daha iyi anlamak için application paketine ikinci gözlemciyi de ekleyelim.

```java
package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.domain.OrderCancelled;
import com.lectures.business.design.sales.domain.OrderConfirmed;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderShipped;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class OrderActivityLog implements Consumer<OrderEvent> {

    private final List<String> entries = new ArrayList<>();

    @Override
    public void accept(OrderEvent event) {
        entries.add(describe(event));
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }

    static String describe(OrderEvent event) {
        return switch (event) {
            case OrderConfirmed confirmed ->
                confirmed.orderId() + " confirmed, total " + confirmed.total();
            case OrderShipped shipped ->
                shipped.orderId() + " shipped on " + shipped.shippedDate();
            case OrderCancelled cancelled ->
                cancelled.orderId() + " cancelled while " + cancelled.previousStatus();
        };
    }
}
```

**switch** ifadesinde **default** dalı yok. **OrderEvent** sealed bir nesne olduğu için derleyici olası tüm alt tipleri zaten biliyor ve hepsinin ele alınıp alınmadığını kontrol ediyor *(exhaustiveness check)*. Sonradan **OrderDelivered** adında yeni bir olay eklendiğinde bu metot derlenmeyecektir ki bu iyi bir şeydir. Enum'ları için şu soruyu göz önüne alalım; Yeni bir durum eklendiğinde kodun hangi noktalarının değişmesi gerektiğini bize derleyici söyleyebilir mi? Bu senaryoda olduğu gibi **sealed** arayüzler ve pattern matching ile birlikte bu kontrolü derleyiciye bırakabiliriz *(Farklı dillerde bu durum nasıl karşılanıyor araştıralım. Rust, OCaml, C# gibi)*

Artık **OrderService** sınıfı publisher nesnesi kullanabilir. Tabii sınıfa yeni bir bağımlılık ekliyoruz ve yapıcı metot parametre sayısını beşin üzerine çıkarıyoruz *(Bunu ileride tekrardan değerlendireceğiz)* Sonrasında **change** metodunda persistence'a kaydetme çağrısının hemen arkasından olayların yayınlanmasını sağlayabiliriz. **OrderService** sınıfının güncel hali aşağıdaki gibidir.

```java
package com.lectures.business.design.sales.application;

import com.lectures.business.design.sales.api.OrderLineRequest;
import com.lectures.business.design.sales.api.OrderSummary;
import com.lectures.business.design.sales.api.PlaceOrderCommand;
import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import com.lectures.business.design.sales.domain.Order;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderId;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import com.lectures.business.design.shared.kernel.Money;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;
import java.util.function.Consumer;

public final class OrderService {

    private static final Currency TRY = Currency.getInstance("TRY");

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final OrderFactory factory;
    private final OrderMapper mapper;
    private final Clock clock;
    private final DomainEventPublisher events;

    public OrderService(OrderRepository orders, CustomerRepository customers,
            OrderFactory factory, OrderMapper mapper, Clock clock, DomainEventPublisher events) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
        this.customers = Objects.requireNonNull(customers, "customers must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.events = Objects.requireNonNull(events, "events must not be null");
    }

    public OrderSummary placeOrder(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Customer customer = customers.getById(CustomerId.of(command.customerId()));

        Order order = factory.draftFor(customer);
        for (OrderLineRequest line : command.lines()) {
            order.addLine(line.productId(), new Money(line.unitPrice(), TRY),
                    line.quantity(), line.discount());
        }
        orders.save(order);
        return mapper.toSummary(order);
    }

    public OrderSummary confirm(int orderId) {
        return change(orderId, Order::confirm);
    }

    public OrderSummary ship(int orderId) {
        return change(orderId, order -> order.ship(LocalDate.now(clock)));
    }

    public OrderSummary cancel(int orderId) {
        return change(orderId, Order::cancel);
    }

    public OrderSummary getOrder(int orderId) {
        return mapper.toSummary(orders.getById(OrderId.of(orderId)));
    }

    private OrderSummary change(int orderId, Consumer<Order> action) {
        Order order = orders.getById(OrderId.of(orderId));
        action.accept(order);
        orders.save(order);
        events.publishAll(order.pullEvents());
        return mapper.toSummary(order);
    }
}
```

Tabii bu güncelleme tahim edileceği üzere OrderService sınıfının kullanıldığı test metotları, SalesModule, BusinessDesign sınıfları gibi birçok bileşende de değişikliğe gitmemiz gerekiyor. Öncelikle nereleri nasıl değiştirmemiz gerektiğini bulmaya çalışalım. Build sırasında hata alan yerleri gözden geçirmek iyi bir fikir olacaktır *(Sonrasında OrderServiceTest, SalesModule içeriklerini kontrol edelim)*

**SalesModule** sınıfının güncel halini aşağıda görebilirsiniz.

```java
package com.lectures.business.design.app;

import com.lectures.business.design.sales.application.CustomerNotifier;
import com.lectures.business.design.sales.application.InProcessEventPublisher;
import com.lectures.business.design.sales.application.OrderActivityLog;
import com.lectures.business.design.sales.application.OrderMapper;
import com.lectures.business.design.sales.application.OrderService;
import com.lectures.business.design.sales.application.SalesReport;
import com.lectures.business.design.sales.application.ShipmentNotificationHandler;
import com.lectures.business.design.sales.domain.OrderEvent;
import com.lectures.business.design.sales.domain.OrderFactory;
import com.lectures.business.design.sales.domain.OrderShipped;
import com.lectures.business.design.sales.domain.SequentialOrderIdGenerator;
import com.lectures.business.design.sales.persistence.CustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryCustomerRepository;
import com.lectures.business.design.sales.persistence.InMemoryOrderRepository;
import com.lectures.business.design.sales.persistence.OrderRepository;
import java.time.Clock;
import java.util.Objects;

public final class SalesModule {

    private final CustomerRepository customers;
    private final OrderService orderService;
    private final SalesReport salesReport;
    private final OrderActivityLog activityLog = new OrderActivityLog();

    private SalesModule(OrderRepository orders, CustomerRepository customers, Clock clock,
            int firstOrderId, CustomerNotifier notifier) {
        this.customers = customers;

        InProcessEventPublisher publisher = new InProcessEventPublisher()
                .subscribe(OrderEvent.class, activityLog)
                .subscribe(OrderShipped.class, new ShipmentNotificationHandler(notifier));

        OrderMapper mapper = new OrderMapper();
        OrderFactory factory = new OrderFactory(new SequentialOrderIdGenerator(firstOrderId), clock);

        this.orderService = new OrderService(orders, customers, factory, mapper, clock, publisher);
        this.salesReport = new SalesReport(orders, mapper);
    }

    public static SalesModule inMemory(Clock clock, int firstOrderId, CustomerNotifier notifier) {
        Objects.requireNonNull(clock, "clock must not be null");
        Objects.requireNonNull(notifier, "notifier must not be null");
        return new SalesModule(new InMemoryOrderRepository(), new InMemoryCustomerRepository(),
                clock, firstOrderId, notifier);
    }

    public CustomerRepository customers() {
        return customers;
    }

    public OrderService orderService() {
        return orderService;
    }

    public SalesReport salesReport() {
        return salesReport;
    }

    public OrderActivityLog activityLog() {
        return activityLog;
    }
}
```

SalesModule composition root görevini üstlenen sınıflardan birisi. Dolayısıyla observer kaydının yapılacağı yer de burası. **InProcessEventPublisher** nesnesini nasıl kullandığımıza dikkat edelim. Nesne tanımı sonrası ardışıl olarak iki kez `subscribe` metodu çağrılarak farklı olay türleri için dinleyiciler ekleniyor. Bunlardan birisi `OrderEvent` türündeki olayları `OrderActivityLog` ile kaydeden dinleyici, diğeri ise `OrderShipped` türündeki olayları `ShipmentNotificationHandler` ile işleyen dinleyici. Dolayısıyla yarın stok düşme talebi geldiğinde tek yapmamız gereken yeni bir gözlemci yazıp bu listeye bir satır eklemekten ibaret olacak. **Order**, **OrderService** sınıflarına dokunmadan bunu yapabileceğiz ve böylece SOLID prensiplerinden `Open/Closed` ilkesine uygun davranmış olacağız.

SalesModule nesnesini kullandığımız yerlerden birisi **app** modülü. Yeni yapıya göre burada da bazı düzenlemeler yapmamız gerekiyor. Örneğin app modülünde yer alan **BusinessDesign** sınıfındaki `runSalesScenario` metodunu ele alalım. Bu metot **SalesModule** sınıfını kullanarak örnek bir senaryo işletiyor. SalesModule artık bir CustomerNotifier ile çalıştığı için buna göre bir düzenleme yapmalıyız. CustomerNotifier türünden bir implementasyona burada işimizi görecektir. app modülü terminal tabanlı bir uygulama olduğu için event bildirimlerini terminale yazdıran bir implementasyon kullanabiliriz.

```java
final class ConsoleCustomerNotifier implements CustomerNotifier {

    @Override
    public void notify(String customerId, String message) {
        System.out.println("  [notify " + customerId + "] " + message);
    }
}
```

Buna göre **runSalesScenario** metodunu da aşağıdaki gibi düzenleyebiliriz.

```java
System.out.println("=== Sales scenario ===");

SalesModule module = SalesModule.inMemory(CLOCK, 10248, new ConsoleCustomerNotifier());
module.customers().save(new Customer(CustomerId.of("VINET"), "Vins et alcools Chevalier",
        new Address("59 rue de l'Abbaye", "Reims", "51100", "France")));
module.customers().save(new Customer(CustomerId.of("TOMSP"), "Toms Spezialitaten",
        new Address("Luisenstr. 48", "Munster", "44087", "Germany")));

OrderService orders = module.orderService();

int shipped = orders.placeOrder(new PlaceOrderCommand("VINET",
        List.of(OrderLineRequest.of(11, "14.00", 10)))).orderId();
orders.confirm(shipped);
orders.ship(shipped);

int confirmed = orders.placeOrder(new PlaceOrderCommand("VINET",
        List.of(OrderLineRequest.of(11, "10.00", 3)))).orderId();
orders.confirm(confirmed);

int cancelled = orders.placeOrder(new PlaceOrderCommand("TOMSP",
        List.of(OrderLineRequest.of(11, "99.00", 100)))).orderId();
orders.cancel(cancelled);

OrderSummary draft = orders.placeOrder(new PlaceOrderCommand("TOMSP",
        List.of(OrderLineRequest.of(11, "20.00", 51))));
System.out.println("Placed draft: " + draft);

try {
    orders.ship(draft.orderId());
} catch (DomainException e) {
    System.out.println("Rejected by the aggregate: " + e.getMessage());
}

SalesReport report = module.salesReport();

System.out.println();
System.out.println("Order counts by status;");
report.countByStatus().forEach((status, count)
        -> System.out.println("  " + status + ": " + count));

System.out.println();
System.out.println("Revenue by customer;");
for (CustomerRevenue row : report.revenueByCustomer()) {
    System.out.println("  " + row.customerId() + ": " + row.revenue() + " " + row.currency());
}

System.out.println();
System.out.println("Totals;");
System.out.println("  " + report.totals());

System.out.println();
System.out.println("Activity log (an observer of every OrderEvent);");
module.activityLog().entries().forEach(entry -> System.out.println("  " + entry));
```

**app** projesini çalıştırdığımızda aşağıdaki çıktıyı elde ederiz.

```bash
cd .\week11-domain-events\app
mvn "-Dexec.mainClass=com.lectures.business.design.app.BusinessDesign" exec:java
```

![Observed output](./images/week_11_00.png)

## Dual-Write Problemi

Şu ana kadar her şey yolunda görünse de dikkat etmemiz gereken bir konu var. Bir observer nesnesi hata fırlatırsa ne olur? Aşağıdaki test metodunu bu açıdan değerlendirmeye çalışalım *([OrderServiceEventsTest](./src/week11-domain-events/sales/src/test/java/com/lectures/business/design/sales/application/OrderServiceEventsTest.java))*

```java
@Test
@DisplayName("the dual-write gap: the order is stored, the notification is lost")
void aFailingObserverLeavesHalfTheWorkDone() {
    int id = placeAndConfirm();
    mailGateway = (customerId, message) -> {
        throw new IllegalStateException("mail server down");
    };

    assertThatThrownBy(() -> service.ship(id)).hasMessage("mail server down");
    assertThat(orders.getById(OrderId.of(id)).status()).isEqualTo(OrderStatus.SHIPPED);
    assertThat(orders.getById(OrderId.of(id)).pullEvents()).isEmpty();
    assertThat(notifications).isEmpty();
    assertThatThrownBy(() -> service.ship(id))
            .isInstanceOf(InvalidStateTransitionException.class);
}
```

Test metodunda neler olduğunu adım adım inceleyelim;

1. `order.ship()` çağrısı başarılı oldu, sipariş bellekte `SHIPPED` durumuna geçti.
2. `order.save()` çağrısı başarılı oldu, sipariş kalıcı olarak `SHIPPED` durumuyla kaydedildi.
3. `pullEvents()` çağrısı olayları aldı ve aggregate olay günlüğünü temizledi, event artık sadece yerel bir değişkende duruyor.
4. Bildirimleri gözlemleyen nesne hata fırlattı, olay kayboldu ve müşteriye bildirim de gitmedi.
5. Kullanıcı hata mesajını gördü ve işlemi tekrar denedi. Bingo! Sipariş zaten `SHIPPED` durumunda olduğu için ikinci deneme `InvalidStateTransitionException` fırlattı.

Görüldüğü üzere sistem tutarsız bir duruma düştü. Sipariş kargolandı ama müşteri bundan haberdar değil ve kullanıcı işlemin başarısız olduğunu düşünüyor. Bu sorunun temel nedeni iki farklı kaynağa *(bu senaryoda sipariş deposu ve e-posta sunucusu)* tek bir atomik işlem içinde veri yazmaya çalışmamız. *(dual-write problemini araştıralım)*

Bu problem için kullanılan bazı yaygın çözümleri aşağıdaki tabloyla özetleyebiliriz.

| **Çözüm** | **Fikir** | **Bedeli *(trade-off)*** |
| --- | --- | --- |
| Observer hatalarını yutmak *(try-catch + log)* | Kullanıcı hatayı görmez | Olay sessizce ve yine kaybolur. |
| Önce yayınla *(publish)*, sonra kaydet *(persist)* | Observer hatası kaydı engeller | Kayıt başarısız olursa olmayan bir olay duyurulmuş olur ve problem tersine döner |
| Observer nesnesini transaction tamamlandıktan sonra çalıştırmak | CDI'daki `@Observes(during = TransactionPhase.AFTER_SUCCESS)` | Gözlemci hata alırsa olay yine kaybolur ama en azından olmayan bir şey duyurulmaz |
| **Transactional Outbox** | Olay, siparişle aynı veritabanı transaction'ı içinde bir `outbox` tablosuna yazılır. Ayrı bir süreç bu tabloyu okuyup olayları yayınlar | Ek bir tablo ve süreç gerekir. Olay *en az bir kez* *(at-least-once)* teslim edilir, bu yüzden gözlemciler idempotent olmalıdır *(Bu ders müferdatımız için ileri seviye bir konu olsa da araştırmaya değer. Özellikle karşıt görüşleri de mutlaka araştırmak gerekir)* |

En sık kullanılan çözüm **Transactional Outbox** desenidir. Ancak bu desen bir veritabanı transaction'ı gerektirir ve bizim henüz bir veritabanımız yok. Bir sonraki bölümde JDBC ile veritabanına geçeceğiz. Lab projesinde ise Outbox deseni RabbitMQ ile birlikte ele alınmaya çalışılacak. Bu bölümdeki test, o desene neden ihtiyaç duyduğumuzu gösteren canlı bir belge niteliğinde şimdilik kalabilir.

## Aynı Yapı CDI ile Nasıl Görünürdü?

**Jakarta CDI**, Observer desenini platformun bir parçası olarak sunar. Aşağıdaki kodu karşılaştırma amacıyla değerlendirebiliriz.

```java
@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderEvent> events;          // DomainEventPublisher'ın karşılığı

    private OrderSummary change(int orderId, Consumer<Order> action) {
        ...
        orders.save(order);
        order.pullEvents().forEach(events::fire);
        ...
    }
}

@ApplicationScoped
public class ShipmentNotificationHandler {

    // subscribe(OrderShipped.class, ...) satırının karşılığı
    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderShipped event) {
        ...
    }
}
```

| **Bu bölümdeki yapı** | **CDI karşılığı** |
| --- | --- | --- |
| `DomainEventPublisher.publish` | `Event<T>.fire()` |
| `subscribe(OrderShipped.class, handler)` | `@Observes OrderShipped` parametresi |
| Senkron, aynı thread | `@Observes` |
| *(yok)* | `fireAsync()` ve `@ObservesAsync` |
| *(yok)* | `@Observes(during = AFTER_SUCCESS)` |

CDI'da aboneliği composition root'un yapmasına gerek yoktur. **Container**, uygulama ayağa kalkarken `@Observes` parametrelerini tarar ve abonelikleri kendisi oluşturur. Tabii bunun dezavantajı da vardır; hangi olayı kimin dinlediğini görmek için artık tek bir sınıfa bakmak yeterli değildir.

## Mimari Kurallara Üç Ekleme

Domain Event kullanımlarını ele aldığımız bu bölümden [ADR-0007-Domain Event'ler ve Senkron Yayınlama](docs/adr/ADR-0007.md) şeklinde bir karar kaydı da oluşturabiliriz. Bu yeni kuralları da ArchitectureTest sınıfına aşağıdaki gibi ekleyebiliriz.

```java
//  ADR-0007 Domain events
@ArchTest
static final ArchRule domain_events_are_records = classes()
        .that().implement(DomainEvent.class)
        .and().areNotInterfaces()
        .should().beAssignableTo(Record.class);

@ArchTest
static final ArchRule domain_events_live_in_the_domain = classes()
        .that().implement(DomainEvent.class)
        .should().resideInAnyPackage("..sales.domain..", "..shared.kernel..");

@ArchTest
static final ArchRule only_application_pulls_events = noClasses()
        .that().resideOutsideOfPackage("..sales.application..")
        .should().callMethod(Order.class, "pullEvents");
```

| **Kural *(ArchRule alanı)*** | **ADR** | **Neyi Doğrular?** |
| --- | :---: | --- |
| **domain_events_are_records** | 0007 | `DomainEvent` arayüzünü uygulayan ve arayüz olmayan her sınıfın bir record olduğunu doğrular. ArchUnit 1.5'te `beRecords()` gibi hazır bir ifade bulunmadığı için tüm record'ların ortak atası olan `java.lang.Record` tipine atanabilirliği kontrol ediyoruz. |
| **domain_events_live_in_the_domain** | 0007 | Olayların yalnızca domain veya shared-kernel paketlerinde tanımlandığını doğrular. application veya persistence paketinde bir olay tanımlamak, olayın iş dilinden kopması demektir. |
| **only_application_pulls_events** | 0007 | `Order.pullEvents()` metodunun yalnızca application paketinden çağrıldığını doğrular. Örneğin persistence paketindeki bir repository olayları çekerse, olaylar kimse tarafından yayınlanmadan kaybolur. |

---

## Bölüm Soruları

// EKLENECEK

## Bu bölümün kodu

- [week11-domain-events](src/week11-domain-events)

```powershell
cd src\week11-domain-events

# Bölüm 11'deki tüm modüllerin testleri
mvn test

# Yalnızca mimari testler ve ihtiyaç duyduğu modüller
mvn -pl quality/architecture-tests -am test
```

<!-- nav:bottom:start -->
---

| ← Önceki Bölüm | İçindekiler | Sonraki Bölüm → |
| :--- | :---: | ---: |
| [Bölüm 10: Uygulama Servisleri ve Manuel Dependency Injection](java-chapter-10.md#top) | [Ders Notları](README.md#top) | [Bölüm 12: Persistence Boundary, JDBC ve Flayway](java-chapter-12.md#top) |
<!-- nav:bottom:end -->
