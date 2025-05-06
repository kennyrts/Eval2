import frappe
from frappe import _
import csv
import pandas as pd
from io import StringIO

@frappe.whitelist()
def import_csv():
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can import CSV"), frappe.PermissionError)
    
    try:
        # Get uploaded files
        material_request_file = frappe.request.files.get('material_request_file')
        supplier_file = frappe.request.files.get('supplier_file')
        quotation_file = frappe.request.files.get('quotation_file')
        
        if not all([material_request_file, supplier_file, quotation_file]):
            frappe.throw(_("All three files are required"))
        
        # Process Supplier file first (as it's referenced by others)
        supplier_content = supplier_file.stream.read().decode('utf-8')
        supplier_df = pd.read_csv(StringIO(supplier_content))
        
        for index, row in supplier_df.iterrows():
            if not frappe.db.exists("Supplier", {"supplier_name": row['supplier_name']}):
                supplier_doc = frappe.get_doc({
                    "doctype": "Supplier",
                    "supplier_name": row['supplier_name'],
                    "country": row['country'],
                    "supplier_type": row['type']
                })
                supplier_doc.insert()
        
        # Process Material Request file
        material_request_content = material_request_file.stream.read().decode('utf-8')
        material_request_df = pd.read_csv(StringIO(material_request_content))
        
        for index, row in material_request_df.iterrows():
            material_request_doc = frappe.get_doc({
                "doctype": "Material Request",
                "transaction_date": row['date'],
                "schedule_date": row['required_by'],
                "material_request_type": row['purpose'],
                "items": [{
                    "item_code": row['item_name'],
                    "qty": row['quantity'],
                    "warehouse": row['target_warehouse']
                }]
            })
            material_request_doc.insert()
        
        # Process Request for Quotation file
        quotation_content = quotation_file.stream.read().decode('utf-8')
        quotation_df = pd.read_csv(StringIO(quotation_content))
        
        # Group by ref_request_quotation to create one RFQ per reference
        for ref, group in quotation_df.groupby('ref_request_quotation'):
            suppliers = []
            for _, row in group.iterrows():
                suppliers.append({
                    "supplier": row['supplier']
                })
            
            # Get the corresponding material request
            material_request = material_request_df[material_request_df['ref'] == ref].iloc[0]
            
            rfq_doc = frappe.get_doc({
                "doctype": "Request for Quotation",
                "transaction_date": material_request['date'],
                "suppliers": suppliers,
                "items": [{
                    "item_code": material_request['item_name'],
                    "qty": material_request['quantity'],
                    "warehouse": material_request['target_warehouse']
                }]
            })
            rfq_doc.insert()
        
        frappe.db.commit()
        return {"message": "Import successful", "files_processed": 3}
        
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(frappe.get_traceback())
        frappe.throw(_("Failed to import CSV files: {0}").format(str(e)))

@frappe.whitelist()
def reset_database():
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can reset database"), frappe.PermissionError)
    
    try:
        # Backup current database state
        frappe.backup()
        
        # Reset database (implement your reset logic here)
        # This is a placeholder - you should implement the actual reset logic
        # based on your specific requirements
        
        frappe.db.sql("SHOW TABLES")  # Example query
        
        frappe.db.commit()
        return True
        
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(frappe.get_traceback())
        frappe.throw(_("Failed to reset database: {0}").format(str(e))) 