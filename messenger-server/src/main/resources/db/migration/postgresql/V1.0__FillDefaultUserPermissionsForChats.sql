insert into
    "UsersPermissions" ("user", "resource", "resource_type", "permissions")
select
    "id",
    null,
    15,
    '{"Chat.Create"}'
from
    "Users"
on conflict ("user", "resource", "resource_type")
    do update
           set
               "permissions" = excluded."permissions"