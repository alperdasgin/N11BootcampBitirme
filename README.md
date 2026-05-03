# N11 Bootcamp Bitirme — E-Commerce

Spring Boot mikroservis mimarisi (product, stock, cart, order, payment, user, notification, mcp) + React frontend + PostgreSQL + RabbitMQ + Elasticsearch + Eureka + API Gateway.

## Çalıştırma

```bash
docker compose up -d --build
```

Frontend: http://localhost:3000  ·  API Gateway: http://localhost:8080  ·  Eureka: http://localhost:8761  ·  RabbitMQ UI: http://localhost:15672 (guest/guest)

## Veri Persistence (önemli)

Uygulama çalışırken oluşturulan ürünler ve stok kayıtları, kapanışta otomatik olarak `backend/exports/` dizinine yazılır ve bir sonraki başlatmada **veriler kaybolmadan** geri yüklenir.

### İki katmanlı persistence

1. **Postgres seviyesi (eski) — `backend/init-db.sql`**
   `./stop.sh` çağrısı `pg_dump --inserts --clean --if-exists` ile tüm 6 DB'yi (`userdb`, `productdb`, `cartdb`, `orderdb`, `paymentdb`, `stockdb`) bu dosyaya yazar. Postgres ilk açılışta `docker-entrypoint-initdb.d/`'den otomatik yükler.

2. **Servis seviyesi (yeni) — `backend/exports/*.json`**
   - `product-service` kapanırken (`@PreDestroy`) → `backend/exports/products-seed.json`
   - `stock-service` kapanırken (`@PreDestroy`) → `backend/exports/stock-seed.json`
   - Servisler ayağa kalkarken DB boşsa **önce export dosyasını** arar, yoksa varsayılan seed'i yükler.

### Tek-tuş kapatma

```bash
./stop.sh
```

Bu script şunu yapar:
1. `export-db.sh`'ı çalıştırır (postgres dump → `init-db.sql`)
2. `docker compose down` ile container'ları durdurur (her servis kendi `@PreDestroy` exportunu yapar)

### Yeniden başlatma

```bash
docker compose up -d
```

Servisler ayağa kalkarken `backend/exports/products-seed.json` ve `backend/exports/stock-seed.json` dosyalarını otomatik yükler.

> **Not:** Postgres volume (`postgres_data`) silinmediği sürece DB zaten kalıcıdır. `exports/` ve `init-db.sql` dosyaları, **takım üyeleri arasında veri paylaşımı** ve **temiz başlangıç senaryoları** (`docker compose down -v` sonrası) için vardır. `exports/` git'e commit edilebilir.

## Admin Panel: Stok Güncelleme Akışı

`AdminDashboard` üzerinden ürün düzenlendiğinde:

1. Frontend → `PUT /api/products/{id}` (API Gateway → product-service:8082)
2. Product-service `productdb`'yi günceller
3. Product-service RabbitMQ üzerinden `ProductSyncEvent` (UPDATED) yayınlar (`product.sync.exchange`)
4. Stock-service `product.sync.queue.stock` queue'sundan event'i alır → `stockdb.product_stock` tablosunu **idempotent** olarak günceller (`availableQuantity` set edilir, `reservedQuantity` korunur)
5. Frontend tabloda yenilendiğinde tek **"Stok"** sütunu güncel `availableQuantity`'i gösterir

Saga akışı (sipariş için reserve/release/commit) bağımsız çalışmaya devam eder; yeni event sadece admin tarafındaki sync için eklenmiştir.
