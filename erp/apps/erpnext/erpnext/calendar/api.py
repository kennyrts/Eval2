import frappe
from frappe.utils import getdate, nowdate
import json
from frappe import _

@frappe.whitelist(allow_guest=True)
def get_purchase_calendar_events(start=None, end=None, document_type='All'):
    """Returns purchase-related events for external calendar integration"""
    if not start:
        start = nowdate()
    if not end:
        end = nowdate()

    events = []
    
    doctypes = {
        "Material Request": "schedule_date",
        "Request for Quotation": "transaction_date",
        "Supplier Quotation": "valid_till",
        "Purchase Order": "schedule_date",
        "Purchase Invoice": "due_date",
        "Payment Entry": "posting_date",
        "Purchase Receipt": "posting_date"
    }
    
    try:
        for doctype, date_field in doctypes.items():
            if document_type != 'All' and document_type != doctype:
                continue

            docs = frappe.get_all(doctype,
                fields=["name", date_field, "status", "modified", "creation"],
                filters=[
                    [date_field, "between", [start, end]],
                    ["docstatus", "!=", "2"]
                ]
            )
            
            for doc in docs:
                event_date = getdate(doc.get(date_field))
                events.append({
                    "id": f"{doctype}-{doc.name}",
                    "title": f"{doctype}: {doc.name}",
                    "start": str(event_date),
                    "end": str(event_date),
                    "doctype": doctype,
                    "docname": doc.name,
                    "status": doc.status,
                    "lastModified": str(doc.modified),
                    "created": str(doc.creation),
                    "allDay": True
                })
        
        return {"success": True, "events": events}
        
    except Exception as e:
        return {"success": False, "error": str(e)} 