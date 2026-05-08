-- Random transfers for accounts only in eu-central-1 region
WITH shuffled AS (
    SELECT
        id,
        row_number() OVER (ORDER BY random()) AS rn
    FROM account where crdb_region='eu-central-1'
),
     pairs AS (
         SELECT
             row_number() OVER () AS seq,
             a.id AS credit_account_id,
             b.id AS debit_account_id,
             75.00::DECIMAL(18,2) AS amount
         FROM shuffled a
                  JOIN shuffled b
                       ON b.rn = a.rn + 1
         WHERE a.rn % 2 = 1
    LIMIT 10
    ),
    batched AS (
SELECT
    array_agg(credit_account_id ORDER BY seq) AS credit_ids,
    array_agg(debit_account_id  ORDER BY seq) AS debit_ids,
    array_agg(amount            ORDER BY seq) AS amounts
FROM pairs
    )
SELECT *
FROM batched,
    fn_create_transfer_batch(credit_ids, debit_ids, amounts);


-- Random transfers for accounts only in us-east-1 region
WITH shuffled AS (
    SELECT
        id,
        row_number() OVER (ORDER BY random()) AS rn
    FROM account where crdb_region='us-east-1'
),
     pairs AS (
         SELECT
                     row_number() OVER () AS seq,
                     a.id AS credit_account_id,
                     b.id AS debit_account_id,
                     75.00::DECIMAL(18,2) AS amount
         FROM shuffled a
                  JOIN shuffled b
                       ON b.rn = a.rn + 1
         WHERE a.rn % 2 = 1
         LIMIT 10
     ),
     batched AS (
         SELECT
             array_agg(credit_account_id ORDER BY seq) AS credit_ids,
             array_agg(debit_account_id  ORDER BY seq) AS debit_ids,
             array_agg(amount            ORDER BY seq) AS amounts
         FROM pairs
     )
SELECT *
FROM batched,
    fn_create_transfer_batch(credit_ids, debit_ids, amounts);

-- Random transfers for accounts only in ap-northeast-1 region
WITH shuffled AS (
    SELECT
        id,
        row_number() OVER (ORDER BY random()) AS rn
    FROM account where crdb_region='ap-northeast-1'
),
     pairs AS (
         SELECT
                     row_number() OVER () AS seq,
                     a.id AS credit_account_id,
                     b.id AS debit_account_id,
                     75.00::DECIMAL(18,2) AS amount
         FROM shuffled a
                  JOIN shuffled b
                       ON b.rn = a.rn + 1
         WHERE a.rn % 2 = 1
         LIMIT 10
     ),
     batched AS (
         SELECT
             array_agg(credit_account_id ORDER BY seq) AS credit_ids,
             array_agg(debit_account_id  ORDER BY seq) AS debit_ids,
             array_agg(amount            ORDER BY seq) AS amounts
         FROM pairs
     )
SELECT *
FROM batched,
    fn_create_transfer_batch(credit_ids, debit_ids, amounts);