import frappe
from frappe import _
import csv
from io import StringIO

@frappe.whitelist()
def import_csv():
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can import CSV"), frappe.PermissionError)
    
    try:
        # Get uploaded files
        material_request_file = frappe.request.files.get('material_request_file')
        supplier_file = frappe.request.files.get('supplier_file')
        
        created_items = []
        
        # Process Supplier file if provided
        if supplier_file:
            supplier_content = supplier_file.stream.read().decode('utf-8')
            supplier_reader = csv.DictReader(StringIO(supplier_content))
            
            for row in supplier_reader:
                # Convert Usa to United States
                country = "United States" if row['country'].lower() == "usa" else row['country']
                
                if not frappe.db.exists("Supplier", {"supplier_name": row['supplier_name']}):
                    supplier = frappe.get_doc({
                        "doctype": "Supplier",
                        "supplier_name": row['supplier_name'],
                        "supplier_type": row['type'],
                        "country": country,
                        "supplier_group": "All Supplier Groups"
                    })
                    supplier.insert()
                    created_items.append(f"Supplier: {row['supplier_name']}")
                else:
                    frappe.log_error(f"Supplier {row['supplier_name']} already exists")
        
        # Process Material Request file if provided
        if material_request_file:
            material_request_content = material_request_file.stream.read().decode('utf-8')
            csv_reader = csv.DictReader(StringIO(material_request_content))
            
            for row in csv_reader:
                # Step 1: Check and create Item Group if needed
                if not frappe.db.exists("Item Group", {"item_group_name": row['item_groupe']}):
                    item_group = frappe.get_doc({
                        "doctype": "Item Group",
                        "item_group_name": row['item_groupe'],
                        "is_group": 0
                    })
                    item_group.insert()
                
                # Step 2: Check and create Item if needed
                if not frappe.db.exists("Item", {"item_code": row['item_name']}):
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
                
                # Step 3: Create Material Request
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
                material_request.submit()
                created_items.append(f"Material Request: {material_request.name}")
        
        frappe.db.commit()
        return {
            "message": "Import successful",
            "created_items": created_items
        }
        
    except Exception as e:
        frappe.db.rollback()
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