# 80/20 veri havuzu – şema

Bu klasörü 80/20 uygulaması üretir. İçindeki dosyalar her yazımda baştan oluşturulur: **elle düzenleme, üzerine yazılır.**
Yön tek taraflıdır (uygulama → Obsidian). Doğruluk kaynağı telefondaki uygulama veritabanıdır.

## Klasör yapısı
- `80-20/YYYY/MM/YYYY-MM-DD.md` – günlük dosyalar (ilk oturumdan bugüne, boş günler dahil)
- `80-20/_veri/*.csv` – ham veri (virgül ayraçlı, UTF-8, BOM yok, boş değer = bilinmiyor)
- `80-20/_veri/SEMA.md` – bu dosya

## Günlük dosya alanları (frontmatter)
| Alan | Anlam |
|---|---|
| `date` | yerel gün |
| `yatirim_dk`, `tuketim_dk`, `toplam_dk` | dakika (tek ondalık); süreler gün sınırına kırpılır |
| `oturum_sayisi` | o gün **başlayan** oturum sayısı |
| `ilk_oturum`, `son_oturum` | `"HH:mm"` (gün sonu `"24:00"`); oturum yoksa yok |
| `en_uzun_oturum_dk` | günün en uzun (kırpılmış) oturumu |
| `ort_odak` | o gün başlayan, puanlanmış oturumların ortalaması; puan yoksa yok |
| `kategoriler` | kategori adı → dakika (o günün kategorileri, büyükten küçüğe) |
| `hedefler` | günlük hedeflerin durumu: `kapsam`, `hedef`, `tip` (min/max), `hedef_dk`, `gerceklesen_dk`, `tamam`, `aktif` |
| `kesinlesti` | `true`: gün bitti, hedef durumu dondurulmuş. `false`: bugün, canlı hesap |

Kategori adı ve etiketi **güncel** haline göre yazılır (etiket değişimi geçmişe de uygulanır, uygulamadaki grafiklerle aynı).
`tamam`: min hedefte gerçekleşen ≥ hedef, max hedefte gerçekleşen ≤ hedef. `aktif`: kategori arşivde değilse true.

## CSV dosyaları
- `oturumlar.csv`: `session_id, session_uuid, start, end, duration_seconds, category_id, category, tag, archived, end_reason, start_source, focus_rating, category_snapshot, tag_snapshot, color_index_snapshot, tz_offset_seconds, app_version`
  – `category`/`tag` kategorinin güncel hali, `*_snapshot` sütunları oturum anındaki hali. `tag`: `INVESTMENT` / `CONSUMPTION`. `focus_rating` boş olabilir.
- `olaylar.csv`: `event_uuid, occurred_at, type, category_id, goal_id, payload, tz_offset_seconds, app_version`
  – `type`: CATEGORY_CREATED/UPDATED/ARCHIVED/RESTORED, GOAL_CREATED/UPDATED/DELETED, SESSION_CANCELLED, SESSION_DISCARDED. `payload` JSON.
- `gunluk_hedef_durumu.csv`: günü bitmiş günlük hedeflerin dondurulmuş durumu (`day, goal_id, scope_key, category_id, tag, goal_type, target_seconds, actual_seconds, achieved, goal_active, frozen_at`)
- `kategoriler.csv`, `hedefler.csv` – şu anki tanımlar
- `cihaz.csv` – cihaz kimliği, model, Android ve uygulama sürümü

Zamanlar ISO 8601 ve saat dilimi ofsetlidir.

## Örnek Dataview sorgusu
```dataview
TABLE yatirim_dk, tuketim_dk, round(yatirim_dk / toplam_dk * 100) + "%" AS "Yatırım %"
FROM "80-20"
WHERE date >= date(today) - dur(30 days) AND toplam_dk > 0
SORT date DESC
```

## Örnek DataviewJS (CSV okuma)
```dataviewjs
const oturumlar = await dv.io.csv("80-20/_veri/oturumlar.csv");
dv.paragraph(oturumlar.length + " oturum");
```
