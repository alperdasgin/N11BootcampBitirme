#!/bin/bash
# ─────────────────────────────────────────────────────────────
# stop.sh
# Verileri kaydeder, sonra uygulamayı durdurur.
# Kullanım: ./stop.sh
# ─────────────────────────────────────────────────────────────

echo "💾 Veriler kaydediliyor..."
./export-db.sh

echo ""
echo "🛑 Uygulama durduruluyor..."
docker compose down

echo ""
echo "✅ Uygulama durduruldu. Veriler init-db.sql'e kaydedildi."
echo ""
echo "Bir sonraki başlatmada veriler otomatik yüklenir:"
echo "  docker compose up"
