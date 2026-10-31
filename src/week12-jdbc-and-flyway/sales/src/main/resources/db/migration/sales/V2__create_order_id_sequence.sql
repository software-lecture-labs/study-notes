-- Order ids now come from the database: an in-process counter restarts at
-- 10248 on every boot and collides with the rows of the previous run.
CREATE SEQUENCE orders_seq START WITH 10248 INCREMENT BY 1;
