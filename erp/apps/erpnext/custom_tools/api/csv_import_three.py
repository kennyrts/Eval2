import frappe
import os
import csv
from frappe import _
from frappe.utils import get_files_path, today, getdate, add_days
from frappe.utils.file_manager import get_file
from datetime import datetime

@frappe.whitelist()
def import_three_csv(file1_url, file2_url, file3_url):
    """Importe les données depuis trois fichiers CSV spécifiques"""
    
    # Vérifier les permissions
    if "System Manager" not in frappe.get_roles(frappe.session.user):
        return {"success": False, "message": "Seuls les administrateurs système peuvent importer des données"}
    
    # Vérifier les fichiers
    if not file1_url or not file2_url or not file3_url:
        return {"success": False, "message": "Veuillez fournir les trois fichiers CSV requis"}
    
    try:
        # Exécuter l'importation directement (pas en arrière-plan)
        results = process_import_files(file1_url, file2_url, file3_url)
        
        if results.get("success"):
            return {
                "success": True,
                "message": "Importation réussie!",
                "details": results.get("details", [])
            }
        else:
            return {
                "success": False,
                "message": results.get("message", "Erreur lors de l'importation"),
                "details": results.get("details", [])
            }
            
    except Exception as e:
        frappe.log_error(f"Erreur lors de l'importation: {str(e)}", "CSV Import Error")
        return {"success": False, "message": f"Erreur: {str(e)}"}

def initialize_base_requirements():
    """Initialise les objets de base nécessaires pour l'importation"""
    
    # Créer une entreprise par défaut si nécessaire
    if not frappe.db.exists("Company", "ITUniversity"):
        try:
            company = frappe.new_doc("Company")
            company.company_name = "ITUniversity"
            company.abbr = "ITU"
            company.default_currency = "EUR"
            company.country = "France"
            company.insert(ignore_permissions=True)
            frappe.db.commit()
        except Exception as e:
            frappe.log_error(f"Erreur lors de la création de l'entreprise par défaut: {str(e)}", "Base Setup Error")
    
    # Créer le groupe d'articles parent de base
    if not frappe.db.exists("Item Group", "All Item Groups"):
        try:
            doc = frappe.new_doc("Item Group")
            doc.item_group_name = "All Item Groups"
            doc.is_group = 1
            # Ignorer tous les champs obligatoires, y compris parent_item_group
            doc.flags.ignore_mandatory = True
            doc.insert(ignore_permissions=True, ignore_mandatory=True)
            frappe.db.commit()
        except Exception as e:
            frappe.log_error(f"Erreur lors de la création du groupe All Item Groups: {str(e)}", "Base Setup Error")
    
    # Créer le groupe de fournisseurs parent
    if not frappe.db.exists("Supplier Group", "All Supplier Groups"):
        try:
            doc = frappe.new_doc("Supplier Group")
            doc.supplier_group_name = "All Supplier Groups"
            doc.is_group = 1
            doc.flags.ignore_mandatory = True
            doc.insert(ignore_permissions=True, ignore_mandatory=True)
            frappe.db.commit()
        except Exception as e:
            frappe.log_error(f"Erreur lors de la création du groupe All Supplier Groups: {str(e)}", "Base Setup Error")
    
    # Vérifier l'existence des UOM (units of measure)
    if not frappe.db.exists("UOM", "Nos"):
        try:
            doc = frappe.new_doc("UOM")
            doc.uom_name = "Nos"
            doc.insert(ignore_permissions=True)
            frappe.db.commit()
        except Exception as e:
            frappe.log_error(f"Erreur lors de la création de l'UOM Nos: {str(e)}", "Base Setup Error")

def process_import_files(file1_url, file2_url, file3_url):
    """Traite les trois fichiers CSV d'importation dans l'ordre:
    1. Item Group
    2. Item
    3. Warehouse
    4. Supplier
    5. Material Request + Request for Quotation (créés ensemble)
    """
    
    temp_dir = os.path.join(get_files_path(), "temp_import_" + frappe.generate_hash())
    details = []
    
    try:
        # Initialiser les objets de base nécessaires
        details.append("Initialisation des objets de base...")
        initialize_base_requirements()
        details.append("✅ Objets de base initialisés")
        
        # Créer le répertoire temporaire
        if not os.path.exists(temp_dir):
            os.makedirs(temp_dir)
        
        # Télécharger les fichiers
        file1_path = save_file_locally(file1_url, os.path.join(temp_dir, "items.csv"))
        file2_path = save_file_locally(file2_url, os.path.join(temp_dir, "suppliers.csv"))
        file3_path = save_file_locally(file3_url, os.path.join(temp_dir, "request_quotation.csv"))
        
        success = True
        
        # 1. Importer Item Group (extraire depuis fichier1.csv)
        details.append("Importation des groupes d'articles...")
        try:
            count = import_item_groups(file1_path)
            details.append(f"✅ {count} groupes d'articles importés avec succès")
        except Exception as e:
            details.append(f"❌ Erreur lors de l'importation des groupes d'articles: {str(e)}")
            success = False
        
        # 2. Importer Item (depuis fichier1.csv)
        details.append("Importation des articles...")
        try:
            count = import_items(file1_path)
            details.append(f"✅ {count} articles importés avec succès")
        except Exception as e:
            details.append(f"❌ Erreur lors de l'importation des articles: {str(e)}")
            success = False
        
        # 3. Importer/vérifier Warehouses (depuis fichier1.csv)
        details.append("Importation des entrepôts...")
        try:
            count = import_warehouses(file1_path)
            details.append(f"✅ {count} entrepôts importés avec succès")
        except Exception as e:
            details.append(f"❌ Erreur lors de l'importation des entrepôts: {str(e)}")
            success = False
        
        # 4. Importer Supplier (depuis fichier2.csv)
        details.append("Importation des fournisseurs...")
        try:
            count = import_suppliers(file2_path)
            details.append(f"✅ {count} fournisseurs importés avec succès")
        except Exception as e:
            details.append(f"❌ Erreur lors de l'importation des fournisseurs: {str(e)}")
            success = False
        
        # 5. Importer Material Request et Request for Quotation ensemble
        details.append("Importation des demandes de matériel et demandes de devis...")
        try:
            # Préparer le mapping des fournisseurs par référence
            supplier_mapping = prepare_supplier_mapping(file3_path)
            # Importer les MR et RFQ ensemble
            mr_count, rfq_count = import_material_requests_and_rfqs(file1_path, supplier_mapping)
            details.append(f"✅ {mr_count} demandes de matériel importées avec succès")
            details.append(f"✅ {rfq_count} demandes de devis importées avec succès")
        except Exception as e:
            details.append(f"❌ Erreur lors de l'importation des demandes: {str(e)}")
            success = False
        
        # Retourner les résultats
        if success:
            return {
                "success": True,
                "message": "Importation terminée avec succès",
                "details": details
            }
        else:
            return {
                "success": False,
                "message": "Des erreurs sont survenues pendant l'importation",
                "details": details
            }
        
    except Exception as e:
        return {
            "success": False,
            "message": f"Erreur lors de l'importation: {str(e)}",
            "details": details
        }
    finally:
        # Nettoyer le répertoire temporaire
        import shutil
        shutil.rmtree(temp_dir, ignore_errors=True)

def save_file_locally(file_url, local_path):
    """Enregistre un fichier depuis le stockage Frappe vers un chemin local"""
    try:
        # Obtenir le contenu du fichier
        file_content = get_file(file_url)[1]
        
        # Écrire en mode binaire
        with open(local_path, 'wb') as f:
            if isinstance(file_content, str):
                f.write(file_content.encode('utf-8'))
            else:
                f.write(file_content)
        
        return local_path
    except Exception as e:
        frappe.log_error(f"Erreur lors de la sauvegarde du fichier {file_url}: {str(e)}", "File Save Error")
        raise e

def read_csv_with_encoding(csv_path):
    """Tente de lire un fichier CSV avec différents encodages"""
    encodings = ['utf-8', 'latin-1', 'cp1252']
    
    for encoding in encodings:
        try:
            with open(csv_path, 'r', encoding=encoding) as csvfile:
                # Lire les premières lignes pour vérifier l'encodage
                reader = csv.DictReader(csvfile)
                data = list(reader)  # Convertir en liste pour tester la lecture
                return data, encoding
        except UnicodeDecodeError:
            if encoding == encodings[-1]:  # Dernière tentative
                frappe.log_error(f"Impossible de lire le fichier CSV avec les encodages: {encodings}", "CSV Import Error")
                raise
            continue
    
    # Si on arrive ici, quelque chose s'est mal passé
    raise Exception("Impossible de lire le fichier CSV avec les encodages disponibles")

def import_item_groups(csv_path):
    """Importe les groupes d'articles depuis le fichier CSV"""
    count = 0
    item_groups = set()
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        # Collecter tous les groupes d'articles uniques
        for row in data:
            item_group = row.get('item_groupe', '').strip()
            if item_group:
                item_groups.add(item_group)
        
        # Créer les groupes d'articles
        for group_name in item_groups:
            try:
                group_name_capitalized = group_name.capitalize()
                if not frappe.db.exists("Item Group", group_name_capitalized):
                    doc = frappe.new_doc("Item Group")
                    doc.item_group_name = group_name_capitalized
                    
                    # Ne pas assigner de parent_item_group pour éviter l'erreur
                    # Ignorer tous les champs obligatoires
                    doc.flags.ignore_mandatory = True
                    doc.insert(ignore_permissions=True, ignore_mandatory=True)
                    count += 1
            except Exception as e:
                frappe.log_error(f"Erreur lors de l'importation du groupe d'articles {group_name}: {str(e)}", "Item Group Import Error")
        
        frappe.db.commit()
        return count
    except Exception as e:
        frappe.log_error(f"Erreur lors de l'import des groupes d'articles: {str(e)}", "Item Group Import Error")
        raise

def import_items(csv_path):
    """Importe les articles depuis le fichier CSV"""
    count = 0
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        for row in data:
            try:
                item_name = row.get('item_name', '').strip()
                if not item_name:
                    continue
                
                item_group = row.get('item_groupe', '').strip().capitalize()
                
                # Vérifier si l'article existe déjà
                if frappe.db.exists("Item", {"item_code": item_name}):
                    continue
                
                # Créer un nouvel article
                doc = frappe.new_doc("Item")
                doc.item_name = item_name
                doc.item_code = item_name  # Utiliser le nom comme code
                
                # Assigner le groupe d'article si possible, sinon laisser vide
                if item_group and frappe.db.exists("Item Group", item_group):
                    doc.item_group = item_group
                
                # Unité de mesure
                doc.stock_uom = "Nos"
                doc.is_stock_item = 1
                
                # Ignorer les champs obligatoires manquants
                doc.flags.ignore_mandatory = True
                doc.insert(ignore_permissions=True, ignore_mandatory=True)
                count += 1
                
            except Exception as e:
                frappe.log_error(f"Erreur lors de l'importation de l'article {row.get('item_name')}: {str(e)}", "Item Import Error")
        
        frappe.db.commit()
        return count
    except Exception as e:
        frappe.log_error(f"Erreur lors de l'import des articles: {str(e)}", "Item Import Error")
        raise

def import_warehouses(csv_path):
    """Importe les entrepôts depuis le fichier CSV"""
    count = 0
    warehouses = set()
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        # Collecter tous les entrepôts uniques
        for row in data:
            warehouse = row.get('target_warehouse', '').strip()
            if warehouse:
                warehouses.add(warehouse)
        
        # Obtenir l'entreprise par défaut
        default_company = frappe.defaults.get_global_default("company") or "ITUniversity"
        
        # Créer les entrepôts
        for warehouse_name in warehouses:
            try:
                # Vérifier si l'entrepôt existe déjà sous n'importe quelle forme
                existing_warehouse = None
                
                # Vérifier par nom exact
                if frappe.db.exists("Warehouse", warehouse_name):
                    existing_warehouse = warehouse_name
                else:
                    # Vérifier par nom d'entrepôt
                    warehouses_by_name = frappe.get_all(
                        "Warehouse", 
                        filters={"warehouse_name": warehouse_name},
                        fields=["name"]
                    )
                    if warehouses_by_name:
                        existing_warehouse = warehouses_by_name[0].name
                
                # Si l'entrepôt n'existe pas, le créer
                if not existing_warehouse:
                    doc = frappe.new_doc("Warehouse")
                    doc.warehouse_name = warehouse_name
                    doc.company = default_company
                    doc.insert(ignore_permissions=True)
                    count += 1
                
            except Exception as e:
                frappe.log_error(f"Erreur lors de l'importation de l'entrepôt {warehouse_name}: {str(e)}", "Warehouse Import Error")
        
        frappe.db.commit()
        return count
    except Exception as e:
        frappe.log_error(f"Erreur lors de l'import des entrepôts: {str(e)}", "Warehouse Import Error")
        raise

def import_suppliers(csv_path):
    """Importe les fournisseurs depuis le fichier CSV"""
    count = 0
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        # Collecter tous les pays uniques pour les créer si nécessaire
        countries = set()
        for row in data:
            country = row.get('country', '').strip()
            if country:
                countries.add(country)
        
        # Créer les pays manquants
        for country_name in countries:
            if not frappe.db.exists("Country", country_name):
                try:
                    doc = frappe.new_doc("Country")
                    doc.country_name = country_name
                    doc.insert(ignore_permissions=True)
                    frappe.db.commit()
                except Exception as e:
                    frappe.log_error(f"Erreur lors de la création du pays {country_name}: {str(e)}", "Country Creation Error")
        
        # Importer les fournisseurs
        for row in data:
            try:
                supplier_name = row.get('supplier_name', '').strip()
                country = row.get('country', '').strip()
                supplier_type = row.get('type', '').strip()
                
                if not supplier_name:
                    continue
                
                # Vérifier si le fournisseur existe déjà
                if frappe.db.exists("Supplier", {"supplier_name": supplier_name}):
                    continue
                
                # Créer un nouveau fournisseur
                doc = frappe.new_doc("Supplier")
                doc.supplier_name = supplier_name
                
                # Assigner le pays seulement s'il existe
                if country and frappe.db.exists("Country", country):
                    doc.country = country
                
                # Assigner le type de fournisseur s'il est fourni
                if supplier_type:
                    doc.supplier_type = supplier_type
                
                # Ignorer les champs obligatoires manquants
                doc.flags.ignore_mandatory = True
                doc.insert(ignore_permissions=True, ignore_mandatory=True)
                count += 1
                
            except Exception as e:
                frappe.log_error(f"Erreur lors de l'importation du fournisseur {row.get('supplier_name')}: {str(e)}", "Supplier Import Error")
        
        frappe.db.commit()
        return count
    except Exception as e:
        frappe.log_error(f"Erreur lors de l'import des fournisseurs: {str(e)}", "Supplier Import Error")
        raise

def prepare_supplier_mapping(csv_path):
    """Prépare un dictionnaire qui mappe les références RFQ aux fournisseurs"""
    supplier_mapping = {}
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        for row in data:
            ref = row.get('ref_request_quotation', '').strip()
            supplier = row.get('supplier', '').strip()
            
            if not ref or not supplier:
                continue
                
            if ref not in supplier_mapping:
                supplier_mapping[ref] = []
            
            if supplier not in supplier_mapping[ref]:
                supplier_mapping[ref].append(supplier)
                
        return supplier_mapping
    
    except Exception as e:
        frappe.log_error(f"Erreur lors de la préparation du mapping des fournisseurs: {str(e)}", "Supplier Mapping Error")
        raise

def import_material_requests_and_rfqs(csv_path, supplier_mapping):
    """
    Importe les demandes de matériel et crée immédiatement les demandes de devis correspondantes
    pour chaque Material Request, puis crée les devis fournisseurs associés.
    """
    mr_count = 0
    rfq_count = 0
    sq_count = 0
    skipped_count = 0
    error_details = []
    
    try:
        data, _ = read_csv_with_encoding(csv_path)
        
        # Obtenir l'entreprise par défaut (ITUniversity)
        default_company = "ITUniversity"
        
        # Obtenir l'entrepôt par défaut ou en créer un si nécessaire
        default_warehouse = get_default_warehouse(default_company)
        
        # Regrouper les données par référence
        grouped_data = {}
        for row in data:
            ref = row.get('ref', '').strip()
            if not ref:
                continue
                
            if ref not in grouped_data:
                grouped_data[ref] = []
                
            grouped_data[ref].append(row)
        
        # Traiter chaque groupe de données (par référence)
        for ref, rows in grouped_data.items():
            try:
                # ---- 1. Créer un Material Request pour cette référence ----
                mr = frappe.new_doc("Material Request")
                mr.material_request_type = rows[0].get('purpose', 'Purchase').strip()
                mr.transaction_date = getdate(parse_date_dmy(rows[0].get('date', '').strip()))
                mr.schedule_date = getdate(parse_date_dmy(rows[0].get('required_by', '').strip()))
                mr.company = default_company
                
                # Si une référence est spécifiée, l'enregistrer dans le titre
                mr.title = f"REF-{ref}"
                
                # Ajouter les articles pour chaque ligne
                items_added = 0
                for row in rows:
                    try:
                        item_name = row.get('item_name', '').strip()
                        quantity = row.get('quantity', '1').strip()
                        required_date = row.get('required_by', '').strip() or today()
                        target_warehouse = row.get('target_warehouse', '').strip()
                        
                        if not item_name:
                            continue
                        
                        # Vérifier si l'article existe
                        if not frappe.db.exists("Item", {"item_code": item_name}):
                            error_message = f"Article {item_name} non trouvé pour la demande de matériel - Ignoré"
                            error_details.append(error_message)
                            frappe.log_error(error_message, "Material Request Import Skipped")
                            continue
                        
                        # Récupérer les informations sur l'unité de mesure de l'article
                        try:
                            item_doc = frappe.get_doc("Item", item_name)
                            item_uom = item_doc.stock_uom
                        except Exception as e:
                            error_message = f"Erreur lors de la récupération des infos de l'article {item_name}: {str(e)}"
                            error_details.append(error_message)
                            frappe.log_error(error_message, "Item Fetch Error")
                            # Utiliser une valeur par défaut pour l'UOM en cas d'erreur
                            item_uom = "Nos"
                        
                        # Utiliser l'entrepôt par défaut si aucun n'est spécifié
                        warehouse_to_use = default_warehouse
                        if target_warehouse:
                            # Rechercher l'entrepôt par nom exact ou partiel pour la société spécifiée
                            warehouses = frappe.get_all(
                                "Warehouse",
                                filters=[
                                    ["company", "=", default_company],
                                    ["name", "like", f"%{target_warehouse}%"]
                                ],
                                fields=["name"],
                                limit=1
                            )
                            
                            if warehouses:
                                warehouse_to_use = warehouses[0].name
                            else:
                                # Si aucun entrepôt correspondant n'est trouvé, créer un nouvel entrepôt
                                try:
                                    wh_doc = frappe.new_doc("Warehouse")
                                    wh_doc.warehouse_name = target_warehouse
                                    wh_doc.company = default_company
                                    wh_doc.flags.ignore_mandatory = True
                                    wh_doc.insert(ignore_permissions=True, ignore_mandatory=True)
                                    frappe.db.commit()
                                    warehouse_to_use = wh_doc.name
                                except Exception as e:
                                    error_message = f"Impossible de créer l'entrepôt {target_warehouse}, utilisation de l'entrepôt par défaut: {str(e)}"
                                    error_details.append(error_message)
                                    frappe.log_error(error_message, "Warehouse Creation Warning")
                        
                        # Ajouter l'article au Material Request
                        mr.append("items", {
                            "item_code": item_name,
                            "qty": float(quantity) if quantity else 1,
                            "schedule_date": getdate(parse_date_dmy(required_date)),
                            "warehouse": warehouse_to_use,
                            "uom": item_uom,
                        })
                        items_added += 1
                        
                    except Exception as item_error:
                        error_message = f"Erreur lors de l'ajout de l'article {row.get('item_name')} à la demande {ref}: {str(item_error)}"
                        error_details.append(error_message)
                        frappe.log_error(error_message, "Material Request Item Error")
                
                # Sauvegarder le Material Request seulement si des articles ont été ajoutés
                if items_added > 0:
                    # Ignorer les champs obligatoires manquants si nécessaire
                    mr.flags.ignore_mandatory = True
                    mr.insert(ignore_permissions=True, ignore_mandatory=True)
                    
                    # Essayer de soumettre la demande, mais continuer même en cas d'erreur
                    try:
                        mr.submit()
                    except Exception as submit_error:
                        error_message = f"Impossible de soumettre la demande de matériel pour la référence {ref}: {str(submit_error)}"
                        error_details.append(error_message)
                        frappe.log_error(error_message, "Material Request Submit Warning")
                    
                    mr_count += 1
                    
                    # ---- 2. Immédiatement créer un Request For Quotation si des fournisseurs sont associés ----
                    if ref in supplier_mapping and supplier_mapping[ref]:
                        try:
                            rfq = frappe.new_doc("Request for Quotation")
                            rfq.transaction_date = mr.transaction_date
                            rfq.company = default_company
                            rfq.title = f"RFQ-{ref}"
                            
                            # Récupérer les détails des éléments du Material Request
                            mr_items = frappe.get_all(
                                "Material Request Item",
                                filters={"parent": mr.name},
                                fields=["name", "item_code", "qty", "schedule_date", "warehouse", "uom"]
                            )
                            
                            # Ajouter les éléments du Material Request au RFQ
                            for item in mr_items:
                                # Obtenir le facteur de conversion UOM
                                conversion_factor = 1.0
                                item_uom = frappe.db.get_value("Item", item.item_code, "stock_uom") or "Nos"
                                
                                if item.uom != item_uom:
                                    try:
                                        uom_conversion = frappe.get_all(
                                            "UOM Conversion Detail",
                                            filters={"parent": item.item_code, "uom": item.uom},
                                            fields=["conversion_factor"]
                                        )
                                        if uom_conversion:
                                            conversion_factor = uom_conversion[0].conversion_factor
                                    except Exception as e:
                                        error_message = f"Erreur de conversion UOM pour {item.item_code}, utilisation de 1.0 par défaut: {str(e)}"
                                        error_details.append(error_message)
                                        frappe.log_error(error_message, "UOM Conversion Warning")
                                
                                rfq.append("items", {
                                    "item_code": item.item_code,
                                    "qty": item.qty,
                                    "schedule_date": item.schedule_date,
                                    "warehouse": item.warehouse,
                                    "material_request": mr.name,
                                    "material_request_item": item.name,
                                    "uom": item_uom,
                                    "stock_uom": item_uom,
                                    "conversion_factor": conversion_factor
                                })
                            
                            # Ajouter les fournisseurs assignés à cette référence
                            suppliers_added = 0
                            for supplier_name in supplier_mapping[ref]:
                                if frappe.db.exists("Supplier", supplier_name):
                                    rfq.append("suppliers", {
                                        "supplier": supplier_name
                                    })
                                    suppliers_added += 1
                                else:
                                    error_message = f"Fournisseur {supplier_name} non trouvé pour la référence {ref}"
                                    error_details.append(error_message)
                                    frappe.log_error(error_message, "Supplier Link Warning")
                            
                            # Sauvegarder le RFQ uniquement si des fournisseurs ont été ajoutés
                            if suppliers_added > 0:
                                rfq.flags.ignore_mandatory = True
                                rfq.insert(ignore_permissions=True, ignore_mandatory=True)
                                rfq_count += 1
                                
                                # Soumettre le RFQ avant de créer des Supplier Quotations
                                try:
                                    rfq.submit()
                                    
                                    # Mettre à jour le RFQ avec un message pour le fournisseur
                                    try:
                                        # Récupérer le document soumis
                                        submitted_rfq = frappe.get_doc("Request for Quotation", rfq.name)
                                        submitted_rfq.message_for_supplier = "Veuillez nous fournir votre meilleure offre pour les articles demandés."
                                        submitted_rfq.db_update()
                                        frappe.db.commit()
                                    except Exception as update_error:
                                        error_message = f"Impossible de mettre à jour le message fournisseur pour la référence {ref}: {str(update_error)}"
                                        error_details.append(error_message)
                                        frappe.log_error(error_message, "RFQ Update Error")
                                    
                                    # ---- 3. Créer un Supplier Quotation pour chaque fournisseur dans le RFQ ----
                                    for supplier in rfq.get("suppliers"):
                                        try:
                                            create_supplier_quotation(rfq, supplier.supplier, default_company, error_details)
                                            sq_count += 1
                                        except Exception as sq_error:
                                            error_message = f"Erreur lors de la création du devis fournisseur pour {supplier.supplier}: {str(sq_error)}"
                                            error_details.append(error_message)
                                            frappe.log_error(error_message, "Supplier Quotation Creation Error")
                                except Exception as submit_error:
                                    error_message = f"Impossible de soumettre la demande de devis pour la référence {ref}: {str(submit_error)}"
                                    error_details.append(error_message)
                                    frappe.log_error(error_message, "RFQ Submit Error")
                            else:
                                error_message = f"Aucun fournisseur valide pour la demande de devis avec référence {ref}"
                                error_details.append(error_message)
                                frappe.log_error(error_message, "RFQ Creation Warning")
                        
                        except Exception as rfq_error:
                            error_message = f"Erreur lors de la création du RFQ pour la référence {ref}: {str(rfq_error)}"
                            error_details.append(error_message)
                            frappe.log_error(error_message, "RFQ Creation Error")
                else:
                    error_message = f"Aucun article valide trouvé pour la demande de matériel avec référence {ref}"
                    error_details.append(error_message)
                    frappe.log_error(error_message, "Material Request Creation Warning")
                    skipped_count += 1
                
            except Exception as ref_error:
                error_message = f"Erreur lors du traitement de la référence {ref}: {str(ref_error)}"
                error_details.append(error_message)
                frappe.log_error(error_message, "Reference Processing Error")
                skipped_count += 1
        
        # Valider toutes les modifications à la fin
        try:
            frappe.db.commit()
        except Exception as commit_error:
            frappe.log_error(f"Erreur lors de la validation des modifications: {str(commit_error)}", "Commit Error")
        
        # Générer un message récapitulatif avec les erreurs
        summary = f"Importation terminée: {mr_count} demandes créées, {rfq_count} demandes de devis, {sq_count} devis fournisseurs, {skipped_count} références ignorées"
        if error_details:
            summary += f" avec {len(error_details)} avertissements/erreurs"
            frappe.log_error(f"{summary}\n\nDétails:\n" + "\n".join(error_details), "Import Summary")
        
        return mr_count, rfq_count
    
    except Exception as e:
        # Journaliser l'erreur mais ne pas interrompre l'importation
        frappe.log_error(f"Erreur générale lors de l'import des demandes: {str(e)}", "General Import Error")
        
        # Essayer de commettre quand même les éléments qui ont été créés
        try:
            frappe.db.commit()
        except:
            pass
        
        # Retourner quand même les éléments créés
        return mr_count, rfq_count

def create_supplier_quotation(rfq, supplier_name, company, error_details=None):
    """
    Crée un Supplier Quotation à partir d'un Request for Quotation pour un fournisseur spécifique.
    Laisse les taux (rates) à 0.
    """
    try:
        # Créer un nouveau Supplier Quotation
        sq = frappe.new_doc("Supplier Quotation")
        sq.supplier = supplier_name
        sq.company = company
        sq.currency = frappe.db.get_value("Company", company, "default_currency")
        sq.transaction_date = rfq.transaction_date
        # Convertir la date de transaction en chaîne, puis ajouter 30 jours
        transaction_date_str = rfq.transaction_date.strftime('%Y-%m-%d')
        sq.valid_till = add_days(transaction_date_str, 30)  # Valide pour 30 jours par défaut
        sq.request_for_quotation = rfq.name
        sq.status = "Draft"
        
        # Ajouter les items du RFQ
        for item in rfq.items:
            sq.append("items", {
                "item_code": item.item_code,
                "qty": item.qty,
                "schedule_date": item.schedule_date,
                "warehouse": item.warehouse,
                "uom": item.uom,
                "stock_uom": item.stock_uom,
                "conversion_factor": item.conversion_factor,
                "description": frappe.db.get_value("Item", item.item_code, "description") or item.item_code,
                "request_for_quotation": rfq.name,
                "request_for_quotation_item": item.name,
                "rate": 0  # Laisser le taux à 0 comme demandé
            })
        
        # Ignorer les champs obligatoires manquants si nécessaire
        sq.flags.ignore_mandatory = True
        sq.insert(ignore_permissions=True, ignore_mandatory=True)
        
        # # Soumettre le Supplier Quotation
        # sq.submit()
        
        return sq.name
    
    except Exception as e:
        if error_details is not None:
            error_message = f"Erreur lors de la création du devis fournisseur pour {supplier_name}: {str(e)}"
            error_details.append(error_message)
        frappe.log_error(f"Erreur lors de la création du devis fournisseur pour {supplier_name}: {str(e)}", "Supplier Quotation Creation Error")
        raise

def get_default_warehouse(company):
    """Récupère ou crée un entrepôt par défaut pour l'entreprise spécifiée"""
    warehouses = frappe.get_all(
        "Warehouse",
        filters={"company": company},
        fields=["name"],
        limit=1
    )
    
    if warehouses:
        return warehouses[0].name
    
    # Créer un entrepôt par défaut si aucun n'existe
    try:
        wh_doc = frappe.new_doc("Warehouse")
        wh_doc.warehouse_name = "Stores"
        wh_doc.company = company
        wh_doc.flags.ignore_mandatory = True
        wh_doc.insert(ignore_permissions=True, ignore_mandatory=True)
        frappe.db.commit()
        return wh_doc.name
    except Exception as e:
        frappe.log_error(f"Erreur lors de la création de l'entrepôt par défaut: {str(e)}", "Warehouse Creation Error")
        return None

def parse_date_dmy(date_str):
    """
    Parse une date au format JJ/MM/AAAA et retourne au format AAAA-MM-JJ.
    Si le format est invalide, retourne la date d'aujourd'hui.
    """
    try:
        if not date_str:
            return today()
        
        # Essayer le format JJ/MM/AAAA
        try:
            dt = datetime.strptime(date_str, '%d/%m/%Y')
            return dt.strftime('%Y-%m-%d')
        except ValueError:
            # Si échec, peut-être que c'est déjà au format AAAA-MM-JJ
            try:
                dt = datetime.strptime(date_str, '%Y-%m-%d')
                return date_str  # Déjà au bon format
            except ValueError:
                # Dernier essai avec MM/JJ/AAAA (pour couvrir tous les formats possibles)
                dt = datetime.strptime(date_str, '%m/%d/%Y')
                return dt.strftime('%Y-%m-%d')
    except Exception as e:
        frappe.log_error(f"Erreur de conversion de date {date_str}: {str(e)}", "Date Parse Error")
        return today() 