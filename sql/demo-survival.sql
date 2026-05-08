select survival_goal from [show databases] where database_name = 'demo';

-- See notes on region survival:
-- alter database demo survive region failure;
alter database demo survive zone failure;
