CREATE TABLE IF NOT EXISTS client_snapshot (
                                 id UUID PRIMARY KEY,
                                 client_id VARCHAR(100) NOT NULL,
                                 as_of_at TIMESTAMPTZ NOT NULL,

                                 CONSTRAINT uq_client_snapshot_client_date
                                     UNIQUE (client_id, as_of_at)
);