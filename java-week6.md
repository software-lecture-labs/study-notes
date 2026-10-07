# Yazılım Mimarileri Üzerine Yardımcı Notlar

Kurumsal çaptakı sistemler karmaşık iş süreçleri barındırır ve tasarımları da bu karmaşıklığı yönetebilecek şekilde olmalıdır. Bu nedenle yazılım mimarileri, sistemin ölçeklenebilirliğini *(scalability)*, bakımını *(maintainability)*, performansını *(performance)*, yönetilebilirliği *(manageability)* ve daha birçok faktörü doğrudan etkiler. Kısaca yazılım mimarisi, **sonradan değiştirilmesi pahalı ve zor olan kararlar bütünü** olarak tanımlanabilir. Esasında doğru yazılım mimarisi yok demek yanlış olmaz. Bunun yerine içeriğe *(context)* uygun trade-off' lar vardır *(trade-off: bir avantajı elde etmek için başka bir avantajdan vazgeçmek)*.

> **Bilgi:** Her mimari bir problemi çözerken beraberinde yeni bir maliyette getirir. Dolayısıyla önce problemi masaya yatırıp ona cevap verecek mimariyi bulmaya çalışmak önemlidir.

## Ölçütler

Popüler yazılım mimarilerini değerlendirmeden önce bazı ölçüt kavramlarına aşina olmakta yarar var. Özellikle **coupling**, **cohesion**, **test edilebilirlik**, **dağıtım** *(deployment)*, **ölçeklenebilirlik** *(scalability)*, **operasyonel maliyet** ve **bilişsel yük** *(cognitive load)* gibi kavramlara aşina olmak mimariler arası karşılaştırmalar yaparken de büyük kolaylık sağlar. Bu ölçütler için aşağıdaki sorular yol gösterici olacaktır.

- **Yazılımdaki bir parçayı değiştirince kaç parça etkileniyor?** -> Coupling *(bağımlılık)* ölçütü ile ilgilidir. Düşük coupling, değişikliklerin etkisini minimize eder.
- **Birlikte değişen şeyler bir arada mı?** -> Cohesion *(içsel bağlılık/uyum)* ölçütü ile ilgilidir. Yüksek cohesion, bir modülün tek bir sorumluluğu olmasını ve değişikliklerin etkisinin sınırlı olmasını sağlar.
- **Bir iş kuralını veritabanı ya da UI olmadan test edebiliyor muyuz?** -> Test edilebilirlik *(testability)* ölçütü ile ilgilidir. Yüksek test edilebilirlik, birim testlerinin kolayca yazılabilmesini, sistemin güvenilirliğinin artmasını anlamına gelir.
- **Sistemi izleme, network ve veri tutarlılığını sağlamak ne kadar zor?** -> Operasyonel maliyet *(operational cost)* ile ilgilidir. Bazı mimariler önemli avantajlar sunarken operasyonel maliyeti artırır.
- **Projeye yeni katılan bir geliştiricisi sisteme ne kadar kolay adapte olabiliyor?** -> Bilişsel yük *(cognitive load)* ölçütü ile ilgilidir. Düşük bilişsel yük, yeni geliştiricilerin sistemi anlamasını ve katkıda bulunmasını kolaylaştırır.

## Popüler Yazılım Mimarileri

Bu derste yüzeysel olarak ele alacağımız mimarileri tarihsel gelişimleri açısından şöyle sıralayabiliriz; 90lı yıllar için katmanlı mimari *(layered architecture)*, 2000'li yıllar için servis odaklı mimari *(SOA - Service Oriented Architecture)*, 2005 Hexagonal mimari *(hexagonal architecture)*, 2008 Onion mimari *(onion architecture)*, 2012 *clean architecture*, 2014 Mikroservis mimari *(microservices architecture)*, 2018 Dikey dilim mimari *(vertical slice architecture)* ve 2020'ler için Modular Monolitik mimari *(modular monolithic architecture)*.

### Katmanlı Mimarisi (Layered Architecture / N-Tier)

Yazılımla tanışan herkesin büyük ihtimalle ilk öğrendiği veya çalıştığı mimari türü olarak düşünülebilir. Uygulama kodu sorumluluklarına göre yatay katmanlara bölünü ve her katman yalnızca bir altındaki katmanı çağırır. Öğrenimi kolay olan bu mimari, küçük ve orta ölçekli projelerde oldukça etkilidir. Ancak katmanlar arası bağımlılıklar arttıkça değişikliklerin etkisi de büyüyebilir, bu nedenle dikkatli tasarım gerektirir. Genellikle 3-Tier veya 5-Tier olarak karşımıza çıkar.

- **3-Tier:** Presentation katmanı (UI) -> Business Logic katmanı (iş mantığı) -> Data Access katmanı (veri erişimi) -> Veritabanı (Database)
- **5-Tier:** Presentation katmanı (UI) -> Application katmanı (Service, API gibi) -> Business Logic katmanı (iş mantığı, domain kuralları) -> Data Access katmanı (veri erişimi) -> Veritabanı (Database)

![Layered architecture](./images/week_06_01.png)

Kabaca yukarıdaki görselde olduğu gibi tarifleyebiliriz. Burada **layer** ve **tier** kavramlarını birbirine karıştırmamak gerekir. **Layer**, yazılımın sorumluluklarına göre ayrılmış mantıksal bir bölümü ifade ederken, **tier**, fiziksel olarak ayrılmış bir katmanı ifade eder. Söz gelimi 3 katmanlı bir mimari tasarım tek bir fiziksel sunucuda çalışabilir ve bu durumda tek bir tier olarak kabul edilir. Tier'ı ağ sınırı olarak da düşünebiliriz. Dolayısıyla bir mimari tasarımda katman sayısı ile tier sayısı her zaman aynı olmak zorunda değildir.

Bir diğer önemli mesele de katmanlar arası bağların katı *(strict)* ya da gevşek *(relaxed)* olmasıdır. Katı bağ, bir katmanın yalnızca bir alt katmanı çağırabilmesini ve doğrudan diğer katmanlara erişememesini ifade eder. Gevşek bağda ise katmanlar arasında daha fazla adım atlamak mümkündür fakat bu bağımlılıkların süratle dağılmasına da sebebiyet verir. Bazen derleme zamanı kontrolleri veya çeşitli testlerle bu kaçaklar bilhassa engellenir *(ArcUnit konusu ve ADR kavramlarını araştıralım)*

Bu mimaride bağımlılıklar yukarıdan aşağıya doğru iner. Yani iş mantığı veritabanına bağımlıdır. Clean Architecture veya Hexagonal Architecture gibi modern yaklaşımlarda ise bağımlılıklar tersine çevrilir ve iş mantığı altyapı detaylarından bağımsız hale getirilir. Yani katmanlı mimaride zayıflık olarak görülen bu bağımlılık ilkesi diğer mimariler için bir avantaj haline gelir.

Dikkat edilmesi gereken noktalardan birisi de **Sinkhole Anti-Pattern**'dir. İsteklerin çoğu katmanlardan geçerken hiçbir iş yapılmıyorsa *(yani alt katman geriye sadece bir çağrı sonucu dönüyorsa)* katmanların maliyet ürettiği söylenir. Eğer bu durum yüzdesel olarak 20'yi geçiyorsa mimariyi gözden geçirmek ve gereksiz katmanları ortadan kaldırmak faydalı olabilir *(Ref: [O'Reilly, Software Architecture Patterns](https://www.oreilly.com/content/software-architecture-patterns/))*

Uygulaması kolay bir mimari olsa da başta da belirttiğim gibi her mimarinin bazı trade-off'ları vardır. Bu yaklaşımda basit bir özellik eklemek *(Örneğin siparişlere not eklenmesi)* tüm katmanlarda değişiklik yapılmasını gerektirebilir ve değişiklik ufak olsa bile yeniden tüm tier'ın deploy edilmesini zorunlu kılabilir. Bu durum özellikle büyük ve dağıtık sistemlerde operasyonel maliyetleri artırabilir. Örneğin bu zayıflık Vertical Slice Architecture'ın ortaya çıkmasına da vesile olmuştur.

### Servis Odalı Mimari *(SOA - Service Oriented Architecture)*

Kurumun iş yetenekleri etrafında organize edilen ve bu yetenekleri destekleyen servislerin bir araya gelmesiyle oluşan bir mimari yaklaşımdır. SOA'da servisler genellikle birbirinden bağımsızdır ve belirli iş süreçlerini yerine getirir. Bu servisler genellikle Enterprise Service Bus (ESB) gibi bir altyapı üzerinden konuşur. Amaç kurum genelinde yeniden kullanım ve entegrasyonu sağlamaktır. Aşağıdaki görsel kabaca mimarinin temel parçalarını göstermektedir.

![Service Oriented Architecture](./images/week_06_02.png)

Servisler aynı veritabanını paylaştıkları için görünmez bir biçimde birbirlerine bağlıdırlar. Servisler sözleşme bazlıdır *(contract-first)*. Buna göre çoğunlukla WSDL/XSD gibi resmi sözleşme standartları söz konususur. Genellikle SOAP *(Simple Object Access Protocol)* veya REST *(Representational State Transfer)* protokolleri kullanılır. Bu kurguda tüm trafik ESB hattı üzerinden geçer. ESB'nin birçok görevi olur. Mesajların yönlendirilmesi, mesaj içeriklerini farklı formatlara çevrilmesi veya değiştirilmesi, protokol geçişleri gibi işlemler ESB tarafından yönetilir. En önemli avantaj eski sistemlerin *(mainframe'ler, paket yazılımlar vs)* organizasyonun kalanıyla tek bir çatı altında konuşturabilmesidir. Pek tabii bu mimarinin de güçlü ve zayıf yönleri vardır. Örneğin zamanla biriken iş mantığı *(business logic)* tek bir ekipde toplanabilir ve tek hata noktası ile darboğaz riski oluşturabilir.

> SOA aslında bir felsefe sunar ve mikro servis mimarisi aslında bu felsefenin bir uygulamasıdır. SOA ve Microservice üzerine referans için [Martin Fowler - Microservices](https://martinfowler.com/articles/microservices.html) makalesine göz atabilirsiniz.

SOA, çok sayıda eski ve hazır sistemin entegre edilmesi gerektiği durumlarda tercih edilen bir yaklaşımdır. Bu nedenle bankacılık, telekom ve kamu sektöründe sıkça kullanılır.

### Mikroservis Mimarisi *(Microservices Architecture)*

EKLENECEK

### Hexagonal Mimari *(Ports and Adapters)*

EKLENECEK

### Onion/ Clean Architecture *(Onion/ Clean Architecture)*

EKLENECEK

### Dikey Dilim Mimari *(Vertical Slice Architecture)*

EKLENECEK

### Modular Monolitik Mimari *(Modular Monolithic Architecture)*

EKLENECEK

## Karar Matrisi

EKLENECEK