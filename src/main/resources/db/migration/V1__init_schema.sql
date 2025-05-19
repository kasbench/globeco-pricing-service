-- Initial schema migration for globeco-pricing-service

SET search_path TO pg_catalog,public;

-- object: public.price | type: TABLE --
CREATE TABLE public.price (
    id serial NOT NULL,
    price_date date NOT NULL,
    ticker varchar(20) NOT NULL,
    price decimal(18,8) NOT NULL,
    price_std float NOT NULL,
    version integer NOT NULL DEFAULT 1,
    CONSTRAINT price_pk PRIMARY KEY (id)
);

-- object: price_ticker_ndx | type: INDEX --
CREATE INDEX price_ticker_ndx ON public.price
USING btree
(
    ticker
); 