import frappe
import subprocess
import os
import uuid
from frappe import _
from frappe.utils.background_jobs import enqueue

@frappe.whitelist()
def reset_site_data():
    """
    Réinitialise les données du site en supprimant les données des doctypes métier
    tout en préservant la structure et les configurations.
    """
    # Vérification des permissions - version simplifiée
    if "System Manager" not in frappe.get_roles(frappe.session.user):
        frappe.throw(_("Only System Managers can reset data"))

    # Faire confirmation supplémentaire via un paramètre de requête
    confirm = frappe.local.form_dict.get('confirm')
    if confirm != 'yes':
        frappe.throw(_("Please confirm the data reset by adding confirm=yes parameter"))
    
    # Obtenir le nom du site actuel
    site_name = frappe.local.site
    
    # Créer un job_id unique pour référence
    job_id = str(uuid.uuid4())
    
    # Exécuter la tâche en arrière-plan
    enqueue(
        execute_data_reset,
        queue='long',
        timeout=600,
        event='reset_site_data',
        site_name=site_name,
        job_id=job_id
    )
    
    return {
        "message": "22222222 Data reset process has started. This might take several minutes depending on the amount of data.",
        "job_id": job_id
    }

def execute_data_reset(site_name, job_id=None):
    """
    Exécute le processus de réinitialisation des données en supprimant les enregistrements
    des doctypes spécifiés.
    """
    try:
        # Journaliser le début du processus
        log_message = f"Starting data reset for site {site_name}"
        if job_id:
            log_message += f" (job_id: {job_id})"
        frappe.log_error(log_message, "Data Reset")
        
        # Liste des doctypes métier à supprimer
        doc_types_to_delete = [
            # Ventes et clients
            "Customer", 
            "Customer Group",
            "Sales Invoice",
            "Sales Order",
            "Sales Receipt",
            
            # Achats et fournisseurs
            "Purchase Invoice", 
            "Purchase Order",
            "Purchase Receipt",
            "Supplier Quotation",
            "Request for Quotation",
            "Material Request",
            "Supplier",
            "Supplier Group",
            
            # Comptabilité
            "Payment Entry",
            "Journal Entry",
            
            # Catalogue produits
            "Item",
            "Item Group",
            
            # Tables associées/détails
            "Purchase Invoice Item",
            "Purchase Order Item",
            "Supplier Quotation Item",
            "Material Request Item",
            "Request for Quotation Item",
            "Stock Ledger Entry",
            "Request for Quotation Supplier",
            "Purchase Receipt Item",
            "Sales Invoice Item",
            "Sales Order Item",
            "Item Price",
            "Item Supplier",
            "Item Tax",
            "Supplier Item",
            "Customer Item"
        ]

        # Journal pour suivre la progression
        results = []
        
        # Désactiver les triggers pour accélérer le processus
        frappe.flags.in_import = True
        
        # Suppression des données pour chaque doctype
        for doctype in doc_types_to_delete:
            try:
                result_msg = f"🧹 Suppression des enregistrements de {doctype}..."
                results.append(result_msg)
                
                # Compter les enregistrements avant suppression
                count_before = frappe.db.count(doctype)
                
                if count_before > 0:
                    # Supprimer les enregistrements
                    frappe.db.delete(doctype)
                    results.append(f"✅ {count_before} enregistrements de {doctype} supprimés")
                else:
                    results.append(f"ℹ️ Aucun enregistrement de {doctype} à supprimer")
            
            except Exception as e:
                error_msg = f"⚠️ Erreur avec {doctype} : {str(e)}"
                results.append(error_msg)
                frappe.log_error(f"Error deleting {doctype}: {str(e)}", "Data Reset Error")
        
        # Réactiver les triggers
        frappe.flags.in_import = False
        
        # Valider les changements
        frappe.db.commit()
        
        # Journaliser les résultats
        success_msg = "\n".join(results) + "\n✅ Nettoyage terminé. Données métiers supprimées, structure conservée."
        frappe.log_error(success_msg, "Data Reset Complete")
        
    except Exception as e:
        frappe.log_error(f"Error during data reset: {str(e)}", "Data Reset Error") 