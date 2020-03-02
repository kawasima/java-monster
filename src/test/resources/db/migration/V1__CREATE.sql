CREATE TABLE user_table(
  id INTEGER,
  name VARCHAR(255)
);

CREATE TABLE user_address_table(
  id IDENTITY,
  user_id INTEGER,
  address VARCHAR(255)
);

INSERT INTO user_table(id, name) VALUES
  (1, 'AAA'),
  (2, 'BBB'),
  (3, 'CCC');

INSERT INTO user_address_table(id, user_id, address) VALUES
  (1, 1, 'Tokyo'),
  (2, 1, 'Osaka');
