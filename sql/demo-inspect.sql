show ranges from table account;
show ranges from index account@primary;

-- Inspect row level replica placement (note it doesnt check for row exists)
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

-- Show range replica distribution for table account
select * from [show ranges from table account] where "start_key" not like '%Prefix%';

-- Show range replica distribution for table account
select distinct
    split_part(split_part(unnest(replica_localities), ',', 1), '=', 2) cloud,
    split_part(split_part(unnest(replica_localities), ',', 2), '=', 2) region,
    split_part(split_part(unnest(replica_localities), ',', 3), '=', 2) az,
    unnest(replicas) replica
    from [show ranges from table account]
order by region,az;

-- Show range replica distribution by row for table account
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

-- Global table
select distinct
    split_part(unnest(replica_localities), ',', 2) replica_locality,
    replicas
from [show ranges from table transfer_code];
