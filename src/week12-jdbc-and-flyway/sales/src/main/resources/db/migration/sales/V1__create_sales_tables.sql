CREATE TABLE customers (
    customer_id   CHAR(5)      NOT NULL,
    company_name  VARCHAR(40)  NOT NULL,
    contact_name  VARCHAR(30),
    street        VARCHAR(60)  NOT NULL,
    city          VARCHAR(15)  NOT NULL,
    postal_code   VARCHAR(10)  NOT NULL,
    country       VARCHAR(15)  NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id)
);

CREATE TABLE order_statuses (
    status  VARCHAR(16) NOT NULL,
    CONSTRAINT pk_order_statuses PRIMARY KEY (status)
);

INSERT INTO order_statuses (status) VALUES ('DRAFT'), ('CONFIRMED'), ('SHIPPED'), ('CANCELLED');

CREATE TABLE orders (
    order_id          INTEGER      NOT NULL,
    customer_id       CHAR(5)      NOT NULL,
    order_date        DATE         NOT NULL,
    status            VARCHAR(16)  NOT NULL,
    ship_street       VARCHAR(60),
    ship_city         VARCHAR(15),
    ship_postal_code  VARCHAR(10),
    ship_country      VARCHAR(15),
    shipped_date      DATE,
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_orders_status FOREIGN KEY (status) REFERENCES order_statuses (status)
);

CREATE TABLE order_lines (
    order_id    INTEGER        NOT NULL,
    line_no     SMALLINT       NOT NULL,
    product_id  INTEGER        NOT NULL,
    unit_price  NUMERIC(10, 2) NOT NULL,
    currency    CHAR(3)        NOT NULL,
    quantity    SMALLINT       NOT NULL,
    discount    NUMERIC(3, 2)  NOT NULL,
    CONSTRAINT pk_order_lines PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (order_id) ON DELETE CASCADE,
    CONSTRAINT ck_order_lines_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_lines_discount CHECK (discount BETWEEN 0 AND 1)
);
