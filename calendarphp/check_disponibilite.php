<?php
try {
    $pdo = new PDO('mysql:host=localhost;dbname=calendar2', 'root', '', array(PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION));
    
    $salle_id = $_POST['salle_id'];
    $debut = $_POST['debut'];
    $fin = $_POST['fin'];

    // Vérifier s'il existe des réservations qui se chevauchent
    $query = "SELECT COUNT(*) as count FROM reservations 
              WHERE salle_id = :salle_id 
              AND (
                  (debut <= :debut AND fin > :debut) OR
                  (debut < :fin AND fin >= :fin) OR
                  (debut >= :debut AND fin <= :fin)
              )";
              
    $stmt = $pdo->prepare($query);
    $stmt->execute([
        'salle_id' => $salle_id,
        'debut' => $debut,
        'fin' => $fin
    ]);
    
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    echo json_encode(['disponible' => ($result['count'] == 0)]);
    
} catch (PDOException $e) {
    echo json_encode(['error' => $e->getMessage()]);
}
?> 