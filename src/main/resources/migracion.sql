-- CoWorkHub · migración completa para MySQL 8: crea las tablas y carga los datos de prueba.
-- Sirve para preparar la base a mano, sin arrancar la aplicación:
--   docker exec -i coworkhub-mysql mysql -u coworkhub -pcoworkhub coworkhub < src/main/resources/migracion.sql
-- El esquema es el mismo que genera Hibernate a partir de las entidades (ddl-auto=create). Al arrancar,
-- la aplicación vuelve a crear las tablas y carga src/main/resources/import.sql, que tiene los mismos datos.
-- ⚠️ Datos ficticios, solo para desarrollo. Las fechas de las reservas son relativas a hoy (CURDATE()).

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------- Esquema

DROP TABLE IF EXISTS `booking_items`;
DROP TABLE IF EXISTS `bookings`;
DROP TABLE IF EXISTS `members`;
DROP TABLE IF EXISTS `room_equipment`;
DROP TABLE IF EXISTS `rooms`;
DROP TABLE IF EXISTS `additional_services`;
DROP TABLE IF EXISTS `equipment`;
DROP TABLE IF EXISTS `locations`;
DROP TABLE IF EXISTS `membership_plans`;
DROP TABLE IF EXISTS `user_accounts`;

CREATE TABLE `user_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(60) NOT NULL,
  `role` enum('ADMIN','MEMBER','RECEPTIONIST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKlxwlgwuy2yrbye2vgs9w9x7mr` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `membership_plans` (
  `included_hours_per_month` int NOT NULL,
  `max_active_bookings` int NOT NULL,
  `overage_discount` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKnconflq2x1uvxvs7sc0mnmh4` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `locations` (
  `closing_time` time NOT NULL,
  `opening_time` time NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `city` varchar(80) NOT NULL,
  `name` varchar(100) NOT NULL,
  `address` varchar(150) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm6whtgwe6ya84f59coio1j1wh` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `additional_services` (
  `unit_price` decimal(12,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKf7cdh07s4bs1qavhcdtiftbv4` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `rooms` (
  `active` bit(1) NOT NULL,
  `capacity` int NOT NULL,
  `hourly_rate` decimal(12,2) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `location_id` bigint NOT NULL,
  `name` varchar(80) NOT NULL,
  `type` enum('DESK','MEETING_ROOM','PRIVATE_OFFICE') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_location_name` (`location_id`,`name`),
  CONSTRAINT `FKn44nnd0bt5f0gwkdtw5ub1esg` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `room_equipment` (
  `equipment_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`equipment_id`,`room_id`),
  KEY `FKiyhaivc7it7c8go6ursatnvte` (`room_id`),
  CONSTRAINT `FKiyhaivc7it7c8go6ursatnvte` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `FKrmdm2ihfq0rjieivqjmnty6hq` FOREIGN KEY (`equipment_id`) REFERENCES `equipment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL,
  `user_account_id` bigint DEFAULT NULL,
  `document_number` varchar(20) NOT NULL,
  `email` varchar(120) NOT NULL,
  `full_name` varchar(120) NOT NULL,
  `status` enum('ACTIVE','SUSPENDED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKd5c97lgxv17hn0p5c4ttbxxqk` (`document_number`),
  UNIQUE KEY `UK9d30a9u1qpg8eou0otgkwrp5d` (`email`),
  UNIQUE KEY `UK3v63j60dtktxj4q0darlbppg1` (`user_account_id`),
  KEY `FKjuwy40aicmdogitnkjj38s78k` (`plan_id`),
  CONSTRAINT `FKdkq38de7wa91vr0tkssv9m8d2` FOREIGN KEY (`user_account_id`) REFERENCES `user_accounts` (`id`),
  CONSTRAINT `FKjuwy40aicmdogitnkjj38s78k` FOREIGN KEY (`plan_id`) REFERENCES `membership_plans` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `bookings` (
  `attendees` int NOT NULL,
  `cancellation_fee` decimal(12,2) NOT NULL,
  `consumed_minutes` int NOT NULL,
  `room_cost` decimal(12,2) NOT NULL,
  `services_cost` decimal(12,2) NOT NULL,
  `total_cost` decimal(12,2) NOT NULL,
  `end_time` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `room_id` bigint NOT NULL,
  `start_time` datetime(6) NOT NULL,
  `status` enum('CANCELLED','COMPLETED','CONFIRMED','PENDING') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_booking_room_start` (`room_id`,`start_time`),
  KEY `ix_booking_member_start` (`member_id`,`start_time`),
  CONSTRAINT `FK2hu43b2w26um1gql30wbf0173` FOREIGN KEY (`member_id`) REFERENCES `members` (`id`),
  CONSTRAINT `FKrgoycol97o21kpjodw1qox4nc` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `booking_items` (
  `applied_unit_price` decimal(12,2) NOT NULL,
  `quantity` int NOT NULL,
  `booking_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `service_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_booking_service` (`booking_id`,`service_id`),
  KEY `FKq2m1rsv5kb7464rmpliamu490` (`service_id`),
  CONSTRAINT `FKq2m1rsv5kb7464rmpliamu490` FOREIGN KEY (`service_id`) REFERENCES `additional_services` (`id`),
  CONSTRAINT `FKrw74irmyat5c39cnjkn02u99m` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------- Datos de prueba
-- ⚠️ Datos ficticios, solo para desarrollo. Contraseñas codificadas con BCrypt: admin/admin123, recepcion/recep123, ana/ana123, luis/luis123.

-- Sedes
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (1, 'Sede Cabecera', 'Bucaramanga', 'Carrera 33 # 48-20', '07:00:00', '20:00:00');
INSERT INTO locations (id, name, city, address, opening_time, closing_time) VALUES (2, 'Sede Cañaveral', 'Floridablanca', 'Calle 30 # 25-71', '08:00:00', '18:00:00');

-- Equipamiento
INSERT INTO equipment (id, name) VALUES (1, 'Televisor 55 pulgadas');
INSERT INTO equipment (id, name) VALUES (2, 'Proyector');
INSERT INTO equipment (id, name) VALUES (3, 'Tablero acrílico');
INSERT INTO equipment (id, name) VALUES (4, 'Kit de videoconferencia');
INSERT INTO equipment (id, name) VALUES (5, 'Aire acondicionado');

-- Salas (la 7 está inactiva)
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (1, 'Sala Andes', 'MEETING_ROOM', 8, 30000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (2, 'Sala Chicamocha', 'MEETING_ROOM', 12, 45000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (3, 'Oficina Guane', 'PRIVATE_OFFICE', 4, 25000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (4, 'Escritorio Ceiba', 'DESK', 1, 8000.00, true, 1);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (5, 'Sala Santurbán', 'MEETING_ROOM', 6, 28000.00, true, 2);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (6, 'Oficina Lebrija', 'PRIVATE_OFFICE', 3, 22000.00, true, 2);
INSERT INTO rooms (id, name, type, capacity, hourly_rate, active, location_id) VALUES (7, 'Sala Morrorico', 'MEETING_ROOM', 10, 35000.00, false, 2);

-- Equipamiento de cada sala
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (1, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 2);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (2, 5);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (3, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (3, 5);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 1);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (5, 3);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (6, 4);
INSERT INTO room_equipment (room_id, equipment_id) VALUES (7, 2);

-- Servicios adicionales
INSERT INTO additional_services (id, name, unit_price) VALUES (1, 'Café y aromáticas (por persona)', 5000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (2, 'Refrigerio (por persona)', 12000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (3, 'Paquete de 10 impresiones', 5000.00);
INSERT INTO additional_services (id, name, unit_price) VALUES (4, 'Soporte técnico (por reserva)', 40000.00);

-- Planes: horas incluidas al mes, % de descuento en horas excedentes, máximo de reservas activas futuras
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (1, 'Flex', 0, 0, 2);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (2, 'Profesional', 10, 10, 3);
INSERT INTO membership_plans (id, name, included_hours_per_month, overage_discount, max_active_bookings) VALUES (3, 'Empresarial', 40, 20, 5);

-- Cuentas: un ADMIN, un RECEPTIONIST y dos MEMBER (ana y luis, vinculadas a su miembro)
INSERT INTO user_accounts (id, username, password, role) VALUES (1, 'admin', '$2a$10$ks1c12SHfg40sDK6i222YOhJ/vGuy8RnwbUj7aJrKBwq4sizxTN4i', 'ADMIN');
INSERT INTO user_accounts (id, username, password, role) VALUES (2, 'recepcion', '$2a$10$KmBmM5sr5rEeC/3qNG92MewzO1cfKXOMBL2An//iDSzwKu15V3wam', 'RECEPTIONIST');
INSERT INTO user_accounts (id, username, password, role) VALUES (3, 'ana', '$2a$10$DvmUld62U/HJ/c/T3DRJMuA/D9bjka0C3sT3fl2GHRj2fURWTmIHa', 'MEMBER');
INSERT INTO user_accounts (id, username, password, role) VALUES (4, 'luis', '$2a$10$MkJWf65.kb1iEBjAE7/qWOH.y04WIxy3ZS1sSsZuEEU4IrOj.2Nhy', 'MEMBER');

-- Miembros (Carlos está suspendido; Marta, Carlos y Sofía no tienen cuenta)
INSERT INTO members (id, document_number, full_name, email, status, plan_id, user_account_id) VALUES (1, '1098000001', 'Ana Gómez', 'ana.gomez@ejemplo.com', 'ACTIVE', 2, 3);
INSERT INTO members (id, document_number, full_name, email, status, plan_id, user_account_id) VALUES (2, '1098000002', 'Luis Pérez', 'luis.perez@ejemplo.com', 'ACTIVE', 1, 4);
INSERT INTO members (id, document_number, full_name, email, status, plan_id, user_account_id) VALUES (3, '1098000003', 'Marta Rueda', 'marta.rueda@ejemplo.com', 'ACTIVE', 3, NULL);
INSERT INTO members (id, document_number, full_name, email, status, plan_id, user_account_id) VALUES (4, '1098000004', 'Carlos Díaz', 'carlos.diaz@ejemplo.com', 'SUSPENDED', 1, NULL);
INSERT INTO members (id, document_number, full_name, email, status, plan_id, user_account_id) VALUES (5, '1098000005', 'Sofía Ardila', 'sofia.ardila@ejemplo.com', 'ACTIVE', 2, NULL);

-- Reservas. Costos según RN-07: Ana, Marta y Sofía nunca pasan de sus horas incluidas (sala = 0);
-- Luis y Carlos tienen el plan Flex (0 h, 0 %): pagan tarifa × horas.
-- Pasadas: 1-5 COMPLETED; 6 CANCELLED tarde (cargo 50 %, no devuelve horas); 7 CANCELLED a tiempo (sin cargo).
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (1, TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:00:00'), TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '11:00:00'), 5, 'COMPLETED', 0.00, 25000.00, 25000.00, 0.00, 120, 1, 1);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (2, TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '08:00:00'), TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '12:00:00'), 1, 'COMPLETED', 32000.00, 0.00, 32000.00, 0.00, 240, 4, 2);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (3, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '14:00:00'), TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '17:00:00'), 10, 'COMPLETED', 0.00, 120000.00, 120000.00, 0.00, 180, 2, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (4, TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '10:00:00'), TIMESTAMP(CURDATE() - INTERVAL 12 DAY, '11:00:00'), 4, 'COMPLETED', 28000.00, 0.00, 28000.00, 0.00, 60, 5, 4);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (5, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '15:00:00'), TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '16:30:00'), 2, 'COMPLETED', 0.00, 0.00, 0.00, 0.00, 90, 3, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (6, TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '16:00:00'), TIMESTAMP(CURDATE() - INTERVAL 3 DAY, '17:00:00'), 3, 'CANCELLED', 30000.00, 0.00, 15000.00, 15000.00, 60, 1, 2);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (7, TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '10:00:00'), TIMESTAMP(CURDATE() - INTERVAL 2 DAY, '11:00:00'), 2, 'CANCELLED', 0.00, 0.00, 0.00, 0.00, 0, 6, 1);
-- Futuras: 8 y 13 CONFIRMED; 9, 10, 11, 12 y 15 PENDING; 14 CANCELLED a tiempo.
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (8, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '10:00:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '12:00:00'), 4, 'CONFIRMED', 0.00, 25000.00, 25000.00, 0.00, 120, 1, 1);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (9, TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '14:00:00'), TIMESTAMP(CURDATE() + INTERVAL 2 DAY, '16:00:00'), 8, 'PENDING', 0.00, 40000.00, 40000.00, 0.00, 120, 2, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (10, TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '09:00:00'), TIMESTAMP(CURDATE() + INTERVAL 3 DAY, '10:30:00'), 3, 'PENDING', 0.00, 0.00, 0.00, 0.00, 90, 5, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (11, TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 4 DAY, '10:00:00'), 1, 'PENDING', 16000.00, 0.00, 16000.00, 0.00, 120, 4, 2);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (12, TIMESTAMP(CURDATE() + INTERVAL 5 DAY, '13:00:00'), TIMESTAMP(CURDATE() + INTERVAL 5 DAY, '14:00:00'), 2, 'PENDING', 0.00, 0.00, 0.00, 0.00, 60, 3, 1);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (13, TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '15:00:00'), TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '17:00:00'), 6, 'CONFIRMED', 0.00, 102000.00, 102000.00, 0.00, 120, 1, 3);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (14, TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '09:00:00'), TIMESTAMP(CURDATE() + INTERVAL 6 DAY, '10:00:00'), 2, 'CANCELLED', 0.00, 0.00, 0.00, 0.00, 0, 6, 5);
INSERT INTO bookings (id, start_time, end_time, attendees, status, room_cost, services_cost, total_cost, cancellation_fee, consumed_minutes, room_id, member_id) VALUES (15, TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '08:00:00'), TIMESTAMP(CURDATE() + INTERVAL 7 DAY, '12:00:00'), 10, 'PENDING', 0.00, 0.00, 0.00, 0.00, 240, 2, 3);

-- Detalles de reserva (servicios), con el precio aplicado al agregarlos
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (1, 5, 5000.00, 1, 1);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (2, 10, 12000.00, 3, 2);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (3, 4, 5000.00, 8, 1);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (4, 1, 5000.00, 8, 3);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (5, 1, 40000.00, 9, 4);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (6, 6, 12000.00, 13, 2);
INSERT INTO booking_items (id, quantity, applied_unit_price, booking_id, service_id) VALUES (7, 6, 5000.00, 13, 1);
