drop table if exists person;
create table person
(
    id      int not null,
    name1   varchar(255),
    age     integer,
    born_at timestamp,
    phone   varchar(255),
    address varchar(255),
    primary key (id)
);
insert into person (id, name1, age, born_at, phone, address)
values (1, 'John smith', 42, '1988-01-12 12:00:00', '123456789', '123 main st');
insert into person (id, name1, age, born_at, phone, address)
values (2, 'bob robert', 46, '1977-02-20 11:12:24', '123456782', '124 main st');
