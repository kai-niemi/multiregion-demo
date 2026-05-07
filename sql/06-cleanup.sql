alter database demo drop super region eu;
alter database demo drop super region us;
alter database demo drop super region ap;

show super regions from database demo;

use demo;
truncate table account cascade;
truncate table transfer cascade;
truncate table transfer_item cascade;
truncate table transfer_code cascade;
