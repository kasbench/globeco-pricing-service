-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0-beta1
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

-- object: new_database | type: DATABASE --
-- DROP DATABASE IF EXISTS new_database;
CREATE DATABASE new_database;
-- ddl-end --


SET search_path TO pg_catalog,public;
-- ddl-end --

-- object: public.price | type: TABLE --
-- DROP TABLE IF EXISTS public.price CASCADE;
CREATE TABLE public.price (
	id serial NOT NULL,
	price_date date NOT NULL,
	ticker varchar(20) NOT NULL,
	price decimal(18,8) NOT NULL,
	price_std float NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT price_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.price OWNER TO postgres;
-- ddl-end --

-- object: price_ticker_ndx | type: INDEX --
-- DROP INDEX IF EXISTS public.price_ticker_ndx CASCADE;
CREATE INDEX price_ticker_ndx ON public.price
USING btree
(
	ticker
);
-- ddl-end --


