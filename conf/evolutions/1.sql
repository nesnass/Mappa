# Users schema
 
# --- !Ups

drop table if exists feature cascade;

drop table if exists muser cascade;

drop table if exists tag cascade;

drop table if exists tag_feature cascade;

drop sequence if exists feature_seq;

drop sequence if exists muser_seq;

drop sequence if exists tag_seq;

# --- !Downs

drop table if exists feature cascade;

drop table if exists muser cascade;

drop table if exists tag cascade;

drop table if exists tag_feature cascade;

drop sequence if exists feature_seq;

drop sequence if exists muser_seq;

drop sequence if exists tag_seq;