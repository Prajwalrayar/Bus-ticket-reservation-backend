-- =============================================================================
-- BUS TICKET BOOKING SYSTEM — SEED DATA
-- Cities: Bangalore, Chennai, Hyderabad
-- 3 Operators | 6 Buses | 2 Routes | 4 Trips | Full Seat Inventory
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE booking_seats;
TRUNCATE TABLE bookings;
TRUNCATE TABLE payments;
TRUNCATE TABLE cancellations;
TRUNCATE TABLE trip_seats;
TRUNCATE TABLE trips;
TRUNCATE TABLE bus_seats;
TRUNCATE TABLE buses;
TRUNCATE TABLE route_stops;
TRUNCATE TABLE routes;
TRUNCATE TABLE operators;
TRUNCATE TABLE saved_passengers;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- USERS (password = BCrypt of "Test@1234")
-- =============================================================================
INSERT INTO users (user_id, user_name, user_email, user_mobile_number, user_password, is_active, created_at, updated_at)
VALUES
  ('u-admin-001', 'Admin User',    'admin@redbus.com',   '9000000001', '$2a$12$MH7.JjWlQGFqTuEP7j./iOwh8c1BkFrPyTL6VsUcRR0lgH6V0UdFu', true, NOW(), NOW()),
  ('u-cust-001',  'Rahul Sharma',  'rahul@example.com',  '9876543210', '$2a$12$MH7.JjWlQGFqTuEP7j./iOwh8c1BkFrPyTL6VsUcRR0lgH6V0UdFu', true, NOW(), NOW()),
  ('u-cust-002',  'Priya Singh',   'priya@example.com',  '9876543211', '$2a$12$MH7.JjWlQGFqTuEP7j./iOwh8c1BkFrPyTL6VsUcRR0lgH6V0UdFu', true, NOW(), NOW()),
  ('u-op-001',    'VRL Operator',  'operator@vrl.com',   '9800000001', '$2a$12$MH7.JjWlQGFqTuEP7j./iOwh8c1BkFrPyTL6VsUcRR0lgH6V0UdFu', true, NOW(), NOW());

-- =============================================================================
-- USER ROLES
-- =============================================================================
INSERT INTO user_roles (user_role_id, user_id, role_name)
VALUES
  ('ur-001', 'u-admin-001', 'ADMIN'),
  ('ur-002', 'u-cust-001',  'PASSENGER'),
  ('ur-003', 'u-cust-002',  'PASSENGER'),
  ('ur-004', 'u-op-001',    'BUS_OPERATOR');

-- =============================================================================
-- OPERATORS
-- =============================================================================
INSERT INTO operators (operator_id, company_name, contact_email, contact_phone, rating, is_active, created_at, updated_at)
VALUES
  ('op-001', 'VRL Travels',       'info@vrl.com',       '080-22001122', 4.5, true, NOW(), NOW()),
  ('op-002', 'SRS Travels',       'info@srs.com',       '080-33002233', 4.2, true, NOW(), NOW()),
  ('op-003', 'KSRTC',             'info@ksrtc.kar.in',  '080-22214001', 4.0, true, NOW(), NOW());

-- =============================================================================
-- BUSES
-- =============================================================================
INSERT INTO buses (bus_id, operator_id, bus_number, registration_number, bus_type, total_seats, amenities, is_active, created_at, updated_at)
VALUES
  ('bus-001', 'op-001', 'VRL-101', 'KA01AB1234', 'AC_SLEEPER',     40, 'WiFi,Charging Port,Blanket,Water Bottle', true, NOW(), NOW()),
  ('bus-002', 'op-001', 'VRL-102', 'KA01AB5678', 'NON_AC_SLEEPER', 40, 'Water Bottle,Blanket',                    true, NOW(), NOW()),
  ('bus-003', 'op-002', 'SRS-201', 'KA02CD1111', 'AC_SEATER',      40, 'WiFi,Charging Port,Snacks',               true, NOW(), NOW()),
  ('bus-004', 'op-002', 'SRS-202', 'KA02CD2222', 'AC_SLEEPER',     40, 'WiFi,Blanket,Charging Port',              true, NOW(), NOW()),
  ('bus-005', 'op-003', 'KSR-301', 'KA57FE3333', 'NON_AC_SEATER',  40, 'None',                                    true, NOW(), NOW()),
  ('bus-006', 'op-003', 'KSR-302', 'KA57FE4444', 'AC_SEATER',      40, 'Charging Port,WiFi',                      true, NOW(), NOW());

-- =============================================================================
-- BUS SEATS (20 LOWER + 20 UPPER for each bus)
-- =============================================================================
-- Helper: bus-001 seats
INSERT INTO bus_seats (bus_seat_id, bus_id, seat_number, seat_position, is_active, created_at, updated_at) VALUES
  ('bs-001-L01','bus-001','L1','LOWER',true,NOW(),NOW()),('bs-001-L02','bus-001','L2','LOWER',true,NOW(),NOW()),
  ('bs-001-L03','bus-001','L3','LOWER',true,NOW(),NOW()),('bs-001-L04','bus-001','L4','LOWER',true,NOW(),NOW()),
  ('bs-001-L05','bus-001','L5','LOWER',true,NOW(),NOW()),('bs-001-L06','bus-001','L6','LOWER',true,NOW(),NOW()),
  ('bs-001-L07','bus-001','L7','LOWER',true,NOW(),NOW()),('bs-001-L08','bus-001','L8','LOWER',true,NOW(),NOW()),
  ('bs-001-L09','bus-001','L9','LOWER',true,NOW(),NOW()),('bs-001-L10','bus-001','L10','LOWER',true,NOW(),NOW()),
  ('bs-001-L11','bus-001','L11','LOWER',true,NOW(),NOW()),('bs-001-L12','bus-001','L12','LOWER',true,NOW(),NOW()),
  ('bs-001-L13','bus-001','L13','LOWER',true,NOW(),NOW()),('bs-001-L14','bus-001','L14','LOWER',true,NOW(),NOW()),
  ('bs-001-L15','bus-001','L15','LOWER',true,NOW(),NOW()),('bs-001-L16','bus-001','L16','LOWER',true,NOW(),NOW()),
  ('bs-001-L17','bus-001','L17','LOWER',true,NOW(),NOW()),('bs-001-L18','bus-001','L18','LOWER',true,NOW(),NOW()),
  ('bs-001-L19','bus-001','L19','LOWER',true,NOW(),NOW()),('bs-001-L20','bus-001','L20','LOWER',true,NOW(),NOW()),
  ('bs-001-U01','bus-001','U1','UPPER',true,NOW(),NOW()),('bs-001-U02','bus-001','U2','UPPER',true,NOW(),NOW()),
  ('bs-001-U03','bus-001','U3','UPPER',true,NOW(),NOW()),('bs-001-U04','bus-001','U4','UPPER',true,NOW(),NOW()),
  ('bs-001-U05','bus-001','U5','UPPER',true,NOW(),NOW()),('bs-001-U06','bus-001','U6','UPPER',true,NOW(),NOW()),
  ('bs-001-U07','bus-001','U7','UPPER',true,NOW(),NOW()),('bs-001-U08','bus-001','U8','UPPER',true,NOW(),NOW()),
  ('bs-001-U09','bus-001','U9','UPPER',true,NOW(),NOW()),('bs-001-U10','bus-001','U10','UPPER',true,NOW(),NOW()),
  ('bs-001-U11','bus-001','U11','UPPER',true,NOW(),NOW()),('bs-001-U12','bus-001','U12','UPPER',true,NOW(),NOW()),
  ('bs-001-U13','bus-001','U13','UPPER',true,NOW(),NOW()),('bs-001-U14','bus-001','U14','UPPER',true,NOW(),NOW()),
  ('bs-001-U15','bus-001','U15','UPPER',true,NOW(),NOW()),('bs-001-U16','bus-001','U16','UPPER',true,NOW(),NOW()),
  ('bs-001-U17','bus-001','U17','UPPER',true,NOW(),NOW()),('bs-001-U18','bus-001','U18','UPPER',true,NOW(),NOW()),
  ('bs-001-U19','bus-001','U19','UPPER',true,NOW(),NOW()),('bs-001-U20','bus-001','U20','UPPER',true,NOW(),NOW());

-- bus-002 seats
INSERT INTO bus_seats (bus_seat_id, bus_id, seat_number, seat_position, is_active, created_at, updated_at) VALUES
  ('bs-002-L01','bus-002','L1','LOWER',true,NOW(),NOW()),('bs-002-L02','bus-002','L2','LOWER',true,NOW(),NOW()),
  ('bs-002-L03','bus-002','L3','LOWER',true,NOW(),NOW()),('bs-002-L04','bus-002','L4','LOWER',true,NOW(),NOW()),
  ('bs-002-L05','bus-002','L5','LOWER',true,NOW(),NOW()),('bs-002-L06','bus-002','L6','LOWER',true,NOW(),NOW()),
  ('bs-002-L07','bus-002','L7','LOWER',true,NOW(),NOW()),('bs-002-L08','bus-002','L8','LOWER',true,NOW(),NOW()),
  ('bs-002-L09','bus-002','L9','LOWER',true,NOW(),NOW()),('bs-002-L10','bus-002','L10','LOWER',true,NOW(),NOW()),
  ('bs-002-L11','bus-002','L11','LOWER',true,NOW(),NOW()),('bs-002-L12','bus-002','L12','LOWER',true,NOW(),NOW()),
  ('bs-002-L13','bus-002','L13','LOWER',true,NOW(),NOW()),('bs-002-L14','bus-002','L14','LOWER',true,NOW(),NOW()),
  ('bs-002-L15','bus-002','L15','LOWER',true,NOW(),NOW()),('bs-002-L16','bus-002','L16','LOWER',true,NOW(),NOW()),
  ('bs-002-L17','bus-002','L17','LOWER',true,NOW(),NOW()),('bs-002-L18','bus-002','L18','LOWER',true,NOW(),NOW()),
  ('bs-002-L19','bus-002','L19','LOWER',true,NOW(),NOW()),('bs-002-L20','bus-002','L20','LOWER',true,NOW(),NOW()),
  ('bs-002-U01','bus-002','U1','UPPER',true,NOW(),NOW()),('bs-002-U02','bus-002','U2','UPPER',true,NOW(),NOW()),
  ('bs-002-U03','bus-002','U3','UPPER',true,NOW(),NOW()),('bs-002-U04','bus-002','U4','UPPER',true,NOW(),NOW()),
  ('bs-002-U05','bus-002','U5','UPPER',true,NOW(),NOW()),('bs-002-U06','bus-002','U6','UPPER',true,NOW(),NOW()),
  ('bs-002-U07','bus-002','U7','UPPER',true,NOW(),NOW()),('bs-002-U08','bus-002','U8','UPPER',true,NOW(),NOW()),
  ('bs-002-U09','bus-002','U9','UPPER',true,NOW(),NOW()),('bs-002-U10','bus-002','U10','UPPER',true,NOW(),NOW()),
  ('bs-002-U11','bus-002','U11','UPPER',true,NOW(),NOW()),('bs-002-U12','bus-002','U12','UPPER',true,NOW(),NOW()),
  ('bs-002-U13','bus-002','U13','UPPER',true,NOW(),NOW()),('bs-002-U14','bus-002','U14','UPPER',true,NOW(),NOW()),
  ('bs-002-U15','bus-002','U15','UPPER',true,NOW(),NOW()),('bs-002-U16','bus-002','U16','UPPER',true,NOW(),NOW()),
  ('bs-002-U17','bus-002','U17','UPPER',true,NOW(),NOW()),('bs-002-U18','bus-002','U18','UPPER',true,NOW(),NOW()),
  ('bs-002-U19','bus-002','U19','UPPER',true,NOW(),NOW()),('bs-002-U20','bus-002','U20','UPPER',true,NOW(),NOW());

-- bus-003 seats (all LOWER for seater bus)
INSERT INTO bus_seats (bus_seat_id, bus_id, seat_number, seat_position, is_active, created_at, updated_at) VALUES
  ('bs-003-01','bus-003','1','LOWER',true,NOW(),NOW()),('bs-003-02','bus-003','2','LOWER',true,NOW(),NOW()),
  ('bs-003-03','bus-003','3','LOWER',true,NOW(),NOW()),('bs-003-04','bus-003','4','LOWER',true,NOW(),NOW()),
  ('bs-003-05','bus-003','5','LOWER',true,NOW(),NOW()),('bs-003-06','bus-003','6','LOWER',true,NOW(),NOW()),
  ('bs-003-07','bus-003','7','LOWER',true,NOW(),NOW()),('bs-003-08','bus-003','8','LOWER',true,NOW(),NOW()),
  ('bs-003-09','bus-003','9','LOWER',true,NOW(),NOW()),('bs-003-10','bus-003','10','LOWER',true,NOW(),NOW()),
  ('bs-003-11','bus-003','11','LOWER',true,NOW(),NOW()),('bs-003-12','bus-003','12','LOWER',true,NOW(),NOW()),
  ('bs-003-13','bus-003','13','LOWER',true,NOW(),NOW()),('bs-003-14','bus-003','14','LOWER',true,NOW(),NOW()),
  ('bs-003-15','bus-003','15','LOWER',true,NOW(),NOW()),('bs-003-16','bus-003','16','LOWER',true,NOW(),NOW()),
  ('bs-003-17','bus-003','17','LOWER',true,NOW(),NOW()),('bs-003-18','bus-003','18','LOWER',true,NOW(),NOW()),
  ('bs-003-19','bus-003','19','LOWER',true,NOW(),NOW()),('bs-003-20','bus-003','20','LOWER',true,NOW(),NOW()),
  ('bs-003-21','bus-003','21','UPPER',true,NOW(),NOW()),('bs-003-22','bus-003','22','UPPER',true,NOW(),NOW()),
  ('bs-003-23','bus-003','23','UPPER',true,NOW(),NOW()),('bs-003-24','bus-003','24','UPPER',true,NOW(),NOW()),
  ('bs-003-25','bus-003','25','UPPER',true,NOW(),NOW()),('bs-003-26','bus-003','26','UPPER',true,NOW(),NOW()),
  ('bs-003-27','bus-003','27','UPPER',true,NOW(),NOW()),('bs-003-28','bus-003','28','UPPER',true,NOW(),NOW()),
  ('bs-003-29','bus-003','29','UPPER',true,NOW(),NOW()),('bs-003-30','bus-003','30','UPPER',true,NOW(),NOW()),
  ('bs-003-31','bus-003','31','UPPER',true,NOW(),NOW()),('bs-003-32','bus-003','32','UPPER',true,NOW(),NOW()),
  ('bs-003-33','bus-003','33','UPPER',true,NOW(),NOW()),('bs-003-34','bus-003','34','UPPER',true,NOW(),NOW()),
  ('bs-003-35','bus-003','35','UPPER',true,NOW(),NOW()),('bs-003-36','bus-003','36','UPPER',true,NOW(),NOW()),
  ('bs-003-37','bus-003','37','UPPER',true,NOW(),NOW()),('bs-003-38','bus-003','38','UPPER',true,NOW(),NOW()),
  ('bs-003-39','bus-003','39','UPPER',true,NOW(),NOW()),('bs-003-40','bus-003','40','UPPER',true,NOW(),NOW());

-- =============================================================================
-- ROUTES
-- =============================================================================
INSERT INTO routes (route_id, source_city, destination_city, distance_km, duration_minutes, is_active, created_at, updated_at)
VALUES
  ('rt-001', 'Bangalore', 'Chennai',    346, 360, true, NOW(), NOW()),
  ('rt-002', 'Bangalore', 'Hyderabad',  574, 480, true, NOW(), NOW()),
  ('rt-003', 'Chennai',   'Hyderabad',  627, 540, true, NOW(), NOW());

-- =============================================================================
-- ROUTE STOPS (Bangalore → Chennai)
-- =============================================================================
INSERT INTO route_stops (route_stop_id, route_id, stop_name, stop_type, sequence_order, landmark, default_time_offset_minutes, created_at, updated_at)
VALUES
  ('rs-001-1', 'rt-001', 'Majestic Bus Stand, Bangalore', 'BOARDING', 1, 'Majestic Metro Station',  0, NOW(), NOW()),
  ('rs-001-2', 'rt-001', 'Hosur Toll',                   'TRANSIT',  2, 'Hosur Petrol Pump',       45, NOW(), NOW()),
  ('rs-001-3', 'rt-001', 'Vellore',                      'TRANSIT',  3, 'CMC Hospital',           150, NOW(), NOW()),
  ('rs-001-4', 'rt-001', 'Koyambedu, Chennai',           'DROPPING', 4, 'Koyambedu Bus Terminus', 360, NOW(), NOW()),
  ('rs-001-5', 'rt-001', 'Kilpauk, Chennai',             'DROPPING', 5, 'Kilpauk Medical College', 375, NOW(), NOW());

-- Route Stops (Bangalore → Hyderabad)
INSERT INTO route_stops (route_stop_id, route_id, stop_name, stop_type, sequence_order, landmark, default_time_offset_minutes, created_at, updated_at)
VALUES
  ('rs-002-1', 'rt-002', 'Majestic Bus Stand, Bangalore', 'BOARDING', 1, 'Majestic Metro Station',    0, NOW(), NOW()),
  ('rs-002-2', 'rt-002', 'Tumkur',                        'TRANSIT',  2, 'Tumkur Bus Stand',         60, NOW(), NOW()),
  ('rs-002-3', 'rt-002', 'Ananthapura',                   'TRANSIT',  3, 'Ananthapura Lake',        240, NOW(), NOW()),
  ('rs-002-4', 'rt-002', 'Mehdipatnam, Hyderabad',        'DROPPING', 4, 'Mehdipatnam X-Roads',     480, NOW(), NOW()),
  ('rs-002-5', 'rt-002', 'Secunderabad',                  'DROPPING', 5, 'Secunderabad Station',    510, NOW(), NOW());

-- =============================================================================
-- TRIPS (4 trips across routes)
-- =============================================================================
INSERT INTO trips (trip_id, bus_id, route_id, travel_date, departure_time, arrival_time, base_fare, is_cancelled, cancellation_reason, created_at, updated_at)
VALUES
  ('trip-001', 'bus-001', 'rt-001', CURDATE() + INTERVAL 1 DAY, '21:00:00', '03:00:00', 1200.00, false, NULL, NOW(), NOW()),
  ('trip-002', 'bus-002', 'rt-001', CURDATE() + INTERVAL 1 DAY, '22:00:00', '05:00:00',  899.00, false, NULL, NOW(), NOW()),
  ('trip-003', 'bus-003', 'rt-002', CURDATE() + INTERVAL 2 DAY, '20:00:00', '04:00:00', 1500.00, false, NULL, NOW(), NOW()),
  ('trip-004', 'bus-004', 'rt-001', CURDATE() + INTERVAL 3 DAY, '23:00:00', '05:30:00', 1100.00, false, NULL, NOW(), NOW());

-- =============================================================================
-- TRIP SEATS for trip-001 (bus-001: 40 seats, AC Sleeper)
-- =============================================================================
INSERT INTO trip_seats (trip_seat_id, trip_id, bus_seat_id, seat_status, seat_fare, lock_expiry_time, locked_by_user_id, version, created_at, updated_at) VALUES
  ('ts-t1-L01','trip-001','bs-001-L01','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L02','trip-001','bs-001-L02','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L03','trip-001','bs-001-L03','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L04','trip-001','bs-001-L04','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L05','trip-001','bs-001-L05','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L06','trip-001','bs-001-L06','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L07','trip-001','bs-001-L07','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L08','trip-001','bs-001-L08','BOOKED',   1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L09','trip-001','bs-001-L09','BOOKED',   1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L10','trip-001','bs-001-L10','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L11','trip-001','bs-001-L11','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L12','trip-001','bs-001-L12','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L13','trip-001','bs-001-L13','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L14','trip-001','bs-001-L14','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L15','trip-001','bs-001-L15','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L16','trip-001','bs-001-L16','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L17','trip-001','bs-001-L17','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L18','trip-001','bs-001-L18','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L19','trip-001','bs-001-L19','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-L20','trip-001','bs-001-L20','AVAILABLE',1200.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U01','trip-001','bs-001-U01','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U02','trip-001','bs-001-U02','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U03','trip-001','bs-001-U03','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U04','trip-001','bs-001-U04','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U05','trip-001','bs-001-U05','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U06','trip-001','bs-001-U06','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U07','trip-001','bs-001-U07','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U08','trip-001','bs-001-U08','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U09','trip-001','bs-001-U09','BOOKED',   1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U10','trip-001','bs-001-U10','BOOKED',   1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U11','trip-001','bs-001-U11','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U12','trip-001','bs-001-U12','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U13','trip-001','bs-001-U13','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U14','trip-001','bs-001-U14','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U15','trip-001','bs-001-U15','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U16','trip-001','bs-001-U16','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U17','trip-001','bs-001-U17','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U18','trip-001','bs-001-U18','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U19','trip-001','bs-001-U19','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t1-U20','trip-001','bs-001-U20','AVAILABLE',1000.00,NULL,NULL,0,NOW(),NOW());

-- Trip-002 seats (bus-002, Non-AC Sleeper)
INSERT INTO trip_seats (trip_seat_id, trip_id, bus_seat_id, seat_status, seat_fare, lock_expiry_time, locked_by_user_id, version, created_at, updated_at) VALUES
  ('ts-t2-L01','trip-002','bs-002-L01','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L02','trip-002','bs-002-L02','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L03','trip-002','bs-002-L03','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L04','trip-002','bs-002-L04','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L05','trip-002','bs-002-L05','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L06','trip-002','bs-002-L06','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L07','trip-002','bs-002-L07','BOOKED',   899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L08','trip-002','bs-002-L08','BOOKED',   899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L09','trip-002','bs-002-L09','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L10','trip-002','bs-002-L10','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L11','trip-002','bs-002-L11','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L12','trip-002','bs-002-L12','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L13','trip-002','bs-002-L13','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L14','trip-002','bs-002-L14','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L15','trip-002','bs-002-L15','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L16','trip-002','bs-002-L16','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L17','trip-002','bs-002-L17','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L18','trip-002','bs-002-L18','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L19','trip-002','bs-002-L19','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-L20','trip-002','bs-002-L20','AVAILABLE',899.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U01','trip-002','bs-002-U01','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U02','trip-002','bs-002-U02','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U03','trip-002','bs-002-U03','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U04','trip-002','bs-002-U04','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U05','trip-002','bs-002-U05','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U06','trip-002','bs-002-U06','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U07','trip-002','bs-002-U07','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U08','trip-002','bs-002-U08','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U09','trip-002','bs-002-U09','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U10','trip-002','bs-002-U10','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U11','trip-002','bs-002-U11','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U12','trip-002','bs-002-U12','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U13','trip-002','bs-002-U13','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U14','trip-002','bs-002-U14','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U15','trip-002','bs-002-U15','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U16','trip-002','bs-002-U16','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U17','trip-002','bs-002-U17','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U18','trip-002','bs-002-U18','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U19','trip-002','bs-002-U19','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t2-U20','trip-002','bs-002-U20','AVAILABLE',749.00,NULL,NULL,0,NOW(),NOW());

-- Trip-003 seats (bus-003, AC Seater, Blr → Hyd)
INSERT INTO trip_seats (trip_seat_id, trip_id, bus_seat_id, seat_status, seat_fare, lock_expiry_time, locked_by_user_id, version, created_at, updated_at) VALUES
  ('ts-t3-01','trip-003','bs-003-01','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-02','trip-003','bs-003-02','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-03','trip-003','bs-003-03','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-04','trip-003','bs-003-04','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-05','trip-003','bs-003-05','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-06','trip-003','bs-003-06','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-07','trip-003','bs-003-07','BOOKED',   1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-08','trip-003','bs-003-08','BOOKED',   1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-09','trip-003','bs-003-09','BOOKED',   1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-10','trip-003','bs-003-10','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-11','trip-003','bs-003-11','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-12','trip-003','bs-003-12','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-13','trip-003','bs-003-13','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-14','trip-003','bs-003-14','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-15','trip-003','bs-003-15','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-16','trip-003','bs-003-16','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-17','trip-003','bs-003-17','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-18','trip-003','bs-003-18','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-19','trip-003','bs-003-19','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-20','trip-003','bs-003-20','AVAILABLE',1500.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-21','trip-003','bs-003-21','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-22','trip-003','bs-003-22','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-23','trip-003','bs-003-23','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-24','trip-003','bs-003-24','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-25','trip-003','bs-003-25','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-26','trip-003','bs-003-26','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-27','trip-003','bs-003-27','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-28','trip-003','bs-003-28','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-29','trip-003','bs-003-29','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-30','trip-003','bs-003-30','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-31','trip-003','bs-003-31','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-32','trip-003','bs-003-32','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-33','trip-003','bs-003-33','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-34','trip-003','bs-003-34','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-35','trip-003','bs-003-35','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-36','trip-003','bs-003-36','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-37','trip-003','bs-003-37','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-38','trip-003','bs-003-38','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-39','trip-003','bs-003-39','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW()),
  ('ts-t3-40','trip-003','bs-003-40','AVAILABLE',1300.00,NULL,NULL,0,NOW(),NOW());
