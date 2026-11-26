package com.pareto.timetracker.domain

/** Oturumun nasıl bittiği. */
enum class EndReason {
    /** Uygulama içinden karta dokunarak durdurma. */
    MANUAL_STOP,

    /** Başka bir kategori başlatılınca önceki oturum otomatik bitti. */
    SWITCHED,

    /** Bildirimdeki Durdur butonu. */
    NOTIFICATION_STOP,

    /** Çalışan kategori arşivlenirken oturum kaydedildi. */
    ARCHIVED,
}

/** Timer'ın nereden başlatıldığı. Şimdilik sadece kart; ileride widget eklenebilir (şema değişmez). */
enum class StartSource { CARD }

/** Olay günlüğündeki olay türleri. Ayrıntılar docs/VERI_HAVUZU.md içinde. */
enum class EventType {
    CATEGORY_CREATED,
    CATEGORY_UPDATED,
    CATEGORY_ARCHIVED,
    CATEGORY_RESTORED,
    GOAL_CREATED,
    GOAL_UPDATED,
    GOAL_DELETED,
    SESSION_CANCELLED,
    SESSION_DISCARDED,
}
