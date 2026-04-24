
Create database expenses_tracker;

use  expenses_tracker;

SELECT * FROM Expense;

truncate TABLE Expense ;

DELETE FROM Expense WHERE category_id = Null;

SELECT * FROM category;

SELECT * FROM client;
SELECT * FROM user;
SELECT * FROM role;

ALTER TABLE client ADD COLUMN budget INT ;

INSERT INTO category (id, name) VALUES
(1, 'Groceries'),
(2, 'Utilities'),
(3, 'Transportation'),
(4, 'Dining Out'),
(5, 'Entertainment'),
(6, 'Shopping'),
(7, 'Travel'),
(8, 'Education');
