insert into users(id, email, password, name, role, created_at) values
(3, 'othercoach@example.com', 'unused', 'Other Coach', 'ROLE_USER', current_timestamp),
(4, 'otherplayer@example.com', 'unused', 'Other Player', 'ROLE_USER', current_timestamp),
(5, 'newplayer@example.com', 'unused', 'New Player', 'ROLE_USER', current_timestamp);
insert into coach_players(coach_id, player_id) values (1, 2), (3, 4);
