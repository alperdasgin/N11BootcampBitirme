#!/bin/bash
# ─────────────────────────────────────────────────────────────
# export-db.sh
# Çalışan postgres container'ından tüm veritabanlarını dışa
# aktarır ve backend/init-db.sql dosyasını günceller.
# Kullanım: ./export-db.sh
# ─────────────────────────────────────────────────────────────

set -e

CONTAINER="ecommerce-postgres"
PG_USER="postgres"
OUTPUT="backend/init-db.sql"
DATABASES=("userdb" "productdb" "cartdb" "orderdb" "paymentdb" "stockdb")

# ── Container çalışıyor mu kontrol et ────────────────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "❌ Hata: '${CONTAINER}' container'ı çalışmıyor."
  echo "   Önce 'docker compose up' ile uygulamayı başlatın."
  exit 1
fi

echo "🔄 Veritabanları dışa aktarılıyor..."
TMPDIR=$(mktemp -d)

for DB in "${DATABASES[@]}"; do
  echo "  → ${DB} aktarılıyor..."
  docker exec "${CONTAINER}" pg_dump \
    -U "${PG_USER}" \
    --no-owner \
    --no-acl \
    --inserts \
    --clean \
    --if-exists \
    "${DB}" > "${TMPDIR}/${DB}.sql"
done

# ── Yeni init-db.sql oluştur ──────────────────────────────────
echo "📝 init-db.sql yazılıyor..."

cat > "${OUTPUT}" << HEADER
-- ============================================================
-- init-db.sql — Otomatik oluşturulmuştur
-- Son güncelleme: $(date '+%Y-%m-%d %H:%M:%S')
-- Bu dosyayı elle düzenlemeyin. export-db.sh çalıştırın.
-- ============================================================

-- Veritabanlarını oluştur
CREATE DATABASE userdb;
CREATE DATABASE productdb;
CREATE DATABASE cartdb;
CREATE DATABASE orderdb;
CREATE DATABASE paymentdb;
CREATE DATABASE stockdb;

HEADER

for DB in "${DATABASES[@]}"; do
  cat >> "${OUTPUT}" << SEPARATOR

-- ============================================================
-- ${DB}
-- ============================================================
\c ${DB}

SEPARATOR
  cat "${TMPDIR}/${DB}.sql" >> "${OUTPUT}"
done

# ── Temizlik ──────────────────────────────────────────────────
rm -rf "${TMPDIR}"

LINES=$(wc -l < "${OUTPUT}")
echo ""
echo "✅ Tamamlandı!"
echo "   Dosya : ${OUTPUT}"
echo "   Boyut : ${LINES} satır"
echo ""
echo "💡 Değişiklikleri kaydetmek için:"
echo "   git add backend/init-db.sql && git commit -m 'chore: update seed data' && git push"
