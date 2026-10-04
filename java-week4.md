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

// DEVAM EDECEĞİZ
