import frappe
from frappe import _

@frappe.whitelist()
def process_file():
    try:
        if not frappe.request.files:
            return {"status": "error", "message": "Aucun fichier n'a été envoyé"}

        uploaded_file = frappe.request.files['file']
        
        if not uploaded_file.filename.endswith('.txt'):
            return {"status": "error", "message": "Le fichier doit être un fichier .txt"}

        # Lire le contenu du fichier
        content = uploaded_file.read().decode('utf-8')
        
        # Vous pouvez ajouter ici d'autres traitements sur le contenu si nécessaire
        
        return {
            "status": "success",
            "message": "OK",
            "content": content
        }
        
    except Exception as e:
        frappe.log_error(frappe.get_traceback(), "Erreur de traitement du fichier")
        return {"status": "error", "message": str(e)} 