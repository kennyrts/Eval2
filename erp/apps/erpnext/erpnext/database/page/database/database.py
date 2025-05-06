import frappe
from frappe import _
import csv
from io import StringIO
from datetime import datetime

@frappe.whitelist()
def import_csv():
    # Add debug logging
    frappe.log_error("import_csv function called")
    print("import_csv function called")  # Console logging
    
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can import CSV"), frappe.PermissionError)
    
    created_items = []
    ref_to_mr = {}  # Pour stocker la correspondance entre ref et Material Request
    
    try:
        # Get uploaded files
        material_request_file = frappe.request.files.get('material_request_file')
        supplier_file = frappe.request.files.get('supplier_file')
        quotation_file = frappe.request.files.get('quotation_file')
        
        # Log received files
        frappe.log_error(f"Files received - Material Request: {bool(material_request_file)}, Supplier: {bool(supplier_file)}, Quotation: {bool(quotation_file)}")
        
        # Process Material Request file if provided
        if material_request_file:
            try:
                material_request_content = material_request_file.stream.read().decode('utf-8')
                csv_reader = csv.DictReader(StringIO(material_request_content))
                
                for row in csv_reader:
                    try:
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
                        material_request.submit()
                        created_items.append(f"Material Request: {material_request.name}")
                        
                        # Store reference to Material Request
                        ref_to_mr[row['ref']] = material_request.name
                        
                        frappe.db.commit()
                    except Exception as e:
                        frappe.db.rollback()
                        frappe.log_error(f"Error processing Material Request row: {str(e)}")
                        raise
            except Exception as e:
                frappe.db.rollback()
                frappe.log_error(f"Error processing Material Request file: {str(e)}")
                raise
        
        # Process Supplier file if provided
        if supplier_file:
            try:
                supplier_content = supplier_file.stream.read().decode('utf-8')
                supplier_reader = csv.DictReader(StringIO(supplier_content))
                
                for row in supplier_reader:
                    try:
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
                            
                            frappe.db.commit()
                    except Exception as e:
                        frappe.db.rollback()
                        frappe.log_error(f"Error processing Supplier row: {str(e)}")
                        raise
            except Exception as e:
                frappe.db.rollback()
                frappe.log_error(f"Error processing Supplier file: {str(e)}")
                raise
        
        # Process Quotation file if provided
        if quotation_file:
            try:
                quotation_content = quotation_file.stream.read().decode('utf-8')
                quotation_reader = csv.DictReader(StringIO(quotation_content))
                
                # Group suppliers by ref_request_quotation
                quotations = {}
                for row in quotation_reader:
                    ref = row['ref_request_quotation']
                    if ref not in quotations:
                        quotations[ref] = []
                    quotations[ref].append(row['supplier'])
                
                # Create RFQ and Supplier Quotations for each group
                for ref, suppliers in quotations.items():
                    try:
                        if ref not in ref_to_mr:
                            continue
                            
                        mr = frappe.get_doc("Material Request", ref_to_mr[ref])
                        
                        # Create Request for Quotation
                        rfq = frappe.get_doc({
                            "doctype": "Request for Quotation",
                            "transaction_date": mr.transaction_date,
                            "company": "Orinasa",
                            "message_for_supplier": "Please provide your best quote for the items listed below.",
                            "suppliers": [{"supplier": supplier} for supplier in suppliers],
                            "items": [{
                                "item_code": item.item_code,
                                "qty": item.qty,
                                "schedule_date": item.schedule_date,
                                "warehouse": item.warehouse,
                                "material_request": mr.name,
                                "material_request_item": item.name,
                                "uom": "Nos",
                                "stock_uom": "Nos",
                                "conversion_factor": 1.0
                            } for item in mr.items]
                        })
                        rfq.insert()
                        rfq.submit()
                        created_items.append(f"Request for Quotation: {rfq.name}")
                        
                        # Create Supplier Quotation for each supplier
                        for supplier in suppliers:
                            sq = frappe.get_doc({
                                "doctype": "Supplier Quotation",
                                "supplier": supplier,
                                "company": "Orinasa",
                                "valid_till": mr.schedule_date,
                                "items": [{
                                    "item_code": item.item_code,
                                    "qty": item.qty,
                                    "rate": 1.0,  # Prix par défaut
                                    "warehouse": item.warehouse,
                                    "request_for_quotation": rfq.name,
                                    "material_request": mr.name
                                } for item in mr.items]
                            })
                            sq.insert()
                            created_items.append(f"Supplier Quotation: {sq.name}")
                            
                        frappe.db.commit()
                    except Exception as e:
                        frappe.db.rollback()
                        frappe.log_error(f"Error processing Quotation group: {str(e)}")
                        raise
            except Exception as e:
                frappe.db.rollback()
                frappe.log_error(f"Error processing Quotation file: {str(e)}")
                raise
                
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
    frappe.log_error("message","reset_database function called")
    if not frappe.has_permission("System Manager"):
        frappe.throw(_("Only System Manager can reset database"), frappe.PermissionError)
    
    try:                
        # Liste des doctypes à supprimer dans l'ordre inverse des dépendances
        doctypes_to_delete = [
            # Documents financiers
            "Payment Entry",
            "Purchase Invoice",
            "Purchase Receipt",
            
            # Documents d'achat
            "Purchase Order",
            "Supplier Quotation",
            "Request for Quotation",
            "Material Request",
            
            # Documents de base
            "Item",
            "Item Group",
            "Supplier"
        ]
        
        deleted_count = {}
        
        for doctype in doctypes_to_delete:
            try:
                # Récupérer tous les documents, y compris les soumis
                docs = frappe.get_all(doctype)
                count = len(docs)
                
                for doc in docs:
                    try:
                        # Récupérer le document complet
                        doc_to_delete = frappe.get_doc(doctype, doc.name)
                        
                        # Si le document est soumis, on l'annule d'abord
                        if doc_to_delete.docstatus == 1:
                            doc_to_delete.cancel()
                            frappe.db.commit()
                        
                        # Supprimer le document
                        frappe.delete_doc(doctype, doc.name, ignore_permissions=True)
                        frappe.db.commit()
                        
                    except Exception as e:
                        frappe.log_error(f"Error deleting {doctype} {doc.name}: {str(e)}")
                
                deleted_count[doctype] = count
                
            except Exception as e:
                frappe.log_error(f"Error processing doctype {doctype}: {str(e)}")
        
        # Préparer le message détaillé
        message = "Base de données réinitialisée avec succès.\n\nDétails des suppressions :\n"
        for doctype, count in deleted_count.items():
            if count > 0:
                message += f"- {doctype}: {count} document(s) supprimé(s)\n"
        
        # Envoyer un message à l'utilisateur
        frappe.msgprint(_(message))
        
        return {
            "message": message,
            "deleted_records": deleted_count,
            "status": "success"
        }
        
    except Exception as e:
        frappe.db.rollback()
        frappe.log_error(frappe.get_traceback())
        error_message = f"Échec de la réinitialisation de la base de données : {str(e)}"
        frappe.msgprint(_(error_message), title="Erreur", indicator="red")
        frappe.throw(_(error_message))

@frappe.whitelist()
def test_function():
    try:
        message = "Test réussi ! La fonction Python a été appelée avec succès."
        file_content = None
        
        # Récupérer la valeur du champ texte
        test_value = frappe.form_dict.get('test_value')
        if test_value:
            message += f" Valeur saisie : {test_value}"
            
        # Récupérer le fichier
        if 'test_file' in frappe.request.files:
            file = frappe.request.files['test_file']
            try:
                file_content = file.stream.read().decode('utf-8')
                message += f"\nContenu du fichier :\n{file_content}"
            except UnicodeDecodeError:
                message += "\nLe fichier n'est pas un fichier texte lisible."
            except Exception as e:
                message += f"\nErreur lors de la lecture du fichier : {str(e)}"
                
        return {
            "message": message,
            "status": "success",
            "file_content": file_content
        }
    except Exception as e:
        frappe.log_error(f"Test Function Error: {str(e)}\nForm Data: {frappe.form_dict}\nFiles: {frappe.request.files}")
        frappe.throw(_("Erreur lors du test : {0}").format(str(e))) 