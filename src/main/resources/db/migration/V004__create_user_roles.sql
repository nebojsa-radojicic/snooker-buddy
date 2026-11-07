-- Preserve existing system authorities; infer business roles only from existing ownership/relationships.
create table user_roles (
    user_id bigint not null references users(id) on delete cascade,
    role varchar(30) not null check (role in ('ROLE_USER', 'ROLE_ADMIN', 'ROLE_COACH', 'ROLE_PLAYER')),
    primary key (user_id, role)
);
insert into user_roles(user_id, role) select id, role from users;
insert into user_roles(user_id, role)
select owner_id, 'ROLE_COACH' from exercises
union select coach_id, 'ROLE_COACH' from coach_players;
insert into user_roles(user_id, role) select player_id, 'ROLE_PLAYER' from coach_players;
alter table users drop column role;
