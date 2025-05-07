# Tutoriel: Créer une nouvelle fonctionnalité pour Custom Tools

Ce tutoriel vous guidera pas à pas dans la création d'une nouvelle fonctionnalité pour le module Custom Tools. Nous allons développer une fonctionnalité simple de compteur de visites qui illustrera l'architecture frontend/backend.

## Prérequis

- Accès au code source du module `custom_tools`
- Connaissances de base en Python et JavaScript
- Environnement de développement ERPNext fonctionnel

## Étape 1: Planifier la fonctionnalité

Notre fonctionnalité de compteur de visites va:
1. Afficher un bouton dans le menu d'aide
2. Quand on clique dessus, montrer une boîte de dialogue avec le nombre de visites
3. Incrémenter le compteur à chaque visite
4. Stocker ce compteur dans la base de données ERPNext

## Étape 2: Créer le backend (Python)

Commençons par créer le fichier backend qui gérera la logique du compteur.

1. Créez un nouveau fichier `api/compteur_visites.py`:

```python
import frappe
from frappe import _

@frappe.whitelist()
def get_increment_counter():
    """
    Récupère et incrémente le compteur de visites.
    Crée le compteur s'il n'existe pas.
    """
    try:
        # Obtenir la valeur actuelle du compteur
        counter_key = "visit_counter"
        current_value = frappe.cache().get_value(counter_key)
        
        if current_value is None:
            # Le compteur n'existe pas encore, l'initialiser
            current_value = 0
        
        # Incrémenter le compteur
        new_value = current_value + 1
        
        # Stocker la nouvelle valeur
        frappe.cache().set_value(counter_key, new_value)
        
        # Journaliser l'événement
        frappe.logger().info(f"Compteur de visites incrémenté: {new_value}")
        
        # Retourner la réponse
        return {
            "success": True,
            "count": new_value,
            "message": _("Compteur incrémenté avec succès")
        }
        
    except Exception as e:
        error_msg = f"Erreur lors de l'incrémentation du compteur: {str(e)}"
        frappe.logger().error(error_msg)
        return {
            "success": False,
            "error": error_msg
        }


@frappe.whitelist()
def reset_counter():
    """
    Réinitialise le compteur de visites à zéro.
    """
    try:
        # Réinitialiser le compteur
        counter_key = "visit_counter"
        frappe.cache().set_value(counter_key, 0)
        
        # Journaliser l'événement
        frappe.logger().info("Compteur de visites réinitialisé")
        
        return {
            "success": True,
            "count": 0,
            "message": _("Compteur réinitialisé avec succès")
        }
        
    except Exception as e:
        error_msg = f"Erreur lors de la réinitialisation du compteur: {str(e)}"
        frappe.logger().error(error_msg)
        return {
            "success": False,
            "error": error_msg
        }
```

## Étape 3: Créer le frontend (JavaScript)

Maintenant, créons l'interface utilisateur qui interagira avec notre backend.

1. Créez un nouveau fichier `public/js/compteur_visites.js`:

```javascript
// Module de compteur de visites
console.log("Compteur de visites chargé");

// Initialisation quand le document est prêt
$(document).ready(function() {
    // Attendre que l'interface soit complètement chargée
    setTimeout(function() {
        // Vérifier si le bouton existe déjà
        if (!$('#compteur-visites-menu-item').length) {
            console.log("Ajout du bouton compteur de visites");
            
            // Créer l'élément de menu
            var compteurMenuItem = $(`
                <li id="compteur-visites-menu-item">
                    <a href="#" class="dropdown-item">
                        <span class="compteur-icon">
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-graph-up" viewBox="0 0 16 16">
                                <path fill-rule="evenodd" d="M0 0h1v15h15v1H0V0Zm10 3.5a.5.5 0 0 1 .5-.5h4a.5.5 0 0 1 .5.5v4a.5.5 0 0 1-1 0V4.9l-3.613 4.417a.5.5 0 0 1-.74.037L7.06 6.767l-3.656 5.027a.5.5 0 0 1-.808-.588l4-5.5a.5.5 0 0 1 .758-.06l2.609 2.61L13.445 4H10.5a.5.5 0 0 1-.5-.5Z"/>
                            </svg>
                        </span>
                        <span class="ml-2">Compteur de visites</span>
                    </a>
                </li>
            `);
            
            // Ajouter l'élément au menu Aide
            $('.dropdown-help .dropdown-menu').append(compteurMenuItem);
            
            // Ajouter l'événement de clic
            $('#compteur-visites-menu-item').on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                // Fermer le dropdown
                $('.dropdown-help .dropdown-toggle').dropdown('hide');
                
                // Ouvrir la boîte de dialogue du compteur
                ouvrirCompteurVisites();
            });
        }
    }, 2000);
});

// Fonction pour ouvrir la boîte de dialogue du compteur
function ouvrirCompteurVisites() {
    // Créer d'abord la boîte de dialogue avec un indicateur de chargement
    var d = new frappe.ui.Dialog({
        title: 'Compteur de Visites',
        fields: [
            {
                fieldname: 'counter_loading_html',
                fieldtype: 'HTML',
                options: `
                    <div class="text-center">
                        <p><b>Chargement du compteur...</b></p>
                        <div class="lds-dual-ring"></div>
                    </div>
                `
            }
        ],
        primary_action_label: 'Fermer',
        primary_action: function() {
            d.hide();
        },
        secondary_action_label: 'Réinitialiser',
        secondary_action: function() {
            // Réinitialiser le compteur
            resetCompteur(d);
        }
    });
    
    // Ajouter le style pour l'animation de chargement
    frappe.dom.set_style(`
        .lds-dual-ring {
            display: inline-block;
            width: 80px;
            height: 80px;
        }
        .lds-dual-ring:after {
            content: " ";
            display: block;
            width: 64px;
            height: 64px;
            margin: 8px;
            border-radius: 50%;
            border: 6px solid #4caf50;
            border-color: #4caf50 transparent #4caf50 transparent;
            animation: lds-dual-ring 1.2s linear infinite;
        }
        @keyframes lds-dual-ring {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .counter-value {
            font-size: 48px;
            font-weight: bold;
            color: #4caf50;
        }
    `);
    
    d.show();
    
    // Appeler le backend pour incrémenter et obtenir le compteur
    frappe.call({
        method: 'custom_tools.api.compteur_visites.get_increment_counter',
        callback: function(response) {
            if (response.message && response.message.success) {
                // Mettre à jour le contenu de la boîte de dialogue avec le compteur
                d.set_field_options('counter_loading_html', `
                    <div class="text-center mb-4">
                        <h4>Nombre de visites</h4>
                        <div class="counter-value">${response.message.count}</div>
                        <p class="text-muted mt-3">Ce compteur est incrémenté à chaque fois que vous ouvrez cette boîte de dialogue.</p>
                    </div>
                `);
            } else {
                // Afficher un message d'erreur
                d.set_field_options('counter_loading_html', `
                    <div class="text-center">
                        <p class="text-danger"><b>Erreur lors du chargement du compteur</b></p>
                        <p>${response.message && response.message.error || "Une erreur inconnue s'est produite."}</p>
                    </div>
                `);
            }
        },
        error: function(err) {
            // Afficher un message d'erreur
            d.set_field_options('counter_loading_html', `
                <div class="text-center">
                    <p class="text-danger"><b>Erreur lors du chargement du compteur</b></p>
                    <p>Impossible de communiquer avec le serveur</p>
                </div>
            `);
            console.error("Erreur API compteur:", err);
        }
    });
}

// Fonction pour réinitialiser le compteur
function resetCompteur(dialog) {
    frappe.confirm(
        'Êtes-vous sûr de vouloir réinitialiser le compteur de visites à zéro?',
        function() {
            // Appeler l'API pour réinitialiser
            frappe.call({
                method: 'custom_tools.api.compteur_visites.reset_counter',
                freeze: true,
                freeze_message: __('Réinitialisation du compteur...'),
                callback: function(response) {
                    if (response.message && response.message.success) {
                        // Mettre à jour l'affichage
                        dialog.set_field_options('counter_loading_html', `
                            <div class="text-center mb-4">
                                <h4>Nombre de visites</h4>
                                <div class="counter-value">0</div>
                                <p class="text-muted mt-3">Le compteur a été réinitialisé avec succès.</p>
                            </div>
                        `);
                        
                        frappe.show_alert({
                            message: 'Compteur réinitialisé',
                            indicator: 'green'
                        }, 3);
                    } else {
                        frappe.show_alert({
                            message: response.message.error || 'Erreur lors de la réinitialisation',
                            indicator: 'red'
                        }, 5);
                    }
                }
            });
        }
    );
}
```

## Étape 4: Configurer les hooks

Maintenant, nous devons mettre à jour le fichier `hooks.py` pour intégrer notre nouvelle fonctionnalité.

1. Ouvrez le fichier `hooks.py` et modifiez-le comme suit:

```python
# Ajouter notre nouveau fichier JavaScript à la liste app_include_js
app_include_js = [
    "/assets/custom_tools/js/custom_tools.js",
    "/assets/custom_tools/js/facebook_button.js",
    "/assets/custom_tools/js/data_csv_tools.js",
    "/assets/custom_tools/js/import_three_csv.js",
    "/assets/custom_tools/js/compteur_visites.js"  # Ajout de notre fichier
]

# Plus bas dans le fichier, ajouter nos méthodes à la liste whitelist_methods
whitelist_methods = [
    "custom_tools.api.data_reset.reset_site_data",
    "custom_tools.api.csv_export.export_csv_data",
    "custom_tools.api.facebook_service.get_facebook_metrics",
    "custom_tools.api.compteur_visites.get_increment_counter",  # Ajout de notre méthode
    "custom_tools.api.compteur_visites.reset_counter"  # Ajout de notre méthode
]
```

## Étape 5: Tester la fonctionnalité

Pour tester notre fonctionnalité:

1. Redémarrez le serveur Frappe pour prendre en compte les modifications, ou exécutez:
   ```
   bench restart
   ```

2. Rafraîchissez la page de votre navigateur

3. Cliquez sur le menu d'aide (?) en haut à droite

4. Vous devriez voir notre nouvel élément "Compteur de visites"

5. Cliquez dessus pour voir la boîte de dialogue s'ouvrir avec le compteur

6. Fermez et rouvrez plusieurs fois pour voir le compteur s'incrémenter

7. Testez le bouton "Réinitialiser" pour remettre le compteur à zéro

## Étape 6: Déboguer en cas de problème

Si votre fonctionnalité ne marche pas comme prévu:

1. Vérifiez les logs du serveur pour voir s'il y a des erreurs Python:
   ```
   bench logs
   ```

2. Ouvrez la console du navigateur (F12) pour vérifier les erreurs JavaScript

3. Vérifiez que vos fichiers ont bien été chargés:
   - Le JS devrait être visible dans les ressources de la page
   - L'API devrait répondre quand vous l'appelez directement

## Conclusion

Félicitations! Vous avez créé une fonctionnalité complète avec:
- Un backend Python qui gère la logique métier
- Un frontend JavaScript qui fournit l'interface utilisateur
- Une intégration avec le framework Frappe via les hooks

Ce pattern peut être utilisé pour créer des fonctionnalités plus complexes en suivant les mêmes principes. N'hésitez pas à explorer le code des autres fonctionnalités du module `custom_tools` pour plus d'inspiration.

## Prochaines étapes

Voici quelques idées pour améliorer cette fonctionnalité:
- Stocker le compteur dans la base de données plutôt que dans le cache
- Ajouter un graphique pour visualiser les visites dans le temps
- Créer un compteur distinct par utilisateur
- Ajouter des statistiques supplémentaires (durée de visite, pages visitées, etc.) 