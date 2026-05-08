-- Connect to eu-central-1: roachprod sql cluster:10
-- Connect to us-east-1: roachprod sql cluster:11
-- Connect to ap-northeast-1: roachprod sql cluster:12

--
-- eu-central-1
--

-- Local latency from eu-central-1
--explain
select id,crdb_region from account
where id='40000000-0000-0000-0000-000000000000' and crdb_region='eu-central-1';

-- Local latency from everywhere (unless all replicas are pinned)
--explain
select id,crdb_region from account
as of system time follower_read_timestamp()
where id='40000000-0000-0000-0000-000000000000';

--
-- us-east-1
--

-- Local latency from us-east-1
select id,crdb_region from account
where id='10000000-0000-0000-0000-000000000000' and crdb_region='us-east-1';

-- Local latency from everywhere
select id,crdb_region from account
as of system time follower_read_timestamp()
where id='10000000-0000-0000-0000-000000000000' and crdb_region='us-east-1';

--
-- ap-northeast-1
--

-- Local latency from ap-northeast-1
select id,crdb_region from account
where id='70000000-0000-0000-0000-000000000000' and crdb_region='ap-northeast-1';

-- Local latency from everywhere
select id,crdb_region from account
as of system time follower_read_timestamp()
where id='70000000-0000-0000-0000-000000000000' and crdb_region='ap-northeast-1';
