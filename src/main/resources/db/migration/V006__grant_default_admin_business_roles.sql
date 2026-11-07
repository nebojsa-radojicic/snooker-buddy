-- Give the seeded admin explicit business roles without changing what ROLE_ADMIN authorizes.
insert into user_roles(user_id, role)
select u.id, business.role
from users u
cross join (values ('ROLE_COACH'), ('ROLE_PLAYER')) as business(role)
where u.id = 1
  and u.email = 'admin@gmail.com'
  and exists (select 1 from user_roles r where r.user_id = u.id and r.role = 'ROLE_ADMIN')
on conflict (user_id, role) do nothing;
