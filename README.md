# 80/20

Pareto Prensibi odaklı zaman takip uygulaması. Zaman yalnızca uygulama içi kronometreyle kaydedilir; kategoriler "Yatırım" ve "Tüketim" olarak gruplanır.

## Özellikler
- Kategori kartları, tek dokunuşla timer (foreground service, bildirimden durdur/iptal, titreşim geri bildirimi)
- Kategori/etiket/renk yönetimi, arşivleme ve kalıcı silme
- Grafikler: Günlük / Haftalık / Aylık / Tüm Zamanlar / Özel aralık, Pasta ve Çubuk
- Hedefler: kategori veya etiket için günlük/haftalık/aylık Min/Max süre hedefi
- Tüm oturumları CSV olarak dışa aktarma (Grafik ekranı > ⋮)
- Obsidian entegrasyonu: günlük markdown dosyaları (Dataview) ve CSV veri havuzu, klasör seçici ile (Syncthing uyumlu)
- Sessiz veri havuzu: oturum ayrıntıları, olay günlüğü, günlük hedef durumları, isteğe bağlı odak puanı (bkz. `docs/VERI_HAVUZU.md`)

## Teknik
- Kotlin, Jetpack Compose, Material 3, Room, Hilt, DataStore
- minSdk 26, paket: `com.pareto.timetracker`
- Her push'ta GitHub Actions debug APK üretir (Actions > son çalıştırma > Artifacts).
