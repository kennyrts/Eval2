# Guide de développement pour Custom Tools

Ce document explique comment étendre et développer de nouvelles fonctionnalités pour le module `custom_tools` dans ERPNext.

## Table des matières

1. [Introduction](#introduction)
2. [Structure du projet](#structure-du-projet)
3. [Ajouter une nouvelle fonctionnalité](#ajouter-une-nouvelle-fonctionnalité)
   - [Partie backend](#partie-backend)
   - [Partie frontend](#partie-frontend)
   - [Configuration des hooks](#configuration-des-hooks)
4. [Bonnes pratiques](#bonnes-pratiques)
5. [Exemples](#exemples)

## Introduction

`custom_tools` est un module d'extension pour ERPNext qui fournit des outils personnalisés et des fonctionnalités additionnelles. Ce guide vous aidera à comprendre la structure du module et comment l'étendre avec vos propres fonctionnalités.

## Structure du projet

La structure de base du module est la suivante:

```
custom_tools/
├── api/                 # Code backend et API REST
│   ├── __init__.py      # Initialisation du package
│   ├── data_reset.py    # API pour réinitialiser les données
│   ├── csv_export.py    # API pour exporter en CSV
│   └── facebook_service.py # API pour les métriques Facebook
├── custom_tools/        # Composants spécifiques à l'application
│   └── page/            # Pages personnalisées
├── public/              # Ressources frontend (JS, CSS)
│   ├── css/             # Styles CSS
│   └── js/              # Scripts JavaScript
│       ├── custom_tools.js        # Script principal
│       ├── data_csv_tools.js      # Fonctionnalités CSV
│       ├── data_reset_button.js   # Bouton de réinitialisation
│       ├── facebook_button.js     # Fonctionnalité Facebook
│       └── import_three_csv.js    # Import CSV
├── config/              # Configuration de l'application
├── __init__.py          # Initialisation du module
├── hooks.py             # Définition des hooks Frappe
├── MANIFEST.in          # Fichier de manifeste pour l'installation
├── requirements.txt     # Dépendances Python
└── setup.py             # Script d'installation
```

## Ajouter une nouvelle fonctionnalité

Pour ajouter une nouvelle fonctionnalité, vous devez généralement créer:
1. Un fichier backend (Python) dans le dossier `api/`
2. Un fichier frontend (JavaScript) dans le dossier `public/js/`
3. Configurer les hooks appropriés dans `hooks.py`

### Partie backend

1. Créez un nouveau fichier Python dans le dossier `api/`. Par exemple, `ma_fonctionnalite.py`:

```python
import frappe
from frappe import _

# Exposer la fonction via l'API
@frappe.whitelist()
def ma_fonction():
    """
    Documentation de la fonction
    """
    try:
        # Votre logique métier ici
        resultat = {
            "success": True,
            "message": "Opération réussie",
            "data": {
                # Vos données à retourner
            }
        }
        return resultat
    except Exception as e:
        # Gestion des erreurs
        frappe.logger().error(f"Erreur dans ma_fonction: {str(e)}")
        return {
            "success": False,
            "error": str(e)
        }
```

Points importants:
- Utilisez `@frappe.whitelist()` pour exposer la fonction via l'API
- Suivez le pattern try/except pour gérer les erreurs proprement
- Utilisez `frappe.logger()` pour journaliser les informations importantes
- Retournez un objet JSON structuré avec au moins une clé `success`

### Partie frontend

1. Créez un nouveau fichier JavaScript dans le dossier `public/js/`. Par exemple, `ma_fonctionnalite.js`:

```javascript
// Description de la fonctionnalité
console.log("Ma fonctionnalité chargée");

// Fonction d'initialisation (appelée quand le document est prêt)
$(document).ready(function() {
    // Attendre que l'interface soit complètement chargée
    setTimeout(function() {
        // Vérifier si l'élément existe déjà pour éviter les doublons
        if (!$('#mon-element').length) {
            console.log("Ajout de ma fonctionnalité à l'interface");
            
            // Créer l'élément d'interface
            var monElement = $(`
                <li id="mon-element">
                    <a href="#" class="dropdown-item">
                        <span class="ml-2">Ma Fonctionnalité</span>
                    </a>
                </li>
            `);
            
            // Ajouter l'élément au menu approprié (exemple: menu Aide)
            $('.dropdown-help .dropdown-menu').append(monElement);
            
            // Ajouter l'événement de clic
            $('#mon-element').on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                // Fermer le dropdown
                $('.dropdown-help .dropdown-toggle').dropdown('hide');
                
                // Appeler votre fonction
                maFonction();
            });
        }
    }, 2000); // Délai pour s'assurer que l'interface est chargée
});

// Fonction principale
function maFonction() {
    // Afficher une boîte de dialogue ou exécuter une action
    var d = new frappe.ui.Dialog({
        title: 'Ma Fonctionnalité',
        fields: [
            {
                fieldname: 'mon_html',
                fieldtype: 'HTML',
                options: `
                    <div class="text-center">
                        <p><b>Contenu de ma fonctionnalité</b></p>
                    </div>
                `
            }
        ],
        primary_action_label: 'Exécuter',
        primary_action: function() {
            // Appeler l'API backend
            frappe.call({
                method: 'custom_tools.api.ma_fonctionnalite.ma_fonction',
                callback: function(response) {
                    if (response.message && response.message.success) {
                        // Traiter la réponse réussie
                        frappe.show_alert({
                            message: 'Opération réussie',
                            indicator: 'green'
                        }, 5);
                    } else {
                        // Traiter l'erreur
                        frappe.show_alert({
                            message: response.message.error || 'Une erreur est survenue',
                            indicator: 'red'
                        }, 5);
                    }
                },
                error: function(err) {
                    console.error("Erreur lors de l'appel API:", err);
                    frappe.show_alert({
                        message: 'Erreur de communication avec le serveur',
                        indicator: 'red'
                    }, 5);
                }
            });
            
            d.hide();
        }
    });
    
    d.show();
}
```

Points importants:
- Utilisez `$(document).ready()` avec un `setTimeout()` pour s'assurer que l'interface est chargée
- Vérifiez si l'élément existe déjà pour éviter les doublons lors des rechargements
- Utilisez `frappe.call()` pour appeler les méthodes backend
- Gérez correctement les réponses et les erreurs

### Configuration des hooks

Pour que votre fonctionnalité soit chargée automatiquement, vous devez l'ajouter dans le fichier `hooks.py`:

1. Ajoutez votre fichier JavaScript à la liste `app_include_js`:

```python
app_include_js = [
    "/assets/custom_tools/js/custom_tools.js",
    "/assets/custom_tools/js/facebook_button.js",
    "/assets/custom_tools/js/data_csv_tools.js",
    "/assets/custom_tools/js/import_three_csv.js",
    "/assets/custom_tools/js/ma_fonctionnalite.js"  # Ajoutez votre fichier ici
]
```

2. Si votre fonctionnalité expose une API, ajoutez-la à la liste `whitelist_methods`:

```python
# Whitelist API methods
whitelist_methods = [
    "custom_tools.api.data_reset.reset_site_data",
    "custom_tools.api.csv_export.export_csv_data",
    "custom_tools.api.facebook_service.get_facebook_metrics",
    "custom_tools.api.ma_fonctionnalite.ma_fonction"  # Ajoutez votre méthode ici
]
```

## Bonnes pratiques

1. **Nommage cohérent**: Utilisez des noms clairs et descriptifs pour vos fichiers et fonctions
2. **Documentation**: Documentez vos fonctions et leur utilisation
3. **Gestion des erreurs**: Gérez toujours les erreurs pour éviter les plantages
4. **Journalisation**: Utilisez `frappe.logger()` pour faciliter le débogage
5. **Internationalisation**: Utilisez `frappe._()` pour les textes qui doivent être traduits
6. **Structure des données**: Suivez un format cohérent pour les réponses API

## Exemples

### Exemple: Fonction de calcul simple

#### Backend (api/calculatrice.py)

```python
import frappe

@frappe.whitelist()
def calculer(operation, a, b):
    """
    Effectue une opération mathématique simple.
    
    Args:
        operation: Type d'opération (add, subtract, multiply, divide)
        a: Premier nombre
        b: Deuxième nombre
        
    Returns:
        dict: Résultat de l'opération
    """
    try:
        # Convertir les entrées en nombres
        a, b = float(a), float(b)
        
        # Effectuer l'opération
        if operation == "add":
            resultat = a + b
        elif operation == "subtract":
            resultat = a - b
        elif operation == "multiply":
            resultat = a * b
        elif operation == "divide":
            if b == 0:
                raise ValueError("Division par zéro impossible")
            resultat = a / b
        else:
            raise ValueError("Opération non supportée")
        
        return {
            "success": True,
            "result": resultat
        }
    except Exception as e:
        frappe.logger().error(f"Erreur de calcul: {str(e)}")
        return {
            "success": False,
            "error": str(e)
        }
```

#### Frontend (public/js/calculatrice.js)

```javascript
// Calculatrice simple
console.log("Calculatrice chargée");

$(document).ready(function() {
    setTimeout(function() {
        if (!$('#calculatrice-menu-item').length) {
            console.log("Ajout du bouton calculatrice");
            
            var calcMenuItem = $(`
                <li id="calculatrice-menu-item">
                    <a href="#" class="dropdown-item">
                        <span class="ml-2">Calculatrice</span>
                    </a>
                </li>
            `);
            
            $('.dropdown-help .dropdown-menu').append(calcMenuItem);
            
            $('#calculatrice-menu-item').on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                
                $('.dropdown-help .dropdown-toggle').dropdown('hide');
                
                ouvrirCalculatrice();
            });
        }
    }, 2000);
});

function ouvrirCalculatrice() {
    var d = new frappe.ui.Dialog({
        title: 'Calculatrice',
        fields: [
            {
                label: 'Premier nombre',
                fieldname: 'a',
                fieldtype: 'Float',
                reqd: 1
            },
            {
                label: 'Opération',
                fieldname: 'operation',
                fieldtype: 'Select',
                options: [
                    { value: 'add', label: 'Addition (+)' },
                    { value: 'subtract', label: 'Soustraction (-)' },
                    { value: 'multiply', label: 'Multiplication (×)' },
                    { value: 'divide', label: 'Division (÷)' }
                ],
                default: 'add',
                reqd: 1
            },
            {
                label: 'Deuxième nombre',
                fieldname: 'b',
                fieldtype: 'Float',
                reqd: 1
            },
            {
                fieldname: 'result_html',
                fieldtype: 'HTML',
                options: `<div id="calc-result" class="text-center mt-3"></div>`
            }
        ],
        primary_action_label: 'Calculer',
        primary_action: function(values) {
            // Appeler l'API backend
            frappe.call({
                method: 'custom_tools.api.calculatrice.calculer',
                args: {
                    operation: values.operation,
                    a: values.a,
                    b: values.b
                },
                callback: function(response) {
                    if (response.message && response.message.success) {
                        // Afficher le résultat
                        $('#calc-result').html(`
                            <div class="alert alert-success">
                                <b>Résultat:</b> ${response.message.result}
                            </div>
                        `);
                    } else {
                        // Afficher l'erreur
                        $('#calc-result').html(`
                            <div class="alert alert-danger">
                                <b>Erreur:</b> ${response.message.error || 'Une erreur est survenue'}
                            </div>
                        `);
                    }
                },
                error: function(err) {
                    console.error("Erreur lors du calcul:", err);
                    $('#calc-result').html(`
                        <div class="alert alert-danger">
                            <b>Erreur:</b> Impossible de communiquer avec le serveur
                        </div>
                    `);
                }
            });
        }
    });
    
    d.show();
}
```

#### Configuration des hooks

```python
# Dans hooks.py
app_include_js = [
    # ... autres fichiers JS existants
    "/assets/custom_tools/js/calculatrice.js"
]

whitelist_methods = [
    # ... autres méthodes existantes
    "custom_tools.api.calculatrice.calculer"
]
```

---

Ce guide vous fournit les bases pour créer vos propres fonctionnalités dans le module `custom_tools`. N'hésitez pas à explorer les exemples existants pour mieux comprendre l'architecture et les patterns de développement. 