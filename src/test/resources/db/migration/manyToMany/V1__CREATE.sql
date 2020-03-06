CREATE TABLE users(
  id INTEGER NOT NULL,
  name VARCHAR(255),
  PRIMARY KEY(id)
);

CREATE TABLE groups(
  id INTEGER NOT NULL,
  name VARCHAR(255),
  PRIMARY KEY(id)
);

CREATE TABLE memberships (
  user_id INTEGER NOT NULL,
  group_id INTEGER NOT NULL,
  PRIMARY KEY(user_id, group_id)
);

ALTER TABLE memberships ADD
  FOREIGN KEY(user_id) REFERENCES users(id);

ALTER TABLE memberships ADD
  FOREIGN KEY(group_id) REFERENCES groups(id);

INSERT INTO users(id, name) VALUES
  (1, 'AAA'),
  (2, 'BBB'),
  (3, 'CCC'),
  (4, 'CCC');

INSERT INTO groups(id, name) VALUES
  (1, 'Administrator'),
  (2, 'Sales'),
  (3, 'Accounts'),
  (4, 'Developers');

INSERT INTO memberships(user_id, group_id) VALUES
  (1, 1),
  (1, 2),
  (2, 2),
  (3, 3),
  (4, 4),
  (4, 3);