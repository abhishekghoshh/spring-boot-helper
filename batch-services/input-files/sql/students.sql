create database university;
use university;
CREATE TABLE students (
    id varchar(255) NOT NULL,
    firstName varchar(255) NOT NULL,
    lastName varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    PRIMARY KEY (ID)
);
INSERT INTO students(id,firstName,lastName,email) VALUES ('111111','John','Doe','jdoe@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111112','Jane','Smith','jsmith@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111113','Sarah','Thomas','sthomas@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111114','Frank','Brown','fbrown@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111115','Mike','Davis','mdavis@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111116','Jennifer','Wilson','jwilson@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111117','Jessica','Garcia','jgarcia@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111118','Fred','Clark','fclark@example.com');
INSERT INTO students(id,firstName,lastName,email) VALUES ('111119','Bob','Lopez','blopez@example.com');
