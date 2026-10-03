# Hafta 03

Birçok domain hassas sayısal değerlere ihtiya duyar. Örneğin finansal uygulamalarda para birimi değerleri veya bilimsel hesaplamalarda ölçüm sonuçları hassasiyet gerektirir. Programlama dillerinde bu tür hassasiyet isteyen değişkenler double, float, BigDecimal gibi veri tipleri ile ifade edilirler. Ancak parasal değerler için double veya float kullanmak yuvarlama hatalarına yol açabilir. Bu nedenle finansal uygulamalarda genellikle BigDecimal tercih edilir. Bir para birimini sadece BigDecimal ile ifade etmek de çoğu zaman yeterli değildir. Zira bu para biriminin hangi ülkeye ait olduğu veya hangi döviz cinsinden olduğu bilgisi de önemlidir. Farklı para birimlerinden değerleri birbirileriyle toplamak hatadır ve bu bir domain kuralı olarak ele alınmalıdır.

Öncelikle double veya float türleri için olası hesaplama hatalarına bir bakalım.

```java
public static void main(String[] args) {
    double total = 0.1 + 0.2;
    System.out.println("0.1 + 0.2 = " + total); // 0.30000000000000004
    float total2 = 0.1f + 0.2f;
    System.out.println("0.1f + 0.2f = " + total2); // 0.3
    float price = 9.14f;
    System.out.println("9.14F * 3 = " + price * 3); // 27.420002
    var total2 = new BigDecimal("0.1").add(new BigDecimal("0.2"));
    System.out.println("BigDecimal 0.1 + 0.2 = " + total2); // 0.3
    System.out.println("New BigDecimal(0.1) = " + new BigDecimal(0.1)); // 0.1000000000000000055511151231257827021181583404541015625
}
```

Bu kod parçasının çıktısını dikkatle inceleyelim.

![week_03_00](./images/week_03_00.png)

İlk örnekte double türü ile yapılan toplama işleminde beklenmeyen bir yuvarlama hatası görüyoruz. 0.1 + 0.2 işleminin sonucu tam olarak 0.3 değil, 0.30000000000000004 olarak karşımıza çıkıyor. f ile tanımlanan float türünde ise bu hata daha az belirgin, ancak hassasiyet kaybı yine de mevcut. Zira 9.14f * 3 işleminin sonucu tam olarak 27.42 değil, 27.420002. En garanti yol BigDecimal kullanımı gibi duruyor ancak orada da dikkat edilmesi gereken nokta, BigDecimal nesnesini double veya float değerlerinden oluşturduğumuzda yine hassasiyet kaybı olabileceğidir. Bu nedenle BigDecimal nesnelerini string değerlerinden oluşturmanın daha güvenli bir yaklaşım olduğunu ifade edebiliriz.

## Value Object Olarak Money Record

Yukarıdaki örnekleri göz önüne aldığımızda parasal değerin doğruluğunu belirli kurallar ile garanti eden, toplama, çarpma ya da indirim oranı uygulama gibi fonksiyonellikler kapsülleyen bir iş nesnesi tasarlamak mantıklı olacaktır. Parasal bir değerin double veya float türler ile çalışmaması bunların yerine BigDecimal kullanması önerilir. Bahsettiğimiz aritmetik işlemler her zaman yeni bir para nesnesi döndürmelidir ve mevcut nesneyi değiştirmemelidir. Bu da immutable bir tasarım yapacağımız anlamına gelir. Farklı para birimleri birbirleriyle aritmetik işleme girmemelidir vb Tüm bunlara istinaden aşağıdaki gibi bir record tasarımı pekala iş görecektir.

```java
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    private static final Currency TL = Currency.getInstance("TRY");

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
    }

    public static Money tl(String amount) {
        return new Money(new BigDecimal(amount), TL);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public Money discountedBy(BigDecimal rate) {
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discount rate must be within [0, 1]: " + rate);
        }
        return new Money(amount.multiply(BigDecimal.ONE.subtract(rate)), currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            // Gerçek senaryolarda IllegalArgumentException yerine özel bir domain exception sınıfı kullanılabilir.
            throw new IllegalArgumentException(
                    "currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
```

Money tipi bir record olarak tasarlanmıştır. Immutable *(değiştirilemez)* bir yapıya sahiptir. Ayrıca Money nesnelerinin birbirleriyle karşılaştırılmasını kontrol altına almak için `Comparable<Money>` arayüzünü uygular ve para birimi uyumsuzluklarını önler. `java.util` tarafından sağlanan Currency sınıfı, para birimlerini temsil etmek için kullanılır. Varsayılan parabirimi TL olarak belirlenmiştir. Compact Constructor kullanılarak amount ve currency alanlarının null olmaması ve amount değerinin para biriminin varsayılan kesir basamağına göre yuvarlanması sağlanır.

Money record tipi üstünden doğrudan çağrılabilen static metotlara da dikkat edelim. Örneğin `Money.tl("100")` ifadesi 100 TL değerinde bir Money nesnesi oluşturur. Benzer şekilde `Money.zero(Currency.getInstance("USD"))` ifadesi belirtilen para biriminde sıfır değerinde bir Money nesnesi döndürür. Bu tip metotlar object user'ın nesne oluşturmadan da Money nesneleriyle çalışabilmesini sağlar.

Money türü için aritmek işlemler plus, minus, times ve discountedBy metotları ile gerçekleştirilir. Bu metotlar, mevcut Money nesnesi üzerinde belirtilen işlemi yapar ve sonucu yeni bir Money nesnesi olarak döndürür. Burada dikkat edilmesi gereken konu her aritmetik operasyonun yeni bir Money nesnesi oluşturmasıdır.

Tasarladığımız Money record'una ait üyeleri aşağıdaki tablo ile özetleyebiliriz.

| **Member** | **Description** |
| --- | --- |
| **amount** | Parasal değeri temsil eden BigDecimal alanı. |
| **currency** | Para birimini temsil eden `java.util`' den gelen Currency alanı. |
| **plus(Money other)** | İki Money nesnesini toplar ve sonucu yeni bir Money nesnesi olarak döndürür. Çalışmak için mevcut Money nesnesi örneğine ihtiyaç duymaz. |
| **tl(String amount)** | Belirtilen miktarda TL cinsinden bir Money nesnesi oluşturur. |
| **zero(Currency currency)** | Belirtilen para biriminde sıfır değerinde bir Money nesnesi oluşturur. |
| **minus(Money other)** | İki Money nesnesini çıkarır ve sonucu yeni bir Money nesnesi olarak döndürür. Çalışmak için mevcut Money nesnesi örneğine ihtiyaç duymaz. |
| **times(int factor)** | Money nesnesini belirtilen katsayı ile çarpar ve sonucu yeni bir Money nesnesi olarak döndürür. Çalışmak için mevcut Money nesnesi örneğine ihtiyaç duymaz. |
| **discountedBy(BigDecimal rate)** | Money nesnesine belirtilen indirim oranını uygular ve sonucu yeni bir Money nesnesi olarak döndürür. Çalışmak için mevcut Money nesnesi örneğine ihtiyaç duymaz. |
| **isZero()** | Money nesnesinin sıfır olup olmadığını kontrol eder. |
| **compareTo(Money other)** | Money nesnelerini karşılaştırır. Comparable arayüzünden gelir ve mevcut Money nesnesi örneğine ihtiyaç duymaz. |
| **toString()** | Money nesnesini okunabilir bir string formatında döndürür. |

Birde Money record'un kullanımına bakalım.

```java
var money = Money.tl("24.60");
System.out.println("money from 24.60 = " + money);
var money2 = Money.tl("24.6").times(3);
System.out.println("money from 24.6 * 3 = " + money2);
var money3 = Money.tl("1000").discountedBy(new BigDecimal("0.15"));
System.out.println("money from 1000 discounted by BigDecimal 0.15 = " + money3);
var money4 = new Money(new BigDecimal("99.50"), Currency.getInstance("USD"));
System.out.println("Unit price is " + money4);
// Para birimi uyuşmazlığı nedeniyle bir istisna fırlatması gerekir
var money5 = Money.tl("50").plus(new Money(new BigDecimal("15"), Currency.getInstance("USD")));
System.out.println(money5);
```

![Money record usage example](./images/week_03_01.png)

Şimdilik Money record tek başına bir anlam ifade etmiyor olabilir. Ancak daha önceden ele aldığımız Product ve sonradan bakacağımız Order türlerindeki fiyat, toplam fiyat gibi alanların artık Money türünden olması, para birimi ve hesaplama konularında daha güvenli ve tutarlı bir yaklaşıma sahip olmamızı sağlayacaktır. Parasal birimlerin garantisini bu türü içerecek diğer iş nesnelerine de taşımış olacağız.

## Sorular

Bu dersle ilgili olarak bizi araştırmaya itecek soruları aşağıdaki bulabilirsiniz.

- Money nesnelerinin neden `immutable` olması gerekir?
- `tl(String amount)` metodu yerine `from(String amount, Currency currency)` gibi bir metot tercih edilebilir miydi?
- `new BigDecimal(0.1)` ile `new BigDecimal("0.1")` neden farklı sonuç üretir? `BigDecimal.valueOf(0.1)` bu ikisinden hangisine benzer davranır ve neden? *(İpucu: `Double.toString` ve IEEE 754)*
- 0.1 sayısı neden ikilik tabanda tam olarak ifade edilemez? Aynı problem onluk tabanda hangi sayılar için yaşanır *(örneğin 1/3)*?
- `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` ifadesi `false`, `compareTo` ise `0` döner. Money record'unun `equals` metodu derleyici tarafından üretildiğine göre, compact constructor'daki `setScale` çağrısı olmasaydı `Money.tl("2.0")` ile `Money.tl("2.00")` eşit kabul edilir miydi? Bir `HashSet<Money>` bu durumda nasıl davranırdı?
- `RoundingMode.HALF_UP` yerine `HALF_EVEN` *(banker's rounding)* hangi senaryolarda tercih edilir? Milyonlarca işlemin yapıldığı bir sistemde bu seçim toplam tutarı nasıl etkiler?
- Yuvarlama her aritmetik işlemden sonra mı yapılmalı, yoksa yalnızca nihai sonuçta mı? Art arda iki kez `discountedBy` uygulandığında kuruş kaybı oluşabilir mi? Deneyerek gösterin.
- 100 TL'lik bir tutarı üç eşit taksite bölmek istediğimizde ne olur? Hiçbir kuruşun kaybolmadığı bir `allocate` / `split` metodu nasıl tasarlanır? *(Martin Fowler'ın Money pattern'ine bakabilirsiniz)*
- `times` metodu neden `int` parametre alıyor? 1.5 kg peynirin fiyatını hesaplamak için `BigDecimal` alan bir overload eklemek hangi yeni riskleri getirir?
- `Currency.getDefaultFractionDigits()` Japon Yeni için 0, Bahreyn Dinarı için 3 döner. Peki 8 ondalık basamağa sahip Bitcoin `java.util.Currency` ile temsil edilebilir mi? Edilemiyorsa Money tasarımı nasıl değişmeli?
- `compareTo` farklı para birimlerinde exception fırlatıyor. Bu, `Comparable` sözleşmesindeki *total ordering* beklentisini ihlal eder mi? Farklı para birimlerinden Money nesnelerini bir `TreeSet` içine koymaya çalışırsak ne olur?
- Money negatif bir değer taşıyabilmeli mi? `minus` sonucunun negatif çıkması bir iade veya borç senaryosunda anlamlı olabilirken, bir ürün fiyatında hata olabilir. Bu kural Money'nin mi yoksa onu kullanan nesnenin *(Product, Order)* mi sorumluluğundadır?
- `IllegalArgumentException` yerine `CurrencyMismatchException` gibi bir domain exception kullanmanın faydası nedir? Bu exception checked mi yoksa unchecked mi olmalı?
- Döviz çevirimi *(örneğin TL'den USD'ye)* Money'nin bir metodu olabilir mi? `money.convertTo(usd)` ile ayrı bir `ExchangeRateService` yaklaşımını Single Responsibility ve bağımlılık yönetimi açısından karşılaştırın.
- Money bir JPA entity'si içinde nasıl saklanır? `@Embeddable` ve `AttributeConverter` yaklaşımlarını araştırın. Veritabanında tutar ve para birimi için iki kolon mu, tek kolon mu kullanılmalı?
- Java ekosisteminde JSR 354 *(JavaMoney / Moneta)* ve Joda-Money gibi kütüphaneler neden ortaya çıkmıştır? Kendi Money tipimizi yazmak ile bu kütüphaneleri kullanmanın artıları ve eksileri nelerdir?
- Hafta 2'deki Product sınıfında `float unitPrice` alanını `Money` ile değiştirdiğimizde hangi hataları derleme zamanında, hangilerini çalışma zamanında yakalamış oluruz?
