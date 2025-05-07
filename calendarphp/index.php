<?php
// Connexion à la base de données
$pdo = new PDO('mysql:host=localhost;dbname=calendar2', 'root', '', array(PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION));

// Récupération des salles
$salles_query = "SELECT * FROM salles";
$salles = $pdo->query($salles_query)->fetchAll(PDO::FETCH_ASSOC);

// Récupération des réservations
$reservations_query = "SELECT r.*, s.nom as salle_nom 
                      FROM reservations r 
                      JOIN salles s ON r.salle_id = s.id";
$reservations_result = $pdo->query($reservations_query);

$reservations = [];
while ($reservation = $reservations_result->fetch(PDO::FETCH_ASSOC)) {
    $reservations[] = [
        'id' => $reservation['id'],
        'title' => $reservation['salle_nom'],
        'start' => $reservation['debut'],
        'end' => $reservation['fin']
    ];
}
?>

<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Calendrier des Réservations</title>
    <link href='https://cdnjs.cloudflare.com/ajax/libs/fullcalendar/3.10.2/fullcalendar.min.css' rel='stylesheet' />
    <script src='https://cdnjs.cloudflare.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>
    <script src='https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js'></script>
    <script src='https://cdnjs.cloudflare.com/ajax/libs/fullcalendar/3.10.2/fullcalendar.min.js'></script>
    <script src='https://cdnjs.cloudflare.com/ajax/libs/fullcalendar/3.10.2/locale/fr.js'></script>
    <style>
        body {
            margin: 40px 10px;
            padding: 0;
            font-family: Arial, sans-serif;
        }
        #calendar {
            max-width: 1100px;
            margin: 0 auto;
        }
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0,0,0,0.4);
        }
        .modal-content {
            background-color: white;
            margin: 15% auto;
            padding: 20px;
            border: 1px solid #888;
            width: 50%;
            border-radius: 5px;
        }
        .close {
            float: right;
            font-size: 28px;
            font-weight: bold;
            cursor: pointer;
        }
        form {
            margin-top: 20px;
        }
        form label {
            display: block;
            margin: 10px 0 5px;
        }
        form select, form input {
            width: 100%;
            padding: 8px;
            margin-bottom: 10px;
        }
        form input[type="submit"] {
            background-color: #4CAF50;
            color: white;
            border: none;
            padding: 10px 20px;
            cursor: pointer;
        }
        .fc-time-grid-container {
            height: auto !important;
            min-height: 500px !important;
        }
        .fc-widget-content {
            background-color: #ffffff;
        }
        .fc-widget-header {
            background-color: #f8f9fa;
        }
        .fc-time-grid .fc-slats td {
            height: 30px;
        }
        .fc-event {
            border: none;
            border-radius: 3px;
            padding: 2px;
            margin: 1px;
        }
        .fc-time-grid-event {
            border-radius: 3px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.12);
        }
        .fc-time-grid-event .fc-time {
            font-weight: bold;
            font-size: 0.9em;
        }
        .fc-time-grid-event .fc-title {
            padding: 0 2px;
        }
        select {
            width: 100%;
            padding: 8px;
            margin-bottom: 15px;
            border: 1px solid #ddd;
            border-radius: 4px;
        }
        
        .modal-content {
            max-width: 400px;
            width: 90%;
        }
        
        label {
            font-weight: bold;
            color: #333;
        }
        input[type="submit"]:disabled {
            background-color: #cccccc;
            cursor: not-allowed;
        }
        .error-message {
            color: #ff0000;
            font-size: 0.9em;
            margin-top: 5px;
            display: none;
        }
    </style>
</head>
<body>
    <div id='calendar'></div>

    <!-- Modal pour la réservation -->
    <div id="reservationModal" class="modal">
        <div class="modal-content">
            <span class="close">&times;</span>
            <h2>Nouvelle réservation</h2>
            <form id="reservationForm" action="reserve.php" method="POST">
                <input type="hidden" name="date" id="date">
                
                <label for="salle_id">Salle :</label>
                <select name="salle_id" id="salle_id" required>
                    <?php foreach ($salles as $salle): ?>
                        <option value="<?= $salle['id'] ?>"><?= htmlspecialchars($salle['nom']) ?></option>
                    <?php endforeach; ?>
                </select>

                <label for="heure_debut">Heure de début :</label>
                <select name="heure_debut" id="heure_debut" required>
                    <?php
                    for ($h = 7; $h < 20; $h++) {
                        for ($m = 0; $m < 60; $m += 30) {
                            $time = sprintf('%02d:%02d', $h, $m);
                            echo "<option value='$time'>$time</option>";
                        }
                    }
                    ?>
                </select>

                <label for="heure_fin">Heure de fin :</label>
                <select name="heure_fin" id="heure_fin" required>
                    <?php
                    for ($h = 7; $h < 20; $h++) {
                        for ($m = 0; $m < 60; $m += 30) {
                            $time = sprintf('%02d:%02d', $h, $m);
                            echo "<option value='$time'>$time</option>";
                        }
                    }
                    ?>
                </select>

                <input type="submit" value="Réserver">
            </form>
        </div>
    </div>

    <script>
        $(document).ready(function() {
            $('#calendar').fullCalendar({
                header: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'month,agendaWeek,agendaDay'
                },
                defaultView: 'month',
                locale: 'fr',
                slotDuration: '00:30:00',
                slotMinTime: '07:00:00',
                slotMaxTime: '20:00:00',
                height: 'auto',
                allDaySlot: false,
                minTime: '07:00:00',
                maxTime: '20:00:00',
                slotLabelFormat: 'HH:mm',
                timeFormat: 'HH:mm',
                axisFormat: 'HH:mm',
                
                views: {
                    agendaWeek: {
                        columnHeaderFormat: 'ddd D/MM',
                        titleFormat: 'D MMMM YYYY',
                        slotLabelInterval: '00:30:00'
                    },
                    agendaDay: {
                        titleFormat: 'dddd D MMMM YYYY'
                    }
                },
                
                events: <?= json_encode($reservations) ?>,
                selectable: true,
                selectHelper: true,
                select: function(start, end) {
                    $('#date').val(start.format('YYYY-MM-DD'));
                    
                    $('#heure_debut').val(start.format('HH:mm'));
                    $('#heure_fin').val(end.format('HH:mm'));
                    
                    $('#reservationModal').show();
                },
                eventClick: function(event) {
                    alert('Réservation : ' + event.title + 
                          '\nDébut : ' + event.start.format('DD/MM/YYYY HH:mm') + 
                          '\nFin : ' + event.end.format('DD/MM/YYYY HH:mm'));
                },
                eventRender: function(event, element) {
                    element.css('font-size', '0.9em');
                    element.find('.fc-time').css('font-weight', 'bold');
                    element.find('.fc-title').css('padding', '2px');
                }
            });

            // Fermeture du modal
            $('.close').click(function() {
                $('#reservationModal').hide();
            });

            $(window).click(function(event) {
                if ($(event.target).is('.modal')) {
                    $('.modal').hide();
                }
            });

            // Validation du formulaire
            $('#reservationForm').on('submit', function(e) {
                e.preventDefault();
                
                var date = $('#date').val();
                var heureDebut = $('#heure_debut').val();
                var heureFin = $('#heure_fin').val();
                var salle_id = $('#salle_id').val();
                
                // Créer les dates complètes
                var debut = date + ' ' + heureDebut + ':00';
                var fin = date + ' ' + heureFin + ':00';
                
                // Validation basique
                if (heureDebut >= heureFin) {
                    alert("L'heure de fin doit être après l'heure de début");
                    return false;
                }

                // Vérifier la disponibilité avant de soumettre
                $.ajax({
                    url: 'check_disponibilite.php',
                    method: 'POST',
                    data: {
                        salle_id: salle_id,
                        debut: debut,
                        fin: fin
                    },
                    dataType: 'json',
                    success: function(response) {
                        if (response.error) {
                            alert('Erreur : ' + response.error);
                            return;
                        }
                        
                        if (response.disponible) {
                            // Ajouter les dates au formulaire
                            $('<input>').attr({
                                type: 'hidden',
                                name: 'debut',
                                value: debut
                            }).appendTo('#reservationForm');
                            
                            $('<input>').attr({
                                type: 'hidden',
                                name: 'fin',
                                value: fin
                            }).appendTo('#reservationForm');
                            
                            // Soumettre le formulaire
                            $('#reservationForm')[0].submit();
                        } else {
                            alert('Cette salle est déjà réservée pour ce créneau horaire.');
                        }
                    },
                    error: function() {
                        alert('Erreur lors de la vérification de la disponibilité');
                    }
                });
            });

            // Ajouter une vérification en temps réel lors du changement de salle ou d'horaire
            $('#salle_id, #heure_debut, #heure_fin').on('change', function() {
                var date = $('#date').val();
                var heureDebut = $('#heure_debut').val();
                var heureFin = $('#heure_fin').val();
                var salle_id = $('#salle_id').val();
                
                if (!date || !heureDebut || !heureFin || heureDebut >= heureFin) {
                    return;
                }
                
                var debut = date + ' ' + heureDebut + ':00';
                var fin = date + ' ' + heureFin + ':00';
                
                $.ajax({
                    url: 'check_disponibilite.php',
                    method: 'POST',
                    data: {
                        salle_id: salle_id,
                        debut: debut,
                        fin: fin
                    },
                    dataType: 'json',
                    success: function(response) {
                        if (response.disponible) {
                            $('input[type="submit"]').prop('disabled', false);
                            $('input[type="submit"]').css('opacity', '1');
                        } else {
                            $('input[type="submit"]').prop('disabled', true);
                            $('input[type="submit"]').css('opacity', '0.5');
                        }
                    }
                });
            });
        });
    </script>
</body>
</html> 