create table person
(
    id    int not null,
    name1 varchar(255),
    age   integer,
    primary key (id)
);
insert into person (id, name1, age)
values (1, 'John', 42);
insert into person (id, name1, age)
values (2, 'bob', 46);