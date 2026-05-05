SET enable_super_regions = 'on';

select survival_goal from [show databases] where database_name = 'demo';

-- (!!) Need at least 3 regions per super region for region survival
-- alter database demo add super region eu values "eu-central-1, eu-west-1, eu-west-2";
-- alter database demo add super region us values "us-east-1, us-east-2, us-west-1";
-- alter database demo add super region au values "ap-northeast-2, ap-northeast-3, ap-northeast-4";

alter database demo add super region eu values "eu-central-1";
alter database demo add super region us values "us-east-1";
alter database demo add super region ap values "ap-northeast-1";

show super regions from database demo;
