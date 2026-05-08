------------------------
-- Baseline account plan
------------------------

-- Global table with static codes (could be enum also but this is to showcase the concept)
insert into transfer_code (id, description)
values ('Generic', 'n/a'),
       ('Fee', 'n/a'),
       ('Refund', 'n/a'),
       ('Chargeback', 'n/a'),
       ( 'Grant', 'n/a'),
       ('Bank', 'n/a');

-- Main accounts with known IDs we are going to use in inspection queries
insert into account (crdb_region, id, balance, name, type, closed, allow_negative)
values
    ('us-east-1','10000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:1', 'Asset', false, 0),
    ('us-east-1','20000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:2', 'Asset', false, 0),
    ('us-east-1','30000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:3', 'Asset', false, 0),
    ('eu-central-1','40000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:4', 'Asset', false, 0),
    ('eu-central-1','50000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:5', 'Asset', false, 0),
    ('eu-central-1','60000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:6', 'Asset', false, 0),
    ('ap-northeast-1','70000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:7', 'Asset', false, 0),
    ('ap-northeast-1','80000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:8', 'Asset', false, 0),
    ('ap-northeast-1','90000000-0000-0000-0000-000000000000'::UUID, '1000000.00', 'user:9', 'Asset', false, 0);

-- Insert some more accounts into each region, just to fill out (optional)
insert into account (crdb_region, balance, name, type, closed, allow_negative)
select 'us-east-1',
       '1000000.00',
       (concat('user:', no::text)),
       'Asset',
       false,
       0
from generate_series(1, 1000) no;

insert into account (crdb_region, balance, name, type, closed, allow_negative)
select 'eu-central-1',
       '1000000.00',
       (concat('user:', no::text)),
       'Asset',
       false,
       0
from generate_series(1, 1000) no;

insert into account (crdb_region, balance, name, type, closed, allow_negative)
select 'ap-northeast-1',
       '1000000.00',
       (concat('user:', no::text)),
       'Asset',
       false,
       0
from generate_series(1, 1000) no;

