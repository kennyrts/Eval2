import frappe
import random
import json
from datetime import datetime

@frappe.whitelist()
def get_facebook_metrics():
    """
    Retourne des métriques simulées pour une intégration Facebook.
    Cette fonction effectue des calculs simples pour simuler des statistiques.
    """
    try:
        # Calculs simples
        current_hour = datetime.now().hour
        base_engagement = 100 + (current_hour * 5)  # Plus d'engagement durant la journée
        
        # Générer des métriques aléatoires basées sur des calculs
        likes = random.randint(base_engagement - 50, base_engagement + 100)
        shares = int(likes * 0.4)  # 40% du nombre de likes
        comments = int(likes * 0.25)  # 25% du nombre de likes
        
        # Calcul d'un score d'engagement
        engagement_score = round((likes + (shares * 2) + (comments * 3)) / 10, 2)
        
        # Calcul d'une prévision de croissance
        growth_prediction = round(engagement_score * (1 + (current_hour / 24)), 2)
        
        # Retourner les résultats
        result = {
            "success": True,
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "metrics": {
                "likes": likes,
                "shares": shares, 
                "comments": comments,
                "engagement_score": engagement_score,
                "growth_prediction": growth_prediction
            },
            "message": "Métriques Facebook calculées avec succès."
        }
        
        # Journaliser le résultat pour suivi
        frappe.logger().info(f"Facebook metrics calculated: {json.dumps(result)}")
        
        return result
        
    except Exception as e:
        error_msg = f"Erreur lors du calcul des métriques Facebook: {str(e)}"
        frappe.logger().error(error_msg)
        return {
            "success": False,
            "error": error_msg
        } 