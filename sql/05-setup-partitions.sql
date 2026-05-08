SET enable_super_regions = 'on';

-- Create 3 super regions with only one region each. This is not enough for data domiciling
-- since theres not enough failure domains and survival is always prioritized over constraint
-- enforcement. To get actual data pinning (and region survival), you need 3 regions per super region.
alter database demo add super region eu values "eu-central-1";
alter database demo add super region us values "us-east-1";
alter database demo add super region ap values "ap-northeast-1";

show super regions from database demo;

-- NOTE: Need at least 3 regions per super region for region survival and at least 3 nodes per region.
-- Example:
-- alter database demo add super region eu values "eu-central-1","eu-west-1","eu-west-2";
-- alter database demo add super region us values "us-east-1","us-east-2","us-west-1";
-- alter database demo add super region ap values "ap-northeast-1","ap-northeast-2","ap-northeast-3";
