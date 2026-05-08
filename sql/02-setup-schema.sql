SET sql_safe_updates = false;
SET allow_unsafe_internals = true;

-- In this demo were only using 3 regions which is enough to showcase data domiciling using
-- super regions with zone survival. Region survival would however need 6 regions minimum
-- across 2 super regions and 3 nodes per region (6x3 nodes).
create database demo
    primary region "eu-central-1"
    regions "us-east-1", "ap-northeast-1";

-- Example of 9 regions for 3 super regions (9x3 nodes):
-- create database demo
--     primary region "eu-central-1"
--     regions "eu-west-1", "eu-west-2",
--     "us-east-1", "us-east-2", "us-west-1",
--     "ap-northeast-1","ap-northeast-2","ap-northeast-3";

use demo;

---------------------------------------
-- Add regions post db-creation (optional)
-----------------------------------------

-- alter database demo primary region "eu-central-1";
-- alter database demo add region "us-east-1";
-- alter database demo add region "ap-northeast-1";

----------------------
-- Table schema
----------------------

create type account_type as enum ('Asset', 'Liability', 'Expense', 'Revenue', 'Equity');

-- Monetary account table serving both fast local reads+writes, thus suitable for RBR locality.
-- This means the voting replicas are pinned to each region based on region key.
create table account
(
    id                 uuid           not null default gen_random_uuid(),
    balance            decimal(19, 3) not null,
    name               varchar(128) null,
    type               account_type   not null,
    closed             boolean        not null default false,
    allow_negative     integer        not null default 0,
    last_modified_time timestamptz    not null default clock_timestamp(),

    primary key (id)
) locality regional by row;

-- The region key (a hidden column named crdb_region) in this demo is supplied at INSERT time.
-- Other options include deriving it rom the gateway node locality flag or by using a computed
-- column to infer it from some business key like city or country.
-- Example assuming we use a 'city' column:
-- ALTER TABLE account
--     ADD COLUMN region crdb_internal_region AS (
--         CASE
--             WHEN city IN ('stockholm', 'helsinki', ..) THEN 'eu-central-1'
--             WHEN city IN ('new york', 'boston', ..) THEN 'eu-east-1'
--             WHEN city IN ('tokyo', 'singapore', ..) THEN 'ap-northeast-1'
--             ELSE 'eu-central-1'
--             END
--         ) STORED NOT NULL;
-- alter table account set locality regional by row as region;

-- Immutable account transfer journal table serving only fast local writes, thus suitable for RBR locality.
-- This means the voting replicas are pinned to each region based on region key.
create table transfer
(
    id            uuid not null default gen_random_uuid(),
    booking_date  date not null default current_date(),
    transfer_date date not null default current_date(),
    transfer_type varchar(12) not null default 'Generic',

    primary key (id)
) locality regional by row;

-- Immutable account transfer legs table serving only writes, thus suitable for RBR locality.
-- This means the voting replicas are pinned to each region based on region key.
create table transfer_item
(
    transfer_id     uuid           not null,
    account_id      uuid           not null,
    amount          decimal(19, 3) not null,
    note            string,
    running_balance decimal(19, 3) not null,

    primary key (transfer_id, account_id)
) locality regional by row;

-- Immutable account transfer legs table serving only reads, thus suitable for global locality.
-- This means there's at least one replica in every region that can serve authoritative reads
-- with local latency.
create table transfer_code
(
    id          varchar(12) not null,
    description string,

    primary key (id)
) locality global;

------------------------------------------
-- Change localities afterwards (optional)
------------------------------------------

-- alter table account set (schema_locked = false);
-- alter table account set locality regional by row;

-- alter table transfer set (schema_locked = false);
-- alter table transfer set locality regional by row;

-- alter table transfer_item set (schema_locked = false);
-- alter table transfer_item set locality regional by row;

-- alter table transfer_code set (schema_locked = false);
-- alter table transfer_code set locality global;

----------------------
-- Constraints
----------------------

alter table account
    add constraint check_account_positive_balance check (balance * abs(allow_negative - 1) >= 0);
alter table account
    add constraint check_account_allow_negative check (allow_negative between 0 and 1);
alter table transfer
    add constraint fk_a foreign key (transfer_type) references transfer_code (id);
alter table transfer_item
    add constraint fk_a foreign key (transfer_id) references transfer (id);
alter table transfer_item
    add constraint fk_b foreign key (account_id) references account (id);

