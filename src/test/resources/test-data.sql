DELETE FROM users;

insert into users(id, email, password, name, created_at) values
(1, 'admin@gmail.com', '$2a$10$hKDVYxLefVHV/vtuPhWD3OigtRyOykRLDdUAp80Z1crSoS1lFqaFS', 'Administrator', CURRENT_TIMESTAMP),
(2, 'demouser@gmail.com', '$2a$10$CDAk0r/V8cvHUj.gzstta.xfvbkJY6kHrjL8pzyQWLsWMzUmByWVC', 'Demo User', CURRENT_TIMESTAMP);


insert into user_roles(user_id, role) values (1, 'ROLE_ADMIN'), (1, 'ROLE_COACH'), (1, 'ROLE_PLAYER'), (2, 'ROLE_COACH'), (2, 'ROLE_PLAYER');
