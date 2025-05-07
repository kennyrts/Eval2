-- Création de la table pour les salles
CREATE TABLE salles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nom VARCHAR(50) NOT NULL    
);

-- Création de la table pour les réservations
CREATE TABLE reservations (
    id INT PRIMARY KEY AUTO_INCREMENT,
    salle_id INT,
    debut DATETIME NOT NULL,
    fin DATETIME NOT NULL,    
    FOREIGN KEY (salle_id) REFERENCES salles(id)
);

-- Insertion des salles
INSERT INTO salles (nom) VALUES 
('Salle A101'),
('Salle B202'),
('Salle C303'),
('Laboratoire 1'),
('Salle de conférence');

-- Insertion des réservations (pour le mois en cours)
INSERT INTO reservations (salle_id, debut, fin) VALUES 
(1, '2025-03-20 09:00:00', '2025-03-20 12:00:00'),
(2, '2025-03-20 14:00:00', '2025-03-20 16:00:00'),
(3, '2025-03-21 10:00:00', '2025-03-21 11:30:00'),
(4, '2025-03-22 09:00:00', '2025-03-22 17:00:00'),
(5, '2025-03-25 13:00:00', '2025-03-25 15:00:00'),
(1, '2025-03-26 09:00:00', '2025-03-26 10:30:00'),
(2, '2025-03-27 15:00:00', '2025-03-27 17:00:00'),
(3, '2025-03-28 11:00:00', '2025-03-28 12:00:00'),
(4, '2025-03-29 14:00:00', '2025-03-29 16:00:00'),
(5, '2025-03-30 10:00:00', '2025-03-30 12:00:00');