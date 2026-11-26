# Veri havuzu (şema sürümü 1)

Uygulama, arayüze hiçbir şey eklemeden arka planda ayrıntılı veri toplar. Doğruluk kaynağı Room veritabanıdır
(`pareto.db`). Obsidian/CSV çıktıları bu tablolardan türetilir.

Genel ilkeler:
- Zamanlar epoch milisaniye (UTC). Saat dilimi ofseti `tz_offset_seconds` ile ayrıca saklanır.
- Olay günlüğü sadece eklenir; kayıtlar güncellenmez.
- Haftanın günü, gün içi saat, hafta/ay numarası saklanmaz; zaman damgasından hesaplanır.
- Kategori **kalıcı silinirse** ona bağlı her şey silinir (oturumlar, hedefler, günlük hedef durumları,
  kategoriye bağlı olaylar). Geride iz kalmaz.

## Tablolar

### sessions
| Sütun | Anlam |
|---|---|
| id, category_id | |
| uuid | benzersiz kayıt kimliği |
| started_at, ended_at | oturum süresi (>= 10 sn olanlar kaydedilir) |
| end_reason | MANUAL_STOP, SWITCHED, NOTIFICATION_STOP, ARCHIVED |
| start_source | CARD (ileride WIDGET) |
| focus_rating | 1–5 veya boş (sorulmadı / dokunulmadı) |
| category_name, category_tag, category_color_index | oturum anındaki kategori görüntüsü |
| tz_offset_seconds | oturum başlangıcındaki UTC ofseti |
| app_version | kaydı yazan uygulama sürümü |

Odak puanı yalnızca uygulama içinden durdurulan ve en az 5 dakika süren oturumlarda sorulur.

### events (sadece eklenir)
`id, uuid, occurred_at, tz_offset_seconds, app_version, type, category_id, goal_id, payload (JSON)`

| type | payload |
|---|---|
| CATEGORY_CREATED | `{name, tag, color_index}` |
| CATEGORY_UPDATED | `{old: {...}, new: {...}}` (aynı alanlar) |
| CATEGORY_ARCHIVED / CATEGORY_RESTORED | yok |
| GOAL_CREATED | `{scope_key, category_id, tag, period, goal_type, target_seconds}` |
| GOAL_UPDATED | `{old: {...}, new: {...}}` |
| GOAL_DELETED | silinen hedefin alanları |
| SESSION_CANCELLED | `{started_at, ended_at, duration_ms}` (kullanıcı çalışan süreyi "Süreyi sil" ile sildi) |
| SESSION_DISCARDED | `{started_at, ended_at, duration_ms, end_reason}` (10 sn altı) |

`goal_id` bilerek yabancı anahtar değildir: hedef silinse de olayı kalır.

### daily_goal_status (günü bitmiş günlük hedefler, boş günler dahil)
`id, day (ISO), goal_id, scope_key, category_id, tag, goal_type, target_seconds, actual_seconds, achieved, goal_active, frozen_at`

- `achieved`: MIN için gerçekleşen >= hedef, MAX için gerçekleşen <= hedef.
- `goal_active`: kategori kapsamlı hedefte kategori o gün arşivdeyse false.
- Her hedef için oluşturulduğu günden dünün sonuna kadar satır yazılır. Bugün dondurulmaz (gün bitince yazılır).
- Dondurma; uygulama açılışında ve hedef değişikliklerinden, etiket değişiminden, arşivleme/geri yüklemeden
  hemen önce çalışır, böylece geçmiş günler eski tanımla kalır.
- Haftalık ve aylık hedefler bu tabloya yazılmaz.

### pool_meta (anahtar-değer)
`device_id` (rastgele, bir kez), `first_run_at`, `device_model`, `android_version`, `app_version`, `pool_schema_version`.

## Toplanmayanlar (bilerek)
Diğer uygulamaların kullanımı, konum, adım sayısı, pil durumu.

## Obsidian çıktısı
Grafik ekranı ⋮ menüsünden vault klasörü seçilince uygulama, seçilen klasörün içine `80-20/` yazar:
- `80-20/YYYY/MM/YYYY-MM-DD.md`: günlük dosyalar (ilk oturumdan bugüne, boş günler dahil)
- `80-20/_veri/*.csv`: oturumlar, olaylar, günlük hedef durumu, kategoriler, hedefler, cihaz
- `80-20/_veri/SEMA.md`: Obsidian içinde okunabilen veri sözlüğü (kaynak: `res/raw/veri_sema.md`)

Veri değişince (2 sn bekleyerek) ve uygulama açılırken otomatik, ayrıca menüden elle yazılır. İçeriği değişmeyen
dosyalar yeniden yazılmaz (SHA-256 karşılaştırması); "Obsidian’ı baştan yaz" hepsini zorla yeniden yazar.

Grafik ekranındaki "Verileri dışa aktar" aynı `oturumlar.csv` biçimini (PoolCsv.sessions) tek dosya olarak yazar.
