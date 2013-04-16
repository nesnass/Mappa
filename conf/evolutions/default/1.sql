# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table s_feature (
  id                        bigint not null,
  coordinate_0              float,
  coordinate_1              float,
  feature_user_id           bigint,
  mapper_user_id            bigint,
  feature_session_id        bigint,
  type                      varchar(255),
  created_time              timestamp,
  descr_url                 varchar(255),
  description               varchar(255),
  mapper_description        varchar(255),
  icon_url                  varchar(255),
  image_url_high_resolution varchar(255),
  image_url_standard_resolution varchar(255),
  image_url_thumbnail       varchar(255),
  name                      varchar(255),
  source_type               integer,
  constraint ck_s_feature_source_type check (source_type in (0,1)),
  constraint pk_s_feature primary key (id))
;

create table s_user (
  id                        bigint not null,
  facebook_id               bigint,
  full_name                 varchar(255),
  profile_picture           varchar(255),
  location_0                bigint,
  location_1                bigint,
  constraint pk_s_user primary key (id))
;

create table s_s3file (
  id                        varchar(40) not null,
  bucket                    varchar(255),
  name                      varchar(255),
  constraint pk_s_s3file primary key (id))
;

create table s_session (
  id                        bigint not null,
  facebook_group_id         bigint,
  title                     varchar(255),
  description               varchar(255),
  constraint pk_s_session primary key (id))
;

create table s_tag (
  id                        bigint not null,
  tag                       varchar(255),
  constraint uq_s_tag_tag unique (tag),
  constraint pk_s_tag primary key (id))
;


create table s_feature_tag (
  s_feature_id                   bigint not null,
  s_tag_id                       bigint not null,
  constraint pk_s_feature_tag primary key (s_feature_id, s_tag_id))
;

create table s_tag_feature (
  s_tag_id                       bigint not null,
  s_feature_id                   bigint not null,
  constraint pk_s_tag_feature primary key (s_tag_id, s_feature_id))
;
create sequence s_feature_seq;

create sequence s_user_seq;

create sequence s_session_seq;

create sequence s_tag_seq;

alter table s_feature add constraint fk_s_feature_featureUser_1 foreign key (feature_user_id) references s_user (id);
create index ix_s_feature_featureUser_1 on s_feature (feature_user_id);
alter table s_feature add constraint fk_s_feature_mapperUser_2 foreign key (mapper_user_id) references s_user (id);
create index ix_s_feature_mapperUser_2 on s_feature (mapper_user_id);
alter table s_feature add constraint fk_s_feature_featureSession_3 foreign key (feature_session_id) references s_session (id);
create index ix_s_feature_featureSession_3 on s_feature (feature_session_id);



alter table s_feature_tag add constraint fk_s_feature_tag_s_feature_01 foreign key (s_feature_id) references s_feature (id);

alter table s_feature_tag add constraint fk_s_feature_tag_s_tag_02 foreign key (s_tag_id) references s_tag (id);

alter table s_tag_feature add constraint fk_s_tag_feature_s_tag_01 foreign key (s_tag_id) references s_tag (id);

alter table s_tag_feature add constraint fk_s_tag_feature_s_feature_02 foreign key (s_feature_id) references s_feature (id);

# --- !Downs

drop table if exists s_feature cascade;

drop table if exists s_feature_tag cascade;

drop table if exists s_user cascade;

drop table if exists s_s3file cascade;

drop table if exists s_session cascade;

drop table if exists s_tag cascade;

drop table if exists s_tag_feature cascade;

drop sequence if exists s_feature_seq;

drop sequence if exists s_user_seq;

drop sequence if exists s_session_seq;

drop sequence if exists s_tag_seq;

