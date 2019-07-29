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


--
-- Data for Name: streams_amp_external; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_fastping; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_http; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_icmp; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_icmp VALUES (3, 'amplet', 'wand.net.nz', 'ipv4', 'random');
INSERT INTO public.streams_amp_icmp VALUES (4, 'amplet', 'google.com', 'ipv4', 'random');


--
-- Data for Name: streams_amp_tcpping; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_throughput; Type: TABLE DATA; Schema: public; Owner: cuz
--



--
-- Data for Name: streams_amp_traceroute; Type: TABLE DATA; Schema: public; Owner: cuz
--

INSERT INTO public.streams_amp_traceroute VALUES (5, 'amplet', 'google.com', 'ipv4', '60');
INSERT INTO public.streams_amp_traceroute VALUES (6, 'amplet', 'wand.net.nz', 'ipv4', '60');


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

SELECT pg_catalog.setval('public.data_amp_traceroute_aspaths_aspath_id_seq', 1, false);


--
-- Name: data_amp_traceroute_paths_path_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.data_amp_traceroute_paths_path_id_seq', 39, true);


--
-- Name: streams_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cuz
--

SELECT pg_catalog.setval('public.streams_id_seq', 8, true);


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
SELECT pg_reload_conf();
