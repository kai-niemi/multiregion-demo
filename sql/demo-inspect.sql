-- Show regions
show regions from database demo;

-- Show survival goal which should be 'zone' (default)
select survival_goal from [show databases] where database_name = 'demo';

-- Show ranges for a table
show ranges from table account;
-- Show ranges for a table primary index
show ranges from index account@primary;

-- Inspect row level replica placement. Note it doesn't check for row exists,
-- just where the DB would place replicas.
select distinct
    split_part(unnest(replica_localities), ',', 2) replica_locality,
    replicas
from [show range from table account for row ('us-east-1', '10000000-0000-0000-0000-000000000000')];

select distinct
    split_part(unnest(replica_localities), ',', 2) replica_locality,
    replicas
from [show range from table account for row ('eu-central-1', '10000000-0000-0000-0000-000000000000')];

select distinct
    split_part(unnest(replica_localities), ',', 2) replica_locality,
    replicas
from [show range from table account for row ('ap-northeast-1', '10000000-0000-0000-0000-000000000000')];

-- Another way to show range replica distribution for a table.
-- Hint: Type \x in the console to flatten table output.
select * from [show ranges from table account] where "start_key" not like '%Prefix%';

-- And yet another way
select distinct
    split_part(split_part(unnest(replica_localities), ',', 1), '=', 2) cloud,
    split_part(split_part(unnest(replica_localities), ',', 2), '=', 2) region,
    split_part(split_part(unnest(replica_localities), ',', 3), '=', 2) az,
    unnest(replicas) replica
    from [show ranges from table account]
order by region,az;

-- Show replica distribution across the 3 regions for a RBR table
with replicas as (
    select distinct
        split_part(unnest(replica_localities), ',', 2) replica_locality,
        replicas
        from [show range from table account for row ('us-east-1', '10000000-0000-0000-0000-000000000000'::UUID)]
    union all
    select distinct
        split_part(unnest(replica_localities), ',', 2) replica_locality,
        replicas
    from [show range from table account for row ('eu-central-1', '10000000-0000-0000-0000-000000000000'::UUID)]
    union all
    select distinct
        split_part(unnest(replica_localities), ',', 2) replica_locality,
        replicas
    from [show range from table account for row ('ap-northeast-1', '10000000-0000-0000-0000-000000000000'::UUID)]
) select * from replicas order by replica_locality;

-- Show replica distribution for global table
select distinct
    split_part(unnest(replica_localities), ',', 2) replica_locality,
    replicas
from [show ranges from table transfer_code];
