
use SparkBitcoinSms;

drop table if exists Users;

CREATE TABLE Users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  firstName VARCHAR (50),
  lastName VARCHAR (50),
  phoneNumber VARCHAR (25),
  balance BIGINT UNSIGNED,
  recoveryPassword VARCHAR (50),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

drop table if exists TrustedUsers;

CREATE TABLE TrustedUsers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumber VARCHAR (25),
  PRIMARY KEY (id)
);

drop table if exists Wallets;

CREATE TABLE Wallets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  userId BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id)
);
