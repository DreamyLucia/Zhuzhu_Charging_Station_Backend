--修改root用户密码以使用系统
CREATE USER 'llzj'@'localhost' IDENTIFIED BY 'Lxy20040513';
ALTER 'llzj'@'localhost' IDENTIFIED BY 'Lxy20040513' PASSWORD EXPIRE NEVER;
ALTER USER 'llzj'@'localhost' IDENTIFIED WITH mysql_native_password BY 'Lxy20040513';
FLUSH PRIVILEGES;

--删除原有重名数据库
DROP DATABASE IF EXISTS charge_db;

--创建需要的数据库
CREATE DATABASE charge_db CHARACTER SET utf8;
GRANT ALL PRIVILEGES ON charge_db.* TO 'llzj'@'localhost';
FLUSH PRIVILEGES;