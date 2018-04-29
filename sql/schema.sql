create table users (
  id bigserial primary key,
  username text unique,
  email text unique,
  password text not null,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  deleted timestamp with time zone
);

create table streams (
  id bigserial primary key,
  name text unique not null,
  title text,
  owner bigint references users(id) not null,
  key text,
  logourl text,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  deleted timestamp with time zone
);

create table subscriptions (
  userid bigint references users(id) not null,
  streamid bigint references streams(id) not null,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  deleted timestamp with time zone
);

create table messages (
  id bigserial primary key,
  userid bigint references users(id) not null,
  streamid bigint references streams(id) not null,
  body text not null,
  created timestamp with time zone not null,
  updated timestamp with time zone not null,
  deleted timestamp with time zone
);