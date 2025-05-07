import frappe
import os
import csv
import json
import uuid
import zipfile
from frappe import _
from frappe.utils import get_files_path, get_site_path
from frappe.utils.file_manager import save_file, get_file

@frappe.whitelist()
def export_csv_data():
    """
    Exporte les données des doctypes métier vers des fichiers CSV
    pour un téléchargement direct.
    """
    # Vérification des permissions
    if "System Manager" not in frappe.get_roles(frappe.session.user):
        frappe.throw(_("Only System Managers can export data"))
    
    try:
        # Créer un ID unique pour le fichier d'export
        export_id = str(uuid.uuid4())
        
        # Créer un répertoire temporaire pour stocker les fichiers CSV
        export_dir = os.path.join(get_files_path(), "temp_export_" + export_id)
        
        # Créer le répertoire temporaire s'il n'existe pas
        if not os.path.exists(export_dir):
            os.makedirs(export_dir)
        
        # Liste des doctypes à exporter
        doc_types_to_export = [
            # Catalogue produits
            "Item"
        ]
        
        # Exporter chaque doctype
        for doctype in doc_types_to_export:
            # Exporter vers CSV
            csv_path = os.path.join(export_dir, f"{doctype}.csv")
            export_doctype_to_csv(doctype, csv_path)
        
        # Créer l'archive ZIP
        zip_filename = f"data_export_{export_id}.zip"
        zip_path = os.path.join(export_dir, zip_filename)
        
        # Ajouter tous les fichiers CSV à l'archive
        with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
            for doctype in doc_types_to_export:
                csv_path = os.path.join(export_dir, f"{doctype}.csv")
                if os.path.exists(csv_path):
                    zipf.write(csv_path, f"{doctype}.csv")
        
        # Lire le contenu du fichier ZIP
        with open(zip_path, 'rb') as f:
            file_content = f.read()
        
        # Nettoyer les fichiers temporaires
        import shutil
        shutil.rmtree(export_dir, ignore_errors=True)
        
        # Renvoyer le fichier directement comme réponse
        frappe.response['type'] = 'download'
        frappe.response['filename'] = zip_filename
        frappe.response['filecontent'] = file_content
        frappe.response['content_type'] = 'application/zip'
        
    except Exception as e:
        frappe.log_error(f"Error during data export: {str(e)}", "Data Export Error")
        frappe.throw(_("Error during data export: {0}").format(str(e)))

def export_doctype_to_csv(doctype, csv_path):
    """
    Exporte les données d'un doctype spécifique vers un fichier CSV
    """
    # Récupérer tous les documents du doctype
    docs = frappe.get_all(
        doctype,
        fields=["*"],
        limit=100000  # Limiter à 100 000 documents par sécurité
    )
    
    if not docs:
        # Pas de données à exporter
        return 0
    
    # Obtenir la liste des champs à partir du premier document
    fields = docs[0].keys() if docs else []
    
    # Écrire les données dans le fichier CSV
    with open(csv_path, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fields)
        writer.writeheader()
        
        for doc in docs:
            # Convertir les objets complexes en JSON
            for field, value in doc.items():
                if isinstance(value, (dict, list)):
                    doc[field] = json.dumps(value)
            
            writer.writerow(doc)
    
    return len(docs) 