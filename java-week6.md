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

EKLENECEK

### Servis Odalı Mimari (SOA - Service Oriented Architecture)

EKLENECEK

### Mikroservis Mimarisi (Microservices Architecture)

EKLENECEK

### Hexagonal Mimari (Ports and Adapters)

EKLENECEK

### Onion/ Clean Architecture

EKLENECEK

### Dikey Dilim Mimari (Vertical Slice Architecture)

EKLENECEK

### Modular Monolitik Mimari (Modular Monolithic Architecture)

EKLENECEK

## Karar Matrisi

EKLENECEK