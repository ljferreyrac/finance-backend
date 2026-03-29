-- =============================================================================
-- V1__initial_schema.sql
--
-- Complete initial schema for finanzas-ia.
-- Consolidates what was previously V1 through V7 into a single migration
-- for clean-database deployments.
--
-- Tables (in dependency order):
--   users, categories, accounts, transactions,
--   tags, transaction_tags, exchange_rates
--
-- PostgreSQL 16+  (gen_random_uuid() is built-in; no pgcrypto needed)
-- =============================================================================


-- =============================================================================
-- FUNCTION: set_updated_at
-- Shared trigger function used by every table that has an updated_at column.
-- =============================================================================
CREATE OR REPLACE FUNCTION set_updated_at()
  RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


-- =============================================================================
-- TABLE: users
-- Core identity table. Auth tokens reference this table.
-- Soft-delete via deleted_at so foreign-key history is preserved.
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,

  -- Preferred display currency for the dashboard; default Peruvian soles
  currency      VARCHAR(3)   NOT NULL DEFAULT 'PEN'
                               CHECK (currency IN ('PEN', 'USD')),

  -- IANA timezone; used for date bucketing in reports
  timezone      VARCHAR(50)  NOT NULL DEFAULT 'America/Lima',

  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  -- NULL = active; non-NULL = soft-deleted account
  deleted_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_users_email
  ON users (email);

CREATE OR REPLACE TRIGGER trg_users_set_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- TABLE: categories
-- User-defined labels for transactions. No hardcoded category list.
-- =============================================================================
CREATE TABLE IF NOT EXISTS categories (
  id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       VARCHAR(100) NOT NULL,
  color      VARCHAR(7),
  icon       VARCHAR(50),
  is_default BOOLEAN      NOT NULL DEFAULT FALSE,
  position   INT          NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

-- At most one default category per user
CREATE UNIQUE INDEX IF NOT EXISTS uidx_categories_one_default_per_user
  ON categories (user_id) WHERE is_default = TRUE;

CREATE INDEX IF NOT EXISTS idx_categories_user_id
  ON categories (user_id);

CREATE OR REPLACE TRIGGER trg_categories_set_updated_at
  BEFORE UPDATE ON categories
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- TABLE: accounts
-- Any financial account a user owns: bank accounts, credit cards,
-- cash envelopes, and digital wallets (Yape, Plin).
--
-- current_balance is maintained by the application layer; negative values
-- are permitted (credit card debt).
-- credit_limit, closing_day, due_day are meaningful only for CREDIT_CARD.
-- linked_account_id allows a wallet to be linked to a parent bank account
-- so balance adjustments flow to the parent.
-- =============================================================================
CREATE TABLE IF NOT EXISTS accounts (
  id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           UUID          NOT NULL
                                    REFERENCES users(id) ON DELETE CASCADE,
  name              VARCHAR(100)  NOT NULL,

  type              VARCHAR(20)   NOT NULL
                                    CHECK (type IN (
                                      'BANK', 'CREDIT_CARD', 'CASH', 'WALLET'
                                    )),

  -- NULL for CASH / WALLET accounts
  bank              VARCHAR(50)
                                    CHECK (bank IN (
                                      'BCP', 'BBVA', 'Interbank', 'Scotiabank',
                                      'Yape', 'Plin', 'other'
                                    )),

  currency          VARCHAR(3)    NOT NULL DEFAULT 'PEN'
                                    CHECK (currency IN ('PEN', 'USD')),

  -- Application-maintained running balance; negative values are valid
  current_balance   NUMERIC(14,2) NOT NULL DEFAULT 0,

  -- Populated for CREDIT_CARD accounts only
  credit_limit      NUMERIC(14,2),

  -- Billing cycle days (1-31); relevant for CREDIT_CARD accounts
  closing_day       SMALLINT      CHECK (closing_day BETWEEN 1 AND 31),
  due_day           SMALLINT      CHECK (due_day BETWEEN 1 AND 31),

  -- Hex color for UI display, e.g. #2563EB
  color             VARCHAR(7),

  -- Only one account per user may be flagged as default (partial index below)
  is_default        BOOLEAN       NOT NULL DEFAULT FALSE,
  is_active         BOOLEAN       NOT NULL DEFAULT TRUE,

  -- Wallet-to-bank linking; balance adjustments flow to the linked account
  linked_account_id UUID
                                    REFERENCES accounts(id) ON DELETE SET NULL,

  created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Enforce at most one default account per user at the database level
CREATE UNIQUE INDEX IF NOT EXISTS uidx_accounts_one_default_per_user
  ON accounts (user_id) WHERE is_default = TRUE;

CREATE INDEX IF NOT EXISTS idx_accounts_user_id
  ON accounts (user_id);

CREATE OR REPLACE TRIGGER trg_accounts_set_updated_at
  BEFORE UPDATE ON accounts
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- TABLE: transactions
-- Central ledger. Each row is one financial event logged by a user.
--
-- EXPENSE / INCOME : account_id required; transfer fields NULL
-- TRANSFER         : from_account_id + to_account_id required; account_id NULL
--
-- amount_local stores the account's native-currency equivalent for
-- cross-currency transactions (e.g. a USD expense on a PEN account).
-- NULL when transaction currency matches the account currency.
--
-- exchange_rate_applied = amount_local / amount; stored for audit / reporting.
-- NULL when currencies match.
-- =============================================================================
CREATE TABLE IF NOT EXISTS transactions (
  id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id                UUID           NOT NULL
                                          REFERENCES users(id) ON DELETE CASCADE,

  type                   VARCHAR(10)    NOT NULL
                                          CHECK (type IN ('EXPENSE', 'INCOME', 'TRANSFER')),

  -- Amount is always positive; direction is captured by type
  amount                 NUMERIC(14,2)  NOT NULL CHECK (amount > 0),

  currency               VARCHAR(3)     NOT NULL DEFAULT 'PEN'
                                          CHECK (currency IN ('PEN', 'USD')),

  -- EXPENSE / INCOME only; NULL for TRANSFER
  account_id             UUID           REFERENCES accounts(id) ON DELETE SET NULL,

  -- TRANSFER only; NULL for EXPENSE / INCOME
  from_account_id        UUID           REFERENCES accounts(id) ON DELETE SET NULL,
  to_account_id          UUID           REFERENCES accounts(id) ON DELETE SET NULL,

  category_id            UUID           REFERENCES categories(id) ON DELETE SET NULL,

  merchant               VARCHAR(255),
  description            TEXT,

  -- Signed Cloud Storage URL; populated after OCR processing
  receipt_url            TEXT,

  -- The date the transaction occurred, not when it was logged
  transaction_date       DATE           NOT NULL,

  -- Cross-currency support
  amount_local           NUMERIC(14,2)  NULL CHECK (amount_local IS NULL OR amount_local > 0),
  exchange_rate_applied  NUMERIC(10,4)  NULL,

  created_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

  -- NULL = active; non-NULL = soft-deleted
  deleted_at             TIMESTAMPTZ,

  -- Enforce correct account field usage per transaction type
  CONSTRAINT chk_transaction_account_fields CHECK (
    (type IN ('EXPENSE', 'INCOME')
       AND account_id      IS NOT NULL
       AND from_account_id IS NULL
       AND to_account_id   IS NULL)
    OR
    (type = 'TRANSFER'
       AND account_id      IS NULL
       AND from_account_id IS NOT NULL
       AND to_account_id   IS NOT NULL)
  )
);

COMMENT ON COLUMN transactions.amount_local IS
  'Amount in account primary currency for cross-currency transactions. User-confirmed PEN equivalent for USD charges. NULL when currencies match.';

COMMENT ON COLUMN transactions.exchange_rate_applied IS
  'Effective rate (amount_local / amount) recorded at transaction time. NULL when currencies match.';

-- Primary list query: paginated transactions for a user ordered by date
CREATE INDEX IF NOT EXISTS idx_transactions_user_date
  ON transactions (user_id, transaction_date DESC);

-- Filter by type (e.g. show only expenses)
CREATE INDEX IF NOT EXISTS idx_transactions_user_type
  ON transactions (user_id, type);

-- FK support: partial because account_id is NULL for TRANSFER rows
CREATE INDEX IF NOT EXISTS idx_transactions_account_id
  ON transactions (account_id) WHERE account_id IS NOT NULL;

-- FK support: partial because from_account_id is NULL for non-TRANSFER rows
CREATE INDEX IF NOT EXISTS idx_transactions_from_account
  ON transactions (from_account_id) WHERE from_account_id IS NOT NULL;

-- FK support: partial because category_id is often NULL
CREATE INDEX IF NOT EXISTS idx_transactions_category_id
  ON transactions (category_id) WHERE category_id IS NOT NULL;

-- Separate active from deleted rows per user
CREATE INDEX IF NOT EXISTS idx_transactions_user_deleted_at
  ON transactions (user_id, deleted_at);

-- Report aggregation by currency and/or type
CREATE INDEX IF NOT EXISTS idx_transactions_user_currency
  ON transactions (user_id, currency, type);

CREATE OR REPLACE TRIGGER trg_transactions_set_updated_at
  BEFORE UPDATE ON transactions
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- TABLE: tags
-- User-owned labels. Name is normalised to lowercase at the DB level
-- so "Food", " food ", and "FOOD" can never coexist for the same user.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tags (
  id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- Stored as trimmed lowercase; enforced by constraint
  name       VARCHAR(50) NOT NULL CHECK (name = lower(trim(name))),

  -- Optional hex color for UI display, e.g. #FF5733
  color      CHAR(7)     CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_tags_user_name UNIQUE (user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_tags_user_id
  ON tags (user_id);

CREATE OR REPLACE TRIGGER trg_tags_set_updated_at
  BEFORE UPDATE ON tags
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- TABLE: transaction_tags
-- Many-to-many junction between transactions and tags.
-- Both sides cascade on delete; no orphaned rows possible.
-- =============================================================================
CREATE TABLE IF NOT EXISTS transaction_tags (
  transaction_id UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
  tag_id         UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
  PRIMARY KEY (transaction_id, tag_id)
);

-- Supports "find all transactions for a given tag" queries
CREATE INDEX IF NOT EXISTS idx_transaction_tags_tag_id
  ON transaction_tags (tag_id);


-- =============================================================================
-- TABLE: exchange_rates
-- Daily buy/sell rates between currency pairs.
-- buy_rate  (compra): rate at which a bank buys foreign currency from you.
-- sell_rate (venta) : rate charged when a foreign-currency transaction is
--                    recorded against a local-currency account.
-- =============================================================================
CREATE TABLE IF NOT EXISTS exchange_rates (
  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  currency_from VARCHAR(3)    NOT NULL,
  currency_to   VARCHAR(3)    NOT NULL,

  buy_rate      NUMERIC(10,4) NOT NULL CHECK (buy_rate > 0),
  sell_rate     NUMERIC(10,4) NOT NULL CHECK (sell_rate > 0),

  -- sell_rate must be >= buy_rate (standard bid-ask spread)
  CONSTRAINT chk_exchange_rates_spread CHECK (sell_rate >= buy_rate),

  rate_date     DATE          NOT NULL,

  -- 'MANUAL' (seed / user entry) or 'SBS' (official Peruvian feed)
  source        VARCHAR(20)   NOT NULL DEFAULT 'MANUAL'
                                CHECK (source IN ('MANUAL', 'SBS')),

  created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_exchange_rates_pair_date UNIQUE (currency_from, currency_to, rate_date)
);

-- Primary lookup: rate for a given pair on a given date
CREATE INDEX IF NOT EXISTS idx_exchange_rates_pair_date
  ON exchange_rates (currency_from, currency_to, rate_date DESC);

CREATE OR REPLACE TRIGGER trg_exchange_rates_set_updated_at
  BEFORE UPDATE ON exchange_rates
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- =============================================================================
-- SEED: approximate Peruvian market rates (USD/PEN) for the deployment date.
-- ON CONFLICT makes every subsequent run a no-op (idempotent).
-- =============================================================================
INSERT INTO exchange_rates (currency_from, currency_to, buy_rate, sell_rate, rate_date, source)
VALUES ('USD', 'PEN', 3.69, 3.74, CURRENT_DATE, 'MANUAL')
ON CONFLICT ON CONSTRAINT uq_exchange_rates_pair_date DO NOTHING;
