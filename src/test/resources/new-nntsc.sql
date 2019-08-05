--
-- PostgreSQL database dump
--

-- Dumped from database version 10.9 (Ubuntu 10.9-0ubuntu0.18.04.1)
-- Dumped by pg_dump version 10.9 (Ubuntu 10.9-0ubuntu0.18.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: _final_most(anyarray); Type: FUNCTION; Schema: public; Owner: cuz
--

CREATE FUNCTION public._final_most(anyarray) RETURNS anyelement
    LANGUAGE sql IMMUTABLE
    AS '
                SELECT a
                FROM unnest($1) a
                GROUP BY 1 ORDER BY count(1) DESC
                LIMIT 1;
            ';


ALTER FUNCTION public._final_most(anyarray) OWNER TO cuz;

--
-- Name: _final_smoke(anyarray); Type: FUNCTION; Schema: public; Owner: cuz
--

CREATE FUNCTION public._final_smoke(anyarray) RETURNS numeric[]
    LANGUAGE sql IMMUTABLE
    AS '
                SELECT array_agg(avg)::numeric[] FROM (
                    SELECT avg(foo), ntile FROM (
                        SELECT foo, ntile(20) OVER (PARTITION BY one ORDER BY foo) FROM (
                            SELECT 1 as one, unnest($1) as foo
                        ) as a WHERE foo IS NOT NULL
                    ) as b GROUP BY ntile ORDER BY ntile
                ) as c;
            ';


ALTER FUNCTION public._final_smoke(anyarray) OWNER TO cuz;

--
-- Name: create_seq(text, text); Type: FUNCTION; Schema: public; Owner: cuz
--

CREATE FUNCTION public.create_seq(_seq text, _schema text DEFAULT 'public'::text) RETURNS void
    LANGUAGE plpgsql STRICT
    AS '
            DECLARE
               _fullname text := quote_ident(_seq);

            BEGIN
            -- If an object of the name exists, the first RAISE EXCEPTION
            -- raises a meaningful message immediately.
            -- Else, the failed cast to regclass raises its own exception
            -- "undefined_table".
            -- This particular error triggers sequence creation further down.

            RAISE EXCEPTION ''Object >>%<< of type "%" already exists.''
               ,_fullname
               ,(SELECT c.relkind FROM pg_class c
                 WHERE  c.oid = _fullname::regclass    -- error if non-existent
                ) USING ERRCODE = ''23505'';

            EXCEPTION WHEN undefined_table THEN -- SQLSTATE ''42P01''
                EXECUTE ''CREATE SEQUENCE '' || _fullname;
                RAISE NOTICE ''New sequence >>%<< created.'', _fullname;

            END
            ';


ALTER FUNCTION public.create_seq(_seq text, _schema text) OWNER TO cuz;

--
-- Name: FUNCTION create_seq(_seq text, _schema text); Type: COMMENT; Schema: public; Owner: cuz
--

COMMENT ON FUNCTION public.create_seq(_seq text, _schema text) IS 'Create new seq if name is free.
                 $1 _seq  .. name of sequence
                 $2 _name .. name of schema (optional; default is "public")';


--
-- Name: most(anyelement); Type: AGGREGATE; Schema: public; Owner: cuz
--

CREATE AGGREGATE public.most(anyelement) (
    SFUNC = array_append,
    STYPE = anyarray,
    INITCOND = '{}',
    FINALFUNC = public._final_most
);


ALTER AGGREGATE public.most(anyelement) OWNER TO cuz;

--
-- Name: smoke(numeric); Type: AGGREGATE; Schema: public; Owner: cuz
--

CREATE AGGREGATE public.smoke(numeric) (
    SFUNC = array_append,
    STYPE = numeric[],
    INITCOND = '{}',
    FINALFUNC = public._final_smoke
);


ALTER AGGREGATE public.smoke(numeric) OWNER TO cuz;

--
-- Name: smokearray(anyarray); Type: AGGREGATE; Schema: public; Owner: cuz
--

CREATE AGGREGATE public.smokearray(anyarray) (
    SFUNC = array_cat,
    STYPE = anyarray,
    INITCOND = '{}',
    FINALFUNC = public._final_smoke
);


ALTER AGGREGATE public.smokearray(anyarray) OWNER TO cuz;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: collections; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.collections (
    id integer NOT NULL,
    module character varying NOT NULL,
    modsubtype character varying,
    streamtable character varying NOT NULL,
    datatable character varying NOT NULL
);


ALTER TABLE public.collections OWNER TO cuz;

--
-- Name: collections_id_seq; Type: SEQUENCE; Schema: public; Owner: cuz
--

CREATE SEQUENCE public.collections_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.collections_id_seq OWNER TO cuz;

--
-- Name: collections_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: cuz
--

ALTER SEQUENCE public.collections_id_seq OWNED BY public.collections.id;


--
-- Name: data_amp_astraceroute; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_astraceroute (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    errors smallint NOT NULL,
    addresses smallint NOT NULL
);


ALTER TABLE public.data_amp_astraceroute OWNER TO cuz;

--
-- Name: data_amp_astraceroute_18; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_astraceroute_18 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    errors smallint NOT NULL,
    addresses smallint NOT NULL
);


ALTER TABLE public.data_amp_astraceroute_18 OWNER TO cuz;

--
-- Name: data_amp_astraceroute_19; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_astraceroute_19 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    errors smallint NOT NULL,
    addresses smallint NOT NULL
);


ALTER TABLE public.data_amp_astraceroute_19 OWNER TO cuz;

--
-- Name: data_amp_astraceroute_5; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_astraceroute_5 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    errors smallint NOT NULL,
    addresses smallint NOT NULL
);


ALTER TABLE public.data_amp_astraceroute_5 OWNER TO cuz;

--
-- Name: data_amp_astraceroute_6; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_astraceroute_6 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    errors smallint NOT NULL,
    addresses smallint NOT NULL
);


ALTER TABLE public.data_amp_astraceroute_6 OWNER TO cuz;

--
-- Name: data_amp_dns; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_dns (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    response_size integer,
    rtt integer,
    ttl smallint,
    query_len smallint,
    total_answer smallint,
    total_authority smallint,
    total_additional smallint,
    opcode smallint,
    rcode smallint,
    flag_rd boolean,
    flag_tc boolean,
    flag_aa boolean,
    flag_qr boolean,
    flag_cd boolean,
    flag_ad boolean,
    flag_ra boolean,
    requests smallint NOT NULL,
    lossrate double precision NOT NULL
);


ALTER TABLE public.data_amp_dns OWNER TO cuz;

--
-- Name: data_amp_external; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_external (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    value integer
);


ALTER TABLE public.data_amp_external OWNER TO cuz;

--
-- Name: data_amp_fastping; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_fastping (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    median integer,
    percentiles integer[],
    lossrate double precision NOT NULL
);


ALTER TABLE public.data_amp_fastping OWNER TO cuz;

--
-- Name: data_amp_http; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_http (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    server_count integer,
    object_count integer,
    duration integer,
    bytes bigint
);


ALTER TABLE public.data_amp_http OWNER TO cuz;

--
-- Name: data_amp_icmp; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_icmp (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    median integer,
    packet_size smallint NOT NULL,
    loss smallint NOT NULL,
    results smallint NOT NULL,
    lossrate double precision NOT NULL,
    rtts integer[]
);


ALTER TABLE public.data_amp_icmp OWNER TO cuz;

--
-- Name: data_amp_tcpping; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_tcpping (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    median integer,
    packet_size smallint NOT NULL,
    loss smallint NOT NULL,
    results smallint NOT NULL,
    icmperrors smallint NOT NULL,
    rtts integer[],
    lossrate double precision NOT NULL
);


ALTER TABLE public.data_amp_tcpping OWNER TO cuz;

--
-- Name: data_amp_throughput; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_throughput (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    bytes bigint,
    packets bigint,
    rate double precision,
    runtime integer
);


ALTER TABLE public.data_amp_throughput OWNER TO cuz;

--
-- Name: data_amp_traceroute; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_id integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    error_type smallint,
    error_code smallint,
    hop_rtt integer[] NOT NULL
);


ALTER TABLE public.data_amp_traceroute OWNER TO cuz;

--
-- Name: data_amp_traceroute_18; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_18 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_id integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    error_type smallint,
    error_code smallint,
    hop_rtt integer[] NOT NULL
);


ALTER TABLE public.data_amp_traceroute_18 OWNER TO cuz;

--
-- Name: data_amp_traceroute_19; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_19 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_id integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    error_type smallint,
    error_code smallint,
    hop_rtt integer[] NOT NULL
);


ALTER TABLE public.data_amp_traceroute_19 OWNER TO cuz;

--
-- Name: data_amp_traceroute_5; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_5 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_id integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    error_type smallint,
    error_code smallint,
    hop_rtt integer[] NOT NULL
);


ALTER TABLE public.data_amp_traceroute_5 OWNER TO cuz;

--
-- Name: data_amp_traceroute_6; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_6 (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_id integer NOT NULL,
    aspath_id integer,
    packet_size smallint NOT NULL,
    error_type smallint,
    error_code smallint,
    hop_rtt integer[] NOT NULL
);


ALTER TABLE public.data_amp_traceroute_6 OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_aspaths (
    aspath_id integer NOT NULL,
    aspath character varying[] NOT NULL,
    aspath_length smallint NOT NULL,
    uniqueas smallint NOT NULL,
    responses smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_aspaths OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths_aspath_id_seq; Type: SEQUENCE; Schema: public; Owner: cuz
--

CREATE SEQUENCE public.data_amp_traceroute_aspaths_aspath_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.data_amp_traceroute_aspaths_aspath_id_seq OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths_aspath_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: cuz
--

ALTER SEQUENCE public.data_amp_traceroute_aspaths_aspath_id_seq OWNED BY public.data_amp_traceroute_aspaths.aspath_id;


--
-- Name: data_amp_traceroute_aspaths_18; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_aspaths_18 (
    aspath_id integer DEFAULT nextval('public.data_amp_traceroute_aspaths_aspath_id_seq'::regclass) NOT NULL,
    aspath character varying[] NOT NULL,
    aspath_length smallint NOT NULL,
    uniqueas smallint NOT NULL,
    responses smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_aspaths_18 OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths_19; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_aspaths_19 (
    aspath_id integer DEFAULT nextval('public.data_amp_traceroute_aspaths_aspath_id_seq'::regclass) NOT NULL,
    aspath character varying[] NOT NULL,
    aspath_length smallint NOT NULL,
    uniqueas smallint NOT NULL,
    responses smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_aspaths_19 OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths_5; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_aspaths_5 (
    aspath_id integer DEFAULT nextval('public.data_amp_traceroute_aspaths_aspath_id_seq'::regclass) NOT NULL,
    aspath character varying[] NOT NULL,
    aspath_length smallint NOT NULL,
    uniqueas smallint NOT NULL,
    responses smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_aspaths_5 OWNER TO cuz;

--
-- Name: data_amp_traceroute_aspaths_6; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_aspaths_6 (
    aspath_id integer DEFAULT nextval('public.data_amp_traceroute_aspaths_aspath_id_seq'::regclass) NOT NULL,
    aspath character varying[] NOT NULL,
    aspath_length smallint NOT NULL,
    uniqueas smallint NOT NULL,
    responses smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_aspaths_6 OWNER TO cuz;

--
-- Name: data_amp_traceroute_pathlen; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_pathlen (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    path_length double precision NOT NULL
);


ALTER TABLE public.data_amp_traceroute_pathlen OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_paths (
    path_id integer NOT NULL,
    path inet[] NOT NULL,
    length smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_paths OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths_path_id_seq; Type: SEQUENCE; Schema: public; Owner: cuz
--

CREATE SEQUENCE public.data_amp_traceroute_paths_path_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.data_amp_traceroute_paths_path_id_seq OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths_path_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: cuz
--

ALTER SEQUENCE public.data_amp_traceroute_paths_path_id_seq OWNED BY public.data_amp_traceroute_paths.path_id;


--
-- Name: data_amp_traceroute_paths_18; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_paths_18 (
    path_id integer DEFAULT nextval('public.data_amp_traceroute_paths_path_id_seq'::regclass) NOT NULL,
    path inet[] NOT NULL,
    length smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_paths_18 OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths_19; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_paths_19 (
    path_id integer DEFAULT nextval('public.data_amp_traceroute_paths_path_id_seq'::regclass) NOT NULL,
    path inet[] NOT NULL,
    length smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_paths_19 OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths_5; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_paths_5 (
    path_id integer DEFAULT nextval('public.data_amp_traceroute_paths_path_id_seq'::regclass) NOT NULL,
    path inet[] NOT NULL,
    length smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_paths_5 OWNER TO cuz;

--
-- Name: data_amp_traceroute_paths_6; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_traceroute_paths_6 (
    path_id integer DEFAULT nextval('public.data_amp_traceroute_paths_path_id_seq'::regclass) NOT NULL,
    path inet[] NOT NULL,
    length smallint NOT NULL
);


ALTER TABLE public.data_amp_traceroute_paths_6 OWNER TO cuz;

--
-- Name: data_amp_udpstream; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_udpstream (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    mean_rtt integer,
    mean_jitter integer,
    min_jitter integer,
    max_jitter integer,
    jitter_percentile_10 integer,
    jitter_percentile_20 integer,
    jitter_percentile_30 integer,
    jitter_percentile_40 integer,
    jitter_percentile_50 integer,
    jitter_percentile_60 integer,
    jitter_percentile_70 integer,
    jitter_percentile_80 integer,
    jitter_percentile_90 integer,
    jitter_percentile_100 integer,
    packets_sent integer NOT NULL,
    packets_recvd integer NOT NULL,
    itu_mos double precision,
    lossrate double precision NOT NULL
);


ALTER TABLE public.data_amp_udpstream OWNER TO cuz;

--
-- Name: data_amp_youtube; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.data_amp_youtube (
    stream_id integer NOT NULL,
    "timestamp" integer NOT NULL,
    total_time integer,
    pre_time integer,
    initial_buffering integer,
    playing_time integer,
    stall_time integer,
    stall_count integer
);


ALTER TABLE public.data_amp_youtube OWNER TO cuz;

--
-- Name: streams_id_seq; Type: SEQUENCE; Schema: public; Owner: cuz
--

CREATE SEQUENCE public.streams_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.streams_id_seq OWNER TO cuz;

--
-- Name: streams_amp_dns; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_dns (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    instance character varying NOT NULL,
    address inet NOT NULL,
    query character varying NOT NULL,
    query_type character varying NOT NULL,
    query_class character varying NOT NULL,
    udp_payload_size integer NOT NULL,
    recurse boolean NOT NULL,
    dnssec boolean NOT NULL,
    nsid boolean NOT NULL
);


ALTER TABLE public.streams_amp_dns OWNER TO cuz;

--
-- Name: streams_amp_external; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_external (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    command character varying NOT NULL
);


ALTER TABLE public.streams_amp_external OWNER TO cuz;

--
-- Name: streams_amp_fastping; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_fastping (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    family character varying NOT NULL,
    packet_size integer NOT NULL,
    packet_rate bigint NOT NULL,
    packet_count bigint NOT NULL,
    preprobe boolean NOT NULL
);


ALTER TABLE public.streams_amp_fastping OWNER TO cuz;

--
-- Name: streams_amp_http; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_http (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    max_connections integer NOT NULL,
    max_connections_per_server smallint NOT NULL,
    max_persistent_connections_per_server smallint NOT NULL,
    pipelining_max_requests smallint NOT NULL,
    persist boolean NOT NULL,
    pipelining boolean NOT NULL,
    caching boolean NOT NULL
);


ALTER TABLE public.streams_amp_http OWNER TO cuz;

--
-- Name: streams_amp_icmp; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_icmp (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    family character varying NOT NULL,
    packet_size character varying NOT NULL
);


ALTER TABLE public.streams_amp_icmp OWNER TO cuz;

--
-- Name: streams_amp_tcpping; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_tcpping (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    port integer NOT NULL,
    family character varying NOT NULL,
    packet_size character varying NOT NULL
);


ALTER TABLE public.streams_amp_tcpping OWNER TO cuz;

--
-- Name: streams_amp_throughput; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_throughput (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    direction character varying NOT NULL,
    address inet NOT NULL,
    duration integer NOT NULL,
    writesize integer NOT NULL,
    tcpreused boolean NOT NULL,
    protocol character varying NOT NULL
);


ALTER TABLE public.streams_amp_throughput OWNER TO cuz;

--
-- Name: streams_amp_traceroute; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_traceroute (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    family character varying NOT NULL,
    packet_size character varying NOT NULL
);


ALTER TABLE public.streams_amp_traceroute OWNER TO cuz;

--
-- Name: streams_amp_udpstream; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_udpstream (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    address inet NOT NULL,
    direction character varying NOT NULL,
    packet_size smallint NOT NULL,
    packet_spacing integer NOT NULL,
    packet_count integer NOT NULL,
    dscp character varying NOT NULL
);


ALTER TABLE public.streams_amp_udpstream OWNER TO cuz;

--
-- Name: streams_amp_youtube; Type: TABLE; Schema: public; Owner: cuz
--

CREATE TABLE public.streams_amp_youtube (
    stream_id integer DEFAULT nextval('public.streams_id_seq'::regclass) NOT NULL,
    source character varying NOT NULL,
    destination character varying NOT NULL,
    quality integer NOT NULL
);


ALTER TABLE public.streams_amp_youtube OWNER TO cuz;

--
-- Name: collections id; Type: DEFAULT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.collections ALTER COLUMN id SET DEFAULT nextval('public.collections_id_seq'::regclass);


--
-- Name: data_amp_traceroute_aspaths aspath_id; Type: DEFAULT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths ALTER COLUMN aspath_id SET DEFAULT nextval('public.data_amp_traceroute_aspaths_aspath_id_seq'::regclass);


--
-- Name: data_amp_traceroute_paths path_id; Type: DEFAULT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths ALTER COLUMN path_id SET DEFAULT nextval('public.data_amp_traceroute_paths_path_id_seq'::regclass);


--
-- Data for Name: collections; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.collections VALUES (1, 'amp', 'icmp', 'streams_amp_icmp', 'data_amp_icmp');
INSERT INTO public.collections VALUES (2, 'amp', 'dns', 'streams_amp_dns', 'data_amp_dns');
INSERT INTO public.collections VALUES (3, 'amp', 'throughput', 'streams_amp_throughput', 'data_amp_throughput');
INSERT INTO public.collections VALUES (4, 'amp', 'tcpping', 'streams_amp_tcpping', 'data_amp_tcpping');
INSERT INTO public.collections VALUES (5, 'amp', 'http', 'streams_amp_http', 'data_amp_http');
INSERT INTO public.collections VALUES (6, 'amp', 'udpstream', 'streams_amp_udpstream', 'data_amp_udpstream');
INSERT INTO public.collections VALUES (7, 'amp', 'traceroute_pathlen', 'streams_amp_traceroute', 'data_amp_traceroute_pathlen');
INSERT INTO public.collections VALUES (8, 'amp', 'astraceroute', 'streams_amp_traceroute', 'data_amp_astraceroute');
INSERT INTO public.collections VALUES (9, 'amp', 'traceroute', 'streams_amp_traceroute', 'data_amp_traceroute');
INSERT INTO public.collections VALUES (10, 'amp', 'youtube', 'streams_amp_youtube', 'data_amp_youtube');
INSERT INTO public.collections VALUES (11, 'amp', 'fastping', 'streams_amp_fastping', 'data_amp_fastping');
INSERT INTO public.collections VALUES (12, 'amp', 'external', 'streams_amp_external', 'data_amp_external');


--
-- Data for Name: data_amp_astraceroute; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_astraceroute_18; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_astraceroute_19; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_astraceroute_5; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_astraceroute_6; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_dns; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_external; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_fastping; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_http; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_icmp; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_tcpping; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_throughput; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_18; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564373430, 40, 1, 60, NULL, NULL, '{31,200,146,337,513,2341,3690,2989,NULL,4254,7724,6246,6851,130456,129620,190165,196003,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564711230, 42, 3, 60, NULL, NULL, '{34,230,423,351,695,4199,4883,9491,8835,130710,130656,184809,185569,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564712430, 44, 5, 60, NULL, NULL, '{29,270,285,472,837,2263,3548,8247,5935,129893,129337,184112,196318,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564713030, 46, 6, 60, NULL, NULL, '{18,163,148,277,589,2312,3051,5093,7301,129723,151778,183684,193425,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564718430, 48, 5, 60, NULL, NULL, '{15,3007,367,1974,1610,3542,4422,5992,6171,132617,129569,184960,196259,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564719030, 50, 6, 60, NULL, NULL, '{15,160,205,351,658,2536,3116,6394,4480,131239,129314,183782,192402,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564719630, 52, 5, 60, NULL, NULL, '{24,12412,170,307,504,2519,5530,5430,6665,130321,129106,190001,199780,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564720230, 54, 8, 60, NULL, NULL, '{42,394,326,540,743,2524,3084,5813,6542,130847,129093,190188,195795,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564720830, 52, 5, 60, NULL, NULL, '{29,271,200,474,413,2511,3120,7556,6300,130261,180615,183969,190395,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564721430, 57, 6, 60, NULL, NULL, '{35,4195,246,372,689,2494,3266,4974,6935,130786,129196,190120,202417,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564722030, 50, 6, 60, NULL, NULL, '{43,573,356,1852,13277,3736,4450,6845,6989,132382,130494,184827,193455,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564722630, 61, 3, 60, NULL, NULL, '{47,203,374,507,999,2687,3276,7167,6483,130068,181439,190407,195294,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564723230, 63, 8, 60, NULL, NULL, '{33,250,193,308,503,2489,3172,6235,7712,131780,130125,184621,189817,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564723830, 61, 3, 60, NULL, NULL, '{39,269,153,417,749,2723,3323,4901,6095,131262,129670,190475,194449,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564724430, 50, 6, 60, NULL, NULL, '{36,349,295,490,593,2311,3129,5118,7455,130294,129565,184320,196785,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564725030, 50, 6, 60, NULL, NULL, '{40,462394,196,411,499,2507,9437,7797,4415,130723,129187,183879,195145,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564725630, 65, 3, 60, NULL, NULL, '{13,158,187,1749,2177,4244,5060,8330,5197,132704,130641,185585,293463,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564726230, 50, 6, 60, NULL, NULL, '{44,202,156,306,493,2426,3327,5940,7391,130934,129193,189989,199479,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564726830, 67, 6, 60, NULL, NULL, '{48,1680,278,391,685,2322,3354,4634,5579,130283,129476,184087,207123,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564727430, 44, 5, 60, NULL, NULL, '{43,840,175,374,691,2388,3241,6915,7773,130011,129199,190353,245850,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564728030, 57, 6, 60, NULL, NULL, '{50,350,246,509,804,11070,8958,5837,4107,130134,129141,184164,190466,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564728630, 69, 3, 60, NULL, NULL, '{33,303,301,485,690,2472,3271,5484,6764,131083,129296,190808,197958,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564729230, 63, 8, 60, NULL, NULL, '{40,280,284,1765,2239,4436,5012,6878,6805,132944,130835,185935,190941,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564729830, 44, 5, 60, NULL, NULL, '{52,141214,239,434,717,2521,3494,5583,6969,129636,129503,183869,193012,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564730430, 50, 6, 60, NULL, NULL, '{45,504,372,480,653,2635,3054,5273,5987,129545,129161,184103,266246,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564731030, 63, 8, 60, NULL, NULL, '{12,149,137,278,583,2539,3420,6764,7441,131143,129125,183756,189870,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564731630, 67, 6, 60, NULL, NULL, '{45,3991,189,317,559,2281,3040,9364,6419,130251,129758,184007,189827,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564732230, 71, 8, 60, NULL, NULL, '{45,491,311,601,908,2393,3518,6486,4093,130841,129840,188454,189504,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564732830, 42, 3, 60, NULL, NULL, '{44,533,332,480,841,3828,26355,6361,6112,132065,130642,192169,207957,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564733430, 48, 5, 60, NULL, NULL, '{38,318,281,504,632,2611,3300,6437,3980,131448,129503,184262,226268,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564734030, 42, 3, 60, NULL, NULL, '{39,420,301,523,463,2404,3091,4265,7340,130983,129893,184107,186225,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564734630, 72, 5, 60, NULL, NULL, '{37,486,317,654,628,2360,3299,8161,7673,130598,129169,184243,190791,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564735230, 73, 8, 60, NULL, NULL, '{25,319,165,351,9869,2539,3381,5604,4538,131169,129346,190418,195823,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564735830, 67, 6, 60, NULL, NULL, '{18,230,190,320,686,2527,3106,10791,7030,129883,129170,183784,196784,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564736430, 73, 8, 60, NULL, NULL, '{40,504,239,1682,1817,3426,5197,7902,8310,132764,130478,185200,191523,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564737030, 73, 8, 60, NULL, NULL, '{42,6173,172,305,447,2316,3003,5649,5934,129676,129254,184163,189655,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564737630, 52, 5, 60, NULL, NULL, '{41,400,313,494,887,15289,4775,4812,4063,130698,129230,189107,190748,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564738230, 65, 3, 60, NULL, NULL, '{31,453,188,413,813,2533,3390,7022,5439,131560,138163,184012,185085,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564738830, 73, 8, 60, NULL, NULL, '{36,297,333,510,828,3140,3220,5286,4719,130573,130053,190263,195848,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564739430, 54, 8, 60, NULL, NULL, '{31,221,191,405,637,2543,3351,7073,6061,130545,129350,184287,189738,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564740030, 57, 6, 60, NULL, NULL, '{34,416,238,2032,9255,4129,5036,8723,7226,133849,130678,185684,191827,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564740630, 57, 6, 60, NULL, NULL, '{35,362,266,637,1397,2648,3518,6207,5037,131002,129559,190712,198054,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564741230, 63, 8, 60, NULL, NULL, '{39,14950,199,283,673,2554,3013,5186,6234,131279,130470,190068,196072,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564741830, 57, 6, 60, NULL, NULL, '{14,7082,147,269,635,2353,9784,6903,3978,130476,129621,190371,199276,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564742430, 52, 5, 60, NULL, NULL, '{31,224,184,347,479,2392,3073,5242,3872,131202,129374,196503,200155,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564743030, 42, 3, 60, NULL, NULL, '{44,50709,200,370,613,2386,3169,5740,7890,131104,129164,184057,200294,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564743630, 65, 3, 60, NULL, NULL, '{14,185,159,295,470,2413,4187,8258,6115,132203,130924,185874,186059,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564744230, 54, 8, 60, NULL, NULL, '{41,375,500,353,1161,2425,3269,5095,4719,132708,129470,183724,189422,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564744830, 42, 3, 60, NULL, NULL, '{16,382,157,339,526,2471,3613,6539,6739,130898,129466,184142,187454,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564745430, 73, 8, 60, NULL, NULL, '{35,222,203,334,490,2463,2976,9809,5301,131208,129667,183676,189672,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564746030, 69, 3, 60, NULL, NULL, '{42,8603,240,402,428,2585,3149,4969,6589,130813,129611,190201,197589,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564746630, 50, 6, 60, NULL, NULL, '{34,479,287,507,678,2685,3201,4980,6278,129977,129517,190391,203892,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564747230, 61, 3, 60, NULL, NULL, '{43,1044,491,623,1770,4413,5064,6196,7826,132264,130836,191995,192747,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564747830, 52, 5, 60, NULL, NULL, '{32,48839,201,503,844,2701,3168,7152,7176,130372,129889,190184,197536,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564748430, 57, 6, 60, NULL, NULL, '{36,23221,290,529,955,2456,3114,6364,4079,131467,129877,183858,196971,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564749030, 73, 8, 60, NULL, NULL, '{34,333,280,438,784,2490,3376,6915,6669,130509,129614,188635,189697,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564749630, 46, 6, 60, NULL, NULL, '{38,437,232,314,1911,2651,3100,5791,6671,131172,129419,183674,193648,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564750230, 44, 5, 60, NULL, NULL, '{40,235,244,369,430,2465,3183,5586,4118,131114,129489,190097,201204,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564750830, 69, 3, 60, NULL, NULL, '{36,384,233,534,1663,3794,4109,6522,5689,131221,130935,191848,192748,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564751430, 52, 5, 60, NULL, NULL, '{52,7161,344,642,843,2566,3366,5936,5993,130772,129573,190331,233911,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564752030, 54, 8, 60, NULL, NULL, '{42,397,312,549,721,2486,3248,7610,4194,131303,129515,183991,189812,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564752630, 54, 8, 60, NULL, NULL, '{53,286,182,287,475,6147,3356,5792,6308,131009,129837,183661,189755,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564753230, 65, 3, 60, NULL, NULL, '{19,278,243,435,512,2758,3232,6470,5997,131165,129433,184945,185371,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564753830, 54, 8, 60, NULL, NULL, '{42,508,334,602,849,2804,3169,6635,7448,130990,129236,190416,196038,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564754430, 65, 3, 60, NULL, NULL, '{46,351,269,1911,2137,3814,4407,8295,6376,131590,130154,191506,193257,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564755030, 48, 5, 60, NULL, NULL, '{54,1315,291,534,688,2581,3345,6555,5020,130189,129579,183875,195456,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564755630, 61, 3, 60, NULL, NULL, '{37,383,323,368,546,2415,3214,6087,7213,129599,129473,184377,186257,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564756230, 44, 5, 60, NULL, NULL, '{49,2396,327,520,785,2571,3159,6779,7037,129781,129824,190552,201039,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564756830, 42, 3, 60, NULL, NULL, '{29,256,276,375,622,2292,3315,4810,4047,130306,129519,190576,192695,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564757430, 50, 6, 60, NULL, NULL, '{29,381,320,353,480,2524,3066,4377,4920,130757,129539,191265,201882,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564758030, 72, 5, 60, NULL, NULL, '{11,155,160,272,578,4213,5041,6752,8363,132743,131089,191873,204695,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564758630, 61, 3, 60, NULL, NULL, '{44,257,199,344,583,2411,3150,6124,6796,131384,130463,184166,183727,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564759230, 46, 6, 60, NULL, NULL, '{29,1609,190,349,446,12800,3248,6050,4869,130137,129389,183948,189875,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564759830, 42, 3, 60, NULL, NULL, '{20,172,141,405,738,2390,3366,5883,5752,131281,129655,184158,186686,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564760430, 48, 5, 60, NULL, NULL, '{34,355,192,276,3971,2297,3154,10410,5541,131459,129978,190154,202792,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564761030, 67, 6, 60, NULL, NULL, '{39,215,215,341,3609,2682,3416,7375,4728,130662,129310,184217,191624,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564761630, 63, 8, 60, NULL, NULL, '{36,538,343,1786,2240,5162,5020,8105,6422,132793,130810,192086,197949,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564762230, 65, 3, 60, NULL, NULL, '{38,183,184,358,908,2463,3082,6104,7194,131536,129797,190479,195221,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564762830, 71, 8, 60, NULL, NULL, '{36,285,259,438,764,2810,3424,4994,7332,130953,129803,183904,189914,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564763430, 54, 8, 60, NULL, NULL, '{13,160,223,936,686,2305,8082,6673,4470,130504,129508,184197,189761,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564764030, 71, 8, 60, NULL, NULL, '{38,2423,201,310,470,2436,3319,5037,5877,131323,129201,183775,189813,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564764630, 69, 3, 60, NULL, NULL, '{48,336,170,395,592,2438,3345,7834,6274,131149,129470,190252,201816,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564765230, 54, 8, 60, NULL, NULL, '{40,50252,273,2102,2583,4404,4986,8289,8743,132217,130830,192387,198035,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564765830, 72, 5, 60, NULL, NULL, '{43,543,260,395,511,2467,3559,6026,6683,131144,130599,184076,197170,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564766430, 52, 5, 60, NULL, NULL, '{38,385,297,541,674,2548,3221,5904,7231,130726,129399,183824,191082,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564767030, 71, 8, 60, NULL, NULL, '{48,346,271,403,704,2762,3329,5355,6287,131358,153312,183960,189863,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564767630, 42, 3, 60, NULL, NULL, '{30,213,364,510,923,2800,3145,6974,5084,131028,129111,184077,228501,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564768230, 52, 5, 60, NULL, NULL, '{44,1601,306,595,1059,2736,4146,8239,5694,131199,129152,183871,190490,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564768830, 42, 3, 60, NULL, NULL, '{35,245,199,542,1791,4494,4990,6597,7287,131364,131305,193698,193418,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564769430, 57, 6, 60, NULL, NULL, '{34,382,286,550,683,2685,3249,6030,7890,131579,129418,184235,208967,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564770030, 65, 3, 60, NULL, NULL, '{28,341,275,507,656,2674,3372,4650,4816,129932,129586,190485,194184,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564770630, 69, 3, 60, NULL, NULL, '{16,187,164,315,673,2345,3207,5514,4463,131354,130125,186387,186631,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564771230, 54, 8, 60, NULL, NULL, '{31,343,271,480,619,2567,4094,6679,6072,130793,129175,184625,190159,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564771830, 72, 5, 60, NULL, NULL, '{19,398,180,362,659,2347,3214,4994,3894,130318,130109,183950,195458,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564772430, 69, 3, 60, NULL, NULL, '{44,482,257,932,1485,3123,3837,7347,6231,131745,131609,185506,190044,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564773030, 63, 8, 60, NULL, NULL, '{28,310,309,396,5523,2604,3312,7247,5209,131614,129153,190220,196147,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564773630, 44, 5, 60, NULL, NULL, '{33,218,169,320,660,2658,3360,6162,5055,129961,129348,191180,202257,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564774230, 42, 3, 60, NULL, NULL, '{34,375,343,364,2465,2423,3238,5869,4333,131035,129517,190606,193349,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564774830, 65, 3, 60, NULL, NULL, '{39,341,265,298,538,2910,3184,7804,7700,130186,129238,184136,189242,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564775430, 42, 3, 60, NULL, NULL, '{38,407,307,472,936,2510,3250,5350,5450,129568,129044,184287,184366,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564776030, 63, 8, 60, NULL, NULL, '{44,266,288,1418,1687,3929,4883,6828,6574,131874,130683,185621,191221,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564776630, 61, 3, 60, NULL, NULL, '{45,2741,314,360,964,2525,3282,5368,6116,130913,129662,184330,190030,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564777230, 57, 6, 60, NULL, NULL, '{48,282,208,382,4044,2592,3421,4756,6955,130647,129644,199103,203765,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564777830, 63, 8, 60, NULL, NULL, '{45,1042,305,497,913,2424,3454,7136,4945,131088,168806,190442,196203,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564778430, 72, 5, 60, NULL, NULL, '{16,329,305,385,430,2379,3190,5919,7020,130028,129376,183831,190751,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564779030, 72, 5, 60, NULL, NULL, '{44,19672,199,457,693,2853,3405,5506,4054,131293,129441,190332,200295,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564779630, 63, 8, 60, NULL, NULL, '{35,298,266,430,2116,13399,4762,8927,8465,132011,131481,185279,190980,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564780230, 63, 8, 60, NULL, NULL, '{34,3491,201,414,694,2495,3192,6123,6305,131291,129491,183995,189819,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564780830, 72, 5, 60, NULL, NULL, '{40,461,234,370,5478,2310,3277,5107,6534,131261,129503,184130,190704,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564781430, 46, 6, 60, NULL, NULL, '{35,541,322,601,695,2365,3139,5209,7105,131361,129546,190174,197655,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564782030, 73, 8, 60, NULL, NULL, '{29,387,229,422,687,2536,3243,6276,7593,130817,129473,184237,189632,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564782630, 46, 6, 60, NULL, NULL, '{37,237,250,296,486,2389,3008,5084,4810,131561,129120,190406,198320,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564783230, 61, 3, 60, NULL, NULL, '{42,374,307,1586,1685,3881,4437,6865,7955,132923,130908,192423,192482,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564783830, 69, 3, 60, NULL, NULL, '{40,683,198,386,705,2758,3371,6466,4417,130575,129520,184236,184136,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564784430, 63, 8, 60, NULL, NULL, '{36,338,170,391,559,2735,3168,5020,6681,130755,130258,190293,195766,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564785030, 57, 6, 60, NULL, NULL, '{36,483,299,576,760,2524,3350,6780,5092,131195,129255,190527,293282,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564785630, 52, 5, 60, NULL, NULL, '{27,451,247,366,550,2602,3523,7249,4283,130025,129287,184180,193145,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564786230, 52, 5, 60, NULL, NULL, '{43,192,182,291,741,2698,3227,6043,6822,130549,129581,190483,198344,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564786830, 46, 6, 60, NULL, NULL, '{19,188,265,1742,1745,4364,4769,6134,8163,131562,130486,191926,200648,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564787430, 57, 6, 60, NULL, NULL, '{52,352,249,358,506,2376,3126,5565,6495,131023,129287,185628,198020,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564788030, 50, 6, 60, NULL, NULL, '{44,3520,309,498,783,2429,3531,6363,6935,131204,129602,190241,204103,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564788630, 61, 3, 60, NULL, NULL, '{38,344691,215,395,715,2386,3175,5126,4690,131246,129392,184053,225198,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564789230, 67, 6, 60, NULL, NULL, '{41,404,279,419,806,2719,3283,5623,6990,131489,129751,184054,195159,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564789830, 67, 6, 60, NULL, NULL, '{29,255,224,289,509,2696,3244,5836,5545,130318,129567,184270,195408,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564790430, 72, 5, 60, NULL, NULL, '{54,12011,171,501,618,2789,4786,6696,7897,131409,131780,185389,231099,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564791030, 57, 6, 60, NULL, NULL, '{44,335,311,514,692,2826,3303,6516,5911,130972,129399,190583,198583,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564791630, 65, 3, 60, NULL, NULL, '{42,385,343,493,822,4635,30798,5802,6089,130275,129058,190429,194040,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564792230, 69, 3, 60, NULL, NULL, '{43,460,218,408,908,2536,3229,6767,5772,130611,129584,190061,196814,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564792830, 73, 8, 60, NULL, NULL, '{21,2663,236,303,519,2333,3185,6002,7530,131177,129805,184058,189577,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564793430, 61, 3, 60, NULL, NULL, '{45,10745,526,415,738,2458,3276,4453,6483,131259,129774,190323,194573,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564794030, 52, 5, 60, NULL, NULL, '{17,189,129,300,2773,4146,22319,5702,5227,130588,131180,191869,198941,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564794630, 72, 5, 60, NULL, NULL, '{35,401,369,595,649,2615,3565,5073,5819,130521,170405,183884,193069,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564795230, 65, 3, 60, NULL, NULL, '{32,73113,170,527,766,2296,3224,6601,7670,131054,129349,184176,185001,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564795830, 63, 8, 60, NULL, NULL, '{29,305,314,383,2471,2217,3014,5117,6804,129879,129125,183871,189367,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564796430, 44, 5, 60, NULL, NULL, '{32,198,167,311,487,2356,3461,7737,7706,129875,129492,190478,196960,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564797030, 63, 8, 60, NULL, NULL, '{67,366,238,513,823,2727,3358,5639,7016,130565,130200,190649,195944,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564797630, 69, 3, 60, NULL, NULL, '{40,385,296,453,2136,3731,4883,6840,6890,132957,131052,191656,198352,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564798230, 65, 3, 60, NULL, NULL, '{50,38631,259,603,798,2710,3603,5227,4322,130210,129603,189118,188638,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564798830, 69, 3, 60, NULL, NULL, '{45,374,324,395,490,2340,37066,6970,6418,129695,129805,183885,189513,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564799430, 72, 5, 60, NULL, NULL, '{33,227,226,414,467,2531,3042,7144,3779,130619,129523,184105,193321,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564800030, 72, 5, 60, NULL, NULL, '{33,222,296,457,573,2283,3378,5767,5603,131541,129309,184129,198750,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564800630, 57, 6, 60, NULL, NULL, '{51,11144,218,325,546,2457,3000,7730,5797,129626,129999,184126,190223,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564801230, 44, 5, 60, NULL, NULL, '{22,29548,343,619,684,2718,17495,8838,6437,131234,130979,185562,193225,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564801830, 63, 8, 60, NULL, NULL, '{29,296,266,418,12628,2778,3243,6083,4358,130662,129381,184178,189811,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564802430, 57, 6, 60, NULL, NULL, '{41,543,329,501,766,2651,3624,7615,5087,130086,129543,184249,194752,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564803030, 67, 6, 60, NULL, NULL, '{40,396,381,601,788,2902,3476,7306,6671,130582,130024,190066,198414,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564803630, 61, 3, 60, NULL, NULL, '{36,258,215,273,526,2402,3212,5411,4934,131220,129379,190034,193972,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564804230, 52, 5, 60, NULL, NULL, '{54,3051,295,511,618,2602,3320,7474,3783,129763,129352,184175,194114,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564804830, 46, 6, 60, NULL, NULL, '{39,596,346,2116,2075,5681,4890,6049,8327,132609,130336,185358,197241,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564805430, 61, 3, 60, NULL, NULL, '{35,1869,256,376,440,2307,3183,7095,7000,131429,129447,190285,239260,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564806030, 52, 5, 60, NULL, NULL, '{21,255,221,449,618,2734,3377,5075,6495,129912,130648,190496,202025,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564806630, 72, 5, 60, NULL, NULL, '{21,166,169,287,12686,2302,3090,5266,6623,129217,130792,184150,193602,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564807230, 42, 3, 60, NULL, NULL, '{40,320,270,451,503,2469,3729,5620,7138,130301,129956,190137,194722,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564807830, 42, 3, 60, NULL, NULL, '{58,379,249,401,621,3222,3513,7694,6386,129820,129448,184287,184099,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564808430, 71, 8, 60, NULL, NULL, '{43,420,403,630,1203,4076,4948,8291,6668,132190,130355,191956,197777,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564809030, 54, 8, 60, NULL, NULL, '{20,288,182,402,535,2304,22773,6538,7243,129772,129613,184374,189399,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564809630, 46, 6, 60, NULL, NULL, '{51,400,251,298,522,2418,11744,5271,6880,129304,129757,190448,200594,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564810230, 52, 5, 60, NULL, NULL, '{34,923,191,424,589,2550,3064,5129,7242,130402,129359,184206,191004,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564810830, 54, 8, 60, NULL, NULL, '{45,2009,218,362,760,2538,3132,12166,6106,131026,163127,190332,196062,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564811430, 72, 5, 60, NULL, NULL, '{20,1383,256,303,604,2269,3307,7145,5050,136308,129650,190484,200665,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564812030, 72, 5, 60, NULL, NULL, '{29,231,257,2196,2692,14395,6470,8136,8743,131906,130284,185762,213813,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564812630, 54, 8, 60, NULL, NULL, '{38,265,217,463,7938,2665,3200,7259,6941,130517,129695,184113,189468,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564813230, 72, 5, 60, NULL, NULL, '{35,1529,269,508,10724,2429,3138,8196,5654,131341,129291,184072,226991,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564813830, 48, 5, 60, NULL, NULL, '{29,300,153,304,471,2312,3158,4656,7497,130584,129153,190592,196728,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564814430, 48, 5, 60, NULL, NULL, '{14,275,158,282,547,2408,3224,5759,5398,129983,129163,190478,197086,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564815030, 44, 5, 60, NULL, NULL, '{37,296,271,389,562,2600,3150,7433,5040,129582,129776,183972,192104,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564815630, 69, 3, 60, NULL, NULL, '{53,711,271,1603,1638,3345,4533,7688,5328,137357,131711,185081,186477,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564816230, 63, 8, 60, NULL, NULL, '{52,52297,257,606,815,2443,3018,5347,4043,130254,129789,184289,189784,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564816830, 52, 5, 60, NULL, NULL, '{53,539,350,607,5775,6150,3204,5461,6800,130741,129301,190454,199351,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564817430, 72, 5, 60, NULL, NULL, '{29,319,256,397,740,2586,3227,7470,6531,130092,129395,184224,197030,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564818030, 73, 8, 60, NULL, NULL, '{43,421,305,488,730,2429,3169,6836,5360,131118,129284,184063,189704,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564818630, 52, 5, 60, NULL, NULL, '{39,342,265,383,634,16060,3224,8534,6728,131047,129472,184175,193138,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564819230, 79, 9, 60, NULL, NULL, '{35,299,240,2051,2552,3961,5103,10830,8544,131347,132894,143486,143646,143199,148255,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564819830, 86, 9, 60, NULL, NULL, '{11,160,153,275,424,2298,3254,4946,6083,129841,131249,143118,142004,147247,142443,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564820430, 90, 9, 60, NULL, NULL, '{39,595,282,471,535,2619,3038,6611,4786,131043,129839,142201,142923,142323,146712,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564821030, 86, 9, 60, NULL, NULL, '{14,344,199,419,894,2507,3043,5759,4893,130149,130670,143402,141762,142651,144784,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564821630, 86, 9, 60, NULL, NULL, '{22,311,222,446,879,2523,3129,6399,4530,130260,130061,143436,141620,142622,148459,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564822230, 79, 9, 60, NULL, NULL, '{41,494,268,438,572,2453,3304,6688,5388,131183,130408,142276,141487,142694,145320,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564822830, 90, 9, 60, NULL, NULL, '{39,330,282,401,601,4092,4979,7362,5562,132073,132730,145400,143029,144344,149737,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564823430, 86, 9, 60, NULL, NULL, '{45,36846,311,650,677,2413,8662,5281,7317,130890,131566,143378,142422,142733,156572,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564824030, 97, 9, 60, NULL, NULL, '{45,670,314,606,987,2292,3069,4542,6880,130514,130268,143813,151795,142655,143069,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564824630, 90, 9, 60, NULL, NULL, '{39,413,248,320,510,2312,3163,6372,7258,130606,129684,142157,143194,142681,145150,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564825230, 97, 9, 60, NULL, NULL, '{14,172,126,252,422,2265,3069,6790,4261,129526,130760,143276,142253,142844,154153,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564825830, 86, 9, 60, NULL, NULL, '{50,337,247,393,443,2283,3147,5777,6813,131356,129767,142550,141598,142337,143443,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564826430, 97, 9, 60, NULL, NULL, '{43,391,308,440,932,4338,5080,8269,8911,132417,131842,144442,143402,144255,153982,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564827030, 90, 9, 60, NULL, NULL, '{45,497,323,606,1035,3775,3344,6159,3610,130729,129665,142775,141498,142782,143824,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564827630, 79, 9, 60, NULL, NULL, '{41,437,260,499,623,2302,3392,4499,6796,129663,131261,141826,141788,142367,143226,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564828230, 97, 9, 60, NULL, NULL, '{44,336,239,398,595,2539,3414,6961,5259,129223,131480,141690,141759,142721,146980,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564828830, 97, 9, 60, NULL, NULL, '{34,297,267,489,682,2319,4103,5476,4769,131037,130669,142989,141865,142537,148353,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564829430, 79, 9, 60, NULL, NULL, '{11,143,140,329,468,2658,3373,6009,7040,129752,131336,143817,141671,142727,148675,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564830030, 90, 9, 60, NULL, NULL, '{45,494,363,1578,2171,3840,4714,7162,6750,133239,130629,145258,142942,144433,150869,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564830630, 90, 9, 60, NULL, NULL, '{41,5673,198,430,752,2431,3309,5619,5995,131500,130541,141962,141471,142647,218166,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564831230, 90, 9, 60, NULL, NULL, '{23,385,296,349,681,2250,15853,5657,4052,131291,130262,142859,141689,142842,147947,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564831830, 97, 9, 60, NULL, NULL, '{16,174,219,485,890,2728,3308,5446,5461,130655,131131,142083,141732,142652,189823,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564832430, 86, 9, 60, NULL, NULL, '{44,349,277,1015,697,2419,3342,6986,5527,130701,129281,142281,142904,142456,144035,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564833030, 79, 9, 60, NULL, NULL, '{16,181,158,434,574,2570,3196,8213,3761,130569,129453,143833,142957,142707,148443,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564833630, 97, 9, 60, NULL, NULL, '{44,26881,215,518,2163,3467,4448,6203,7334,131061,131629,144640,145086,144230,147080,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564834230, 90, 9, 60, NULL, NULL, '{24,282,263,379,595,2565,3478,5666,6231,131149,130154,142346,143311,142433,149755,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564834830, 97, 9, 60, NULL, NULL, '{45,50564,272,393,688,2359,3246,4789,6963,130777,130592,142581,142248,142934,144809,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564835430, 90, 9, 60, NULL, NULL, '{31,563,292,1225,714,16331,3426,5814,3913,130380,130835,143324,142208,149218,145778,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564836030, 86, 9, 60, NULL, NULL, '{45,384,312,603,832,2786,3459,6171,7516,131599,131492,143065,141655,142773,142840,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564836630, 97, 9, 60, NULL, NULL, '{46,350,420,635,654,2426,3242,6769,4023,131413,130741,143836,145350,150126,147226,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564837230, 79, 9, 60, NULL, NULL, '{34,336,369,3004,2154,4344,4894,6575,7695,132178,131240,143390,144230,144037,151790,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564837830, 79, 9, 60, NULL, NULL, '{44,356,308,599,1163,2736,8814,5730,7281,130315,131244,143466,141702,142750,145215,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564838430, 86, 9, 60, NULL, NULL, '{21,1128,911,306,668,2463,3431,6404,4674,130431,131062,142636,141852,142466,142367,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564839030, 86, 9, 60, NULL, NULL, '{41,73421,355,393,698,2524,3094,5908,5595,131264,131401,143354,141785,146140,148293,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564839630, 97, 9, 60, NULL, NULL, '{38,402,447,357,586,2871,3333,6824,3947,129631,131114,142244,141720,142716,145105,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564840230, 97, 9, 60, NULL, NULL, '{44,342764,295,375,385,2286,3272,4976,5800,129795,129467,142611,212984,142739,156726,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564840830, 90, 9, 60, NULL, NULL, '{41,398,313,501,724,3722,4578,6775,6812,132677,131681,144787,145100,144370,147077,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564841430, 86, 9, 60, NULL, NULL, '{13,193,226,336,1005,10010,3190,7108,5627,129316,130538,143537,141675,142911,149003,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564842030, 86, 9, 60, NULL, NULL, '{44,503,328,600,737,2861,3161,7367,4689,129789,129909,143211,141945,142302,167803,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564842630, 86, 9, 60, NULL, NULL, '{36,1059,204,375,486,2258,3300,7259,6616,131725,130122,143413,141661,142482,149723,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564843230, 86, 9, 60, NULL, NULL, '{47,438,224,310,650,2332,3651,6113,6858,130765,130249,143238,141984,142530,153557,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564843830, 79, 9, 60, NULL, NULL, '{45,509,282,462,7336,3367,3272,7444,4617,130096,129523,142801,142007,142373,199637,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564844430, 90, 9, 60, NULL, NULL, '{39,470,278,1352,19468,3855,4465,6774,7081,130814,131329,144563,145135,145078,151037,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564845030, 79, 9, 60, NULL, NULL, '{49,504,326,616,936,2698,3230,7216,5519,130303,131245,142847,141889,142504,148367,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564845630, 79, 9, 60, NULL, NULL, '{42,7957,326,519,691,35571,3134,10745,4278,131090,130621,142921,142055,142684,148978,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564846230, 79, 9, 60, NULL, NULL, '{45,1883,308,621,10034,2494,3289,5275,5002,131059,129524,142173,149379,142601,146041,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564846830, 97, 9, 60, NULL, NULL, '{45,2225,368,402,565,2784,3382,6961,4123,130776,131025,142559,142058,142974,144980,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564847430, 90, 9, 60, NULL, NULL, '{52,242,208,263,563,2257,3214,6021,4812,130583,131018,143238,143309,142829,145687,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564848030, 79, 9, 60, NULL, NULL, '{17,156,130,265,434,2293,4435,8071,6401,132570,130783,143794,143479,144018,143167,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564848630, 90, 9, 60, NULL, NULL, '{41,647,388,514,743,2858,3318,4206,5896,129665,129450,142641,141966,142712,160234,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564849230, 86, 9, 60, NULL, NULL, '{41,159975,261,353,671,3083,3391,6122,4624,130295,129388,143790,142056,143181,145786,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564849830, 86, 9, 60, NULL, NULL, '{39,416,364,446,536,2544,3524,6721,3918,129519,129380,143709,173165,142797,147709,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564850430, 86, 9, 60, NULL, NULL, '{14,210,196,375,532,2476,3798,5964,5324,129945,131025,143912,141577,142617,200387,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564851030, 90, 9, 60, NULL, NULL, '{36,562,262,505,706,2262,3774,6872,4923,130769,129716,143785,141463,144711,148889,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564851630, 86, 9, 60, NULL, NULL, '{36,325,240,2031,2036,4517,5012,6471,6058,131839,131610,144244,143221,144235,183650,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564852230, 97, 9, 60, NULL, NULL, '{38,250,358,330,761,12690,3359,5546,6084,131192,129617,142605,141701,142711,145467,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564852830, 97, 9, 60, NULL, NULL, '{33,1881,180,325,537,2305,3332,7175,4209,129360,129828,142374,142137,142405,143009,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564853430, 86, 9, 60, NULL, NULL, '{35,389,247,366,522,2546,3083,7679,6352,130491,130091,143535,141679,142580,149270,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564854030, 86, 9, 60, NULL, NULL, '{40,503,243,480,477,2186,3066,4969,7456,129574,130583,143645,141717,142611,148089,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564854630, 90, 9, 60, NULL, NULL, '{39,408,317,634,464,2280,3531,7307,5370,130764,130262,142302,141767,145859,188096,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564855230, 86, 9, 60, NULL, NULL, '{39,474,356,1681,2286,4075,4691,6708,6917,132059,131563,144309,143688,143993,151760,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564855830, 79, 9, 60, NULL, NULL, '{29,215,223,323,424,2684,3144,5316,6579,131340,131152,142715,141803,142452,148474,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564856430, 97, 9, 60, NULL, NULL, '{44,395,307,434,529,2799,4742,4983,6380,129833,130461,143768,141966,142447,143404,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564857030, 90, 9, 60, NULL, NULL, '{43,579,311,626,617,2621,3268,8730,7636,130676,129288,143917,141994,142664,149189,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564857630, 79, 9, 60, NULL, NULL, '{46,345,324,615,566,2452,3000,7594,3914,130330,131395,142075,141839,142368,149839,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564858230, 90, 9, 60, NULL, NULL, '{38,312,228,444,935,2424,3277,7559,6777,130815,130807,143575,141803,142710,150365,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564858830, 90, 9, 60, NULL, NULL, '{29,296,227,481,768,17183,4456,6827,6892,132541,132793,145021,143290,143731,147072,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564859430, 90, 9, 60, NULL, NULL, '{51,234,204,509,666,2411,3219,10035,5061,130309,129889,142038,142048,142454,149472,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564860030, 86, 9, 60, NULL, NULL, '{35,335,224,340,761,2861,3508,4971,5754,131544,129686,143966,143328,161097,145003,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564860630, 86, 9, 60, NULL, NULL, '{44,525,335,640,668,2412,3074,6145,6132,130620,130734,144055,141940,142755,143173,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564861230, 79, 9, 60, NULL, NULL, '{40,405,312,618,629,2515,3146,5376,6046,129686,130302,142122,141638,142425,145575,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564861830, 79, 9, 60, NULL, NULL, '{39,8163,291,587,1007,2593,3287,5483,6625,130821,130690,143199,141559,142728,165533,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564862430, 79, 9, 60, NULL, NULL, '{39,428,359,1725,8284,3929,5021,8275,7188,131931,133101,143908,144745,145208,146931,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564863030, 90, 9, 60, NULL, NULL, '{39,243,219,312,568,2315,3373,5677,5675,131122,131285,142333,141710,142726,145755,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564863630, 97, 9, 60, NULL, NULL, '{41,4227,378,448,719,2498,3277,5454,6992,130290,130326,142806,141503,142630,178203,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564864230, 79, 9, 60, NULL, NULL, '{56,365,340,612,1074,2693,3296,6395,4551,130278,129975,143626,141932,142559,142832,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564864830, 79, 9, 60, NULL, NULL, '{39,920,295,429,623,2625,3549,5582,5802,131513,130103,143082,141838,151001,149590,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564865430, 90, 9, 60, NULL, NULL, '{51,415,311,590,799,2565,3302,4685,4583,129787,129779,143889,141771,142642,223788,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564866030, 79, 9, 60, NULL, NULL, '{44,313,339,498,777,2648,4778,8684,6252,131344,131160,143282,144525,144235,151904,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564866630, 79, 9, 60, NULL, NULL, '{45,498,306,583,776,2837,3162,6510,4158,130615,131146,143226,141814,142758,212614,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564867230, 79, 9, 60, NULL, NULL, '{41,500,356,365,605,2443,3263,11254,6345,131217,131353,143603,141933,142430,144519,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564867830, 90, 9, 60, NULL, NULL, '{44,375,240,403,745,2378,3213,7116,7384,130144,131453,142858,141874,142710,149245,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564868430, 97, 9, 60, NULL, NULL, '{15,161,329,394,525,29486,3548,6181,6894,131365,131679,143016,141982,142572,142071,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564869030, 90, 9, 60, NULL, NULL, '{40,370,248,318,610,2408,2999,6017,5156,131283,130383,142189,141873,142422,143253,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564869630, 86, 9, 60, NULL, NULL, '{34,484,298,385,879,2593,4288,7761,7989,131429,132674,143783,144195,143933,145864,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564870230, 86, 9, 60, NULL, NULL, '{39,413,303,613,754,2673,4671,7169,7153,129548,129200,142837,141858,143411,144497,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564870830, 90, 9, 60, NULL, NULL, '{44,501,313,603,813,2722,3343,7451,7657,129677,131505,142227,141747,142689,147367,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564871430, 79, 9, 60, NULL, NULL, '{45,3769,366,505,655,2194,3041,5600,6997,131403,129491,143028,141512,153317,249503,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564872030, 90, 9, 60, NULL, NULL, '{39,230,203,337,700,2554,3396,6083,7643,131104,130218,142199,142830,142688,143172,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564872630, 90, 9, 60, NULL, NULL, '{21,956,281,514,588,2286,3316,5530,7457,130751,130235,142251,141693,142731,146283,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564873230, 86, 9, 60, NULL, NULL, '{44,47429,252,2209,1826,3974,4867,8957,9309,130129,130869,143974,143459,143915,150420,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564873830, 79, 9, 60, NULL, NULL, '{11,210,202,281,496,2277,3236,5364,4334,130625,130291,143635,141703,142761,143301,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564874430, 79, 9, 60, NULL, NULL, '{15,190,196,466,3559,2265,10058,7673,7281,130987,129390,142752,141801,142323,146497,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564875030, 86, 9, 60, NULL, NULL, '{14,1406,243,405,7496,2606,3136,7555,5012,129208,130870,143435,141900,142475,147495,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564875630, 86, 9, 60, NULL, NULL, '{44,526,483,615,2799,7432,3494,8002,6016,129910,129811,142623,141546,142498,144268,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564876230, 97, 9, 60, NULL, NULL, '{45,390,307,495,629,2505,3363,6703,5005,130630,130371,143597,142050,142650,146040,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564876830, 90, 9, 60, NULL, NULL, '{33,12595,419,407,516,2767,5011,7025,8754,131946,132262,142907,144411,144424,144219,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564877430, 86, 9, 60, NULL, NULL, '{34,199,156,308,597,2644,3475,6873,6480,130384,130318,142969,141786,142721,147907,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564878030, 90, 9, 60, NULL, NULL, '{40,501,306,587,766,2432,3175,7094,6103,129757,129684,142399,141930,156579,149859,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564878630, 86, 9, 60, NULL, NULL, '{29,244,168,263,577,2701,3484,4965,6923,130751,131053,143506,141894,143507,198547,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564879230, 79, 9, 60, NULL, NULL, '{50,8215,335,525,650,2732,3220,5674,6792,130213,129816,142031,141977,142653,147430,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564879830, 86, 9, 60, NULL, NULL, '{41,309,219,376,406,2467,3357,6595,6452,130748,130603,143578,178073,142505,142328,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564880430, 79, 9, 60, NULL, NULL, '{44,522,334,613,711,2630,5222,9341,5532,132066,132645,145317,143252,144498,149273,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564881030, 90, 9, 60, NULL, NULL, '{37,489,288,491,461,2664,3193,6738,3613,131130,129761,142250,141936,142686,143193,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564881630, 97, 9, 60, NULL, NULL, '{44,521,272,475,2859,3120,15553,5724,6120,131352,130602,142045,141761,147399,143804,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564882230, 97, 9, 60, NULL, NULL, '{51,7089,224,307,1364,2319,2951,5104,5438,130127,129470,142906,141607,142706,149502,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564882830, 90, 9, 60, NULL, NULL, '{19,52488,345,531,701,2479,3355,7213,7285,129735,131134,141989,141733,142326,151093,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564883430, 79, 9, 60, NULL, NULL, '{44,522,304,607,1086,2590,3241,6219,4964,130227,131347,142324,142009,142668,243046,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564884030, 90, 9, 60, NULL, NULL, '{44,44978,261,2355,2553,4637,4722,8197,5703,132113,131853,143949,143325,144032,149636,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564884630, 86, 9, 60, NULL, NULL, '{38,412,340,480,807,2550,3200,6192,5055,129689,129636,143469,141627,142447,142827,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564885230, 86, 9, 60, NULL, NULL, '{44,543,500,597,12893,2712,3078,6719,5104,129700,130582,142256,141973,142629,148831,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564885830, 90, 9, 60, NULL, NULL, '{36,6282,251,295,592,2359,3205,5939,4156,129470,130248,141702,145546,142587,150257,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564886430, 97, 9, 60, NULL, NULL, '{43,96686,248,442,573,2470,3131,5138,7787,129389,129958,143658,141665,142567,200333,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564887030, 90, 9, 60, NULL, NULL, '{43,3462,208,394,595,2304,3127,6621,6482,130072,130247,143485,143184,142789,144604,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564887630, 97, 9, 60, NULL, NULL, '{51,7102,305,619,835,2495,4581,6233,6559,130825,131984,143751,145135,143890,143311,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564888230, 79, 9, 60, NULL, NULL, '{19,1081,1154,1175,928,2495,3248,6045,6887,129708,129784,142686,142003,142727,143550,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564888830, 90, 9, 60, NULL, NULL, '{35,562,228,273,554,16626,3285,7316,6343,129941,130622,142893,142492,142742,148853,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564889430, 97, 9, 60, NULL, NULL, '{30,1199,289,485,454,2780,3217,6017,6715,130976,131554,143208,142046,142599,147874,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564890030, 86, 9, 60, NULL, NULL, '{37,279,256,360,467,6219,3260,6986,7453,129879,130809,143425,141650,142742,142420,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564890630, 97, 9, 60, NULL, NULL, '{44,496,304,620,1138,2547,4649,7083,5994,130160,130510,142240,143141,142686,149040,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564891230, 86, 9, 60, NULL, NULL, '{36,336,276,1983,2154,4340,26238,7979,9157,131345,131435,144551,148277,144501,149329,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564891830, 90, 9, 60, NULL, NULL, '{52,4094,216,621,542,2528,3359,6551,4882,130050,129800,143223,141666,142548,145348,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564892430, 97, 9, 60, NULL, NULL, '{15,190,172,341,1976,2226,3039,6089,5546,131613,130705,142896,142002,142502,145563,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564893030, 79, 9, 60, NULL, NULL, '{16,262,242,585,1129,2301,3088,7481,6755,129247,129889,142860,141821,142276,148896,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564893630, 90, 9, 60, NULL, NULL, '{35,524,229,318,3655,2310,2991,7470,5623,131742,131503,142301,141748,142455,147354,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564894230, 86, 9, 60, NULL, NULL, '{40,187,166,349,661,2466,3184,6143,5127,131226,130752,142923,141892,142693,199428,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564894830, 86, 9, 60, NULL, NULL, '{44,328,332,514,19892,3762,4762,11769,6568,131975,131865,144859,143045,144448,147458,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564895430, 97, 9, 60, NULL, NULL, '{44,505,375,475,573,2425,3153,6883,6928,131349,130586,142698,141708,142618,142348,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564896030, 86, 9, 60, NULL, NULL, '{11,158,174,355,645,2497,3033,6845,4377,129796,129933,143377,141823,142451,237827,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564896630, 86, 9, 60, NULL, NULL, '{17,220,125,326,520,2516,3281,5665,4288,131145,131111,143101,141418,142590,156168,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564897230, 86, 9, 60, NULL, NULL, '{13,403,234,501,891,2417,3026,7785,5180,131455,130136,141775,141917,142403,145684,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564897830, 90, 9, 60, NULL, NULL, '{41,518,192,280,382,2451,3004,4974,6258,130603,130927,142215,141937,142577,148288,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564898430, 97, 9, 60, NULL, NULL, '{43,218,150,491,580,2572,5642,8311,5641,131387,130523,142886,143427,144036,149014,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564899030, 90, 9, 60, NULL, NULL, '{17,344,179,280,423,2579,3439,5255,7081,130722,130843,141842,141682,142734,142925,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564899630, 79, 9, 60, NULL, NULL, '{37,412,224,535,733,2492,3134,5193,7417,130219,130934,143058,141826,142476,146315,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564900230, 90, 9, 60, NULL, NULL, '{40,416,234,327,623,2453,3412,6423,4390,131248,130607,142839,141646,142832,147075,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564900830, 79, 9, 60, NULL, NULL, '{37,348,240,497,676,2358,3109,5131,4128,129990,131413,143439,141814,142849,146937,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564901430, 97, 9, 60, NULL, NULL, '{34,258,215,388,626,2487,3405,10047,4175,130042,131277,142276,141541,142795,178673,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564902030, 86, 9, 60, NULL, NULL, '{47,413,276,2091,2049,4261,5377,7837,7195,130902,131962,145062,142411,144172,182731,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564902630, 90, 9, 60, NULL, NULL, '{11,201,161,392,495,2723,3467,5549,4331,129928,129369,142168,141914,142251,150375,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564903230, 90, 9, 60, NULL, NULL, '{40,710,307,589,776,2456,3091,6117,6883,131174,130694,143326,141683,142512,154706,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564903830, 97, 9, 60, NULL, NULL, '{23,27800,161,361,19106,2791,3381,6144,6911,130594,130751,142023,141883,142637,161018,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564904430, 86, 9, 60, NULL, NULL, '{37,1588,311,532,428,2651,31481,6484,7389,130653,129370,142497,141913,142641,165153,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564905030, 97, 9, 60, NULL, NULL, '{15,2709,157,313,471,2653,3506,5898,7105,131563,129943,142181,141764,142798,146432,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564905630, 97, 9, 60, NULL, NULL, '{50,8318,254,347,21304,2869,4231,7331,6022,131253,131752,144255,143047,143597,147360,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564906230, 90, 9, 60, NULL, NULL, '{36,187,190,388,831,2417,3299,5482,4466,130557,129643,143724,141895,142584,175914,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564906830, 86, 9, 60, NULL, NULL, '{23,45450,154,276,600,2777,3068,6900,6191,131415,130203,142220,141421,142270,148927,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564907430, 79, 9, 60, NULL, NULL, '{33,343,239,502,660,2594,3219,7236,6635,129995,129773,142146,141852,142587,145058,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564908030, 79, 9, 60, NULL, NULL, '{42,520,336,5741,830,2523,3276,4564,5954,129608,130569,141987,143030,142394,147158,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564908630, 79, 9, 60, NULL, NULL, '{43,85748,261,391,463,2246,3046,7141,5863,130078,130039,143477,141476,142683,144798,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564909230, 79, 9, 60, NULL, NULL, '{45,18144,495,613,2816,3851,4223,6781,4866,132085,132428,144204,142861,143915,151033,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564909830, 79, 9, 60, NULL, NULL, '{24,2951,182,493,808,2527,3236,6369,7574,130244,131141,143908,147791,142639,149191,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564910430, 90, 9, 60, NULL, NULL, '{39,452,325,498,673,2376,3147,5870,4805,130169,129484,142138,141552,142728,150007,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564911030, 90, 9, 60, NULL, NULL, '{40,407,313,635,644,2468,3410,5508,7174,131019,130318,143006,141842,142679,149163,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564911630, 90, 9, 60, NULL, NULL, '{23,211,175,295,501,2565,3158,5354,5609,129455,130053,142977,143181,142663,142568,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564912230, 86, 9, 60, NULL, NULL, '{38,281,196,429,652,2376,3062,7400,6260,130161,130265,142944,141856,142791,146339,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564912830, 97, 9, 60, NULL, NULL, '{45,407,364,323,621,2832,4453,5571,6535,133277,132901,143992,144413,144217,148641,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564913430, 79, 9, 60, NULL, NULL, '{46,519,307,635,515,2584,3218,5407,5902,129607,131371,141986,142386,142921,142554,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564914030, 79, 9, 60, NULL, NULL, '{45,489,349,347,612,2465,3174,7126,4147,130899,130053,141730,142108,142593,142559,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564914630, 90, 9, 60, NULL, NULL, '{44,8575,213,597,812,2551,3080,5843,4478,130918,130401,143744,141875,142754,142179,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564915230, 97, 9, 60, NULL, NULL, '{45,330,495,603,1069,2491,3130,4100,7087,130154,131365,141863,143720,142853,223365,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564915830, 79, 9, 60, NULL, NULL, '{18,167,207,391,524,2478,3324,6139,6095,130576,131362,142989,142361,142672,146813,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564916430, 97, 9, 60, NULL, NULL, '{49,353,287,407,1464,3392,4558,6097,6375,131293,131229,144194,143125,144346,208943,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564917030, 97, 9, 60, NULL, NULL, '{44,501,491,590,668,2520,3124,4983,7121,131213,131553,143942,141776,142519,167939,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564917630, 79, 9, 60, NULL, NULL, '{40,298,223,286,386,2462,3228,6909,3617,130667,130004,142289,141424,142575,149670,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564918230, 79, 9, 60, NULL, NULL, '{47,966,487,449,874,3466,21850,7196,3763,130750,130558,141831,141623,142714,149793,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564918830, 97, 9, 60, NULL, NULL, '{19,200,173,361,885,7248,3277,5756,5122,129541,129806,142094,143034,142677,149804,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564919430, 90, 9, 60, NULL, NULL, '{41,504,301,635,683,2539,7054,5012,4882,129611,130658,143104,143060,142674,145480,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564920030, 97, 9, 60, NULL, NULL, '{29,155,158,1675,1781,3883,4508,6171,8216,130916,132254,143591,143566,144090,147244,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564920630, 86, 9, 60, NULL, NULL, '{41,390,296,536,668,2743,3135,7738,5953,129847,130713,141780,141569,142368,143291,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564921230, 90, 9, 60, NULL, NULL, '{44,390,198,588,570,2452,3135,5942,4500,130735,130321,143376,141583,143993,148538,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564921830, 90, 9, 60, NULL, NULL, '{42,30275,310,424,695,7675,3552,5082,4554,129970,129724,142304,141580,142608,146286,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564922430, 90, 9, 60, NULL, NULL, '{38,367,246,352,980,2402,3049,4976,8987,130617,130883,142318,142136,142751,143273,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564923030, 86, 9, 60, NULL, NULL, '{39,396,315,516,490,2325,3178,6046,5042,129470,129807,143902,141579,143530,146518,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564923630, 86, 9, 60, NULL, NULL, '{38,383,319,1424,2010,3674,4042,7300,5346,131958,131413,145218,143599,144107,144958,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564924230, 90, 9, 60, NULL, NULL, '{37,4620,265,399,658,2443,3082,6300,6317,130068,131063,143479,141893,142680,146415,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564924830, 90, 9, 60, NULL, NULL, '{39,307,283,479,686,2782,3498,7749,3659,130758,130071,143239,142022,142451,147139,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564925430, 79, 9, 60, NULL, NULL, '{36,928,155,292,467,2340,3408,7742,3978,130469,130509,143576,143105,142713,186105,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564926030, 90, 9, 60, NULL, NULL, '{44,493,306,608,849,2816,3102,5643,5351,130548,131039,142153,381910,142782,142063,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564926630, 79, 9, 60, NULL, NULL, '{44,514,315,495,980,11298,3287,6434,4546,131362,130680,142540,141919,142310,148821,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564927230, 79, 9, 60, NULL, NULL, '{12,636,111,257,31542,4145,4766,7718,7136,131916,132169,143591,142563,144491,147672,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564927830, 97, 9, 60, NULL, NULL, '{34,236,214,324,539,2426,3303,5460,5556,130516,130491,143740,142033,142869,145924,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564928430, 79, 9, 60, NULL, NULL, '{53,563,344,611,718,2664,3401,6958,5648,131386,130901,143697,141358,142744,144129,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564929030, 86, 9, 60, NULL, NULL, '{53,4875,350,503,811,2512,3181,5119,3843,131614,131140,143524,141720,142589,251447,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564929630, 97, 9, 60, NULL, NULL, '{36,371,172,305,395,2420,3099,6022,4192,131435,130414,143937,141634,142846,159151,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564930230, 97, 9, 60, NULL, NULL, '{50,220,173,334,567,2851,3230,6309,7016,130605,131371,143880,141580,142749,145914,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564930830, 79, 9, 60, NULL, NULL, '{45,532,323,601,1042,2517,4509,9548,7775,131839,132432,144047,143073,144281,150365,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564931430, 97, 9, 60, NULL, NULL, '{47,24504,322,539,648,2641,3330,4288,6741,129213,129990,142587,199829,142377,150112,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564932030, 90, 9, 60, NULL, NULL, '{39,392,303,512,751,2280,3354,7083,5460,131040,131160,142859,142938,142727,145731,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564932630, 79, 9, 60, NULL, NULL, '{16,201,178,391,535,2189,13977,5434,5585,130195,131291,142883,142793,142618,142624,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564933230, 79, 9, 60, NULL, NULL, '{52,228,344,625,7804,2674,3236,6036,5650,130400,130596,143504,141620,142710,144082,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564933830, 86, 9, 60, NULL, NULL, '{41,271,245,293,666,2198,8381,5832,5814,129700,130448,142514,142044,142653,142900,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564934430, 97, 9, 60, NULL, NULL, '{32,247,357,2220,2284,4412,4847,11227,6024,131947,131937,144369,143158,143983,145301,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564935030, 90, 9, 60, NULL, NULL, '{40,24217,241,347,578,2445,34603,7004,7422,130381,131178,143314,143208,142378,143484,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564935630, 86, 9, 60, NULL, NULL, '{13,327,162,255,483,3378,3000,8421,7221,130158,130489,143132,141937,142681,145309,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564936230, 79, 9, 60, NULL, NULL, '{49,517,326,605,846,6195,3421,6531,5734,129516,129457,143927,141628,142330,145670,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564936830, 79, 9, 60, NULL, NULL, '{42,5254,374,1083,658,2412,3358,5889,3844,129795,129986,142478,141915,142803,143654,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564937430, 86, 9, 60, NULL, NULL, '{42,443,222,307,430,3187,3218,5817,3832,131289,130954,142541,141820,142554,146531,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564938030, 86, 9, 60, NULL, NULL, '{31,288,211,393,671,3896,4969,7180,5651,131460,131345,144057,145312,144023,148395,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564938630, 97, 9, 60, NULL, NULL, '{44,516,218,457,841,2700,3197,5487,4219,130066,130201,142902,141952,142740,148296,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564939230, 97, 9, 60, NULL, NULL, '{45,828,328,504,937,2351,3311,4150,7326,130742,130837,142226,141922,142836,145890,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564939830, 90, 9, 60, NULL, NULL, '{17,299,134,246,748,2512,3272,10303,5628,130258,131596,141805,141998,142734,149833,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564940430, 86, 9, 60, NULL, NULL, '{44,437,308,494,973,2661,3170,6715,4545,129769,130864,142296,141619,142315,145117,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564941030, 79, 9, 60, NULL, NULL, '{35,181,269,424,567,2458,3212,6568,6354,130905,130071,142227,142490,142626,175282,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564941630, 97, 9, 60, NULL, NULL, '{43,498,323,606,771,2423,4989,7690,7714,131987,131663,144094,144687,143576,218743,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564942230, 97, 9, 60, NULL, NULL, '{32,216,204,319,542,3200,3320,6607,5757,129344,131250,142793,141833,142416,142383,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564942830, 97, 9, 60, NULL, NULL, '{12,148,151,281,707,2401,3579,6754,7103,131475,131644,144094,141882,142441,180649,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564943430, 86, 9, 60, NULL, NULL, '{38,253,202,361,22429,4869,3594,6287,4546,129771,131431,142283,141873,142714,147827,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564944030, 90, 9, 60, NULL, NULL, '{48,27537,492,609,901,2561,3329,7254,4262,131371,130159,142593,141646,142822,143626,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564944630, 97, 9, 60, NULL, NULL, '{51,33674,252,430,1313,2361,3187,5103,4080,130461,130504,142235,141819,143621,254754,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564945230, 97, 9, 60, NULL, NULL, '{48,294,285,2295,2866,5372,5063,7367,9083,132691,130982,142940,143255,143931,147720,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564945830, 90, 9, 60, NULL, NULL, '{45,1022,167,366,828,2316,3257,6540,6253,129853,130158,141996,141883,142247,149030,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564946430, 79, 9, 60, NULL, NULL, '{35,220,239,390,702,2295,3212,7481,6523,131496,130366,142383,141853,142883,142601,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564947030, 90, 9, 60, NULL, NULL, '{41,484,256,365,12873,2513,3286,5867,4643,130198,131656,142404,141815,160402,149124,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564947630, 79, 9, 60, NULL, NULL, '{42,243,222,513,942,2621,3143,7233,6407,129704,129867,142087,143263,142765,152816,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564948230, 90, 9, 60, NULL, NULL, '{38,195372,198,318,4178,2584,3123,5376,5989,130761,131383,143957,141460,142374,146247,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564948830, 97, 9, 60, NULL, NULL, '{34,324,152,405,825,4309,5022,7492,7928,132328,132832,145157,144471,144790,157456,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564949430, 86, 9, 60, NULL, NULL, '{45,391,310,488,852,2603,3314,5048,6529,130086,130958,142519,141694,142492,142727,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564950030, 86, 9, 60, NULL, NULL, '{39,1465,247,291,642,2278,3247,19118,5430,131661,130754,143912,143135,142745,149472,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564950630, 90, 9, 60, NULL, NULL, '{46,565,278,313,520,2444,2973,17199,4309,129384,131279,142455,141840,142574,147522,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564951230, 97, 9, 60, NULL, NULL, '{45,4074,223,298,428,2253,3259,19340,5545,131452,130412,142460,141953,142742,147027,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564951830, 79, 9, 60, NULL, NULL, '{51,689,283,441,3968,2518,3381,5455,6670,130588,129508,143346,141935,142624,145200,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564952430, 97, 9, 60, NULL, NULL, '{45,499,378,492,751,4204,4612,6265,7348,132134,130992,143393,144595,143977,143913,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564953030, 86, 9, 60, NULL, NULL, '{62,2214,267,415,774,2698,3020,7352,5763,130296,131583,142418,141602,142707,147975,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564953630, 97, 9, 60, NULL, NULL, '{55,583,272,478,718,8340,3232,6791,7073,130953,131275,141924,142640,142613,154920,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564954230, 97, 9, 60, NULL, NULL, '{45,17264,300,622,615,2480,3004,8515,6352,129279,130693,143189,141596,144784,142196,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564954830, 90, 9, 60, NULL, NULL, '{48,601,238,520,499,2615,2960,7652,5288,129858,130769,146591,141519,143504,147802,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564955430, 90, 9, 60, NULL, NULL, '{44,681,336,617,5460,2986,3415,6136,8014,130842,130108,143703,141788,142516,145279,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564956030, 79, 9, 60, NULL, NULL, '{42,546,326,451,801,2554,4978,7253,7613,131769,130866,143671,143218,143290,144250,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564956630, 86, 9, 60, NULL, NULL, '{11,203,127,1249,658,2470,3017,5844,6877,129775,131034,143376,141714,142389,149329,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564957230, 86, 9, 60, NULL, NULL, '{36,278,272,377,667,2774,3079,6757,5683,129309,131273,143027,141773,142538,145951,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564957830, 97, 9, 60, NULL, NULL, '{13,307,220,329,855,2625,3036,4760,5986,130257,130828,142215,141870,142621,142206,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_18 VALUES (18, 1564958430, 79, 9, 60, NULL, NULL, '{44,432,330,3084,636,2339,3110,7086,5756,130429,129341,142747,141620,142425,146867,NULL,NULL,NULL,NULL,NULL}');


--
-- Data for Name: data_amp_traceroute_19; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564373430, 41, 2, 60, NULL, NULL, '{46,166,183,367,6921,2347,2780,2714,NULL,3991,6537,6091,6697,130288,138065,138559,139592,140306,155859,166217,177890,194797,196944,208267,211460,272862,273495,264797,430788,431936}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564711230, 43, 4, 60, 3, 10, '{29,391,230,345,754,4106,4728,6443,7278,130725,138996,138826,140936,142310,157461,167386,178692,190885,198339,209092,212366,275406,275678,266566,431122,430939,442724,430899,432582,429708}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564712430, 45, 4, 60, 3, 10, '{46,5319,247,470,647,2795,3066,6530,8129,131283,139267,139019,139564,140654,155610,165698,177559,189212,196657,208040,211304,272941,274011,262483,428831,430214,440811,429296,432392,429331}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564713030, 47, 7, 60, 3, 10, '{33,382,209,399,516,2306,3081,4829,6697,129854,138375,137324,139360,139966,155534,165827,177379,189333,197331,208608,211825,273458,274675,266088,440671,438985,427688,432246,427748}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564718430, 49, 7, 60, 3, 10, '{39,422,214,1823,1459,3391,4695,7944,7699,130560,140025,140308,141110,141602,156627,167174,179507,191008,198292,209370,212763,275440,275252,266972,441419,440875,427782,432017,429569}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564719030, 51, 7, 60, 3, 10, '{35,430,226,368,552,9988,3190,7042,4444,129757,138133,137192,139305,140053,155846,165861,177754,189151,196084,207015,210427,273700,273096,266397,437382,438792,426983,428969,427346}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564719630, 53, 4, 60, 3, 10, '{45,508,332,445,2363,2374,3059,5337,6510,130168,137963,137689,139774,140234,156247,166086,177470,189454,196697,207813,211178,273263,273622,264161,428698,430416,440545,429068,431371,429486}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564720230, 55, 7, 60, 3, 10, '{45,503,219,334,483,2350,3438,4946,6695,130999,139239,137549,139656,140163,155766,166166,177831,189629,196479,207452,210413,273487,273277,262257,439300,438784,427283,428683,427477}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564720830, 56, 7, 60, 3, 10, '{16,178,166,1573,606,2470,3068,6146,4932,129873,137496,137022,139649,140224,155581,165967,177634,189674,196681,207845,212254,274382,273703,265461,440131,439215,427580,428633,427722}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564721430, 58, 7, 60, 3, 10, '{15,272,148,394,688,2422,3375,5899,5938,129248,137349,145910,139576,140141,157202,165813,177456,189462,196788,207764,211468,272656,273742,264201,454337,438799,427047,428291,427453}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564722030, 59, 4, 60, 3, 10, '{45,614,528,2095,2120,5467,4527,7331,5138,130904,139114,139441,141273,141341,157212,167743,179335,190616,198611,209620,213415,274227,276030,267023,434228,431764,441676,429398,430627,431187}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564722630, 62, 4, 60, 3, 10, '{17,207,154,271,497,2567,3218,4571,5991,129919,139076,137610,139968,145516,155722,166164,177731,189766,197295,207669,211578,273357,274194,264279,430728,432321,440882,429301,433537,429143}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564723230, 64, 7, 60, 3, 10, '{31,214,272,459,785,2513,3152,4777,7475,131625,139806,137751,140193,140920,156685,166877,177660,189489,197954,208943,212584,273774,274721,266207,440652,440192,427907,432875,428860}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564723830, 45, 4, 60, 3, 10, '{44,5012,341,531,731,2702,3155,5638,4150,130770,137769,140559,139274,140510,155902,165738,177747,189631,197093,207991,211497,273165,274191,263837,430447,432416,440836,429406,431663,429551}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564724430, 55, 7, 60, 3, 10, '{43,18034,231,519,581,2273,3124,4841,7236,130127,138662,180481,139558,140314,155948,166210,177533,189760,196369,207059,210769,274187,274192,263652,438352,438769,427273,429886,427619}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564725030, 56, 7, 60, 3, 10, '{48,511,226,338,767,2318,8999,7645,4260,130570,137913,137654,139675,140419,155405,166087,177848,189613,197057,208043,211295,274056,273986,263695,442082,439505,428241,428801,428392}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564725630, 66, 4, 60, 3, 10, '{44,562,357,1736,2214,3757,4962,8178,8694,131124,140070,138894,140623,142074,157652,167891,179169,190529,197902,209647,212146,275168,275346,264389,433990,431466,442915,429844,432876,431840}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564726230, 43, 4, 60, 3, 10, '{46,208,268,359,523,2621,3534,5372,7239,130780,138902,138482,139186,139984,155708,166259,178241,189643,196332,207406,210692,273618,273482,264415,429940,432345,440694,433281,430368,429255}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564726830, 53, 4, 60, 3, 10, '{46,410,341,615,765,2469,3054,5231,6103,130434,139408,137353,139411,140702,155825,166372,178236,189452,197932,208972,212218,273896,274802,266272,430772,432253,441350,430041,433117,430405}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564727430, 55, 7, 60, 3, 10, '{36,486,212,302,458,2450,3264,7221,3987,129588,138331,137199,139553,140483,155974,166440,178000,189685,196193,207191,210500,273730,273441,265602,437863,439013,427612,429906,427242}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564728030, 68, 7, 60, 3, 10, '{42,20206,294,597,521,2561,5608,5683,3954,129983,139366,137322,139330,140571,155936,166264,177865,189870,196918,208134,211674,272865,274227,263474,441127,438841,427526,428875,427642}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564728630, 58, 7, 60, 3, 10, '{51,3200,182,321,13403,11315,3327,8060,5296,130176,138642,137602,139714,140577,155576,166447,177820,189511,197385,208633,212500,273578,274540,266189,439388,439758,501884,429112,428622}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564729230, 70, 4, 60, 3, 10, '{49,466,512,2090,2688,4223,4951,7957,6432,132337,141435,138763,141501,141910,157126,166887,179037,190896,198335,209721,213174,276311,275483,266607,434026,450110,442638,429851,431899,431393}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564729830, 66, 4, 60, 3, 10, '{44,501,310,604,547,2364,3058,4770,4980,129788,138857,138889,139723,140332,155817,166254,177898,189508,196310,207474,210581,274051,273354,262430,430498,428628,443493,429571,431948,430087}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564730430, 66, 4, 60, 3, 10, '{29,493,303,299,516,2582,3268,6481,5122,131684,137588,137564,139984,140222,155968,166258,177545,189376,196375,207532,211119,273784,273382,264523,431924,429101,444222,429518,430674,429939}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564731030, 55, 7, 60, 3, 10, '{44,51058,191,509,651,2760,3643,6831,5179,130988,138527,137510,139945,140437,156158,166189,177808,189761,196552,207441,210454,273818,273465,264107,438192,438726,427878,428581,427581}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564731630, 68, 7, 60, 3, 10, '{39,416,379,333,659,2431,3192,9515,6726,130113,137895,137504,139450,140140,155815,166399,177793,189537,197029,208100,211935,273031,274019,264522,440742,441104,427079,430229,428414}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564732230, 66, 4, 60, 3, 10, '{45,1632,159,348,667,2509,3391,7764,5500,131150,139357,138594,139656,140238,155649,166256,177659,189824,196852,208062,211357,274347,274139,265010,439898,431795,441206,430058,430869,431068}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564732830, 56, 7, 60, 3, 10, '{44,573,326,2386,2777,3672,26203,9008,7251,132628,138985,139026,140874,142101,157580,167530,179177,191412,199190,209573,212549,276027,275710,265688,440860,441381,428148,429044,430002}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564733430, 64, 7, 60, 3, 10, '{44,691,313,621,780,2349,3298,6923,5143,131776,137588,137339,139672,139998,155636,166259,177944,189766,197823,208885,211928,273256,274547,263696,439567,439517,428209,430239,428334}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564734030, 68, 7, 60, 3, 10, '{14,270,156,279,579,12215,3079,5718,4143,130892,138478,138572,140058,140510,155664,166065,178017,189599,196657,208085,211579,273332,274282,265644,437825,440072,427311,428393,427208}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564734630, 47, 7, 60, 3, 10, '{45,515,237,313,680,2392,3192,6945,3892,130538,139288,138534,139747,140412,156110,166280,178040,189487,196875,208229,211370,272922,273961,265486,439487,439021,427488,429259,427783}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564735230, 64, 7, 60, 3, 10, '{37,572,230,440,8923,2386,3228,5844,7638,129900,137806,137239,139715,140209,155455,166253,177951,189571,197274,208337,211107,272965,274154,262500,439620,439398,427449,428805,427289}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564735830, 55, 7, 60, 3, 10, '{36,483,258,515,698,2472,3261,5318,4660,130356,137473,138361,139374,139994,155274,166224,177869,189562,196259,207434,210903,273423,273381,263650,439523,441031,427985,428568,427427}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564736430, 66, 4, 60, 3, 10, '{45,94750,429,2482,2340,4387,4303,7130,4603,132216,140010,138913,141122,141692,156814,167781,179086,191087,197298,208878,212595,275509,274654,265284,431124,431943,442560,429134,430691,431425}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564737030, 55, 7, 60, 3, 10, '{46,652,311,631,5831,2470,3137,6291,6086,129846,145476,137109,139362,140358,155976,166225,179226,189685,196344,207471,210732,273902,273374,262740,441215,438493,427477,428008,427507}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564737630, 53, 4, 60, 3, 10, '{45,517,498,621,1949,2731,3481,5211,4040,130030,137759,137084,139768,140349,156004,166317,177586,189901,197185,207995,211649,273090,274070,265181,432251,431645,443399,429595,430544,429751}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564738230, 45, 4, 60, 3, 10, '{42,1277,148,510,839,2613,3280,7742,5697,131404,137848,137430,139667,140636,155995,165928,177812,189473,197155,208153,211186,272947,274091,262802,433651,432656,441044,429176,431867,429409}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564738830, 58, 7, 60, 3, 10, '{45,1823,410,647,451,2988,3072,5170,4565,130422,139301,137603,139402,140503,161776,165949,178113,189157,196557,207753,211574,272901,274141,264278,437915,441520,427470,430060,427907}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564739430, 56, 7, 60, 3, 10, '{45,1123,305,626,802,2665,3392,4945,6693,130679,138954,150752,139731,140098,155952,166176,177723,189758,196184,207559,210775,273802,273375,262918,439145,439180,427025,428674,427381}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564740030, 56, 7, 60, 3, 10, '{31,316,319,1807,9106,4534,4880,7938,7598,133995,139206,138745,141311,142167,157003,167575,179381,191130,198415,209803,212659,275958,275859,267355,438916,443237,430141,430063,429619}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564740630, 51, 7, 60, 3, 10, '{46,192,391,811,1455,3916,3670,6358,5173,131149,138426,138033,139776,141323,156382,166622,178661,190115,196887,208013,212003,274827,273999,264166,441864,440413,428084,430062,428660}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564741230, 62, 4, 60, 3, 10, '{18,179,180,286,574,2404,3231,6921,4367,131098,137494,138745,139584,140597,155895,166045,177712,189266,196913,208172,211738,273228,274377,266036,432916,431389,440811,429654,431564,429103}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564741830, 70, 4, 60, 3, 10, '{43,20039,249,312,509,2789,5669,5298,5456,130625,137911,138928,139390,140321,155950,166135,177850,189997,196403,207391,210467,273611,272984,264276,428997,432072,441011,429480,430409,429572}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564742430, 51, 7, 60, 3, 10, '{46,15783,305,400,711,2416,3034,6948,7479,131404,138790,137428,139729,139935,155838,166211,177796,190097,196240,207678,211077,273730,273320,264999,437752,438773,428005,429131,427578}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564743030, 74, 4, 60, 3, 10, '{50,423,326,623,707,2333,3178,6188,3948,131268,137658,137509,139830,140161,155595,166202,177712,189502,196145,207509,210868,273653,273111,263395,430002,428633,443271,429517,430001,429315}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564743630, 45, 4, 60, 3, 10, '{39,461,352,1696,1766,3887,4416,7111,7260,130751,140010,138948,140597,141744,157380,167127,179925,190591,198122,210384,213763,274882,276760,265136,431685,431517,442917,430233,445831,431776}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564744230, 43, 4, 60, 3, 10, '{48,529,389,531,929,2664,3174,6439,5812,132856,137678,137253,139220,140325,156119,166534,177582,189282,196169,207421,210827,273695,273292,262987,433103,432774,441097,429385,430483,429417}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564744830, 49, 7, 60, 3, 10, '{41,254,256,14630,1007,2721,3222,6113,6590,130907,139368,137499,139813,140063,155941,165812,177802,189744,197110,208016,211615,274549,273781,263312,438946,439781,428444,429605,428718}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564745430, 49, 7, 60, 3, 10, '{45,44159,226,372,795,2310,3009,4900,5823,131033,138582,137175,139640,140494,155658,166267,177368,189793,196223,207582,211086,273375,273130,262544,441246,439819,427850,429049,427805}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564746030, 56, 7, 60, 3, 10, '{46,463,310,697,488,2342,3141,5243,6747,130963,138007,137576,139530,140402,156216,166618,177407,189872,195872,207070,210686,273325,273399,262010,438626,439623,427381,429766,427986}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564746630, 55, 7, 60, 3, 10, '{41,581,215,418,663,2586,3080,4490,5497,129883,137322,137897,139816,140671,155943,166201,177843,189590,196246,207683,210744,273670,273437,263568,437851,439208,428618,428561,427886}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564747230, 55, 7, 60, 3, 10, '{39,574,516,1756,1553,3496,4976,8231,7906,132768,140098,140019,140600,141323,156910,167864,178772,191204,197668,209672,212837,275793,275005,263791,441264,440910,427784,446400,430104}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564747830, 56, 7, 60, 3, 10, '{46,31512,307,541,609,2672,3173,4349,6809,130038,138099,137487,139824,140540,155600,166389,177772,189722,196398,207382,211022,274199,273446,265364,441957,438995,427988,430201,427881}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564748430, 55, 7, 60, 3, 10, '{13,158,383,392,532,2412,3234,5668,7552,131608,138016,137558,139609,140589,155692,166559,177904,189890,196556,207489,210458,274047,273474,265886,439794,439920,427501,429323,428165}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564749030, 56, 7, 60, 3, 10, '{50,3403,332,426,712,2737,3153,5521,6508,130658,137768,137793,140040,140589,155760,166148,177824,189845,196108,207862,210733,273756,273041,263596,439627,439336,427747,430883,427822}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564749630, 51, 7, 60, 3, 10, '{11,162,117,282,21926,2434,3461,7067,6488,130567,138529,138595,139840,140771,155679,166027,177749,190125,196348,207473,210991,274156,273380,264408,440376,438903,427709,431362,428100}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564750230, 70, 4, 60, 3, 10, '{52,602,337,486,836,2398,3307,6640,4272,131267,137968,137230,139665,140507,155884,166187,177880,189776,195969,207318,211083,274032,273363,264776,428981,432646,441026,429533,432008,430239}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564750830, 51, 7, 60, 3, 10, '{45,811,528,1505,1798,3365,4182,6406,8406,132318,140464,139867,141205,142089,157144,168105,179316,191325,197923,208797,213030,275531,274594,264848,442716,440735,427892,434997,429316}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564751430, 53, 4, 60, 3, 10, '{41,399,308,401,924,2749,3298,5313,5512,129276,137903,138919,139517,140338,155764,166372,177700,189364,197285,208160,211437,273166,274041,264864,431613,432540,440796,429685,430752,429592}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564752030, 62, 4, 60, 3, 10, '{45,505,311,630,859,2520,3846,6224,5952,129624,139357,139006,139690,140554,156078,166041,177942,189663,197624,208665,212033,273683,274962,265022,429476,432383,443263,430251,430748,430759}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564752630, 43, 4, 60, 3, 10, '{15,224,132,277,610,7141,3186,6920,5343,131194,137673,138747,139368,140317,156136,166193,177516,189680,197129,207728,211516,274125,273668,265662,432905,432090,441733,429691,432168,430429}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564753230, 47, 7, 60, 3, 10, '{48,745,275,427,778,2523,3102,6223,5466,131009,138563,137534,139896,140337,156088,166087,177688,189322,196602,208132,211862,272891,273881,265557,441655,439028,428548,440685,427957}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564753830, 70, 4, 60, 3, 10, '{41,657,183,327,666,2505,3443,7287,6493,131171,137645,137487,139832,140570,155999,166038,178182,189550,196311,207212,210884,273388,273332,265060,432128,429997,441006,428979,430719,429600}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564754430, 62, 4, 60, 3, 10, '{44,576,438,1836,1753,3697,4403,7413,5807,130895,139587,139444,140904,141882,156888,166800,178774,190778,198260,208870,212667,274053,275269,265891,432671,432032,442366,446908,430771,431648}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564755030, 58, 7, 60, 3, 10, '{40,301,235,425,615,2453,3213,6218,4823,129920,138052,137508,139617,140344,156300,166201,178095,189572,197536,209001,212249,273947,274698,263810,441866,440015,428641,443045,428572}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564755630, 53, 4, 60, 3, 10, '{48,513,312,627,945,2643,3203,6273,6379,131335,138364,137502,139354,140171,155868,166555,177979,189500,197439,208721,212551,273872,274861,266030,430848,436418,441609,430063,432566,431231}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564756230, 55, 7, 60, 3, 10, '{44,557,306,613,601,2418,3425,6363,6880,129634,138567,137216,139719,140385,155883,166607,177751,189873,196212,207689,210653,273553,273611,262554,438534,439312,427394,430090,428016}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564756830, 70, 4, 60, 3, 10, '{40,240,334,464,510,2368,3336,6653,5929,129873,139251,137515,139657,140311,155750,166474,177622,189619,196257,207512,211155,273890,273231,264019,433003,429074,440816,429466,432062,430133}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564757430, 43, 4, 60, 3, 10, '{45,475,324,518,615,2550,3051,6929,5645,130902,138613,137606,140075,140317,155472,166032,177946,189848,196946,207650,211757,274513,274040,264455,431057,432281,441565,429860,431577,430006}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564758030, 55, 7, 60, 3, 10, '{51,565,209,2159,2228,3974,4989,6065,7926,131583,139926,139216,140542,142157,157298,167679,179309,191620,197602,209142,212651,275490,274341,266545,439626,441045,427847,430219,429538}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564758630, 43, 4, 60, 3, 10, '{44,497,307,633,469,2319,3168,4297,5452,131540,138167,137130,139616,140352,155583,165960,177376,189740,196423,207339,210940,273960,273374,263942,430979,432098,440712,429899,430647,429274}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564759230, 51, 7, 60, 3, 10, '{35,24146,287,380,479,12059,3096,5994,4773,129985,138051,137593,139664,140431,155735,166570,177920,189835,196150,207411,211108,273698,273136,264225,441054,439142,427320,428408,427719}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564759830, 64, 7, 60, 3, 10, '{23,472,266,560,2473,2431,3457,6452,7149,129734,137173,137584,140146,140549,155917,165907,177849,189243,197151,208200,211937,273373,273860,264226,442076,439201,428325,429009,428014}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564760430, 70, 4, 60, 3, 10, '{45,342,248,519,1032,2798,3167,6094,4798,130228,138400,137689,139836,140258,155504,165931,177565,189538,196182,207117,210778,273829,273593,265879,432177,432417,440817,429821,430303,429571}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564761030, 43, 4, 60, 3, 10, '{56,696,179,289,3429,2529,3264,6261,5677,130814,139122,183328,139621,140066,156099,165846,177798,189797,196252,207550,210846,273663,273373,262691,430368,432860,441087,429651,431864,429603}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564761630, 53, 4, 60, 3, 10, '{38,554,520,1933,2066,4334,5038,9139,6783,132275,139487,139212,141774,142297,157820,167714,179776,190578,197373,208469,212793,274404,275151,263462,431818,431952,442265,429546,430547,430716}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564762230, 68, 7, 60, 3, 10, '{46,637,227,285,696,2568,3154,5080,6664,131382,138079,137715,139412,140351,155515,166101,178166,189793,197324,208050,211800,272802,274266,264120,438680,439476,428050,429835,427845}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564762830, 56, 7, 60, 3, 10, '{42,332,310,632,651,2549,3455,5447,3768,130791,138677,138392,139678,139878,155538,165903,178229,189543,196493,208122,211483,274584,273900,265812,439971,440028,429927,429782,428489}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564763430, 45, 4, 60, 3, 10, '{31,545,206,332,747,2793,8179,6822,4623,130655,139029,181049,139284,140369,155877,165863,178124,189689,196564,207921,212067,272754,274061,266039,429973,430710,441019,678678,435160,429453}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564764030, 68, 7, 60, 3, 10, '{41,11915,305,608,469,2491,3669,5550,5553,131172,138871,137310,139421,139927,156023,165962,177799,189619,196852,208440,211671,272728,274563,265387,440476,439272,427625,428937,427856}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564764630, 70, 4, 60, 3, 10, '{42,454,328,626,857,2337,3242,7687,6165,130994,137740,138645,139572,140498,156019,166069,177807,189770,196781,208206,211371,274245,273698,267452,429341,433098,441272,430035,432281,430183}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564765230, 59, 4, 60, 3, 10, '{45,570,556,1521,16354,4108,4946,6383,7575,135799,140131,139029,141130,141992,157560,167730,178674,191611,198523,210140,213877,275867,276069,282003,431543,432443,442393,429721,444421,432129}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564765830, 59, 4, 60, 3, 10, '{44,630,305,447,703,3167,3676,6610,5686,131420,138542,137756,139752,140820,155984,166424,177831,189327,196745,208032,211710,272901,275534,264024,429714,431650,441168,430344,431438,429623}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564766430, 62, 4, 60, 3, 10, '{44,715,226,292,457,2830,3344,6860,5255,130632,137631,137599,139464,140799,155930,166060,178044,189675,197683,208690,212171,273551,274867,264128,432839,431306,441372,430515,433255,430197}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564767030, 56, 7, 60, 3, 10, '{40,440,241,483,1008,2393,3520,4880,6135,131208,137461,139258,139959,140362,155970,166221,178094,189580,196848,208216,211471,274437,273979,266151,438961,440018,428559,429288,428468}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564767630, 47, 7, 60, 3, 10, '{48,5053,319,614,501,2382,3235,5822,7014,131319,138159,138600,139616,140509,156298,166075,177896,189635,197371,208094,211618,273051,274256,262795,439686,439183,427386,428584,427767}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564768230, 74, 4, 60, 3, 10, '{43,492,602,837,1544,3605,3795,5105,5163,131051,138778,137241,139751,140454,155711,166054,178141,189885,197060,207941,211566,274194,274080,264976,430045,432001,441418,429967,433076,430765}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564768830, 43, 4, 60, 3, 10, '{39,541,532,1543,2165,3882,4790,6417,5453,131412,141280,140656,141124,142037,156788,167652,179430,191157,198243,209037,212685,276149,275441,266998,431411,431706,443108,429982,432174,433258}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564769430, 47, 7, 60, 3, 10, '{46,898,222,358,846,2562,3165,7456,7251,131734,138931,172574,139433,140154,155951,165627,177654,189314,196976,208559,211334,272996,274273,264873,438240,439392,427444,429210,427701}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564770030, 53, 4, 60, 3, 10, '{34,395,388,607,929,2764,3221,7190,5497,130080,138896,137669,139373,140186,155841,166139,177812,189421,196821,208327,211522,272809,274087,281807,431080,445028,440897,429546,432555,429708}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564770630, 74, 4, 60, 3, 10, '{43,239,225,594,1136,2514,3585,5673,4313,131207,139673,138858,139766,140054,155446,166435,178056,189748,197410,207837,211793,274324,273954,266441,431528,438266,441771,429712,432441,429957}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564771230, 49, 7, 60, 3, 10, '{44,514,343,630,870,7155,3938,6525,5903,130945,139474,138239,140192,140328,155730,166477,178045,190242,197076,208682,211965,275428,274193,264762,450732,439619,428398,433326,428505}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564771830, 58, 7, 60, 3, 10, '{52,235,253,625,523,2353,3503,4830,4145,130466,139548,137423,139436,140755,155857,166043,177907,189384,196792,208090,211932,272966,273979,266086,441792,438926,427867,429091,427899}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564772430, 51, 7, 60, 3, 10, '{18,209,192,1084,1639,3272,3990,7112,6625,131591,140980,139357,141129,141962,157201,167469,179420,191567,197738,209134,212428,275725,275418,264781,441695,440290,427376,429296,429178}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564773030, 62, 4, 60, 3, 10, '{43,358,226,370,5276,2451,3158,7094,5133,131461,137217,137530,139676,140127,155946,166058,178087,189457,196961,208276,211632,273152,274332,264181,442295,431639,441096,429455,430231,429545}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564773630, 56, 7, 60, 3, 10, '{45,45736,267,376,662,2504,3208,6010,4889,130114,138941,137537,139947,147195,155653,166149,177901,189953,196441,207855,210578,273985,273329,263337,441162,439238,427262,428845,428171}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564774230, 53, 4, 60, 3, 10, '{41,3622,191,293,2394,2339,3088,5720,4138,131181,139464,137616,139714,140469,156126,165572,177642,189770,197286,208078,211806,273095,274094,263577,432009,431821,441063,429468,431678,429629}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564774830, 66, 4, 60, 3, 10, '{44,796,501,262,482,2816,3389,7439,7546,130034,137891,170700,139756,140520,155636,166217,177997,189583,196447,207495,211190,273589,273412,263895,431662,432480,441068,429559,431168,429557}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564775430, 43, 4, 60, 3, 10, '{15,3801,180,272,531,2234,3173,7112,6032,129672,138716,137452,139999,140060,155790,165924,177895,189795,197160,208230,211558,274432,274112,265044,431873,441193,441650,429871,432678,429736}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564776030, 45, 4, 60, 3, 10, '{34,388,343,2022,1579,3831,4597,8137,7201,131726,140896,138775,141557,141708,157611,167868,179604,191329,198815,208477,213101,274632,275772,265930,432886,432663,444450,429457,431248,430929}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564776630, 66, 4, 60, 3, 10, '{26,445,378,481,458,2491,3481,6854,5332,131058,137602,138437,139768,140628,156118,165915,178109,190055,197088,207902,211502,274723,273946,265242,430835,432246,442930,430067,432214,430037}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564777230, 49, 7, 60, 3, 10, '{14,206,161,279,517,2514,3311,5345,7194,130403,139474,137535,139296,140144,155853,165940,177654,189360,196643,207835,210940,273752,273535,275592,440754,439585,427399,429930,427660}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564777830, 70, 4, 60, 3, 10, '{38,211,169,327,600,2326,3456,4843,6690,130992,137909,137408,139811,140112,155830,166484,177775,189584,197049,207870,211496,274542,274170,263472,433227,430559,441761,429945,432417,429753}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564778430, 43, 4, 60, 3, 10, '{44,402,332,595,476,2584,3343,6873,6207,130172,138816,137493,139934,140414,155681,166007,177458,190112,196352,207521,210905,273924,273010,264328,438803,432184,440908,429150,430807,429721}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564779030, 66, 4, 60, 3, 10, '{48,358,318,299,539,2700,3250,5351,3884,131448,138253,137633,139644,140503,155405,166343,177887,189744,196538,207882,211552,274753,274253,273056,430507,432399,442298,429861,430772,429893}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564779630, 64, 7, 60, 3, 10, '{44,584,225,1705,1641,11897,4611,8775,8314,131862,139199,138249,141315,141754,157182,167676,179209,190388,198641,209719,212641,274560,275493,266408,442931,441185,427758,428632,429166}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564780230, 59, 4, 60, 3, 10, '{34,226,264,338,710,2800,3388,5085,6284,131024,139548,137327,139697,140779,155924,166131,177765,189569,197658,208616,212194,273544,274717,263432,430459,433353,441644,429927,432210,430148}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564780830, 62, 4, 60, 3, 10, '{50,353,342,625,825,2900,3303,4561,7838,130629,138848,137645,139555,140344,156195,165729,177707,189409,197867,208628,212675,273971,274953,268015,430014,431018,441570,430033,431367,429752}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564781430, 62, 4, 60, 3, 10, '{44,342,310,597,827,2501,3180,5596,7266,131512,139307,137510,139500,140431,155926,165749,177520,189692,197586,208692,212200,278621,274843,264182,439097,432390,441248,429876,430944,430165}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564782030, 55, 7, 60, 3, 10, '{44,556,301,631,825,2803,3135,5399,3950,130293,138704,138583,139670,140522,155884,165832,177822,189764,197199,208126,211732,274465,273918,265016,441144,441406,428368,430295,428352}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564782630, 70, 4, 60, 3, 10, '{54,493,242,509,917,2301,3309,5312,4915,129571,138923,137550,139863,140271,155413,166099,177958,189971,196236,207101,210811,273586,273316,262837,433063,431279,443020,429472,432098,429604}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564783230, 59, 4, 60, 3, 10, '{17,216,193,1594,1623,3206,4583,7219,6715,132255,139426,138881,141507,141691,157435,167426,179733,191312,198149,209831,213221,275071,275904,267050,431455,432420,441946,429653,430038,430890}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564783830, 56, 7, 60, 3, 10, '{41,8527,150,288,553,2605,3219,6312,4263,130425,137816,137440,139736,140766,155995,166171,177709,189662,196235,207310,210904,273614,273288,262645,438580,441472,427553,428588,427612}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564784430, 58, 7, 60, 3, 10, '{15,385,192,352,543,2553,3104,4922,6996,130792,139007,137496,139736,140705,155929,166335,177879,189668,196730,208276,211849,273378,274072,264951,441646,439312,427629,429197,427627}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564785030, 68, 7, 60, 3, 10, '{44,876,183,422,651,2477,3343,5453,4869,131041,138168,137622,139732,140811,155944,165933,177702,189490,196950,208013,211294,272997,274145,263781,438394,439251,427840,428616,427756}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564785630, 47, 7, 60, 3, 10, '{39,562,337,275,453,2447,3373,6264,5109,130172,137831,137563,139411,140685,155886,166374,177874,189521,197534,208327,212198,273882,274617,265706,439825,440373,428171,430576,428051}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564786230, 53, 4, 60, 3, 10, '{41,491,316,606,10425,2544,3251,7378,4311,131170,138392,137529,139501,140371,156000,165768,177380,189451,197157,208340,211392,272794,274249,265020,429846,431379,442295,429379,430104,429426}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564786830, 68, 7, 60, 3, 10, '{33,543,215,1648,1593,4210,4614,8033,6119,131407,141041,138687,141103,141804,157097,167985,179199,191790,198533,209980,212957,274848,276193,264080,440366,440008,427435,429546,429706}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564787430, 47, 7, 60, 3, 10, '{45,541,304,346,422,2197,3030,6591,5221,131116,138282,137278,139607,140752,155914,165686,177920,189082,196827,207786,211690,272708,274755,263494,438643,441282,427964,430177,427827}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564788030, 55, 7, 60, 3, 10, '{36,481,230,434,640,2390,2999,7290,6687,131288,138420,138268,139795,140399,156054,166197,177988,189677,197222,208134,211702,274500,273826,264642,442089,439949,428371,429991,428195}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564788630, 56, 7, 60, 3, 10, '{45,505,304,305,451,2500,3051,4592,3946,131468,138801,137800,139868,140636,155422,165949,177748,189641,196311,207328,211199,273657,273450,264995,439325,438800,427715,431308,427552}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564789230, 43, 4, 60, 3, 10, '{45,6649,162,329,414,2356,3508,4847,4013,131344,139441,137969,139739,140574,156038,166269,177737,189730,196437,207900,210786,273588,273383,264023,431011,431900,440876,429339,434045,429495}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564789830, 58, 7, 60, 3, 10, '{40,3227,385,346,737,2726,3086,6683,4857,130121,139118,137211,139708,140368,155669,166195,177774,189474,197142,208709,212107,273328,274391,263399,441015,440089,427958,429321,428565}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564790430, 45, 4, 60, 3, 10, '{50,482,255,624,2430,3791,4484,8575,8046,131964,140051,139138,140720,141631,157213,167697,178713,190987,198508,209333,213168,275043,275550,263845,433014,433109,442238,429358,431396,429773}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564791030, 62, 4, 60, 3, 10, '{46,1197,170,289,565,3979,3285,5323,5825,131315,138082,137313,139655,140342,155840,166067,177533,189507,197776,208794,211846,274035,274895,264846,432685,437624,441621,429777,431030,430126}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564791630, 74, 4, 60, 3, 10, '{44,699,313,418,702,2707,26611,5647,6022,130118,137511,137208,139588,140393,155613,166430,177938,189666,195929,207526,210870,273696,273361,263855,430581,432150,440707,429071,430001,429530}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564792230, 55, 7, 60, 3, 10, '{41,500,346,336,521,2578,3305,7411,5942,130457,138500,137677,139387,140232,155698,166164,177833,190039,195982,207489,210933,273408,273374,265680,439722,438936,427600,437667,427950}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564792830, 64, 7, 60, 3, 10, '{53,8504,307,590,950,2478,3142,6771,6025,131035,138288,184028,139694,140141,156296,166094,177812,189676,197305,208179,211645,272655,275553,262526,441950,439227,427811,430107,427728}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564793430, 43, 4, 60, 3, 10, '{46,336,494,628,568,2303,3123,4491,6284,131412,138098,182154,139620,140630,155503,166349,178016,189710,196186,206995,210772,273606,273650,262189,429667,429383,440842,429364,431131,429436}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564794030, 70, 4, 60, 3, 10, '{42,549,526,2362,2619,3997,22170,5918,4848,130436,138844,138912,141599,141815,157179,167697,179494,190966,197707,209515,212494,275646,274073,275921,432273,431381,445135,429882,432612,431870}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564794630, 47, 7, 60, 3, 10, '{35,482,298,437,671,2306,6397,7129,4968,131378,139215,137646,139358,140337,155705,166082,177530,189605,196874,207969,211516,272910,273890,265419,442246,439206,429948,429165,427592}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564795230, 70, 4, 60, 3, 10, '{34,393,369,371,614,2525,3336,5851,4044,130448,138172,138695,139897,140326,156160,165859,177322,189423,196541,207434,211064,273744,273312,265604,430413,431192,440558,429232,431765,429096}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564795830, 62, 4, 60, 3, 10, '{14,235,162,282,2756,2282,3112,5264,7039,129786,139239,137634,139607,140237,156274,166412,177789,189788,196388,207996,211553,273371,273843,263307,432774,431368,441206,429290,430187,429107}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564796430, 51, 7, 60, 3, 10, '{62,364,509,601,641,2601,3189,6916,5133,129723,137913,139084,139654,140824,155810,165812,177768,189767,196540,208185,211779,274496,273826,273236,442223,439778,428113,429605,428527}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564797030, 43, 4, 60, 3, 10, '{13,208,263,509,537,2594,3497,5424,3818,130414,139208,157083,139579,140374,155736,166211,177562,189909,196343,207530,210812,273900,274725,264650,429236,431883,440562,429266,429971,429335}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564797630, 53, 4, 60, 3, 10, '{45,1174,362,1548,1832,3855,4459,6479,5455,132799,140241,138681,141406,141955,157588,168071,179021,190918,198158,209413,213417,274214,275720,263837,433298,430898,444356,429502,431696,430730}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564798230, 70, 4, 60, 3, 10, '{39,248,222,329,609,2762,3449,5731,3729,130061,138429,137708,139771,140509,155797,166261,177641,189859,196120,207459,210958,273601,273287,264192,432078,431567,440643,428988,430668,429253}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564798830, 59, 4, 60, 3, 10, '{43,152060,179,260,692,2304,36300,6816,6265,129540,139507,137351,139810,140309,155562,165637,177707,189620,197188,208624,212201,273908,274736,273064,433253,430827,441773,429901,432421,430098}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564799430, 56, 7, 60, 3, 10, '{47,345,351,314,379,2434,3152,6861,7439,131389,137270,137420,140029,140572,155774,166092,183114,189532,197035,208212,211558,274461,274039,264370,440487,439657,428277,430385,428249}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564800030, 49, 7, 60, 3, 10, '{43,490,344,499,554,2393,3200,6202,5755,129465,138404,138581,139401,140349,155508,166279,177526,190074,196474,207872,210973,273661,273551,262375,439012,439254,427617,429734,427612}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564800630, 49, 7, 60, 3, 10, '{47,501,337,422,700,2624,3154,7884,5952,129782,139292,137617,139369,140632,155454,165921,178020,189645,196137,207240,211199,273745,273342,265574,438106,439464,427604,430376,427321}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564801230, 43, 4, 60, 3, 10, '{44,559,539,2418,2220,4257,17340,8683,5924,131537,140839,139266,140585,141408,157821,167703,179356,191165,197890,208585,212277,275175,274826,264394,430885,432105,442858,429430,431752,430566}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564801830, 64, 7, 60, 3, 10, '{46,699,306,637,12476,2626,3400,5775,4207,130512,137787,137252,139807,140535,156156,166355,178073,189317,197260,208652,212174,273796,274445,265870,439394,442248,428441,429958,428313}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564802430, 68, 7, 60, 3, 10, '{47,348,208,311,597,2334,3236,4706,4654,129939,137504,137306,139965,140465,156020,166076,177547,189861,196885,207958,211285,272634,274128,263836,438954,439032,427674,428883,427595}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564803030, 70, 4, 60, 3, 10, '{41,339,215,330,731,2868,3318,6137,3749,130084,137569,137213,139848,139971,155523,166132,177841,190090,197234,208353,211120,274097,273859,269326,431138,431932,441478,429839,431094,429984}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564803630, 62, 4, 60, 3, 10, '{39,253,175,276,445,2257,3061,5259,4783,131072,137502,138775,139881,140624,155695,166075,178154,189747,197440,208893,212648,273409,274705,266170,431906,431468,441617,623426,431560,429679}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564804230, 49, 7, 60, 3, 10, '{13,372,387,446,747,2727,3340,6705,3626,129611,138701,137228,139751,140494,155806,166291,177711,189284,196074,207512,210789,273662,273246,265309,441352,441263,427559,430102,427497}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564804830, 47, 7, 60, 3, 10, '{47,4853,523,2196,2587,4481,4690,6114,6118,132113,139031,140535,141062,141707,157766,168040,179095,191264,197899,208678,213562,274963,279316,263766,443618,440722,427365,429678,429062}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564805430, 43, 4, 60, 3, 10, '{37,399,366,512,730,2363,3312,7724,7169,131583,137766,140472,139635,140833,155779,165868,177876,190015,196561,207340,210812,273545,273445,265972,430285,431654,440784,459859,431145,429152}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564806030, 47, 7, 60, 3, 10, '{42,242,313,593,797,3787,3280,6814,4993,129757,139618,137472,139713,140063,155683,165964,177937,189665,196703,207694,211220,272949,274482,265090,439966,439032,427613,428876,427657}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564806630, 51, 7, 60, 3, 10, '{53,399,341,605,502,2306,3427,4731,6149,131572,138757,137417,139745,140881,155859,165865,177726,189515,197377,207827,211645,274505,274113,263916,439574,439935,428027,435702,428320}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564807230, 58, 7, 60, 3, 10, '{40,398,376,580,521,2672,3823,6329,6656,130149,139002,169274,139868,140383,155951,165984,178103,189293,197256,208432,212431,273360,274691,265112,441149,439815,428329,430002,428405}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564807830, 70, 4, 60, 3, 10, '{40,250,164,304,469,3069,3358,6321,7591,129974,138704,138834,139496,140854,156142,166447,177712,189716,196215,207130,210858,273864,273430,279230,432652,432069,440312,429913,434574,429167}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564808430, 68, 7, 60, 3, 10, '{32,465,355,2017,2422,3924,4797,7780,7074,132100,140479,138554,141377,142057,157347,167413,179168,190372,199009,210307,213558,275759,276051,266460,442566,441268,428487,429888,429210}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564809030, 51, 7, 60, 3, 10, '{35,234,220,333,740,2610,21848,6383,7090,129618,139025,137776,139677,140258,155653,166448,177860,189939,196907,208288,211437,274531,273696,264381,441817,439648,428110,429184,428488}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564809630, 66, 4, 60, 3, 10, '{44,47168,314,438,675,2571,11897,6587,5690,129458,138267,137618,140166,140226,156019,166334,177421,189421,197156,208481,211109,274867,274059,265477,430605,431575,441641,430042,432131,430043}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564810230, 45, 4, 60, 3, 10, '{45,539,306,375,880,2721,3223,5440,5979,129929,138156,137543,139721,140319,155933,166262,177666,189767,196988,208068,211589,272981,274183,263408,429209,431910,441028,429379,431176,429494}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564810830, 56, 7, 60, 3, 10, '{45,4630,308,623,655,2291,3096,10101,5082,131161,137710,137641,139695,140625,156004,166245,177704,189742,196440,207236,210799,274120,273413,265694,440641,438736,427656,428935,427775}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564811430, 49, 7, 60, 3, 10, '{40,363,300,368,700,2422,3072,7533,5204,136461,139392,137329,139273,140471,156046,165983,177940,189904,196600,207406,210908,273679,273220,263528,439958,439265,427885,438708,427808}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564812030, 56, 7, 60, 3, 10, '{41,316,571,2003,26001,3895,4810,7337,6888,131489,139025,139716,140835,141574,156175,167559,179364,191308,197994,208569,212166,275430,274807,266653,442606,440994,427693,428759,428920}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564812630, 66, 4, 60, 3, 10, '{39,247,369,589,833,2508,3045,7105,6599,130650,137832,137509,139753,140453,155772,166341,177517,189826,196225,207116,210650,273605,273251,265111,430368,431889,441006,429595,430903,429568}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564813230, 59, 4, 60, 3, 10, '{42,502,339,602,441,2388,2964,5399,5484,131494,137913,138699,139953,140665,155657,166008,177571,189331,196507,207922,211685,272696,274238,263841,430960,437387,440843,429537,431426,429557}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564813830, 59, 4, 60, 3, 10, '{55,418,377,590,1035,2273,3234,5383,4143,130432,138246,137824,139356,140648,156159,166314,177755,189705,196732,207883,211166,273263,274010,265628,430275,432082,441003,429866,430971,429404}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564814430, 45, 4, 60, 3, 10, '{45,510,512,631,3155,2461,3388,6806,6511,129830,138834,137524,139310,140309,155818,165982,177612,189178,197106,208416,211633,273142,274195,277439,430449,431354,440573,429446,430098,429549}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564815030, 43, 4, 60, 3, 10, '{42,395,302,496,695,2528,3065,6079,4870,129735,139116,137439,139523,140247,155613,166013,177558,189511,196122,207402,210805,273689,273204,263392,430794,429342,440781,430558,430311,429476}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564815630, 45, 4, 60, 3, 10, '{49,557,524,1879,1811,3753,4520,5416,5177,133169,139493,139242,141231,141818,157192,167801,179458,191336,198259,209422,213003,274645,275782,263683,434144,432331,442339,429596,429818,431042}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564816230, 66, 4, 60, 3, 10, '{23,54938,128,250,417,2252,3019,7694,4142,130360,138655,137366,139991,140384,155905,166113,177502,189674,196307,207411,210816,273727,273395,281009,430000,431139,440936,429052,430889,429283}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564816830, 55, 7, 60, 3, 10, '{51,532,359,661,884,2517,3139,5572,4012,130587,139098,138768,139453,140612,155897,166336,177790,190063,196835,207933,211818,274549,273909,266967,438906,439631,428356,430298,428591}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564817430, 51, 7, 60, 3, 10, '{29,232,186,320,656,3654,3380,7624,6684,130244,138553,137887,139616,140473,155993,166544,178060,189914,196417,207469,210899,273963,273528,264093,439062,439039,427589,434572,427408}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564818030, 56, 7, 60, 3, 10, '{48,229,209,295,515,2315,3156,5653,5193,131268,137642,137158,139665,140241,155910,166216,177962,189917,197133,207799,211153,276049,274036,265639,441191,441940,427944,430848,428318}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564818630, 74, 4, 60, 3, 10, '{45,627,237,266,483,15909,3072,8383,6574,130894,138830,138704,140014,140464,156035,165885,177440,190123,196547,207928,211386,274398,273871,264891,433348,429662,441338,430035,431152,430211}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564819230, 45, 4, 60, 3, 10, '{13,251,346,2073,1972,3810,4949,10646,8528,131254,139406,140028,141227,141113,156933,166651,179414,190903,197899,209567,213330,274160,275586,263956,438426,432074,442687,429534,430725,430430}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564819830, 49, 7, 60, 3, 10, '{45,557,243,620,623,2535,3442,9486,5491,130900,137574,137434,139652,140281,155705,166419,178051,189670,196890,208307,211586,274498,274194,265024,441194,439435,428274,429410,428518}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564820430, 51, 7, 60, 3, 10, '{34,365,320,315,450,2522,2984,5831,5432,130891,137608,139331,140089,140052,155753,166191,177552,189278,195848,207386,211113,273755,273385,263828,438751,439448,427545,428327,427409}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564821030, 66, 4, 60, 3, 10, '{45,507,308,600,528,2694,3493,5854,4786,130411,137558,137938,139600,140383,155982,166518,178028,189991,195963,207344,211021,273917,272970,262715,432419,430859,440586,429315,430735,429400}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564821630, 59, 4, 60, 3, 10, '{46,2327,322,588,986,2896,3298,7025,7240,130414,138600,137354,139924,140729,155745,165439,177503,189278,196791,208196,211226,272607,274269,265441,436617,430853,441000,429258,431534,429231}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564822230, 51, 7, 60, 3, 10, '{52,7599,269,398,724,2321,3122,6593,5269,131031,137847,137188,139376,140533,155778,166165,177524,189990,196579,207545,211051,273716,273235,262559,440469,441466,427746,429376,427391}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564822830, 66, 4, 60, 3, 10, '{14,3684,184,2149,5554,3807,5037,7356,8048,130263,140239,139321,141035,142158,157550,167428,179892,190997,197735,209074,212344,275707,275098,265782,432166,431795,442793,429375,431652,430867}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564823430, 70, 4, 60, 3, 10, '{40,409,492,356,995,2398,8257,6391,6039,130735,138277,137432,139679,140570,155892,166019,177748,189545,197194,207929,211068,274484,273802,264988,433377,431953,441578,430018,432068,430115}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564824030, 74, 4, 60, 3, 10, '{45,496,170,266,494,2531,3079,5062,5009,130355,139124,137787,139724,140235,156128,165829,177627,189378,196300,207398,210497,273655,273483,262690,429566,432020,440569,428971,430355,429488}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564824630, 59, 4, 60, 3, 10, '{49,341,312,591,730,2349,3175,6740,3414,130007,138071,138736,139576,140101,156198,166028,177450,189631,197545,208452,211760,273661,274813,266517,430340,431783,441282,429733,431371,429985}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564825230, 45, 4, 60, 3, 10, '{45,492,317,608,544,3836,3432,5172,4998,129699,137914,137452,139979,140199,155886,166029,177979,189433,197618,208715,212344,273568,274360,265482,429577,431772,441077,430053,432660,429989}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564825830, 49, 7, 60, 3, 10, '{49,655,337,453,783,2403,3129,5929,6926,131450,138087,138504,139334,140411,156016,166414,177614,189548,196365,207500,210395,274125,273368,265117,441125,439445,427467,429857,427522}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564826430, 68, 7, 60, 3, 10, '{46,567,312,1838,5443,4153,4781,9598,8838,131002,139677,164715,141422,141648,157782,167676,180957,190821,198816,210398,213276,275380,275980,265867,443598,440746,428498,429063,429911}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564827030, 53, 4, 60, 3, 10, '{42,494,307,394,589,2526,3192,7300,6462,130548,138735,137180,139353,140054,155809,165967,177740,189508,197444,208966,212339,273421,274646,263576,433031,430770,441607,430182,430938,430005}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564827630, 43, 4, 60, 3, 10, '{39,235,368,519,712,2831,3546,5855,5609,129512,139374,138449,139697,140282,155783,166113,178029,189678,197138,208023,211496,274307,273705,265090,432347,429303,441494,430059,431994,430108}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564828230, 62, 4, 60, 3, 10, '{43,507,239,430,752,2374,3146,6827,5105,131573,137408,138546,139308,139989,156186,166453,177675,189099,197663,208654,212278,273540,274511,263295,430740,432587,441535,429617,432082,430064}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564828830, 53, 4, 60, 3, 10, '{39,542,301,600,664,2396,3116,5725,5779,131190,139348,137187,139388,140692,155825,166391,177860,189559,197088,208607,211774,273105,273819,264172,431924,431968,441018,429406,430383,429255}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564829430, 56, 7, 60, 3, 10, '{16,197,186,341,517,2671,3292,6561,5713,129596,138708,137300,139745,140440,156163,165943,177963,189808,197173,207694,211480,274282,274138,265503,441378,439852,428108,429675,428090}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564830030, 68, 7, 60, 3, 10, '{45,670,227,1175,2019,3688,4564,6550,7210,133092,139258,139801,141286,142480,157160,167496,179183,190969,198536,209024,212846,274105,275588,263834,449808,441013,433424,433028,429171}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564830630, 70, 4, 60, 3, 10, '{45,581,308,618,771,2301,3522,6160,6166,131347,139593,138855,139455,140429,155647,166282,177491,189868,196072,207302,210944,273768,273307,265716,430643,431269,440825,429325,431801,429578}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564831230, 56, 7, 60, 3, 10, '{42,370,304,589,710,3591,19446,5811,4206,131445,138340,140576,139656,140625,155614,166158,178065,189480,196799,208383,211691,274530,273631,263946,448000,439785,428259,431093,428321}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564831830, 70, 4, 60, 3, 10, '{46,502,307,611,652,2738,3294,5252,5599,130545,137952,137546,139763,140689,155915,166309,177352,189673,197186,208216,211686,274333,273921,263770,432974,431510,441150,430004,432356,429897}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564832430, 59, 4, 60, 3, 10, '{41,417,226,516,804,2654,3257,5262,7345,130294,137585,137708,139756,140418,155894,165888,177692,189360,196940,208202,211627,273133,274083,264504,431113,431903,440828,429075,432030,429452}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564833030, 43, 4, 60, 3, 10, '{45,503,315,356,477,2417,3397,6112,5462,130720,139058,137647,139496,140660,155729,166196,178057,189834,197378,207929,211517,274181,273942,267158,429885,432496,443414,430075,430911,430133}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564833630, 55, 7, 60, 3, 10, '{34,304,249,2093,2505,3610,4961,6760,8443,131213,140062,138928,141481,142150,156983,168604,179093,190916,198151,209408,212533,275399,275017,264650,440425,440510,427701,429817,429778}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564834230, 59, 4, 60, 3, 10, '{45,2338,304,601,973,2630,3345,4391,6003,130835,139471,137383,139866,140374,156024,166382,177634,189893,197842,208599,212428,273269,274454,264574,431558,431862,441359,430032,432141,430148}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564834830, 66, 4, 60, 3, 10, '{40,3909,195,410,536,2616,3295,6909,5545,130931,138806,136996,139487,140598,155978,166578,177924,189700,195908,207341,210814,273763,273425,265810,431807,432102,440816,429356,432045,429628}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564835430, 58, 7, 60, 3, 10, '{43,490,256,359,802,16945,3577,5966,4080,130227,138575,137367,139354,140843,155585,165829,178115,189518,197313,208709,212220,273836,275065,265996,440798,439954,428013,430817,428496}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564836030, 51, 7, 60, 3, 10, '{41,527,287,581,815,2866,3255,5244,7489,130423,139272,137667,139677,140144,156080,166606,177534,189935,196534,207181,211113,273814,273521,265772,440393,439027,427897,430060,427443}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564836630, 49, 7, 60, 3, 10, '{46,340,497,614,497,2658,3205,6879,7401,129610,139216,137519,139832,140478,155670,166146,177830,189652,196058,207119,210738,274074,273369,262069,438769,438999,427787,428949,427884}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564837230, 64, 7, 60, 3, 10, '{45,567,528,2023,3379,4508,4682,6012,6089,132052,139851,139183,141474,141613,157115,167419,179726,191282,198352,210372,213028,274835,275694,263654,442800,440806,427880,429225,431229}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564837830, 70, 4, 60, 3, 10, '{42,8492,327,581,716,2580,8661,5558,7452,130161,139081,137084,139677,140637,155767,165843,177684,189467,196571,207147,210851,273691,273300,262007,429696,431849,441138,429411,431664,430172}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564838430, 43, 4, 60, 3, 10, '{40,422,175,409,692,2733,3359,5328,4747,130245,139030,137665,139658,140189,155756,166009,177832,189533,195878,207192,210869,273762,273636,272861,430339,429235,440630,429140,431764,429364}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564839030, 62, 4, 60, 3, 10, '{39,240,209,344,708,2399,3096,5370,6494,131417,138126,137409,139813,140372,155509,165674,177451,189296,196391,208421,211495,273098,274283,271650,431861,431654,440977,429692,436858,430623}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564839630, 53, 4, 60, 3, 10, '{40,300,246,570,940,3056,3240,5511,6031,129779,138287,137587,139579,140397,155921,166108,177437,189224,197349,207893,211589,273219,274107,264208,431149,431698,441005,429354,431382,429700}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564840230, 45, 4, 60, 3, 10, '{35,104306,306,270,378,2188,3120,6402,4227,129642,139268,137705,139626,140601,155843,166009,177401,189626,197007,208162,211659,273113,274195,265069,430540,431934,440828,429533,434464,430178}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564840830, 56, 7, 60, 3, 10, '{46,551,550,1503,1658,3565,4550,6676,6659,132523,140437,150221,140771,142158,157187,167196,179378,191416,198045,209088,212726,275675,275046,267410,442440,441596,428429,430489,428922}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564841430, 56, 7, 60, 3, 10, '{40,499,220,378,528,10921,3341,4879,3835,129289,139473,137698,139703,140247,155451,166612,178022,189718,196758,208317,211322,274039,273881,266329,442230,439731,428302,429361,428583}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564842030, 66, 4, 60, 3, 10, '{14,454,190,288,707,2497,3212,5257,4073,129952,139531,137709,139962,140331,156101,166480,177918,189737,196578,207635,211612,274458,273621,265116,432542,431652,441527,430215,431639,429836}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564842630, 43, 4, 60, 3, 10, '{45,3042,410,599,805,2748,3174,7118,5673,129711,137686,137488,139723,140353,155960,166462,178164,189954,196133,207522,210907,273624,273247,264421,429786,432087,440498,429291,430353,429804}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564843230, 51, 7, 60, 3, 10, '{46,538,505,619,800,2597,3511,5637,7162,130440,139095,138642,140003,140469,155361,165871,177597,189538,197176,207528,210787,273922,273287,264815,448467,439203,427700,430610,427514}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564843830, 51, 7, 60, 3, 10, '{40,261,305,407,6725,3214,3246,7348,4462,129942,139252,137556,139339,140443,155874,166237,177935,189922,197184,208079,211192,274742,274043,265764,441916,439593,429476,430922,428153}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564844430, 55, 7, 60, 3, 10, '{40,474,286,2141,2391,5218,4847,8854,5497,131458,139165,138574,140878,142046,157052,167475,179491,192117,197547,209114,212453,275385,274603,266986,440322,440455,427543,428607,429322}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564845030, 43, 4, 60, 3, 10, '{41,1045,321,4949,7931,2660,3254,5003,7404,129783,138766,137289,139994,140009,155704,166403,178026,189591,196457,207314,210503,273659,273394,269091,431978,430775,440557,429401,431643,429507}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564845630, 74, 4, 60, 3, 10, '{43,6821,396,508,853,35414,3020,10593,7919,129435,138227,137705,139665,140540,155669,166206,177689,189740,197249,208222,211559,274586,274123,277836,432514,432951,441632,430044,431956,430140}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564846230, 51, 7, 60, 3, 10, '{46,6859,309,543,839,2410,3403,4476,4177,130912,138681,137639,139711,140362,155920,166334,178027,189636,196947,208111,211440,274363,274082,264741,442236,439533,428364,433766,428599}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564846830, 53, 4, 60, 3, 10, '{41,486,243,370,600,2518,3231,6419,4478,130925,138797,137492,139570,140792,156025,166112,177990,189587,197700,208721,212277,273885,274602,266740,430581,431747,441717,429846,431936,430056}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564847430, 53, 4, 60, 3, 10, '{45,555,312,448,550,2278,3171,6985,4330,130429,139428,137387,139332,140645,155890,165953,177651,189347,196454,208052,211883,272989,274253,264196,431559,431154,441074,429381,431346,430547}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564848030, 55, 7, 60, 3, 10, '{48,530,536,1982,1700,3936,4626,7050,7774,132630,138909,139121,141503,142323,157239,167676,178391,191395,198407,209599,212806,275994,274944,266243,441159,441069,428051,430530,429359}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564848630, 62, 4, 60, 3, 10, '{38,407,381,332,507,2588,3304,4203,5216,129568,139717,138682,139836,140348,155604,165939,177398,189445,197253,208140,211703,273194,273974,263876,430537,432017,442698,429438,432825,429710}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564849230, 59, 4, 60, 3, 10, '{44,507,334,519,826,3238,3062,6655,4453,130449,138835,137675,139781,140148,156277,166406,177875,189157,196956,207813,211516,273227,274366,265629,433196,431422,440975,429438,430270,429405}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564849830, 70, 4, 60, 3, 10, '{45,358,251,289,382,2394,3370,5832,4647,129674,138915,138878,139385,140423,155565,165911,177614,189335,195958,207112,211061,273947,273440,262911,429929,430678,440875,429313,430435,429357}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564850430, 45, 4, 60, 3, 10, '{44,529,306,320,436,2324,3645,5811,5171,129792,139311,137607,139550,140336,155956,166006,177932,189546,196817,208106,211560,273201,274100,264023,431355,430592,442684,429146,431078,429545}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564851030, 56, 7, 60, 3, 10, '{46,500,491,686,860,2333,3867,7026,5078,130922,137741,137628,139766,140250,155626,166172,177270,189596,197361,207900,211840,274669,274112,263383,444544,440157,428328,431276,428964}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564851630, 74, 4, 60, 3, 10, '{44,11286,215,403,2363,3950,5048,8249,6372,131994,139586,140696,141228,142014,157701,167277,179125,191345,198981,209870,213387,275493,275468,272519,434848,431639,442849,429947,432036,430867}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564852230, 55, 7, 60, 3, 10, '{33,197,271,418,721,2621,3211,5253,4939,129782,137253,137263,140043,140311,155903,166430,177856,189735,196878,208302,211454,274408,274100,264556,440639,439820,427974,430103,428526}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564852830, 66, 4, 60, 3, 10, '{45,538,348,595,419,2207,3237,5458,5717,129514,138552,139098,139779,140151,156162,166514,177785,189523,196529,208402,211340,274720,273911,263777,430598,429830,441495,429952,432689,430190}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564853430, 64, 7, 60, 3, 10, '{49,515,309,597,824,3350,3186,7833,6506,130645,137502,137204,139295,140411,156038,166176,177544,189586,197583,208792,212136,273921,274669,266749,442687,439478,428300,430756,428348}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564854030, 70, 4, 60, 3, 10, '{39,237,214,526,538,2394,3163,5044,7608,129729,138381,137262,139819,140461,155584,166257,177812,189882,196425,207660,210549,273881,273541,264564,432849,429923,440908,429457,430242,429477}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564854630, 47, 7, 60, 3, 10, '{15,667,166,264,360,2371,3155,7094,5218,130614,137493,137704,139749,140290,155694,165669,178036,189264,197440,208521,211801,273723,275041,263025,440846,439935,457258,430271,428446}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564855230, 66, 4, 60, 3, 10, '{39,544,388,1935,2306,3749,4537,6661,6191,132640,139140,139025,141533,142248,156945,167887,179643,191368,197530,209157,212534,275243,275240,265094,431522,433188,442376,429459,431162,430782}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564855830, 68, 7, 60, 3, 10, '{46,534,330,397,616,2835,3248,5415,6739,131197,138781,137697,139444,140207,155846,166272,177892,189492,197566,208976,211791,273975,274659,265169,442197,439553,428320,444822,428505}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564856430, 47, 7, 60, 3, 10, '{28,486,216,428,567,2460,5749,4800,5506,130330,137758,138836,139691,140553,155450,165664,177604,189592,197806,208745,212096,273763,274815,273843,440654,439876,428204,430087,429320}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564857030, 66, 4, 60, 3, 10, '{43,6077,305,447,671,2438,3355,4998,6872,130286,139124,137561,139802,140451,155997,166203,177964,189848,196873,208137,211275,274114,274941,263686,431802,432349,441713,430050,431111,430095}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564857630, 70, 4, 60, 3, 10, '{13,215,178,283,456,2301,2999,5409,6088,130188,139635,137085,139693,140340,156069,165968,177702,189896,196195,207471,211145,273714,274319,265260,442143,431786,440943,429325,431662,429169}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564858230, 47, 7, 60, 3, 10, '{48,464,301,616,19149,2584,3370,8939,6931,130967,138152,137624,139751,140652,155739,166298,177992,189436,196925,208222,211618,272859,274772,263505,442384,439426,427863,429051,427561}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564858830, 56, 7, 60, 3, 10, '{38,334,346,2115,2820,9271,4802,7410,7660,131048,140101,138908,141156,141538,156920,167588,179646,191469,197557,209167,212054,275135,274237,266194,443929,440981,427797,428890,429155}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564859430, 62, 4, 60, 3, 10, '{38,487,484,576,616,2471,3142,10599,5226,130156,138886,137275,139553,140107,155997,166167,177791,189081,197229,208654,212193,273957,274609,266538,433626,431502,441560,430021,432532,429977}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564860030, 49, 7, 60, 3, 10, '{38,338,213,474,507,12089,3137,5931,3916,130527,138488,137452,139515,140615,155764,165944,178100,190008,196190,207693,210405,273801,273478,263004,439174,438971,427610,428311,427953}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564860630, 70, 4, 60, 3, 10, '{44,519,315,595,878,2750,3198,7393,3796,130577,138308,137707,139754,140591,156034,165849,177752,189748,196429,207580,210461,273673,273348,266017,432429,432373,441520,429346,431654,429506}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564861230, 74, 4, 60, 3, 10, '{45,527,501,583,858,2612,3303,5492,6348,129592,138811,137551,139329,140405,155768,166391,177655,190020,196185,207477,210731,274210,273402,265032,432123,432475,440684,429172,437225,429385}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564861830, 74, 4, 60, 3, 10, '{15,177,183,271,525,2441,3136,5490,6565,130667,138304,137167,139701,140224,155867,166167,177806,189810,196662,207896,211188,274562,274175,270869,432857,431694,441784,429957,431550,429728}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564862430, 64, 7, 60, 3, 10, '{45,3553,535,2128,7732,3776,4867,8121,7034,131779,141155,140177,140528,142109,157194,167611,179190,191413,198386,209154,212690,274203,275276,263718,442476,440760,427355,428741,429288}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564863030, 70, 4, 60, 3, 10, '{39,250,204,322,712,2360,3448,6637,5982,131018,138039,137419,139744,140382,155613,166161,178220,189749,195942,207435,210479,273940,273359,262836,431032,431474,441083,429016,431121,429157}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564863630, 66, 4, 60, 3, 10, '{38,690,480,623,917,2301,3691,7336,6010,130440,137700,137704,139333,140441,155616,166402,177719,189611,196642,207616,210835,273734,273445,264877,430961,431831,440925,429363,554880,429460}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564864230, 103, 7, 60, 3, 10, '{51,343,349,588,478,2544,3100,4290,3653,130126,139453,138113,139924,140401,155695,166415,177502,189593,196730,207856,211215,273141,274231,264410,432162,434047,422044,424571,422222}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564864830, 103, 7, 60, 3, 10, '{44,510,198,506,841,2747,3395,5431,5652,131364,139632,137479,139628,140351,155965,166280,177507,189390,197287,208123,211742,273185,274098,263083,427370,436056,422414,424112,422264}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564865430, 66, 4, 60, 3, 10, '{44,2582,339,366,678,2395,3388,5530,7334,130272,137107,137610,139582,140355,155390,166147,177902,189583,197255,208240,211509,274551,274085,262958,430363,432197,441529,430063,432142,430108}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564866030, 70, 4, 60, 3, 10, '{16,240,180,2126,1792,4125,4508,7198,5526,130780,140876,138890,140810,141919,157318,167592,179222,191283,198122,209041,212536,275651,274465,264453,433325,433482,441760,429466,430576,431094}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564866630, 68, 7, 60, 3, 10, '{41,491,332,618,537,2558,3333,5837,4065,130466,137657,138482,139907,140687,156085,166086,178138,189329,196580,208011,211886,273030,273770,265281,438424,439441,427719,429075,427613}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564867230, 62, 4, 60, 3, 10, '{41,68695,195,303,473,2349,3109,6951,6618,130897,138070,137657,139601,140385,156286,166346,177682,189545,197346,208186,211403,273135,275291,265294,429776,431548,440950,429285,430980,429278}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564867830, 45, 4, 60, 3, 10, '{40,961,298,604,807,2681,3296,5908,5344,129871,138516,138251,139738,140550,155644,165984,177654,189583,197539,208809,211879,273295,274527,265893,431696,431389,442539,430002,430815,430136}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564868430, 66, 4, 60, 3, 10, '{17,180,130,246,510,2316,3140,5383,6710,131499,138544,137633,139807,140419,155671,166295,178094,190008,196381,207759,210769,273542,273556,263895,430345,432040,440760,429314,432014,429246}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564869030, 53, 4, 60, 3, 10, '{45,506,339,399,779,2936,3330,5508,4999,131104,138010,137341,139656,140610,156302,165595,177706,189347,196541,208070,211983,273046,274039,264712,432891,429945,440968,429447,431757,429571}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564869630, 53, 4, 60, 3, 10, '{47,246,255,1747,1871,3928,4419,6272,8812,132579,139657,138595,141457,141866,157539,167670,178963,191172,198075,209102,213025,274802,275635,267115,431119,432110,442095,429481,430340,430406}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564870230, 49, 7, 60, 3, 10, '{45,4846,211,304,745,2347,3248,5263,6574,129701,137688,137275,139467,140415,156008,166197,177398,189801,197166,208440,211784,274422,273828,265180,440858,440056,428276,429673,428414}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564870830, 74, 4, 60, 3, 10, '{47,507,311,507,772,2524,3339,7141,3755,129523,137956,137547,140182,140240,155441,166057,178026,189676,196295,207445,211157,273954,273025,273181,429339,430128,440849,429322,431397,429401}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564871430, 66, 4, 60, 3, 10, '{44,684,305,593,724,2508,3193,5689,7092,129385,137321,138352,139698,140071,155864,166176,177813,189653,196560,207168,211181,273598,273388,262777,438064,431937,441065,429314,432156,429453}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564872030, 64, 7, 60, 3, 10, '{45,333,301,329,597,2403,3243,6610,6990,130951,137828,137303,139356,140752,155725,166227,177898,189355,197456,208370,212675,273879,274424,265177,442337,439560,428265,429942,428351}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564872630, 62, 4, 60, 3, 10, '{42,195,150,300,475,2546,2999,5434,7476,130597,139005,137635,139501,140564,156214,166265,177854,189629,197851,208792,212202,273457,274503,265346,430428,431969,441582,429718,432210,429695}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564873230, 58, 7, 60, 3, 10, '{52,288,441,1963,2077,3943,4980,7277,8760,132175,140572,138567,141226,141501,157460,168113,179249,190542,198533,209516,212951,274005,275562,266425,442257,441204,427652,430170,429448}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564873830, 53, 4, 60, 3, 10, '{51,341,338,315,591,2432,3287,5518,4487,130777,137600,137521,139692,140425,155965,166322,177774,189440,197537,209001,212213,273815,274725,265027,432978,431776,441206,430359,431598,429827}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564874430, 45, 4, 60, 3, 10, '{42,290608,311,600,30553,2555,16377,7828,7487,130834,138139,137767,139744,140337,156308,165929,177921,189812,196846,207546,211170,272906,274187,265009,431233,432193,440809,429440,430228,429523}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564875030, 53, 4, 60, 3, 10, '{28,480,281,584,8079,2761,3288,7708,5165,129360,139223,137698,139432,140274,155608,165901,178040,189463,196982,207933,211676,272589,273901,265462,431942,431301,440718,659659,431920,429595}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564875630, 64, 7, 60, 3, 10, '{45,586,178,593,839,2384,3509,5390,5849,130064,137748,137197,139280,139992,155735,165624,177445,189464,197798,208325,212551,273980,274933,266716,441893,439738,428137,430853,428410}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564876230, 47, 7, 60, 3, 10, '{39,273,250,629,795,2685,3498,6437,4796,130783,138886,137743,139547,140379,156179,166242,177701,189318,197434,208324,212269,273450,274793,263023,438741,439963,428069,430836,432339}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564876830, 58, 7, 60, 3, 10, '{44,13838,279,1663,2122,3976,4535,7222,9217,131784,140082,138112,140979,141724,157830,166825,179170,190910,198498,209840,213438,274663,275890,264578,442315,441380,669429,430339,430017}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564877430, 47, 7, 60, 3, 10, '{35,418,333,436,634,2224,3377,6466,6678,130534,138108,138045,139221,140671,155973,166356,177686,189629,197464,208977,212630,273882,274463,263156,441297,439773,428171,430429,428335}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564878030, 49, 7, 60, 3, 10, '{33,501,156,271,543,2237,3283,5924,6254,129904,139144,138555,139741,140394,156086,166229,177786,189350,197126,208155,211500,274417,274104,264019,439398,439515,428072,430972,428247}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564878630, 62, 4, 60, 3, 10, '{39,419,185,306,479,2602,3333,7373,4358,130602,139531,137476,139568,140403,155995,165755,177937,189712,196850,207809,211270,273065,274114,264888,431631,428762,440753,429109,430095,429453}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564879230, 74, 4, 60, 3, 10, '{50,340,342,325,468,2486,11674,6171,6327,130787,138815,137324,139822,140443,155863,166222,177913,189655,196770,208059,211166,274683,273854,263445,429755,431674,443089,429978,432395,429991}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564879830, 55, 7, 60, 3, 10, '{41,349,273,494,403,2733,3512,6187,6985,130900,138707,137502,139719,140501,155560,165780,177900,189602,196572,208346,211641,274543,274023,266162,442474,440218,428280,430186,428004}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564880430, 64, 7, 60, 3, 10, '{43,88660,443,2047,2484,4441,5064,9186,9464,132347,139858,139718,141239,142689,157747,167457,179330,190129,199014,210042,213650,275248,275999,263969,440841,443528,428332,429226,429792}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564881030, 43, 4, 60, 3, 10, '{30,210,220,334,572,2599,3124,4102,6801,131528,138123,137456,139635,140102,156007,166607,178032,189590,196999,208062,211362,274258,274025,264460,430232,432158,441664,430042,431515,429824}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564881630, 74, 4, 60, 3, 10, '{44,36002,220,304,719,2245,10158,4920,6738,131503,138503,137253,139595,140371,155952,166179,178001,190049,196242,207508,210730,273680,273378,263205,430682,431903,440876,429371,430736,429565}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564882230, 58, 7, 60, 3, 10, '{33,247,200,292,1482,2470,3069,5201,5626,130282,137719,137211,139549,140431,155746,165642,178134,189340,197510,208676,211854,273777,274753,265957,441786,439930,428163,429501,428308}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564882830, 66, 4, 60, 3, 10, '{44,515,312,301,556,2366,3100,5855,3838,129849,138084,137434,139711,140079,155552,166567,177523,190052,196406,207472,210855,273939,273011,265786,429310,430685,440879,429313,430886,429341}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564883430, 68, 7, 60, 3, 10, '{29,627,218,449,508,2450,3055,6638,5171,130561,139161,137515,139689,140794,156200,166226,177865,189766,196997,208243,211963,272836,274354,265109,442152,438995,427375,429227,427472}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564884030, 68, 7, 60, 3, 10, '{38,311,347,2143,2400,4485,4570,7625,6116,131963,140151,139055,141095,142545,158100,167574,179169,190607,199222,209738,213341,273810,275286,263554,442359,440586,427691,428958,428949}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564884630, 74, 4, 60, 3, 10, '{38,843,337,354,541,2381,3199,5204,6116,131269,138891,173150,139722,140335,155744,166495,177982,189612,196529,207449,210729,274068,273206,265875,430364,430049,440966,429018,432559,429429}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564885230, 55, 7, 60, 3, 10, '{42,3554,220,629,644,2399,2988,6029,6643,129969,138482,137623,140056,140256,155909,166205,177898,190066,197195,208290,211877,274532,275496,262912,440552,439685,428146,429542,428680}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564885830, 58, 7, 60, 3, 10, '{14,2965,145,387,747,2513,3360,6094,4323,129317,138339,137995,140153,140499,156154,166188,177910,189654,197405,208366,212395,273499,274719,266443,442368,439813,428224,429920,427991}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564886430, 51, 7, 60, 3, 10, '{42,689,315,414,811,2867,3143,6176,6557,130979,137494,137533,139789,140036,155746,166056,177718,189687,196494,207490,210586,273595,273345,264252,441707,439167,427641,430104,427844}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564887030, 51, 7, 60, 3, 10, '{41,489,304,577,709,2325,3203,4886,4511,129757,139329,137615,139814,140574,155661,166085,177685,189570,197283,208344,211550,274310,274051,263996,441420,439859,428017,430936,428355}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564887630, 58, 7, 60, 3, 10, '{33,448,453,1582,1452,3508,4217,6288,7136,131271,140917,138505,141076,141867,157031,167931,179146,190706,198786,209814,213724,275602,275506,265774,442721,441151,428297,431106,429748}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564888230, 74, 4, 60, 3, 10, '{38,257,300,599,619,2431,3001,5980,6529,129560,138840,163952,139455,140001,155889,166249,177535,189667,195867,207478,210876,274137,273450,263857,431251,431873,442951,429342,430291,429319}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564888830, 64, 7, 60, 3, 10, '{43,400,155,286,447,16476,3132,7164,6191,129789,138156,137402,139690,140496,156067,165884,177488,189576,197552,208934,212651,273894,274515,276611,442703,442099,428440,429188,428316}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564889430, 43, 4, 60, 3, 10, '{41,515,303,325,482,2448,5587,6193,4484,131436,137697,137849,139722,139934,156019,166316,177552,189922,196960,207907,211466,274473,273948,263266,433565,431290,441145,430084,431513,430259}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564890030, 43, 4, 60, 3, 10, '{45,534,308,345,493,6400,3412,5124,5566,129602,138869,138342,139649,140341,156160,166142,177686,189612,196289,207114,210889,274064,273431,263022,429063,431321,443539,429313,430129,429304}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564890630, 58, 7, 60, 3, 10, '{35,57348,293,587,983,2517,4555,6930,5810,130007,138733,137430,139785,140428,156340,165786,178127,189157,196803,208077,211229,272904,273860,262911,439626,439181,427668,430394,427623}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564891230, 43, 4, 60, 3, 10, '{44,1573,455,2234,2373,3740,25736,9438,7211,131717,139745,138631,141044,141704,157105,167833,179194,191323,198309,209660,212834,275624,275866,266760,439239,432199,443522,433188,431884,431768}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564891830, 70, 4, 60, 3, 10, '{53,341,339,338,431,2520,3078,6396,4726,129897,137745,137223,139593,140710,155977,166021,177578,189391,197030,208252,211844,274168,273987,264328,433682,431243,441439,430117,432520,430040}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564892430, 55, 7, 60, 3, 10, '{41,2032,302,579,2185,2213,3115,6184,5698,129641,137710,137351,139742,140391,155978,166391,177855,189761,196235,207363,211668,273760,273678,262412,441012,438934,427332,434907,427822}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564893030, 68, 7, 60, 3, 10, '{46,541,490,589,897,2244,2990,7371,6710,131185,139242,137359,139350,140266,155786,165955,177452,189506,197396,208661,212323,273873,274772,266893,438726,439826,427899,430201,428400}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564893630, 62, 4, 60, 3, 10, '{44,7769,372,497,4101,2461,3151,7619,5810,131593,138866,137506,139769,140426,155824,166402,177934,189765,197553,209238,212226,273840,274787,265592,438055,432361,441516,577537,430793,430962}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564894230, 43, 4, 60, 3, 10, '{44,489,240,405,547,2398,3164,5498,4960,131378,137482,137411,139677,140703,155588,166142,178166,189621,196168,207326,210837,273817,273586,263107,430921,431756,440618,429387,430683,429465}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564894830, 49, 7, 60, 3, 10, '{37,321,511,2134,2687,4584,4991,7484,5992,131991,139745,140730,141058,141961,156983,167173,179356,191261,197628,208706,212136,275490,275112,267480,442927,440989,427384,431506,429495}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564895430, 45, 4, 60, 3, 10, '{40,5803,399,592,882,2421,3439,4985,5638,130972,139447,138106,139669,140466,155911,165898,177695,189746,197690,208588,212407,274065,274470,263131,433193,430584,441365,429933,432395,433688}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564896030, 49, 7, 60, 3, 10, '{41,328,312,612,502,2483,3103,7173,4449,129891,137187,137438,140030,140050,155806,166176,177753,190068,196966,208658,211268,274217,273724,266556,439773,439564,428205,429573,428566}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564896630, 68, 7, 60, 3, 10, '{44,519,307,420,410,2494,3182,5512,4138,130994,138241,137419,139554,140246,155851,165691,177393,189504,196755,208056,211476,272947,274184,266596,440079,439235,427334,434340,427661}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564897230, 74, 4, 60, 3, 10, '{17,155,143,265,648,2643,3482,7939,5332,129471,139550,137441,139605,140001,155901,165790,177380,189587,196303,207382,211172,273703,273546,266330,428858,433673,441039,429613,430363,429086}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564897830, 66, 4, 60, 3, 10, '{52,355,160,282,345,2358,3246,5114,5762,130454,138089,137380,139474,140226,155977,166032,177545,189598,196542,207467,210487,273718,272991,263039,431299,432502,440517,429024,430835,429259}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564898430, 68, 7, 60, 3, 10, '{32,22287,215,1569,1673,3877,5490,7141,6634,131533,138286,138867,141066,142130,157662,167332,179184,191113,198703,209619,214182,275019,276186,266569,439841,441087,428032,430593,429323}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564899030, 45, 4, 60, 3, 10, '{44,446,303,509,713,15077,3339,6898,5406,129638,138009,137446,139514,140731,156233,166406,177605,189844,197081,207967,211217,275657,273925,262522,430886,429864,440967,429369,430495,429368}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564899630, 58, 7, 60, 3, 10, '{13,394,321,575,576,2339,3122,5498,6974,130066,138088,137222,139450,146704,155931,165895,178058,189538,197489,208724,212428,273567,274420,263227,439402,440031,428271,429958,428136}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564900230, 47, 7, 60, 3, 10, '{47,468,217,490,765,2529,3381,6550,7629,130049,137634,138497,139920,140319,156161,165866,177585,189437,197009,207805,211617,273036,274244,262183,441310,439389,427326,430123,427531}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564900830, 64, 7, 60, 3, 10, '{39,481,234,520,797,2718,3198,6109,6870,130138,138877,137522,139789,140340,156061,165827,177510,189487,197755,208687,212598,273793,274710,270375,439782,445593,428400,429998,428545}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564901430, 47, 7, 60, 3, 10, '{48,357,324,605,577,2634,3414,6769,4013,130191,138089,137388,139520,140383,155977,165792,177352,189649,196623,208603,211514,273286,274187,263799,439820,439127,427825,429675,427729}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564902030, 43, 4, 60, 3, 10, '{14,188,178,1939,2063,4096,5226,6526,8344,131050,139910,179217,140788,142295,157105,167745,179211,190968,197844,208884,211993,275412,274339,267388,433904,431409,442168,429361,430587,429176}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564902630, 58, 7, 60, 3, 10, '{12,189,231,409,440,2510,3314,5396,4178,129755,137832,137272,139610,140833,155891,166244,177673,189306,197828,208686,212097,273681,274672,265362,439342,439697,427936,431641,428217}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564903230, 49, 7, 60, 3, 10, '{40,430,220,516,714,2451,3049,6350,5036,131026,138664,138557,140015,140308,155879,165907,177965,189738,196455,207440,210847,273985,273007,263353,439271,438982,427633,428920,427830}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564903830, 49, 7, 60, 3, 10, '{14,180,110,241,499,2490,3436,4995,5777,131640,138191,137180,139686,140523,155666,166047,177731,190093,196969,208091,211476,274298,273663,263329,440773,439694,428259,430570,427978}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564904430, 47, 7, 60, 3, 10, '{34,243,152,311,517,2349,27887,7036,6654,130804,137965,137221,139590,140350,155766,165594,177774,189484,196500,208044,211748,272636,274175,265006,442339,439177,427721,432243,427365}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564905030, 45, 4, 60, 3, 10, '{41,360,306,369,793,2501,3354,4819,8014,129765,137879,137507,139707,140380,155954,166183,178201,189341,196593,208209,211725,273346,273990,264803,431328,431712,440891,429658,431814,429535}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564905630, 68, 7, 60, 3, 10, '{52,646,542,1530,1603,3720,4357,7534,6400,131789,140894,139820,140933,141198,157437,166964,178954,190952,198752,210179,212780,274402,275377,264409,440177,440268,427540,434809,429203}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564906230, 59, 4, 60, 3, 10, '{39,398,244,285,678,2263,3144,5328,4311,130402,138721,137461,139621,140351,155963,165944,177881,189661,196885,208001,211530,273323,274287,264938,433198,431774,440884,429183,431258,429053}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564906830, 47, 7, 60, 3, 10, '{42,505,339,341,742,2590,3151,7798,7873,131562,137783,136992,139602,140084,155815,165723,178028,189521,197155,208343,211483,273302,274194,265901,440375,440319,427588,428925,427652}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564907430, 47, 7, 60, 3, 10, '{37,489,292,588,833,2419,3438,7496,3725,129998,139009,138393,139676,140524,156161,166077,177911,189135,196878,208161,211771,273228,273986,262437,441080,439306,427658,429759,427565}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564908030, 66, 4, 60, 3, 10, '{25,272,178,344,2786,2441,3048,6065,5918,130034,139203,137382,139636,140381,155987,166273,177804,189689,196313,207408,210557,273708,273380,262851,431879,429883,440698,429413,430480,429105}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564908630, 68, 7, 60, 3, 10, '{45,496,306,603,740,2400,3189,7288,6206,129889,137189,137335,140055,140628,156241,166229,177862,189708,196969,207719,211735,272586,274046,265893,438937,439292,427626,431451,427480}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564909230, 70, 4, 60, 3, 10, '{43,28068,364,2050,2663,3699,4072,7579,7774,132659,140690,140314,141404,141644,156982,167957,180047,190729,197458,209283,212074,275226,274557,264784,431500,431646,442630,429340,431761,430653}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564909830, 66, 4, 60, 3, 10, '{41,323,328,349,735,2524,3496,7553,7725,130394,138420,137590,139551,140380,155525,166156,177689,189892,196048,207174,210557,273785,273110,263556,432118,428910,440833,428906,431036,428977}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564910430, 68, 7, 60, 3, 10, '{14,463,165,372,2689,2285,3049,5297,6723,129770,139436,162458,139687,140845,156011,165928,178135,189441,197168,209017,212322,273544,274845,266506,442322,442373,437787,432284,428186}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564911030, 56, 7, 60, 3, 10, '{46,583,496,481,543,2738,4242,6840,4154,130236,138636,137627,139628,140368,155489,166393,178098,190026,196914,207733,211402,274428,274216,263801,441042,439894,428292,430210,428244}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564911630, 47, 7, 60, 3, 10, '{36,246,249,439,630,2396,3238,5303,5635,129430,139084,137477,139733,140397,155863,166280,178162,189299,196631,207736,211580,273197,273979,265028,438896,439202,427358,429074,427719}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564912230, 51, 7, 60, 3, 10, '{45,2935,504,587,861,2582,3273,8212,6411,130315,137650,137562,139457,140611,155534,166059,177413,189680,197087,208217,211588,274871,273792,264457,440875,439718,428231,429355,428371}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564912830, 53, 4, 60, 3, 10, '{38,1050,240,1850,1841,3788,4545,6545,5061,131353,139457,140379,141235,142213,157215,167469,179278,191240,198979,209805,213352,274620,275303,266426,434086,431664,442294,429171,431425,431180}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564913430, 66, 4, 60, 3, 10, '{39,625,287,293,578,2542,3196,5251,5378,131566,138642,137544,139883,140438,155932,166280,177773,189757,196086,207515,210989,274299,273489,265202,431139,430659,441025,429401,429853,429175}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564914030, 59, 4, 60, 3, 10, '{41,602,309,639,656,3522,3312,4912,5564,130928,137386,138398,139579,140399,156221,165968,177992,189742,196910,208057,211506,272650,275395,262936,429599,430954,440944,431220,434255,429442}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564914630, 74, 4, 60, 3, 10, '{40,3235,516,318,8864,2421,3243,7982,4916,129919,137530,137620,139702,140744,155668,166305,177967,189772,196868,208131,211365,274849,274095,263151,433125,432213,441605,429618,445446,429966}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564915230, 43, 4, 60, 3, 10, '{45,345,311,658,781,2561,3068,7735,6401,130440,137716,138591,139375,139986,155868,166491,178200,189738,196289,207325,210849,273544,273423,264231,432861,429044,440947,429048,430854,429662}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564915830, 105, 7, 60, 3, 10, '{32,224,219,481,3946,2383,3282,7106,6256,130429,137673,153795,139772,140495,156052,166134,177576,189476,196650,207115,210696,273419,273001,264726,438409,434831,427255,431435,427270}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564916430, 56, 7, 60, 3, 10, '{46,562,449,2250,1311,3241,4407,5946,6186,131443,140292,138153,140815,141505,157087,167933,178983,191286,197798,208366,211911,275190,275092,267019,451971,436343,427638,432294,429254}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564917030, 105, 7, 60, 3, 10, '{45,3565,144,509,849,2407,3150,4175,6260,131065,137794,137544,139996,140523,155751,166217,177803,189732,196658,208042,211762,274372,273905,262732,442373,435049,428098,431673,428230}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564917630, 106, 7, 60, 3, 10, '{46,343,171,304,485,2218,3163,7063,3836,130761,138449,137223,139218,140742,156078,166317,177839,189696,196594,207809,210523,273739,273408,263790,440702,434604,427263,431578,427811}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564918230, 64, 7, 60, 3, 10, '{45,556,354,325,666,2295,21767,7043,3610,130600,139211,139806,139449,140377,155904,166052,177567,189625,197552,208882,212649,273472,274442,263188,439640,435840,428155,431675,428358}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564918830, 107, 7, 60, 3, 10, '{45,328,310,2242,732,7098,3126,5826,4905,129688,138092,138005,139548,140750,155917,166564,177818,189576,196569,207630,210822,273629,273428,265244,441051,434694,427781,430227,427781}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564919430, 56, 7, 60, 3, 10, '{44,505,311,524,596,2504,6604,4949,4785,129461,139085,137532,139671,140650,155685,166324,177761,189517,196162,207362,210824,273578,273462,264796,438412,435373,427744,449756,427586}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564920030, 58, 7, 60, 3, 10, '{44,545,338,1706,1759,3329,4360,6019,8065,130769,139300,138483,141471,141957,157009,168259,179336,191261,199381,210851,214080,273407,276504,268190,442469,436505,428241,431531,430029}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564920630, 68, 7, 60, 3, 10, '{45,2866,251,289,622,2659,3275,6997,5798,129696,138327,137206,139353,139888,156289,166323,177964,189223,197586,208742,212297,273647,274737,264631,439339,435320,428301,434055,428321}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564921230, 108, 7, 60, 3, 10, '{43,349,316,500,1026,2633,3267,4683,4918,130839,137782,137564,139281,140203,155929,166338,177668,189624,197598,209013,212362,273658,274779,262465,439303,435473,428224,430716,428409}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564921830, 58, 7, 60, 3, 10, '{35,398,302,594,751,2596,3133,5916,5045,129660,137806,137250,139634,140766,156183,165859,177666,189603,197819,208781,212422,273769,274643,266299,442633,435551,428029,431759,428211}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564922430, 68, 7, 60, 3, 10, '{45,519,305,602,733,2758,3301,8613,8831,130467,137315,138962,139537,140481,155747,165672,177972,189789,196583,208233,211462,272568,274016,262919,438651,434567,427704,431082,427374}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564923030, 108, 7, 60, 3, 10, '{13,412,218,396,1217,2427,3246,4954,4407,129605,138094,137533,139745,140667,155785,166010,177675,189728,196916,207962,211595,272558,274294,273738,438877,434780,427308,434036,427669}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564923630, 109, 7, 60, 3, 10, '{51,444,297,1631,2161,3364,4451,7439,5488,132102,140406,138532,141491,142235,156792,167688,179278,191445,198885,209842,212900,274975,276067,267456,440292,436101,429904,432250,429469}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564924230, 110, 7, 60, 3, 10, '{41,515,297,596,729,2520,3266,5260,6027,130296,137999,137218,139636,140343,156262,166272,177551,189580,197553,208630,212302,273716,274409,263396,442232,435401,428679,432497,427980}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564924830, 51, 7, 60, 3, 10, '{41,539,343,317,533,2630,3343,5250,6001,130606,138705,137607,139663,140052,155767,166280,177512,190021,197077,208114,211459,273998,274092,265626,440289,435535,429405,432174,428420}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564925430, 49, 7, 60, 3, 10, '{38,557,301,606,903,2605,3254,7605,3827,130321,139262,139741,139670,140452,155727,165965,177935,189672,196173,207458,210924,273843,274650,262554,439939,434777,427602,430659,428786}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564926030, 64, 7, 60, 3, 10, '{35,407,361,536,1174,2528,3006,5241,6579,131062,138337,138795,139633,140709,156115,165665,177646,189460,197899,208760,211949,273902,274820,263280,438982,435373,428332,434073,428621}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564926630, 51, 7, 60, 3, 10, '{44,364,328,353,856,2792,3621,5961,4434,131212,139418,137644,139608,140567,156100,166380,177718,189668,196364,207772,210825,273867,273160,264104,439829,434588,427588,431411,427670}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564927230, 68, 7, 60, 3, 10, '{37,296,224,1790,2186,3894,5087,7528,7335,132076,140179,138763,140736,142948,157120,167464,179206,191390,199194,209927,213158,275310,275988,265913,442891,438816,428090,432266,429491}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564927830, 64, 7, 60, 3, 10, '{45,494,325,388,706,4370,3391,6486,4699,131071,138314,138892,139779,140533,155903,166226,177616,189469,197756,208667,212139,273958,274918,264206,439697,434975,428446,430318,428448}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564928430, 109, 7, 60, 3, 10, '{38,403,243,268,571,2562,3178,4455,4450,131236,138578,137575,139496,140715,155981,165713,177510,189087,196949,208152,211654,273111,274089,263962,439423,434679,427555,430116,427907}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564929030, 110, 7, 60, 3, 10, '{46,501,305,588,7211,3135,3575,4403,4778,129304,139238,138542,139574,140374,155868,166290,177661,189609,197497,208615,212422,273285,274649,264117,441611,435161,428541,431233,428971}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564929630, 110, 7, 60, 3, 10, '{46,124487,210,489,491,2340,3111,6386,4110,131659,139087,137401,139614,140257,155642,165961,177944,189562,197791,208518,212267,273858,274659,262962,439713,435456,428936,431914,429614}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564930230, 56, 7, 60, 3, 10, '{44,394,313,531,808,2530,3335,6965,7120,130753,138160,138571,139492,140699,155942,166433,177947,189549,196942,208397,211552,273995,273896,263539,438526,435606,428946,431084,428606}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564930830, 68, 7, 60, 3, 10, '{45,549,526,2176,2583,4489,4359,9395,7621,131684,139820,139216,140470,142156,157393,167439,179421,191022,198839,209918,213244,274462,275693,266016,443471,436684,428622,431845,429564}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564931430, 106, 7, 60, 3, 10, '{37,519,488,583,625,2733,3222,6368,3700,131644,139257,137515,139545,140652,155735,166247,177971,189836,196867,208164,211158,274255,273952,264041,441036,435415,428181,428756,428347}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564932030, 105, 7, 60, 3, 10, '{37,239,203,352,400,3631,3264,5270,5408,130944,138593,137530,140084,140713,155532,165982,177905,189845,196393,207354,210882,273634,272977,263701,438394,434896,432744,431867,427446}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564932630, 110, 7, 60, 3, 10, '{62,8888,230,488,688,2293,14073,5586,5738,130365,138126,137570,139387,140716,156216,166060,177578,189515,197258,208193,211342,272910,273955,264750,439861,434756,427328,431715,427758}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564933230, 110, 7, 60, 3, 10, '{36,188,317,583,774,2284,3115,5330,6815,130959,138333,137482,139405,140338,156071,165739,177901,189761,196869,208042,211698,273216,273966,262377,440827,434713,427675,429789,428275}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564933830, 45, 4, 60, 3, 10, '{46,290,311,302,811,2282,8478,5985,5969,129853,138280,137667,139852,139987,156007,166331,177684,189708,196568,208006,211606,273314,274243,262323,430276,431350,440608,430442,432051,430661}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564934430, 68, 7, 60, 3, 10, '{44,595,530,1985,2728,4563,5000,8885,6570,131654,139248,139295,141670,142058,157367,168143,179315,190653,198421,210023,212816,274265,275269,264175,441749,443524,429245,432570,430814}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564935030, 47, 7, 60, 3, 10, '{44,42104,372,483,898,2431,35755,7159,3669,129734,139217,137337,139810,140260,155948,165688,178036,189524,196489,208182,211907,273336,273787,266134,438389,439167,429437,428140,428529}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564935630, 62, 4, 60, 3, 10, '{53,10256,230,309,1106,2264,4424,4509,5840,130006,137799,138706,139653,140430,156039,165880,177679,189585,197808,208748,212210,273877,274700,265644,431965,432736,441635,429814,430833,430215}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564936230, 49, 7, 60, 3, 10, '{44,544,308,338,620,2619,3570,6582,5736,131226,138448,137605,139415,140328,155406,166450,177715,189636,196445,207631,210852,274007,273271,263128,442232,441222,427818,429942,427800}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564936830, 74, 4, 60, 3, 10, '{48,358,262,562,504,2259,3204,5737,7845,130101,138370,137643,139249,140346,155540,166052,177769,189814,196972,208500,211384,274673,273701,266180,433282,432286,440879,430045,432511,430115}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564937430, 62, 4, 60, 3, 10, '{16,183,128,270,418,2402,3343,6607,7395,129217,138987,137826,139738,140227,155659,166391,177512,189426,197125,208057,211495,273061,274146,263834,431971,431891,440444,429228,430300,429102}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564938030, 51, 7, 60, 3, 10, '{48,9586,540,2369,20050,4380,4281,7285,6074,131524,139663,139714,141235,142238,157610,168054,179605,190001,197759,208881,211713,275015,274801,265960,443295,440258,427703,428648,429153}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564938630, 68, 7, 60, 3, 10, '{50,549,328,1104,687,2593,3296,6024,6755,130000,137721,137256,139763,140380,156139,165941,178043,189507,200421,208749,212417,273951,274935,265706,440749,439760,428353,429989,428157}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564939230, 62, 4, 60, 3, 10, '{32,3628,190,317,548,2350,3152,6517,4519,130248,138253,137371,139718,140352,155753,165945,177868,189186,197245,207689,211265,272713,274584,262275,431522,431637,440953,429311,431229,429224}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564939830, 64, 7, 60, 3, 10, '{41,409,226,405,516,2571,3373,6854,7445,130412,138329,137630,139667,140497,156328,166474,177968,189681,196671,207707,211572,273130,274206,262907,441936,438567,428006,430244,427681}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564940430, 62, 4, 60, 3, 10, '{42,345524,181,293,567,2290,3108,6285,6842,129842,138074,137714,139337,139919,155835,166032,177814,189237,197911,208859,212220,275803,274830,264867,429880,431703,441622,429999,431244,430167}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564941030, 47, 7, 60, 3, 10, '{45,544,308,341,735,2776,3420,5840,5924,130758,138410,137550,139573,140685,156092,166358,177533,189489,196924,208322,211904,273170,274224,266246,439416,439137,427428,429590,427817}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564941630, 58, 7, 60, 3, 10, '{46,598,467,1890,2483,4229,9009,8194,8388,132703,140729,139197,140786,141386,156905,167334,179387,190835,198576,209625,212659,274531,276006,263982,442353,440455,427530,430597,428900}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564942230, 53, 4, 60, 3, 10, '{49,10858,313,598,793,2540,3517,6938,6310,129904,138048,169841,139573,140416,156047,165748,178030,189716,197594,208447,212547,274002,275005,266064,432181,432305,441513,430109,434550,429776}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564942830, 53, 4, 60, 3, 10, '{43,331,338,497,733,2772,3162,5069,4122,131122,139308,137538,139652,140283,156109,166110,177814,189425,196562,208131,211286,273008,274053,264451,432800,430495,440809,429377,440241,429407}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564943430, 55, 7, 60, 3, 10, '{43,275,399,620,22271,4716,3437,5461,5176,129925,137746,137575,139679,140616,155824,165902,177901,189755,196521,207746,211057,273998,273088,265686,440382,438934,427513,430197,427921}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564944030, 45, 4, 60, 3, 10, '{43,398,278,446,545,2344,3331,6617,8182,130892,138414,137261,139426,140512,156365,166415,177818,189672,197011,208084,211187,273340,274206,268198,429842,432229,440883,429369,431346,429455}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564944630, 64, 7, 60, 3, 10, '{53,219,260,434,976,2325,3298,5379,6163,130554,139406,139122,141102,140526,155691,166123,177988,189285,196896,207842,211582,272960,274162,263211,438950,439184,427503,431833,428766}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564945230, 66, 4, 60, 3, 10, '{47,367,295,2189,2479,5218,4909,7083,9179,132847,139141,138755,141043,141377,158104,167870,179328,191282,197692,208802,212097,275597,275230,265105,433421,430718,442535,429042,430679,430975}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564945830, 70, 4, 60, 3, 10, '{38,48384,198,337,670,2276,3097,5240,6100,129700,137404,138635,139676,140397,155756,166631,177898,189216,195832,207415,210809,273765,273468,262548,429143,432248,440846,429362,429765,429538}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564946430, 51, 7, 60, 3, 10, '{13,187,223,320,526,2434,3143,7017,6371,131344,137892,147877,139858,140339,155258,166548,177895,189475,196576,207549,210767,274031,273063,263832,441755,438751,427811,428528,427203}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564947030, 55, 7, 60, 3, 10, '{19,202,171,297,13116,2666,3439,5982,4850,130047,139307,137651,139893,140395,155769,166103,177718,189609,196501,208216,211562,274140,274046,264127,441202,441533,428214,429613,428005}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564947630, 70, 4, 60, 3, 10, '{47,9533,348,606,606,2436,3146,6196,4829,129550,137816,137496,139718,140257,155620,167537,177829,189496,196709,208053,211345,274611,274029,266456,431355,432524,441627,429738,431086,430053}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564948230, 55, 7, 60, 3, 10, '{45,520,309,331,4276,2735,3275,5526,6158,130608,138471,138498,139427,140275,155381,166317,178023,189679,196896,208085,211383,274227,274070,265673,438809,439333,427996,430233,428284}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564948830, 66, 4, 60, 3, 10, '{45,522,359,2423,2371,4202,5020,9310,8052,131293,138897,139112,141155,141548,157740,167673,179432,191232,197394,208147,211627,275007,274990,265520,430703,447202,442066,560809,431570,430382}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564949430, 45, 4, 60, 3, 10, '{16,261,161,336,10855,2410,3096,4843,3825,130582,138112,137579,139624,140343,155864,166147,177336,189534,196865,208114,211724,273338,274189,261948,432263,441973,440549,429130,430624,429153}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564950030, 47, 7, 60, 3, 10, '{44,507,309,434,579,2321,3304,16606,3961,129671,137814,137594,139557,140316,155683,165854,177789,189445,196814,208107,211556,273219,274410,262105,440974,438856,427493,432238,427754}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564950630, 64, 7, 60, 3, 10, '{44,505,407,295,467,2394,2985,18997,6306,129837,138408,137438,139723,140426,155771,166246,178151,189781,196730,208121,211869,273134,273761,265697,438571,439241,449735,428866,427619}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564951230, 56, 7, 60, 3, 10, '{45,24266,314,594,687,2750,3170,19161,4995,131662,138748,137558,139366,140253,155963,166126,177685,189995,195943,207260,210744,273266,272965,264255,439170,438788,427599,429955,427326}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564951830, 49, 7, 60, 3, 10, '{40,485,281,360,3748,2364,3230,5303,6516,130435,137991,138759,139395,140567,155909,166227,177489,189186,196813,208031,211534,274402,273695,263758,441319,439521,428616,429609,428097}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564952430, 66, 4, 60, 3, 10, '{43,692,324,2532,2470,3976,4542,8303,7652,132745,139929,138750,140773,141497,157040,167762,178807,191168,198264,209445,212869,275854,275682,266140,434013,432629,442825,430052,432299,431432}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564953030, 66, 4, 60, 3, 10, '{44,10039,300,341,639,2714,3596,7508,5917,130439,138539,137662,139364,140461,155453,166599,177506,189526,196839,208336,211834,274544,274067,265602,429868,431977,441333,430181,431659,430129}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564953630, 47, 7, 60, 3, 10, '{34,534,289,429,675,2903,3273,5105,5657,130360,139139,137349,139566,140286,156038,165947,177892,189336,197348,208449,212201,273305,274412,266893,439290,439822,428139,430325,428146}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564954230, 47, 7, 60, 3, 10, '{28,220,206,326,511,2659,3101,8664,6359,129371,137213,137251,139850,140510,155933,165880,177710,189322,196730,207990,211458,273125,273948,263554,439434,438869,427301,428905,427283}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564954830, 43, 4, 60, 3, 10, '{17,228,130,254,430,3780,3111,7802,5438,130011,137758,183494,139496,140693,155572,166187,177568,189320,195938,207420,210552,273668,273014,264609,431208,437597,440750,446358,441852,429539}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564955430, 70, 4, 60, 3, 10, '{44,15838,373,579,5303,2834,3262,5984,7861,130690,138918,137589,139890,140394,155639,166176,177606,189718,196883,208220,211511,274562,273677,264008,433838,430275,441236,429910,431734,429807}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564956030, 51, 7, 60, 3, 10, '{30,312,264,1618,1981,4429,5150,8290,6873,131011,139653,139156,141000,141196,156439,167429,179392,191219,198085,208992,212531,275664,275117,267244,442506,441148,427806,430807,429267}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564956630, 74, 4, 60, 3, 10, '{39,243,480,355,599,2405,3238,5007,6293,129627,137757,137493,139738,140414,156030,166230,177722,189939,197279,208054,211445,274365,274106,264798,432184,431844,441627,429782,431174,430121}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564957230, 45, 4, 60, 3, 10, '{44,421,345,616,857,2849,3274,6167,5826,131387,138051,137525,139787,140376,156088,166008,177847,189549,196825,207666,211643,273036,274224,263920,429381,432531,440730,429486,430960,429647}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564957830, 68, 7, 60, 3, 10, '{43,611,343,599,697,2473,2932,6424,4157,130103,139273,137587,139416,140474,156352,166433,177488,189435,197451,207762,211687,273091,274166,263426,439338,438789,427439,428622,427566}');
INSERT INTO public.data_amp_traceroute_19 VALUES (19, 1564958430, 49, 7, 60, 3, 10, '{29,413,221,351,16162,2458,3188,7418,5907,130576,138319,137444,139327,140247,155842,166175,177688,189300,196199,207141,210422,273498,273252,263191,438803,438730,427363,428040,427390}');


--
-- Data for Name: data_amp_traceroute_5; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811770, 1,  NULL, 60, NULL, NULL, '{49,345,234,410,612,2281,3845,42634,35890,36061,36197,36170}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811780, 1,  NULL, 60, NULL, NULL, '{37,73911,242,311,471,3566,4020,35617,35800,36028,36031,35955}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811790, 3,  NULL, 60, NULL, NULL, '{15,269,156,348,573,2471,3723,35888,35727,36974,36396,35946}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811800, 1,  NULL, 60, NULL, NULL, '{30,227,219,356,735,2572,3823,35812,35744,36050,36152,35930}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811810, 3,  NULL, 60, NULL, NULL, '{44,99474,203,301,430,2352,3884,35871,36505,37342,36640,36173}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811820, 3,  NULL, 60, NULL, NULL, '{15,268,188,333,652,2642,3696,35679,35733,37022,36424,36006}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811830, 4,  NULL, 60, NULL, NULL, '{34,223,190,366,674,2362,3835,35841,36764,36201,36094,36024}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811840, 4,  NULL, 60, NULL, NULL, '{36,181,167,294,9182,2545,4175,35689,35933,36046,36102,35991}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811850, 5,  NULL, 60, NULL, NULL, '{42,395,330,474,4807,2437,3929,35671,35800,37088,36043,36148}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811860, 4,  NULL, 60, NULL, NULL, '{32,7446,214,303,417,19831,3687,35693,35814,36051,36105,36023}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811870, 5,  NULL, 60, NULL, NULL, '{36,605,165,292,433,2369,3914,35823,35771,37140,36133,35619}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811880, 5,  NULL, 60, NULL, NULL, '{49,341,248,455,18813,2426,3866,35829,35739,37109,36254,35778}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811890, 6,  NULL, 60, NULL, NULL, '{40,183,190,338,412,2270,3755,35690,35711,37221,36125,35981}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811900, 6,  NULL, 60, NULL, NULL, '{26,236,183,307,670,2489,4061,45661,35878,37058,36109,36022}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811910, 7,  NULL, 60, NULL, NULL, '{48,375,183,395,520,2372,4010,35924,35949,36135,36277,35727}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811920, 7,  NULL, 60, NULL, NULL, '{39,210,225,311,396,2316,3714,35690,36978,36156,36140,35649}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811930, 7,  NULL, 60, NULL, NULL, '{49,21967,164,319,2122,4013,3751,35689,36523,36108,36086,35968}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811940, 8,  NULL, 60, NULL, NULL, '{39,312,322,448,827,2525,3810,36933,35735,36044,36741,36040}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811950, 8,  NULL, 60, NULL, NULL, '{43,254,239,369,822,2539,11369,39772,35750,36141,36717,36100}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811960, 9,  NULL, 60, NULL, NULL, '{46,453,339,310,395,2380,3809,35852,36914,37690,36097,36075}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811970, 9,  NULL, 60, NULL, NULL, '{46,333,337,475,627,2642,3941,35633,35693,37585,36186,36118}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811980, 8,  NULL, 60, NULL, NULL, '{50,433,149,289,13054,2501,3857,35643,35882,36120,36607,36140}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562811990, 9,  NULL, 60, NULL, NULL, '{32,242,219,367,604,2546,3723,35744,35800,37024,36119,36128}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812000, 10, NULL, 60, NULL, NULL, '{46,455,319,458,503,3385,3876,35698,37369,36898,36895,36132}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812010, 10, NULL, 60, NULL, NULL, '{38,322,162,304,680,2310,3817,35591,35793,37170,36972,36052}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812020, 11, NULL, 60, NULL, NULL, '{40,254,180,298,573,2505,3935,35751,35814,35985,36967,36030}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812030, 10, NULL, 60, NULL, NULL, '{39,42157,127,313,583,2310,12508,35746,35780,36916,37133,36195}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812040, 11, NULL, 60, NULL, NULL, '{31,208,214,318,411,2353,4110,35732,35774,36142,36882,36028}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812050, 11, NULL, 60, NULL, NULL, '{55,445,250,409,674,2334,50395,36046,38106,36124,36892,36134}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812060, 12, NULL, 60, NULL, NULL, '{39,239,226,363,598,2546,4066,35663,35742,37208,36980,36469}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812070, 13, NULL, 60, NULL, NULL, '{45,551,307,348,888,2472,3877,35759,35718,36273,37111,36500}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812080, 13, NULL, 60, NULL, NULL, '{49,378,315,409,679,3355,4196,35805,35899,36229,37248,36438}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812090, 12, NULL, 60, NULL, NULL, '{47,365,221,373,505,2374,3766,35632,37564,36908,36662,36338}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812100, 13, NULL, 60, NULL, NULL, '{49,537,314,458,19115,2292,3759,35802,40021,36270,37284,36552}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812110, 12, NULL, 60, NULL, NULL, '{44,6524,138,338,617,2404,4035,35682,37224,37676,37409,36508}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812120, 14, NULL, 60, NULL, NULL, '{46,232,181,354,479,2520,79362,36059,35977,37307,36422,36027}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812130, 15, NULL, 60, NULL, NULL, '{42,416,254,354,716,2451,3962,35721,38354,36412,36311,36214}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812140, 15, NULL, 60, NULL, NULL, '{48,357,314,627,734,2717,3891,35606,35776,36215,36222,36360}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812150, 15, NULL, 60, NULL, NULL, '{43,237,164,529,9959,2820,4013,35713,37171,36313,36369,36314}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812160, 16, NULL, 60, NULL, NULL, '{26,7845,218,499,616,2471,3756,35888,35803,36955,36209,36211}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812170, 16, NULL, 60, NULL, NULL, '{47,420,343,651,729,2590,35147,35818,36080,37611,36281,36321}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812180, 16, NULL, 60, NULL, NULL, '{48,207,401,313,1202,2239,3835,35862,35706,37135,36245,36327}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812190, 17, NULL, 60, NULL, NULL, '{48,524,197,416,712,2432,3770,35882,35815,37342,36033,36015}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812200, 18, NULL, 60, NULL, NULL, '{51,337,334,465,556,3210,3739,35591,35679,36051,35709,36255}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812210, 18, NULL, 60, NULL, NULL, '{38,1607,164,367,448,2390,3811,35624,35800,36040,35649,35658}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812220, 17, NULL, 60, NULL, NULL, '{44,52091,254,395,777,2633,3837,35896,35683,37335,35829,35728}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812230, 18, NULL, 60, NULL, NULL, '{19,9045,193,319,532,2337,3835,35691,35760,36092,35802,35972}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812240, 17, NULL, 60, NULL, NULL, '{47,1269,232,358,457,2579,3970,35941,35864,37065,35891,36160}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812250, 17, NULL, 60, NULL, NULL, '{12,279,139,276,568,3819,3962,35560,35732,37240,35702,35968}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812260, 14, NULL, 60, NULL, NULL, '{51,252,220,344,515,2571,3857,35772,44178,37036,36136,36054}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812270, 14, NULL, 60, NULL, NULL, '{43,80298,201,378,495,2524,3969,35728,35819,37031,36137,36047}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812280, 19, NULL, 60, NULL, NULL, '{39,259,224,360,453,2457,3777,35689,35721,36114,36165,36106}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812290, 19, NULL, 60, NULL, NULL, '{44,1631,167,316,17088,2419,3901,35669,35855,36122,36053,36012}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812300, 19, NULL, 60, NULL, NULL, '{19,203,185,333,14120,4760,3825,35657,35767,36045,36081,36000}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812310, 20, NULL, 60, NULL, NULL, '{31,182,180,341,510,NULL,NULL,NULL,NULL,36921,36663,36444}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812320, 21, NULL, 60, NULL, NULL, '{42,404,371,551,908,2413,3745,35836,35793,NULL,NULL,36813}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812330, 22, NULL, 60, NULL, NULL, '{54,360,368,594,834,2374,3792,35689,35703,36341,36033,36373}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812340, 23, NULL, 60, NULL, NULL, '{54,539,341,650,788,2565,3776,35680,35918,37116,36487,36038}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812350, 22, NULL, 60, NULL, NULL, '{54,332,142,265,437,2294,3651,38163,37979,36068,35892,35626}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812360, 23, NULL, 60, NULL, NULL, '{56,40240,233,369,439,2616,3784,35939,35757,36962,36387,35738}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812370, 22, NULL, 60, NULL, NULL, '{45,538,311,634,744,2441,3844,36934,35790,36115,36131,36152}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812380, 23, NULL, 60, NULL, NULL, '{18,206,179,324,435,8186,3775,40339,36114,36941,36204,36001}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812390, 25, NULL, 60, NULL, NULL, '{39,419,373,631,525,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812040, 24, NULL, 60, NULL, NULL, '{42,234,324,447,9454,9977,4000,35680,38628,NULL,36219,36172}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812410, 26, NULL, 60, NULL, NULL, '{34,1084,236,464,749,NULL,NULL,NULL,NULL,37146,NULL,36070}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812420, 27, NULL, 60, NULL, NULL, '{16,221,164,315,580,2508,3908,35781,35867,36376,36736,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812430, 28, NULL, 60, NULL, NULL, '{22,246,170,293,450,NULL,NULL,NULL,NULL,37331,NULL,35605}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812440, 29, NULL, 60, NULL, NULL, '{44,377,221,334,532,NULL,NULL,NULL,NULL,37129,36684,36380}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812450, 30, NULL, 60, NULL, NULL, '{31,237,255,332,637,2360,3795,35827,36035,36102,36134,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812460, 31, NULL, 60, NULL, NULL, '{49,340,229,300,602,3480,4269,35788,35868,NULL,36558,36324}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812470, 32, NULL, 60, NULL, NULL, '{17,889,159,300,10514,2417,3864,35823,36148,36359,NULL,36394}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812480, 31, NULL, 60, NULL, NULL, '{15,168,231,314,663,2401,4316,35853,35811,NULL,36105,36193}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812490, 30, NULL, 60, NULL, NULL, '{45,349,226,363,839,2701,3822,35933,35690,36044,36115,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812500, 33, NULL, 60, NULL, NULL, '{13,188,187,343,449,2360,5144,45861,35880,37184,36414,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812510, 34, NULL, 60, NULL, NULL, '{54,277,248,523,1745,2618,4016,35899,35870,36111,36433,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812520, 31, NULL, 60, NULL, NULL, '{39,270,239,344,860,2564,4021,35668,35663,NULL,36356,36197}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812530, 35, NULL, 60, NULL, NULL, '{36,182,175,296,470,2296,NULL,NULL,NULL,NULL,NULL}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812540, 36, NULL, 60, NULL, NULL, '{44,828,249,346,433,2293,NULL,NULL,NULL,5751,NULL,7433,NULL,9206,NULL,169605,170608,170068,169297}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812550, 37, NULL, 60, NULL, NULL, '{48,2955,312,317,1251,2398,NULL,NULL,NULL,NULL,37329,36140}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812560, 38, NULL, 60, NULL, NULL, '{34,234,233,404,713,2306,NULL,4217,NULL,37326,37068,36209}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812570, 39, NULL, 60, NULL, NULL, '{35,2178,231,354,583,2291,7645,4395,36032,37084,36628,36146}');
INSERT INTO public.data_amp_traceroute_5 VALUES (5, 1562812580, 30, NULL, 60, NULL, NULL, '{18,182,215,292,640,5931,4206,35984,36088,36144,36216,NULL,NULL,NULL,NULL,NULL}');


--
-- Data for Name: data_amp_traceroute_6; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811770, 2, NULL, 60, NULL, NULL, '{28,189}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811780, 2, NULL, 60, NULL, NULL, '{20,154}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811790, 2, NULL, 60, NULL, NULL, '{14,137}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811800, 2, NULL, 60, NULL, NULL, '{34,309}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811810, 2, NULL, 60, NULL, NULL, '{28,217}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811820, 2, NULL, 60, NULL, NULL, '{31,167}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811830, 2, NULL, 60, NULL, NULL, '{36,187}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811840, 2, NULL, 60, NULL, NULL, '{13,295}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811850, 2, NULL, 60, NULL, NULL, '{57,223}');
INSERT INTO public.data_amp_traceroute_6 VALUES (6, 1562811860, 2, NULL, 60, NULL, NULL, '{40,165}');

--
-- Data for Name: data_amp_traceroute_aspaths; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_aspaths_18; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (1, '{1.-2,1.681,1.-2,1.681,3.38022,1.9500,1.-1,1.9500,5.4637,1.0,1.397212,5.-1}', 22, 6, 17);
INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (3, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,4.4637,1.0,1.396563,5.-1}', 18, 6, 13);
INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (5, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,4.4637,1.0,1.396564,5.-1}', 18, 6, 13);
INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (6, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,4.4637,1.0,1.396565,5.-1}', 18, 6, 13);
INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (8, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,4.4637,1.0,1.397212,5.-1}', 18, 6, 13);
INSERT INTO public.data_amp_traceroute_aspaths_18 VALUES (9, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,6.4637,1.0,1.7342,5.-1}', 20, 6, 15);


--
-- Data for Name: data_amp_traceroute_aspaths_19; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_aspaths_19 VALUES (2, '{1.-2,1.681,1.-2,1.681,3.38022,1.9500,1.-1,1.9500,6.4637,12.174,2.3741}', 30, 6, 30);
INSERT INTO public.data_amp_traceroute_aspaths_19 VALUES (4, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,5.4637,12.174,4.3741,2.33764}', 30, 7, 30);
INSERT INTO public.data_amp_traceroute_aspaths_19 VALUES (7, '{1.-2,1.681,1.-2,1.681,1.38022,2.9500,5.4637,12.174,3.3741,2.33764}', 29, 7, 29);


--
-- Data for Name: data_amp_traceroute_aspaths_5; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_aspaths_6; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_pathlen; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_paths; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_traceroute_paths_18; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_paths_18 VALUES (40, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.254,210.7.33.255,203.109.152.33,NULL,203.118.150.21,134.159.174.37,202.84.223.42,202.84.219.126,202.84.138.82,202.40.149.177,206.126.236.170,217.30.84.127,NULL,NULL,NULL,NULL,NULL}', 22);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (42, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.143.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (44, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.144.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (46, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.145.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (48, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.144.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (50, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.145.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (52, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.144.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (54, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,217.30.84.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (57, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.145.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (61, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.143.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (63, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,217.30.84.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (65, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.143.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (67, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.145.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (69, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,209.131.143.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (71, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.40.149.177,206.126.236.170,217.30.84.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (72, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,209.131.144.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (73, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.40.149.177,206.126.236.170,217.30.84.127,NULL,NULL,NULL,NULL,NULL}', 18);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (79, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.202,202.84.143.198,202.84.247.41,198.32.176.30,209.131.156.127,NULL,NULL,NULL,NULL,NULL}', 20);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (86, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.202,202.84.143.198,202.84.247.41,198.32.176.30,209.131.156.127,NULL,NULL,NULL,NULL,NULL}', 20);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (90, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.202,202.84.143.198,202.84.247.41,198.32.176.30,209.131.156.127,NULL,NULL,NULL,NULL,NULL}', 20);
INSERT INTO public.data_amp_traceroute_paths_18 VALUES (97, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.202,202.84.143.198,202.84.247.41,198.32.176.30,209.131.156.127,NULL,NULL,NULL,NULL,NULL}', 20);


--
-- Data for Name: data_amp_traceroute_paths_19; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_paths_19 VALUES (41, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.254,210.7.33.255,203.109.152.33,NULL,203.118.150.21,134.159.174.37,202.84.223.42,202.84.219.126,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.1.162,168.209.1.162}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (43, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (45, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (47, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (49, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (51, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (53, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (55, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (56, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (58, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (59, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (62, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (64, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (66, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (68, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.134,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (70, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (74, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.1.162,168.209.1.162,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 30);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (103, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.134,168.209.201.66,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (105, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (106, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (107, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.13,154.54.44.142,154.54.42.98,154.54.31.90,154.54.44.170,154.54.7.130,154.54.29.174,154.54.0.222,154.54.82.37,154.54.57.106,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (108, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (109, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.33,202.84.223.37,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);
INSERT INTO public.data_amp_traceroute_paths_19 VALUES (110, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,134.159.174.29,202.84.223.105,202.84.138.82,202.84.143.206,202.84.247.17,154.54.11.45,154.54.43.9,154.54.44.138,154.54.41.146,154.54.5.90,154.54.42.166,154.54.6.222,154.54.26.130,66.28.4.238,154.54.82.33,154.54.56.78,149.6.148.130,168.209.100.213,168.209.1.169,196.37.155.180,196.216.3.132,196.216.2.6}', 29);


--
-- Data for Name: data_amp_traceroute_paths_5; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_paths_5 VALUES (1, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,108.170.235.195,172.217.25.174}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (3, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,108.170.235.193,172.217.25.174}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (4, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,108.170.233.195,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (5, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,108.170.233.193,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (6, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,74.125.37.155,172.217.25.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (7, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,74.125.37.201,172.217.25.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (8, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,209.85.243.243,216.58.199.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (9, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,209.85.243.145,216.58.199.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (10, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,209.85.247.127,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (11, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,209.85.247.133,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (12, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,209.85.253.177,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (13, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,209.85.253.181,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (14, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,209.85.255.175,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (15, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,209.85.254.119,216.58.200.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (16, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,209.85.250.139,216.58.200.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (17, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,108.170.232.11,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (18, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,209.85.142.137,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (19, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,209.85.255.165,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (20, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,NULL,NULL,NULL,NULL,108.170.247.33,209.85.253.177,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (21, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,NULL,NULL,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (22, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,209.85.248.253,216.58.199.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (23, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,209.85.248.173,216.58.199.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (24, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,NULL,209.85.255.175,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (25, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,NULL,NULL,NULL,NULL,NULL}', 10);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (26, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,NULL,NULL,NULL,NULL,108.170.247.49,NULL,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (27, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,209.85.247.133,NULL,NULL,NULL,NULL,NULL}', 16);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (28, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,NULL,NULL,NULL,NULL,108.170.247.49,NULL,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (29, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,NULL,NULL,NULL,NULL,108.170.247.49,209.85.247.127,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (30, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,108.170.233.195,NULL,NULL,NULL,NULL,NULL}', 16);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (31, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,NULL,108.170.233.193,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (32, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.65,NULL,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (33, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.33,209.85.255.175,NULL,NULL,NULL,NULL,NULL}', 16);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (34, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.81,74.125.37.201,NULL,NULL,NULL,NULL,NULL}', 16);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (35, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,NULL,NULL,NULL,NULL}', 11);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (36, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,NULL,NULL,210.7.33.252,NULL,210.7.33.252,NULL,210.7.33.252,NULL,216.239.50.173,108.170.247.49,209.85.247.127,172.217.167.78}', 19);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (37, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,NULL,NULL,NULL,209.85.247.133,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (38, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,210.7.33.252,NULL,108.170.247.49,209.85.247.127,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (39, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.251,209.85.148.186,108.170.247.49,209.85.247.127,172.217.167.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (60, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,108.170.247.49,209.85.247.127,NULL,NULL,NULL,NULL,NULL}', 16);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (75, '{172.17.0.1,130.217.250.59}', 2);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (76, '{172.17.0.1,130.217.248.251}', 2);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (77, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.251,210.7.33.247,210.7.33.245,206.81.80.17,108.170.245.124,172.253.71.138,108.170.230.23,216.239.50.173,108.170.247.81,209.85.142.137,216.58.196.142}', 18);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (78, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.251,210.7.33.247,210.7.33.245,206.81.80.17,108.170.245.124,172.253.71.138,108.170.230.23,216.239.50.173,216.239.46.150,108.170.247.81,209.85.142.137,216.58.196.142}', 19);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (80, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,203.109.180.151,203.109.130.2,108.170.247.33,209.85.243.145,216.58.199.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (81, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.65,209.85.243.243,216.58.199.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (82, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.33,209.85.243.145,216.58.199.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (83, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.33,108.170.235.193,172.217.25.174}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (84, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.65,108.170.235.195,172.217.25.174}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (85, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.81,209.85.142.137,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (87, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.49,108.170.232.11,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (88, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.65,209.85.253.181,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (89, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.33,209.85.253.177,172.217.167.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (91, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.81,108.170.233.195,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (92, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.49,108.170.233.193,172.217.25.46}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (93, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.65,209.85.255.165,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (94, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.33,209.85.255.175,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (95, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.33,209.85.250.139,216.58.200.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (96, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.65,209.85.254.119,216.58.200.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (98, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.49,209.85.248.173,216.58.199.78}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (99, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.49,74.125.37.155,172.217.25.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (100, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,203.109.152.30,203.109.152.29,203.109.180.151,203.109.130.2,108.170.247.81,74.125.37.201,172.217.25.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (101, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,NULL,210.7.33.248,209.85.148.186,108.170.247.33,209.85.255.175,216.58.203.110}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (102, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.248,209.85.148.186,NULL,NULL,216.58.196.142}', 12);
INSERT INTO public.data_amp_traceroute_paths_5 VALUES (104, '{172.17.0.1,130.217.248.251,10.5.5.1,130.217.2.4,210.7.39.9,210.7.33.252,210.7.33.253,210.7.33.251,209.85.148.186,108.170.247.33,209.85.253.177,172.217.167.110}', 12);


--
-- Data for Name: data_amp_traceroute_paths_6; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.data_amp_traceroute_paths_6 VALUES (2, '{172.17.0.1,130.217.250.15}', 2);


--
-- Data for Name: data_amp_udpstream; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: data_amp_youtube; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_dns; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_dns VALUES (1, 'amplet', '8.8.8.8', '8.8.8.8', '8.8.8.8', 'wand.net.nz', 'AAAA', 'IN', 4096, false, false, false);
INSERT INTO public.streams_amp_dns VALUES (2, 'amplet', '1.1.1.1', '1.1.1.1', '1.1.1.1', 'wand.net.nz', 'AAAA', 'IN', 4096, false, false, false);
INSERT INTO public.streams_amp_dns VALUES (15, 'amplet', 'ns1.dns.net.nz', 'ns1.dns.net.nz', '202.46.190.130', 'dns.net.nz', 'NS', 'IN', 4096, false, false, false);
INSERT INTO public.streams_amp_dns VALUES (16, 'amplet', 'a.root-servers.net', 'a.root-servers.net', '198.41.0.4', 'example.com', 'NS', 'IN', 4096, false, false, false);


--
-- Data for Name: streams_amp_external; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_fastping; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_http; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_http VALUES (17, 'amplet', 'https://wand.net.nz/', 24, 8, 2, 4, true, false, false);


--
-- Data for Name: streams_amp_icmp; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_icmp VALUES (3, 'amplet', 'wand.net.nz', 'ipv4', 'random');
INSERT INTO public.streams_amp_icmp VALUES (4, 'amplet', 'google.com', 'ipv4', 'random');
INSERT INTO public.streams_amp_icmp VALUES (10, 'amplet', 'wand.net.nz', 'ipv4', '84');
INSERT INTO public.streams_amp_icmp VALUES (11, 'amplet', 'cloud.google.com', 'ipv4', '84');
INSERT INTO public.streams_amp_icmp VALUES (12, 'amplet', 'www.cloudflare.com', 'ipv4', '84');
INSERT INTO public.streams_amp_icmp VALUES (13, 'amplet', 'afrinic.net', 'ipv4', '84');
INSERT INTO public.streams_amp_icmp VALUES (14, 'amplet', 'download.microsoft.com', 'ipv4', '84');


--
-- Data for Name: streams_amp_tcpping; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_tcpping VALUES (9, 'amplet', 'wand.net.nz', 443, 'ipv4', '64');


--
-- Data for Name: streams_amp_throughput; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_traceroute; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_traceroute VALUES (5, 'amplet', 'google.com', 'ipv4', '60');
INSERT INTO public.streams_amp_traceroute VALUES (6, 'amplet', 'wand.net.nz', 'ipv4', '60');
INSERT INTO public.streams_amp_traceroute VALUES (18, 'amplet', 'a.root-servers.net', 'ipv4', '60');
INSERT INTO public.streams_amp_traceroute VALUES (19, 'amplet', 'afrinic.net', 'ipv4', '60');


--
-- Data for Name: streams_amp_udpstream; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_youtube; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Name: collections_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.collections_id_seq', 12, true);


--
-- Name: data_amp_traceroute_aspaths_aspath_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.data_amp_traceroute_aspaths_aspath_id_seq', 9, true);


--
-- Name: data_amp_traceroute_paths_path_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.data_amp_traceroute_paths_path_id_seq', 110, true);


--
-- Name: streams_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.streams_id_seq', 21, true);


--
-- Name: collections collections_module_modsubtype_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_module_modsubtype_key UNIQUE (module, modsubtype);


--
-- Name: collections collections_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.collections
    ADD CONSTRAINT collections_pkey PRIMARY KEY (id);


--
-- Name: data_amp_traceroute_aspaths_18 data_amp_traceroute_aspaths_18_aspath_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_18
    ADD CONSTRAINT data_amp_traceroute_aspaths_18_aspath_key UNIQUE (aspath);


--
-- Name: data_amp_traceroute_aspaths_18 data_amp_traceroute_aspaths_18_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_18
    ADD CONSTRAINT data_amp_traceroute_aspaths_18_pkey PRIMARY KEY (aspath_id);


--
-- Name: data_amp_traceroute_aspaths_19 data_amp_traceroute_aspaths_19_aspath_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_19
    ADD CONSTRAINT data_amp_traceroute_aspaths_19_aspath_key UNIQUE (aspath);


--
-- Name: data_amp_traceroute_aspaths_19 data_amp_traceroute_aspaths_19_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_19
    ADD CONSTRAINT data_amp_traceroute_aspaths_19_pkey PRIMARY KEY (aspath_id);


--
-- Name: data_amp_traceroute_aspaths_5 data_amp_traceroute_aspaths_5_aspath_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_5
    ADD CONSTRAINT data_amp_traceroute_aspaths_5_aspath_key UNIQUE (aspath);


--
-- Name: data_amp_traceroute_aspaths_5 data_amp_traceroute_aspaths_5_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_5
    ADD CONSTRAINT data_amp_traceroute_aspaths_5_pkey PRIMARY KEY (aspath_id);


--
-- Name: data_amp_traceroute_aspaths_6 data_amp_traceroute_aspaths_6_aspath_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_6
    ADD CONSTRAINT data_amp_traceroute_aspaths_6_aspath_key UNIQUE (aspath);


--
-- Name: data_amp_traceroute_aspaths_6 data_amp_traceroute_aspaths_6_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths_6
    ADD CONSTRAINT data_amp_traceroute_aspaths_6_pkey PRIMARY KEY (aspath_id);


--
-- Name: data_amp_traceroute_aspaths data_amp_traceroute_aspaths_aspath_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths
    ADD CONSTRAINT data_amp_traceroute_aspaths_aspath_key UNIQUE (aspath);


--
-- Name: data_amp_traceroute_aspaths data_amp_traceroute_aspaths_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_aspaths
    ADD CONSTRAINT data_amp_traceroute_aspaths_pkey PRIMARY KEY (aspath_id);


--
-- Name: data_amp_traceroute_paths_18 data_amp_traceroute_paths_18_path_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_18
    ADD CONSTRAINT data_amp_traceroute_paths_18_path_key UNIQUE (path);


--
-- Name: data_amp_traceroute_paths_18 data_amp_traceroute_paths_18_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_18
    ADD CONSTRAINT data_amp_traceroute_paths_18_pkey PRIMARY KEY (path_id);


--
-- Name: data_amp_traceroute_paths_19 data_amp_traceroute_paths_19_path_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_19
    ADD CONSTRAINT data_amp_traceroute_paths_19_path_key UNIQUE (path);


--
-- Name: data_amp_traceroute_paths_19 data_amp_traceroute_paths_19_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_19
    ADD CONSTRAINT data_amp_traceroute_paths_19_pkey PRIMARY KEY (path_id);


--
-- Name: data_amp_traceroute_paths_5 data_amp_traceroute_paths_5_path_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_5
    ADD CONSTRAINT data_amp_traceroute_paths_5_path_key UNIQUE (path);


--
-- Name: data_amp_traceroute_paths_5 data_amp_traceroute_paths_5_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_5
    ADD CONSTRAINT data_amp_traceroute_paths_5_pkey PRIMARY KEY (path_id);


--
-- Name: data_amp_traceroute_paths_6 data_amp_traceroute_paths_6_path_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_6
    ADD CONSTRAINT data_amp_traceroute_paths_6_path_key UNIQUE (path);


--
-- Name: data_amp_traceroute_paths_6 data_amp_traceroute_paths_6_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths_6
    ADD CONSTRAINT data_amp_traceroute_paths_6_pkey PRIMARY KEY (path_id);


--
-- Name: data_amp_traceroute_paths data_amp_traceroute_paths_path_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths
    ADD CONSTRAINT data_amp_traceroute_paths_path_key UNIQUE (path);


--
-- Name: data_amp_traceroute_paths data_amp_traceroute_paths_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_paths
    ADD CONSTRAINT data_amp_traceroute_paths_pkey PRIMARY KEY (path_id);


--
-- Name: streams_amp_dns streams_amp_dns_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_dns
    ADD CONSTRAINT streams_amp_dns_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_dns streams_amp_dns_source_destination_query_address_query_type_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_dns
    ADD CONSTRAINT streams_amp_dns_source_destination_query_address_query_type_key UNIQUE (source, destination, query, address, query_type, query_class, udp_payload_size, recurse, dnssec, nsid, instance);


--
-- Name: streams_amp_external streams_amp_external_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_external
    ADD CONSTRAINT streams_amp_external_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_external streams_amp_external_source_destination_command_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_external
    ADD CONSTRAINT streams_amp_external_source_destination_command_key UNIQUE (source, destination, command);


--
-- Name: streams_amp_fastping streams_amp_fastping_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_fastping
    ADD CONSTRAINT streams_amp_fastping_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_fastping streams_amp_fastping_source_destination_family_packet_size__key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_fastping
    ADD CONSTRAINT streams_amp_fastping_source_destination_family_packet_size__key UNIQUE (source, destination, family, packet_size, packet_rate, packet_count, preprobe);


--
-- Name: streams_amp_http streams_amp_http_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_http
    ADD CONSTRAINT streams_amp_http_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_http streams_amp_http_source_destination_max_connections_max_con_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_http
    ADD CONSTRAINT streams_amp_http_source_destination_max_connections_max_con_key UNIQUE (source, destination, max_connections, max_connections_per_server, max_persistent_connections_per_server, pipelining_max_requests, persist, pipelining, caching);


--
-- Name: streams_amp_icmp streams_amp_icmp_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_icmp
    ADD CONSTRAINT streams_amp_icmp_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_icmp streams_amp_icmp_source_destination_packet_size_family_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_icmp
    ADD CONSTRAINT streams_amp_icmp_source_destination_packet_size_family_key UNIQUE (source, destination, packet_size, family);


--
-- Name: streams_amp_tcpping streams_amp_tcpping_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_tcpping
    ADD CONSTRAINT streams_amp_tcpping_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_tcpping streams_amp_tcpping_source_destination_port_family_packet_s_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_tcpping
    ADD CONSTRAINT streams_amp_tcpping_source_destination_port_family_packet_s_key UNIQUE (source, destination, port, family, packet_size);


--
-- Name: streams_amp_throughput streams_amp_throughput_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_throughput
    ADD CONSTRAINT streams_amp_throughput_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_throughput streams_amp_throughput_source_destination_direction_address_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_throughput
    ADD CONSTRAINT streams_amp_throughput_source_destination_direction_address_key UNIQUE (source, destination, direction, address, duration, writesize, tcpreused, protocol);


--
-- Name: streams_amp_traceroute streams_amp_traceroute_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_traceroute
    ADD CONSTRAINT streams_amp_traceroute_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_traceroute streams_amp_traceroute_source_destination_packet_size_famil_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_traceroute
    ADD CONSTRAINT streams_amp_traceroute_source_destination_packet_size_famil_key UNIQUE (source, destination, packet_size, family);


--
-- Name: streams_amp_udpstream streams_amp_udpstream_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_udpstream
    ADD CONSTRAINT streams_amp_udpstream_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_udpstream streams_amp_udpstream_source_destination_address_direction__key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_udpstream
    ADD CONSTRAINT streams_amp_udpstream_source_destination_address_direction__key UNIQUE (source, destination, address, direction, packet_size, packet_spacing, packet_count, dscp);


--
-- Name: streams_amp_youtube streams_amp_youtube_pkey; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_youtube
    ADD CONSTRAINT streams_amp_youtube_pkey PRIMARY KEY (stream_id);


--
-- Name: streams_amp_youtube streams_amp_youtube_source_destination_quality_key; Type: CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.streams_amp_youtube
    ADD CONSTRAINT streams_amp_youtube_source_destination_quality_key UNIQUE (source, destination, quality);


--
-- Name: data_amp_astraceroute_18_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_astraceroute_18_timestamp_idx ON public.data_amp_astraceroute_18 USING btree ("timestamp");


--
-- Name: data_amp_astraceroute_19_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_astraceroute_19_timestamp_idx ON public.data_amp_astraceroute_19 USING btree ("timestamp");


--
-- Name: data_amp_astraceroute_5_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_astraceroute_5_timestamp_idx ON public.data_amp_astraceroute_5 USING btree ("timestamp");


--
-- Name: data_amp_astraceroute_6_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_astraceroute_6_timestamp_idx ON public.data_amp_astraceroute_6 USING btree ("timestamp");


--
-- Name: data_amp_astraceroute_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_astraceroute_timestamp_idx ON public.data_amp_astraceroute USING btree ("timestamp");


--
-- Name: data_amp_dns_rtt_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_dns_rtt_idx ON public.data_amp_dns USING btree (rtt);


--
-- Name: data_amp_dns_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_dns_timestamp_idx ON public.data_amp_dns USING btree ("timestamp");


--
-- Name: data_amp_external_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_external_timestamp_idx ON public.data_amp_external USING btree ("timestamp");


--
-- Name: data_amp_fastping_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_fastping_timestamp_idx ON public.data_amp_fastping USING btree ("timestamp");


--
-- Name: data_amp_http_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_http_timestamp_idx ON public.data_amp_http USING btree ("timestamp");


--
-- Name: data_amp_icmp_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_icmp_timestamp_idx ON public.data_amp_icmp USING btree ("timestamp");


--
-- Name: data_amp_tcpping_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_tcpping_timestamp_idx ON public.data_amp_tcpping USING btree ("timestamp");


--
-- Name: data_amp_throughput_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_throughput_timestamp_idx ON public.data_amp_throughput USING btree ("timestamp");


--
-- Name: data_amp_traceroute_18_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_18_timestamp_idx ON public.data_amp_traceroute_18 USING btree ("timestamp");


--
-- Name: data_amp_traceroute_19_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_19_timestamp_idx ON public.data_amp_traceroute_19 USING btree ("timestamp");


--
-- Name: data_amp_traceroute_5_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_5_timestamp_idx ON public.data_amp_traceroute_5 USING btree ("timestamp");


--
-- Name: data_amp_traceroute_6_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_6_timestamp_idx ON public.data_amp_traceroute_6 USING btree ("timestamp");


--
-- Name: data_amp_traceroute_pathlen_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_pathlen_timestamp_idx ON public.data_amp_traceroute_pathlen USING btree ("timestamp");


--
-- Name: data_amp_traceroute_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_traceroute_timestamp_idx ON public.data_amp_traceroute USING btree ("timestamp");


--
-- Name: data_amp_udpstream_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_udpstream_timestamp_idx ON public.data_amp_udpstream USING btree ("timestamp");


--
-- Name: data_amp_youtube_timestamp_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX data_amp_youtube_timestamp_idx ON public.data_amp_youtube USING btree ("timestamp");


--
-- Name: streams_amp_dns_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_dns_destination_idx ON public.streams_amp_dns USING btree (destination);


--
-- Name: streams_amp_dns_query_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_dns_query_idx ON public.streams_amp_dns USING btree (query);


--
-- Name: streams_amp_dns_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_dns_source_idx ON public.streams_amp_dns USING btree (source);


--
-- Name: streams_amp_external_command_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_external_command_idx ON public.streams_amp_external USING btree (command);


--
-- Name: streams_amp_external_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_external_destination_idx ON public.streams_amp_external USING btree (destination);


--
-- Name: streams_amp_external_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_external_source_idx ON public.streams_amp_external USING btree (source);


--
-- Name: streams_amp_fastping_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_fastping_destination_idx ON public.streams_amp_fastping USING btree (destination);


--
-- Name: streams_amp_fastping_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_fastping_source_idx ON public.streams_amp_fastping USING btree (source);


--
-- Name: streams_amp_http_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_http_destination_idx ON public.streams_amp_http USING btree (destination);


--
-- Name: streams_amp_http_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_http_source_idx ON public.streams_amp_http USING btree (source);


--
-- Name: streams_amp_icmp_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_icmp_destination_idx ON public.streams_amp_icmp USING btree (destination);


--
-- Name: streams_amp_icmp_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_icmp_source_idx ON public.streams_amp_icmp USING btree (source);


--
-- Name: streams_amp_tcpping_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_tcpping_destination_idx ON public.streams_amp_tcpping USING btree (destination);


--
-- Name: streams_amp_tcpping_port_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_tcpping_port_idx ON public.streams_amp_tcpping USING btree (port);


--
-- Name: streams_amp_tcpping_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_tcpping_source_idx ON public.streams_amp_tcpping USING btree (source);


--
-- Name: streams_amp_throughput_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_throughput_destination_idx ON public.streams_amp_throughput USING btree (destination);


--
-- Name: streams_amp_throughput_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_throughput_source_idx ON public.streams_amp_throughput USING btree (source);


--
-- Name: streams_amp_traceroute_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_traceroute_destination_idx ON public.streams_amp_traceroute USING btree (destination);


--
-- Name: streams_amp_traceroute_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_traceroute_source_idx ON public.streams_amp_traceroute USING btree (source);


--
-- Name: streams_amp_udpstream_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_udpstream_destination_idx ON public.streams_amp_udpstream USING btree (destination);


--
-- Name: streams_amp_udpstream_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_udpstream_source_idx ON public.streams_amp_udpstream USING btree (source);


--
-- Name: streams_amp_youtube_destination_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_youtube_destination_idx ON public.streams_amp_youtube USING btree (destination);


--
-- Name: streams_amp_youtube_source_idx; Type: INDEX; Schema: public; Owner: cuz
--

CREATE INDEX streams_amp_youtube_source_idx ON public.streams_amp_youtube USING btree (source);


--
-- Name: data_amp_astraceroute_18 data_amp_astraceroute_18_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_astraceroute_18
    ADD CONSTRAINT data_amp_astraceroute_18_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_18(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_astraceroute_19 data_amp_astraceroute_19_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_astraceroute_19
    ADD CONSTRAINT data_amp_astraceroute_19_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_19(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_astraceroute_5 data_amp_astraceroute_5_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_astraceroute_5
    ADD CONSTRAINT data_amp_astraceroute_5_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_5(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_astraceroute_6 data_amp_astraceroute_6_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_astraceroute_6
    ADD CONSTRAINT data_amp_astraceroute_6_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_6(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_18 data_amp_traceroute_18_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_18
    ADD CONSTRAINT data_amp_traceroute_18_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_18(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_18 data_amp_traceroute_18_path_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_18
    ADD CONSTRAINT data_amp_traceroute_18_path_id_fkey FOREIGN KEY (path_id) REFERENCES public.data_amp_traceroute_paths_18(path_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_19 data_amp_traceroute_19_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_19
    ADD CONSTRAINT data_amp_traceroute_19_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_19(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_19 data_amp_traceroute_19_path_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_19
    ADD CONSTRAINT data_amp_traceroute_19_path_id_fkey FOREIGN KEY (path_id) REFERENCES public.data_amp_traceroute_paths_19(path_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_5 data_amp_traceroute_5_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_5
    ADD CONSTRAINT data_amp_traceroute_5_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_5(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_5 data_amp_traceroute_5_path_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_5
    ADD CONSTRAINT data_amp_traceroute_5_path_id_fkey FOREIGN KEY (path_id) REFERENCES public.data_amp_traceroute_paths_5(path_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_6 data_amp_traceroute_6_aspath_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_6
    ADD CONSTRAINT data_amp_traceroute_6_aspath_id_fkey FOREIGN KEY (aspath_id) REFERENCES public.data_amp_traceroute_aspaths_6(aspath_id) ON DELETE CASCADE;


--
-- Name: data_amp_traceroute_6 data_amp_traceroute_6_path_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cuz
--

ALTER TABLE ONLY public.data_amp_traceroute_6
    ADD CONSTRAINT data_amp_traceroute_6_path_id_fkey FOREIGN KEY (path_id) REFERENCES public.data_amp_traceroute_paths_6(path_id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

