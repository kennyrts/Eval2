import frappe
from frappe import _
import csv
from io import StringIO

@frappe.whitelist()
def import_csv():
    frappe.log_error("DEBUG: Starting import_csv function")
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can import CSV"), frappe.PermissionError)
    
    try:
        # Get uploaded file
        frappe.log_error("DEBUG: Getting material request file")
        material_request_file = frappe.request.files.get('material_request_file')
        
        if not material_request_file:
            frappe.throw(_("Material Request file is required"))
        
        frappe.log_error("DEBUG: Reading file content")
        # Process Material Request file
        material_request_content = material_request_file.stream.read().decode('utf-8')
        csv_reader = csv.DictReader(StringIO(material_request_content))
        
        created_requests = []
        
        frappe.log_error("DEBUG: Starting to process rows")
        for row in csv_reader:
            frappe.log_error(f"DEBUG: Processing row: {row}")
            # Step 1: Check and create Item Group if needed
            if not frappe.db.exists("Item Group", {"item_group_name": row['item_groupe']}):
                frappe.log_error(f"DEBUG: Creating item group {row['item_groupe']}")
                item_group = frappe.get_doc({
                    "doctype": "Item Group",
                    "item_group_name": row['item_groupe'],
                    "is_group": 0
                })
                item_group.insert()
                frappe.log_error(f"DEBUG: Item group {row['item_groupe']} created")
            else:
                frappe.log_error(f"DEBUG: Item group {row['item_groupe']} already exists")
            
            # Step 2: Check and create Item if needed
            if not frappe.db.exists("Item", {"item_code": row['item_name']}):
                frappe.log_error(f"DEBUG: Creating item {row['item_name']}")
                item = frappe.get_doc({
                    "doctype": "Item",
                    "item_code": row['item_name'],
                    "item_name": row['item_name'],
                    "item_group": row['item_groupe'],
                    "is_stock_item": 1,
                    "stock_uom": "Nos",
                    "default_unit_of_measure": "Nos"
                })
                item.insert()
                frappe.log_error(f"DEBUG: Item {row['item_name']} created")
            else:
                frappe.log_error(f"DEBUG: Item {row['item_name']} already exists")
            
            # Step 3: Create Material Request
            frappe.log_error("DEBUG: Creating material request")
            from datetime import datetime
            date_obj = datetime.strptime(row['date'], '%d/%m/%Y')
            required_date_obj = datetime.strptime(row['required_by'], '%d/%m/%Y')
            
            # Transform warehouse name
            warehouse = "All Warehouses - O" if row['target_warehouse'] == "All Warehouse" else row['target_warehouse'] + " - O"
            
            material_request = frappe.get_doc({
                "doctype": "Material Request",
                "material_request_type": row['purpose'],
                "transaction_date": date_obj.strftime('%Y-%m-%d'),
                "schedule_date": required_date_obj.strftime('%Y-%m-%d'),
                "company": "Orinasa",
                "items": [{
                    "item_code": row['item_name'],
                    "qty": float(row['quantity']),
                    "warehouse": warehouse,
                    "schedule_date": required_date_obj.strftime('%Y-%m-%d')
                }]
            })
            material_request.insert()
            
            # Step 4: Submit the Material Request
            frappe.log_error("DEBUG: Submitting material request")
            material_request.submit()
            
            created_requests.append(material_request.name)
        
        frappe.db.commit()
        frappe.log_error("DEBUG: Import completed successfully")
        return {
            "message": "Import successful",
            "created_requests": created_requests
        }
        
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(f"ERROR: Import failed with error: {str(e)}")
        frappe.log_error(frappe.get_traceback())
        frappe.throw(_("Failed to import CSV file: {0}").format(str(e)))

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