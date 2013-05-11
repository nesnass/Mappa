# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table s_feature (
  _id                       bigint not null,
  origin_id                 varchar(255),
  type                      varchar(255),
  feature_user_id           varchar(255),
  feature_mapper_id         varchar(255),
  feature_session_id        bigint,
  gtype                     varchar(255),
  lng                       float,
  lat                       float,
  image_standard_resolution_file_id bigint,
  image_thumbnail_file_id   bigint,
  created_time              timestamp,
  descr_url                 varchar(255),
  description               varchar(255),
  mapper_description        varchar(255),
  icon_url                  varchar(255),
  source_type               varchar(255),
  standard_resolution       varchar(255),
  thumbnail                 varchar(255),
  constraint pk_s_feature primary key (_id))
;

create table s_user (
  id                        varchar(255) not null,
  full_name                 varchar(255),
  profile_picture           varchar(255),
  lng                       float,
  lat                       float,
  constraint pk_s_user primary key (id))
;

create table s_s3file (
  id                        bigint not null,
  uuid                      varchar(40),
  bucket                    varchar(255),
  type                      varchar(255),
  constraint pk_s_s3file primary key (id))
;

create table s_session (
  id                        bigint not null,
  facebook_group_id         bigint,
  stitle                    varchar(255),
  sdescription              varchar(255),
  constraint pk_s_session primary key (id))
;

create table s_tag (
  id                        bigint not null,
  tag                       varchar(255),
  constraint uq_s_tag_tag unique (tag),
  constraint pk_s_tag primary key (id))
;


create table s_feature_tag (
  s_feature__id                  bigint not null,
  s_tag_id                       bigint not null,
  constraint pk_s_feature_tag primary key (s_feature__id, s_tag_id))
;

create table s_tag_feature (
  s_tag_id                       bigint not null,
  s_feature__id                  bigint not null,
  constraint pk_s_tag_feature primary key (s_tag_id, s_feature__id))
;
create sequence s_feature_seq;

create sequence s_user_seq;

create sequence s_s3file_seq;

create sequence s_session_seq;

create sequence s_tag_seq;

alter table s_feature add constraint fk_s_feature_featureUser_1 foreign key (feature_user_id) references s_user (id);
create index ix_s_feature_featureUser_1 on s_feature (feature_user_id);
alter table s_feature add constraint fk_s_feature_featureMapper_2 foreign key (feature_mapper_id) references s_user (id);
create index ix_s_feature_featureMapper_2 on s_feature (feature_mapper_id);
alter table s_feature add constraint fk_s_feature_featureSession_3 foreign key (feature_session_id) references s_session (id);
create index ix_s_feature_featureSession_3 on s_feature (feature_session_id);
alter table s_feature add constraint fk_s_feature_imageStandardReso_4 foreign key (image_standard_resolution_file_id) references s_s3file (id);
create index ix_s_feature_imageStandardReso_4 on s_feature (image_standard_resolution_file_id);
alter table s_feature add constraint fk_s_feature_imageThumbnailFil_5 foreign key (image_thumbnail_file_id) references s_s3file (id);
create index ix_s_feature_imageThumbnailFil_5 on s_feature (image_thumbnail_file_id);



alter table s_feature_tag add constraint fk_s_feature_tag_s_feature_01 foreign key (s_feature__id) references s_feature (_id);

alter table s_feature_tag add constraint fk_s_feature_tag_s_tag_02 foreign key (s_tag_id) references s_tag (id);

alter table s_tag_feature add constraint fk_s_tag_feature_s_tag_01 foreign key (s_tag_id) references s_tag (id);

alter table s_tag_feature add constraint fk_s_tag_feature_s_feature_02 foreign key (s_feature__id) references s_feature (_id);

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

drop sequence if exists s_s3file_seq;

drop sequence if exists s_session_seq;

drop sequence if exists s_tag_seq;

