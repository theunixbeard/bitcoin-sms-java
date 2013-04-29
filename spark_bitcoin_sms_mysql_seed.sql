
use SparkBitcoinSms;

drop table if exists Users;

CREATE TABLE Users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR (50),
  phoneNumber VARCHAR (25),
  balance BIGINT UNSIGNED,
  recoveryPassword VARCHAR (50),
  bitcoinAddress VARCHAR(40),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumber),
  index (bitcoinAddress),
  PRIMARY KEY (id)
);

drop table if exists UnconfirmedUsers;

CREATE TABLE UnconfirmedUsers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR (50),
  phoneNumber VARCHAR (25),
  balance BIGINT UNSIGNED,
  recoveryPassword VARCHAR (50),
  confirmationCode VARCHAR(20),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumber),
  PRIMARY KEY (id)
);

drop table if exists UnconfirmedTransfers;

CREATE TABLE UnconfirmedTransfers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumberTo VARCHAR (25),
  phoneNumberFrom VARCHAR (25),
  amount BIGINT UNSIGNED,
  confirmationCode VARCHAR(20),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumberTo),
  index (phoneNumberFrom),
  PRIMARY KEY (id)
);

drop table if exists Transfers;

CREATE TABLE Transfers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumberTo VARCHAR (25),
  phoneNumberFrom VARCHAR (25),
  amount BIGINT UNSIGNED,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumberTo),
  index (phoneNumberFrom),
  PRIMARY KEY (id)
);

drop table if exists UnconfirmedWithdrawals;

CREATE TABLE UnconfirmedWithdrawals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumber VARCHAR (25),
  amount BIGINT UNSIGNED,
  confirmationCode VARCHAR(20),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumber),
  PRIMARY KEY (id)
);

drop table if exists Withdrawals;

CREATE TABLE Withdrawals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumber VARCHAR (25),
  amount BIGINT UNSIGNED,
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  index (phoneNumber),
  PRIMARY KEY (id)
);

drop table if exists TrustedUsers;

CREATE TABLE TrustedUsers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  phoneNumber VARCHAR (25),
  index (phoneNumber),
  PRIMARY KEY (id)
);

drop table if exists Wallets;

CREATE TABLE Wallets (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  userId BIGINT UNSIGNED NOT NULL,
  index (userId),
  PRIMARY KEY (id)
);
