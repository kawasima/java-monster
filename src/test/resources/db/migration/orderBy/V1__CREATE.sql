CREATE TABLE accounts(
  id INTEGER NOT NULL,
  email_address VARCHAR(255) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE comments(
  id SERIAL NOT NULL,
  body VARCHAR(2000) NOT NULL,
  author_id INTEGER NOT NULL,
  PRIMARY KEY(id)
);

INSERT INTO accounts(id, email_address) VALUES
  (1, 'aaa@example.com'),
  (2, 'bbb@example.com'),
  (3, 'ccc@example.com'),
  (4, 'ddd@example.com'),
  (5, 'eee@example.com');

INSERT INTO comments(body, author_id) VALUES
  ('I have a pen', 1),
  ('I have two pens', 2),
  ('I have three pens', 3),
  ('I have four pens', 4),
  ('I have five pens', 5),
  ('I have six pens', 1),
  ('I have seven pens', 1),
  ('I have eight pens', 2);
