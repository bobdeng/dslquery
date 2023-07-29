drop table if exists person;
create table person
(
    id      int not null,
    name1   varchar(255),
    age     integer,
    born_at timestamp,
    phone   varchar(255),
    address varchar(255),
    zip_code varchar(255),
    org_id integer,
    primary key (id)
);
create table org
(
    id      int not null,
    name   varchar(255),
    area_id integer,
    primary key (id)
);
create table area
(
    id      int not null,
    name   varchar(255),
    primary key (id)
);
insert into area (id, name) values (1, 'cn');
insert into area (id, name) values (2, 'usa');
insert into org (id, name, area_id) values (1, 'ms', 1);
insert into org (id, name, area_id) values (2, 'google', 2);

insert into person (id, name1, age, born_at, phone, address,zip_code, org_id)
values (1, 'John smith', 42, '1988-01-12 12:00:00', '123456789', '123 main st', '12345',1);
insert into person (id, name1, age, born_at, phone, address,zip_code,org_id)
values (2, 'bob robert', 46, '1977-02-20 11:12:24', '123456782', '124 main st', '12346',2);
