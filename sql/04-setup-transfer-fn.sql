-- A CTE that creates a balanced, multi-legged monetary transaction
-- between select account IDs.
CREATE OR REPLACE FUNCTION fn_create_transfer_batch(
    p_credit_account_ids UUID[],
    p_debit_account_ids  UUID[],
    p_amounts            DECIMAL(18,2)[]
)
RETURNS TABLE (
    seq               INT,
    transfer_id       UUID,
    credit_account_id UUID,
    debit_account_id  UUID,
    amount            DECIMAL(18,2)
)
LANGUAGE SQL
AS $$
WITH
inputs AS (
    SELECT
        c.ord::INT AS seq,
        c.account_id AS credit_account_id,
        d.account_id AS debit_account_id,
        a.amount
    FROM unnest(p_credit_account_ids) WITH ORDINALITY AS c(account_id, ord)
    JOIN unnest(p_debit_account_ids)  WITH ORDINALITY AS d(account_id, ord)
      ON d.ord = c.ord
    JOIN unnest(p_amounts)            WITH ORDINALITY AS a(amount, ord)
      ON a.ord = c.ord
),
validated AS (
    SELECT *
    FROM inputs
    WHERE credit_account_id <> debit_account_id
),
headers AS (
    SELECT
        seq,
        gen_random_uuid() AS transfer_id,
        credit_account_id,
        debit_account_id,
        amount
    FROM validated
),
insert_head AS (
    INSERT INTO transfer (id)
    SELECT transfer_id
    FROM headers
    RETURNING id
),
account_events AS (
    SELECT
        h.seq,
        h.transfer_id,
        h.credit_account_id AS account_id,
        h.amount            AS delta
    FROM headers h

    UNION ALL

    SELECT
        h.seq,
        h.transfer_id,
        h.debit_account_id  AS account_id,
        -h.amount           AS delta
    FROM headers h
),
base_balances AS (
    SELECT
        a.id AS account_id,
        a.balance AS base_balance
    FROM account a
    JOIN (
        SELECT account_id FROM account_events
        GROUP BY account_id
    ) t
      ON t.account_id = a.id
),
event_balances AS (
    SELECT
        e.seq,
        e.transfer_id,
        e.account_id,
        e.delta,
        b.base_balance
          + SUM(e.delta) OVER (
                PARTITION BY e.account_id
                ORDER BY e.seq
                ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
            ) AS running_balance
    FROM account_events e
    JOIN base_balances b
      ON b.account_id = e.account_id
),
insert_items AS (
    INSERT INTO transfer_item (
        transfer_id,
        account_id,
        amount,
        running_balance
    )
    SELECT
        transfer_id,
        account_id,
        delta,
        running_balance
    FROM event_balances
    RETURNING transfer_id
),
account_deltas AS (
    SELECT
        account_id,
        SUM(delta) AS delta
    FROM account_events
    GROUP BY account_id
),
update_accounts AS (
    UPDATE account a
       SET balance = a.balance + d.delta
      FROM account_deltas d
     WHERE a.id = d.account_id
    RETURNING a.id, a.balance
)
SELECT
    h.seq,
    h.transfer_id,
    h.credit_account_id,
    h.debit_account_id,
    h.amount
FROM headers h
ORDER BY h.seq;
$$;
