<?php
try {
    $pdo = new PDO('mysql:host=localhost;dbname=calendar2', 'root', '', array(PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION));
    
    $query = "INSERT INTO reservations (salle_id, debut, fin) VALUES (:salle_id, :debut, :fin)";
    $stmt = $pdo->prepare($query);
    $stmt->execute([
        'salle_id' => $_POST['salle_id'],
        'debut' => $_POST['debut'],
        'fin' => $_POST['fin']
    ]);

    header('Location: index.php');
} catch (PDOException $e) {
    echo "Erreur : " . $e->getMessage();
}
?> 