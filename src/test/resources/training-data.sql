insert into users(id, email, password, name, created_at) values
(3, 'othercoach@example.com', 'unused', 'Other Coach', current_timestamp),
(4, 'otherplayer@example.com', 'unused', 'Other Player', current_timestamp),
(5, 'newplayer@example.com', 'unused', 'New Player', current_timestamp);
insert into coach_players(coach_id, player_id) values (1, 2), (3, 4);

insert into user_roles(user_id, role) values (3, 'ROLE_COACH'), (4, 'ROLE_PLAYER'), (5, 'ROLE_PLAYER');
