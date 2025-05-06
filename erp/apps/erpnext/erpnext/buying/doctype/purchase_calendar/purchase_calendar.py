# Copyright (c) 2025, Frappe Technologies Pvt. Ltd. and contributors
# For license information, please see license.txt

import frappe
from frappe.model.document import Document
from frappe.utils import getdate


class PurchaseCalendar(Document):
	# begin: auto-generated types
	# This code is auto-generated. Do not modify anything in this block.

	from typing import TYPE_CHECKING

	if TYPE_CHECKING:
		from frappe.types import DF

		color: DF.Color | None
		end: DF.Datetime | None
		reference_name: DF.DynamicLink | None
		reference_type: DF.Literal["Material Request", "Request for Quotation", "Supplier Quotation", "Purchase Order", "Purchase Invoice", "Purchase Invoice", "Purchase Receipt"]
		start: DF.Datetime | None
		status: DF.Data | None
		title: DF.Data | None
	# end: auto-generated types

	pass

@frappe.whitelist()
def get_events(start, end, filters=None):
	"""Retourne les événements pour le calendrier des achats"""
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
	
	for doctype, date_field in doctypes.items():
		# Récupérer les documents pour chaque type
		docs = frappe.get_all(doctype,
			fields=["name", date_field, "status"],
			filters=[
				[date_field, "between", [start, end]],
				["docstatus", "!=", "2"]  # Exclure les documents annulés
			]
		)
		
		for doc in docs:
			event_date = getdate(doc.get(date_field))
			events.append({
				"title": f"{doctype}: {doc.name}",
				"start": event_date,
				"end": event_date,
				"reference_type": doctype,
				"reference_name": doc.name,
				"status": doc.status,
				"color": colors.get(doctype),
				"all_day": 1
			})
	
	return events
