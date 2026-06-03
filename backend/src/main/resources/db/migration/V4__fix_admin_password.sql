-- Flyway Migration: V4__fix_admin_password.sql
-- GIS Platform - Fix admin user password hash
-- Author: GIS Platform Team
-- Description: Update admin user password hash to verified BCrypt hash

-- Fix admin password hash (verified against plaintext: admin123)
UPDATE sys_user SET password = '$2a$10$4exXS7dfb3HRJB416D79KOChjl2d4hmJOSm2Cycdo7mkE3onlysfO'
WHERE username = 'admin';

-- Verify update
SELECT 'Admin password updated' AS status, username FROM sys_user WHERE username = 'admin';
