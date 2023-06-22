create table person
(
    id      int not null,
    name1   varchar(255),
    age     integer,
    born_at timestamp,
    primary key (id)
);
insert into person (id, name1, age,born_at)
values (1, 'John', 42, '1988-01-12 12:00:00');
insert into person (id, name1, age,born_at)
values (2, 'bob', 46, '1977-02-20 11:12:24');