// Runs automatically on first start
// Root creds are root/root (docker-compose), we create an app user and indexes.

const admin = new Mongo().getDB('admin');

// Create application DB and user
const dbName = process.env.MONGO_INITDB_DATABASE || 'rates';
const appUser = process.env.MONGO_USER || 'app';
const appPass = process.env.MONGO_PASSWORD || 'app';

admin.createUser({
  user: appUser,
  pwd: appPass,
  roles: [
    { role: "readWrite", db: dbName }
  ]
});

// Switch to application DB
const db = new Mongo().getDB(dbName);

/*
Document-per-date structure (mirrors API nicely):
{
  _id: "2025-11-01|EUR",       // rate_date|base
  rate_date: "2025-11-01",
  base: "EUR",
  rates: { USD: 1.07, GBP: 0.84, ... },
  fetched_at: ISODate(),
  raw: { ...original JSON... }
}
*/
db.createCollection("fx_daily");

// Helpful indexes:
// 1) Query by date range
db.fx_daily.createIndex({ rate_date: 1 });
// 2) Query by base
db.fx_daily.createIndex({ base: 1 });

// Example upsert filter for later in apps:
// db.fx_daily.updateOne(
//   { _id: "2025-11-01|EUR" },
//   { $set: { rate_date: "2025-11-01", base: "EUR", rates: { USD: 1.07 }, fetched_at: new Date() } },
//   { upsert: true }
// );
