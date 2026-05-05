select survival_goal from [show databases] where database_name = 'demo';
alter database demo survive region failure;
alter database demo survive zone failure;
