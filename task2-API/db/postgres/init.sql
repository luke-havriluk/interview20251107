-- Runs automatically on container first start

-- 1 row per (date, base, currency)
CREATE TABLE IF NOT EXISTS fx_rates (
  rate_date      date           NOT NULL,   -- API date
  base_currency  text           NOT NULL,   -- e.g., 'EUR'
  currency       text           NOT NULL,   -- e.g., 'USD'
  rate           numeric(18,8)  NOT NULL,
  fetched_at     timestamptz    NOT NULL DEFAULT now(),
  source_etag    text,
  payload_json   jsonb
);

ALTER TABLE fx_rates
  ADD CONSTRAINT fx_rates_pk PRIMARY KEY (rate_date, base_currency, currency);

CREATE INDEX IF NOT EXISTS fx_rates_by_currency ON fx_rates(currency, rate_date);
CREATE INDEX IF NOT EXISTS fx_rates_by_date     ON fx_rates(rate_date);

-- Idempotent UPSERT example (for later use from apps):
-- INSERT INTO fx_rates (rate_date, base_currency, currency, rate, fetched_at, payload_json)
-- VALUES ('2025-01-10','EUR','USD',1.087500, now(), '{"source":"demo"}'::jsonb)
-- ON CONFLICT (rate_date, base_currency, currency)
-- DO UPDATE SET rate = EXCLUDED.rate,
--               fetched_at = now(),
--               payload_json = EXCLUDED.payload_json;
