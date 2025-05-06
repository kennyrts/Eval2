import frappe
from frappe.utils import getdate, nowdate
import json

@frappe.whitelist()
def get_events(start=None, end=None, filters=None):
    """Retourne les événements pour le calendrier des achats"""
    if not start:
        start = nowdate()
    if not end:
        end = nowdate()
    if filters:
        filters = json.loads(filters)

    frappe.msgprint(_(f"Recherche d'événements du {start} au {end}"), title="Debug", indicator="blue")
    
    events = []
    
    # Liste des doctypes à inclure avec leurs champs de date
    doctypes = {
        "Material Request": "schedule_date",
        "Request for Quotation": "transaction_date",
        "Supplier Quotation": "valid_till",
        "Purchase Order": "schedule_date",
        "Purchase Invoice": "due_date",
        "Payment Entry": "posting_date",
        "Purchase Receipt": "posting_date"
    }
    
    # Couleurs pour chaque type de document
    colors = {
        "Material Request": "#7575ff",
        "Request for Quotation": "#ffa00a",
        "Supplier Quotation": "#ffb1b1",
        "Purchase Order": "#ffd343",
        "Purchase Invoice": "#98d85b",
        "Payment Entry": "#ff7846",
        "Purchase Receipt": "#7cd6fd"
    }
    
    try:
        for doctype, date_field in doctypes.items():
            # Si un type de document est sélectionné et que ce n'est pas celui-ci, passer
            if filters and filters.get('document_type') != 'All' and filters.get('document_type') != doctype:
                continue

            frappe.msgprint(_(f"Recherche des documents de type {doctype}"), title="Debug", indicator="blue")
            
            # Récupérer les documents pour chaque type
            docs = frappe.get_all(doctype,
                fields=["name", date_field, "status"],
                filters=[
                    [date_field, "between", [start, end]],
                    ["docstatus", "!=", "2"]  # Exclure les documents annulés
                ]
            )
            
            if docs:
                frappe.msgprint(_(f"Trouvé {len(docs)} documents pour {doctype}"), title="Debug", indicator="green")
            
            for doc in docs:
                event_date = getdate(doc.get(date_field))
                events.append({
                    "name": doc.name,
                    "title": f"{doctype}: {doc.name}",
                    "start": event_date,
                    "end": event_date,
                    "doctype": doctype,
                    "docname": doc.name,
                    "status": doc.status,
                    "color": colors.get(doctype),
                    "all_day": 1
                })
        
        frappe.msgprint(_(f"Total des événements trouvés : {len(events)}"), title="Debug", indicator="blue")
        return events
        
    except Exception as e:
        error_msg = f"Erreur dans get_events: {str(e)}"
        frappe.msgprint(_(error_msg), title="Erreur", indicator="red")
        return [] 