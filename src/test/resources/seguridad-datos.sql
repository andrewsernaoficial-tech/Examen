-- ============================================================
--  TEST DATA – seguridad-datos.sql
--  Contraseñas verificadas por MigracionesSeguridadTest:
--    admin      → Admin2026*
--    vendedor   → Vendedor2026*
--    almacen    → Almacen2026*
--    supervisor → Supervisor2026*
--    inactivo   → Inactivo2026*
-- ============================================================

insert into roles (nombre, descripcion)
values ('ADMIN',      'Administra usuarios, ve reportes y opera todo el sistema');
insert into roles (nombre, descripcion)
values ('VENDEDOR',   'Atiende clientes y registra ventas');
insert into roles (nombre, descripcion)
values ('ALMACENERO', 'Mantiene categorías y productos');

-- ── Empleados ─────────────────────────────────────────────────
insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('40112233','Rosa Elena','Mendoza Ticona','Administradora',
        'rmendoza@pharmasoft.pe','951000111',true,CURRENT_TIMESTAMP);
insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('41223344','Lucia','Ramos Cutipa','Vendedora',
        'lramos@pharmasoft.pe','951000222',true,CURRENT_TIMESTAMP);
insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('42334455','Pedro','Quispe Mamani','Almacenero',
        'pquispe@pharmasoft.pe',null,true,CURRENT_TIMESTAMP);
insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('43445566','Ana','Torres Vilca','Jefa de tienda',
        'atorres@pharmasoft.pe','951000444',true,CURRENT_TIMESTAMP);
insert into empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
values ('44556677','Jorge','Apaza Huanca','Vendedor',
        'japaza@pharmasoft.pe',null,true,CURRENT_TIMESTAMP);

-- ── Usuarios (BCrypt de las contraseñas indicadas) ─────────────
-- Admin2026*
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
values ('admin',
        '$2a$10$NNNgOCCajIjVDGhg3u7sHOwLlLrVMdIALH/64AmPlFmmpiG1yinGa',
        true,
        (select id from empleados where dni = '40112233'),
        CURRENT_TIMESTAMP);
-- Vendedor2026*
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
values ('vendedor',
        '$2a$10$EXmYQXoQUBed/ZQHfPVA1.eTtSKOdgQpsz3/ZODq.za2hyGfV40We',
        true,
        (select id from empleados where dni = '41223344'),
        CURRENT_TIMESTAMP);
-- Almacen2026*
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
values ('almacen',
        '$2a$10$ciG8Sz1SQTaIK5Cau0G/5u1LqIPQvcujkc5hPRaGO/Fd7ChrFBkFK',
        true,
        (select id from empleados where dni = '42334455'),
        CURRENT_TIMESTAMP);
-- Supervisor2026*
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
values ('supervisor',
        '$2a$10$j8xXh/XBHa/3jkViqNkMs.oZtvG6kL/QyF49kDAnnDrqktpT75sJK',
        true,
        (select id from empleados where dni = '43445566'),
        CURRENT_TIMESTAMP);
-- Inactivo2026*  (estado = false)
insert into usuarios (username, password, estado, empleado_id, fecha_creacion)
values ('inactivo',
        '$2a$10$oi118hH.Nfj8jY0/MOIWJ.i34FhMDm2jy0/OpPMjSMUPh1.W/C6De',
        false,
        (select id from empleados where dni = '44556677'),
        CURRENT_TIMESTAMP);

-- ── Asignación de roles ───────────────────────────────────────
insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r
where u.username = 'admin'    and r.nombre = 'ADMIN';

insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r
where u.username = 'vendedor' and r.nombre = 'VENDEDOR';

insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r
where u.username = 'almacen'  and r.nombre = 'ALMACENERO';

-- supervisor tiene VENDEDOR + ALMACENERO (2 roles → supervisorSumaPermisos)
insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r
where u.username = 'supervisor' and r.nombre in ('VENDEDOR','ALMACENERO');

insert into usuario_roles (usuario_id, rol_id)
select u.id, r.id from usuarios u, roles r
where u.username = 'inactivo' and r.nombre = 'VENDEDOR';