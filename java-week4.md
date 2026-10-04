# Hafta 04

İş nesnelerini incelemek için işe Customer tasarımı ile başlamıştık. İlk tasarım birçok kusuru içermekteydi. Elimizdeki materyaller ve kavramlar arttıkça iş nesnelerinin daha sağlam ve güvenilir bir şekilde tasarlanabileceğini fark ediyoruz. Kobay veritabanımız Northwind'in Customers tablosunu ifade edecek sınıf tasarımını yeniden değerlendirelim. Domain kuralına göre her customerın ID bilgisinin 5 karakterden oluşan bir değer olması bekleniyor *(ALFKI, VINET gibi)*. Ayrıca artık bir müşterinin adres bilgisine ait detaylar domain katmanında Address tipi ile ifade edilmekte. Bunlara ek diğer iş kuralları ile birlikte Customer sınıfını bir aggregate olarak ele alabiliriz.

## Customer Sınıfı Tasarımı

```java
public final class Customer {

    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Z]{5}$");

    private final String customerId;
    private String companyName;
    private String contactName;
    private Address address;

    public Customer(String customerId, String companyName, Address address) {
        this.customerId = normaliseId(customerId);
        this.companyName = requireText(companyName, "companyName");
        this.address = Objects.requireNonNull(address, "address must not be null");

    }

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

    public String customerId() {
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

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    private static String normaliseId(String value) {
        String id = requireText(value, "customerId").toUpperCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "customerId must be exactly five letters (A-Z): " + value);
        }
        return id;
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
}
```

Customer sınıfının bu yeni tasarımındaki üyeleri kısaca tanıyalım.

| **Üye** | **Açıklama** |
| --- | --- |
| `customerId` | Müşterinin benzersiz kimliğini ifade eder. Beş harfli bir metin olmalıdır. |
| `companyName` | Şirket adını ifade eder. Boş olamaz. |
| `contactName` | İletişim kurulan kişinin adını ifade eder. Boş olabilir. |
| `address` | Müşterinin adresini ifade eder. Address türünden bir nesnedir. Boş olamaz. |
| `ID_PATTERN` | Müşteri kimliğinin doğrulanmasında kullanılan regex ifadesidir. |
| `requireText` | Metin alanlarının boş olup olmadığını kontrol eden yardımcı metot. Sınıf içi üyelere hizmet ettiğinde private static olarak tanımlanmıştır. |
| `normaliseId` | Müşteri kimliğini normalize eden ve doğrulayan yardımcı metot. Private static olarak tanımlanmıştır. |
| `equals` | Override edilen bu metot iki Customer nesnesinin eşitliğini sadece customerId alanını karşılaştırarak belirler. |
| `hashCode` | Override edilen bu metot Customer nesnesinin hash kodunu customerId alanına göre üretir. |
| `toString` | Override edilen bu metot Customer nesnesinin string temsilini döndürür. |
| `clearContact` | İletişim kurulan kişinin adını temizleyen metot. `contactName` alanını `null` yapar. |
| `relocateTo` | Müşterinin adresini güncelleyen metot. `address` alanını verilen yeni adresle değiştirir. Hatırlayalım Address bir record'a dönüştü ve immutable. |
| `renameTo` | Müşterinin şirket adını güncelleyen metot. `companyName` alanını verilen yeni adla değiştirir. |
| `assignContact` | İletişim kurulan kişinin adını güncelleyen metot. `contactName` alanını verilen yeni adla değiştirir. |

Sistemdeki bir müşteri ID bilgisi ile diğerlerinden ayrılır. Bu nedenle `equals` ve `hashCode` metotları sadece `customerId` alanına göre işlem yapar. customerId alanı müşteri kimliğini ifade eder ve Customer sınıfının bir Entity olacağını gösterir. Diğer yandan bir müşteri address bilgisini de Address record türü ile taşır. Customer nesnesi dolaşıma girdiğinde yapıcı metot üzerinden bir Address bilgisinin de sağlanması gerekir. Buna göre Customer'ın bir aggregate root olarak değerlendirilmesi mantıklıdır. *(Tartışalım, kime Entity kime Value Object denir, aggregate root ne zaman ortaya çıkar)*

Burada duralım ve bir önceki derste tasarladığımız Order sınıfında müşteri bilgisinin nasıl ele alındığına bakalım. Hatırlanacağı üzere bir siparişin müşteri ile de ilişkilendirilmesi beklenir. Order sınıfında müşteri bağlantısı String türünden olan customerId alanı üzerinden karşılanmaktadır. Bu yaklaşım, müşteri bilgilerini doğrudan Order nesnesine dahil etmek yerine sadece müşteri kimliğini saklamayı tercih eder. Bu bilinçli bir tercihtir zira Order nesnesi dolaşımdayken beraberinde bir Customer nesne örneğini dolaştırmayı gerektirmez. Buradaki durum Order ve OrderLine arasındaki ilişki gibi değildir. Bir sipariş, sipariş kalemleri olduğu sürece anlamlı bir nesnedir. Benzer teori Customer ve Address arasında da geçerlidir; bir Customer nesnesi dolaşımdayken beraberinde Address nesnesini taşır, zira bir müşterinin adresi olmadan müşteri tam anlamıyla tanımlanamaz. Bu ifadeler yoruma açıktır. Zira Order üzerinde müşteri bilgisini Customer türünden bir nesne ile taşımak yasak değildir fakat bir maliyeti olabilir.

## Aggregate Sınırları, Parça, Köken *(Root)* ilişkileri

Şu sorunun cevabını arayalım; *Order sınıfı neden Customer türünden bir nesne tutmaz?*

Orde sınıfının Customer bilgisini nesne olarak tuttuğunu düşünelim. Kuvvetle muhtemel aşağıdakine benzer bir tasarıma gideriz.

```java
public final class Order{
    private final Customer customer;
    private final List<OrderLine> orderLines = new ArrayList<>();
}
```

Şimdi bu tasarıma aşağıdaki soruları soralım *(Elimizde Address, Order, OrderLine ve Customer tasarımlarının güncel halleri var)*

- **Bu tasarıma göre bir siparişi yüklemek başka neleri yükler?** Şu anki tasarımımıza göre sipariş yüklenirken beraberinde Customer nesnesini ve dolayısıyla Customer'ın Address bilgisini yükler. İlaveten OrderLine nesneleri de yüklenir. Tek bir sipariş satırını okumak istediğimizde ortama gereğinden fazla nesne yüklemiş oluruz. Buradaki basit kurguda dahi nesne grafiği bir noktadan sonra kontrolden çıkabilir.
- **Bir işlemde kaç nesne değişir?** Order nesnesini kullanan object user açısından baktığımızda pekala bir sipariş bilgisi üzerinden müşteri bilgisi kod yoluyla değiştirilebilir. Yani müşterinin contact bilgisi alakalı olmadığı halde sipariş bilgisi üzerinden değiştirilebilir. Özellikle transaction sınırının belirsizleştiği bir durumla karşı karşıya kalırız. Görüldüğü üzere domain tasarımı transaction sınırları dahil birçok yeri etkileyen bir faktör.
- **Bu iki nesne aynı modülde midir?** Sistem tasarlanırken bu enstrümanların aynı modül içerisinde olacağının bir garantisiz yoktur. Örneğin Order ve OrderLine, sales modülünün bir parçası iken Customer ve Address, crm modülünün bir parçası olabilir. Bu durumda Order sınıfının Customer nesnesini doğrudan tutması beraberinde bazı zorluklar da getirecektir. İki modül arasındak iletişimin nasıl sağlanacağı sorusu bunun güzel bir örneğidir. Bir API üzerinden iletişim kurmak ya da bir mesajlaşma altyapısı kullanmak gibi çözümler söz konusudur. Böyle bir senaryoda Order sınıfının Crm modülüne ait bir Customer nesnesini doğrudan tutması mümkün olmayacaktır. Kendi modülünde pekala bir Customer tasarımı içerebilir ancak müşteri bilgisinin asıl sahibi olan Crm ile senkronizasyon mekanizmasını da düşünmek zorunda kalırız.

> Görüldüğü üzere bir siparişin ilişkili olduğu müşteri bilgisini de içeren bir sınıfı bildiğimiz yollarla tasarlamak oldukça kolayken, tasarımın yer aldığı domain'in kullanıldığı bağlamda dikkat edilmesi gereken birçok nokta ortaya çıkmaktadır.

Buraya kadar anlattıklarımızdan yola çıkarak iki aggregate arasında refernas kimlikleri ile ilişki kurmanın daha uygun olduğunu söyleyebiliriz. Yani Order sınıfı doğrudan Customer nesnesini tutmak yerine sadece Customer'ın kimliğini tutar. Bu yaklaşım, yukarıda bahsedilen yükleme, değişim ve modül bağımlılığı sorunlarını minimize eder. Ek olarak bu yöntem aggregate'ler arası bileşen bağımlılığını da basitleştirir ve primitive değerler üzerinden yürütülmesini sağlar.

Var olan nesnelerimizi bu bağlamda aşağıdaki tabloda olduğu gibi değerlendirebiliriz.

| **Referans** | **Nasıl kurulur?** | **Neden tercih edilir?** |
| --- | --- | --- |
| **Order -> Customer** | String türden customerId tutulur. | Muhtemelen aggregat'ler ayrı modüllerde yer alacak ayrı transaction sınırları olacaktır. |
| **Order -> OrderLine** | `List<OrderLine>` nesnesi ile tutulur. | Bir sipariş, içerdiği sipariş kalemleri olmadan anlamlı değildir. Parça, root object'in bir parçası olarak kabul edilir. Şöyle de düşünebiliriz, bir sipariş silindiğinde veya iptal ediliğinde beraberindeki sipariş kalemlerinin varlığını sürdürmesi beklenmez. |
| **Order -> Address** | Address türünden nesne tutulur. | Address bir value object olarak tanımlanmıştır. Kimliği olmadığı, paylaşılan değil kopyalanan bir nesne olduğu için doğrudan nesne olarak tutulması uygundur. *(Onu DateTime, BigDecimal gibi diğer value object'ler gibi düşünelim)* Şu iş kuralını düşünelim birde; bir siparişin teslimat adresi değiştiğinde, bu değişikliğin sadece ilgili siparişin adresini etkilemesi gerekir, müşteri adresini değil. |
| **Order -> Money** | Money türünden nesne tutulur. | Money bir value object olarak tanımlanmıştır. |
| **Customer -> Address** | Address türünden nesne tutulur. | Address bir value object olarak tanımlanmıştır. |

Sistemin bütününe baktığımızda bir müşterinin siparişleri olduğunu biliriz ancak sipariş bilgilerini Customer sınıfında `List<Order>` gibi bir koleksiyon ile tutmayız. Çünkü;

- Sipariş kavramı müşterinin bir parçası değildir, kendi kimliği vardır *(orderId)* ve kendi yaşam döngüsüne sahiptir.
- Bir müşteri nesnesini belleğe almak onun verdiği tüm siparişleri belleğe yüklemek anlamına gelmez. Sayısız sipariş vermiş bir müşteriyi temsil eden nesneyi siparişleri ile birlikte belleğe taşıdığınızı düşünün, imkansız değil ama mantıklı da değil.
- `customer.getOrders().add(newOrder)` gibi bir kod yazabildiğimiz anda, Order nenesi için geçerli olan kuralları *(confirm ve ship fonksiyonlarını düşünelim)* atlamış oluruz.

Şunu unutmayalım ki bir müşterinin siparişlerini bulmak bir sorgu *(query)* işlemidir, bir aggregate'in parçası olma durumu değildir. Örneğin bunu ilerleyen zamanlarda bir repository nesnesi üzerinden `OrderRepository.findByCustomer(customerId)` gibi bir metotla gerçekleştirebiliriz.

Şu ana kadarki bilgilerden yola çıkarak Customer, Order, OrderLine, Address, Money nesneleri arasındaki ilişkileri aşağıdaki şekilde özetleyebiliriz.

![Aggregate diagram](./images/week_04_00.png)

## OrderStatus'u Bir State Machine Olarak Değerlendirmek

Daha önceden tasarladığımız Order sınıfını tekrardan değerlendirelim. Siparişin durumu aslında sipariş nesnesinin anlık durumunu da ifade eder. Bu durumu Order sınıfının önceki versiyonunda cancel, confirm, ship gibi metotlar içerisinde requireStatus metodu yardımıyla ele almıştık. Bunda herhangi bir sorun olmamakla birlikte statüler arası geçişleri üç ayrı yerde ele almak durumunda kalıyoruz. Aslında bir siparişin olası durumları arasındaki geçişlerin merkezi bir yerde görünür kılınması yeni bir durum eklendiğinde veya mevcut bir durum değiştirildiğinde tüm geçişlerin kolayca gözden geçirilmesini sağlayacaktır. Sipariş durumlarını temsil eden OrderStatus enum'ını bir state machine olarak ele almak bu sorunu çözebilir. Şimdi OrderStatus tasarımını aşağıdaki gibi değiştirelim.

```java
public enum OrderStatus {
    DRAFT,
    CONFIRMED,
    SHIPPED,
    CANCELLED;

    private Set<OrderStatus> allowedTransitions;

    static {
        DRAFT.allowedTransitions = EnumSet.of(CONFIRMED, CANCELLED);
        CONFIRMED.allowedTransitions = EnumSet.of(SHIPPED, CANCELLED);
        SHIPPED.allowedTransitions = EnumSet.noneOf(OrderStatus.class);
        CANCELLED.allowedTransitions = EnumSet.noneOf(OrderStatus.class);
    }

    public Set<OrderStatus> allowedTransitions() {
        return Collections.unmodifiableSet(allowedTransitions);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions.contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }

    public boolean isModifiable() {
        return this == DRAFT;
    }
}
```

Görüldüğü üzere Java'nın zengin enum özellikleri onu davranışları *(behaviors)* ve durum bazlı geçişleri *(state transitions)* merkezi bir şekilde tanımlayabilen güçlü bir araç haline getirdi. Dolayısıyla OrderStatus enum nesnesini artık sipariş durumlarını yöneten merkezi bir otorite olarak düşünebiliriz. İzin verilen geçişler Set veri yapısı içerisinde tutulur. static kod bloğu içerisinde her bir durum için izin verilen durum geçişleri de tanımlanır. Örneğin DRAFT durumundan yalnızca CONFIRMED veya CANCELLED durumlarına geçiş yapılabilir veya iptal edilen bir sipariş hiçbir duruma geçiş yapmaz ve bu `EnumSet.noneOf(OrderStatus.class)` ile ifade edilir. Veri yapısı birkaç fonksiyonelliği de dışarıya açar. allowedTransitions metodu izin verilen geçişleri değiştirilemez bir Set olarak döner. canTransitionTo metodu belirli bir duruma geçişin mümkün olup olmadığını bildirir, isTerminal metodu siparişin artık değiştirilemez bir durumda olup olmadığını belirtir. isModifiable metodu ise siparişin henüz taslak aşamasında olup olmadığını döndürür gibi. *(Aşağıdaki kod parçasını çalıştırarak deneyin)*

```java
public class OrderStatusTest {
    public static void main(String[] args) {
        OrderStatus draft = OrderStatus.DRAFT;
        System.out.println("DRAFT allowed transitions: " + draft.allowedTransitions());
        System.out.println("Can DRAFT transition to CONFIRMED? " + draft.canTransitionTo(OrderStatus.CONFIRMED));
        System.out.println("Is DRAFT terminal? " + draft.isTerminal());
        System.out.println("Is DRAFT modifiable? " + draft.isModifiable());
    }
}
```

> Ele aldığımız yaklaşım her ne kadar avantajlı gözükse de state'lerin çok fazla olduğu senaryolarda enum yapısının yönetimi ve izin verilen geçişlerin takibi zorlaşır. Bunu ilerleyen derslerde State Design Pattern ile yeniden değerlendirebiliriz.

Buna göre Order sınıfını yeniden tasarlayabiliriz ama öncesinde önemli olan bir diğer konuyu da ele alalım.

## Domain Nesnelerinde Exception Yönetimi

// EKLENECEK