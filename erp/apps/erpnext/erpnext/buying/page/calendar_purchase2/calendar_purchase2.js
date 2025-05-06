frappe.pages['calendar-purchase2'].on_page_load = function(wrapper) {
    let page = frappe.ui.make_app_page({
        parent: wrapper,
        title: __('Purchase Calendar'),
        single_column: true
    });

    // Créer le conteneur du calendrier
    let calendar_wrapper = $('<div></div>').appendTo(page.main);

    let options = {
        doctype: 'Purchase Calendar',
        parent: calendar_wrapper,
        page: page,
        initial_view: 'month'
    };

    // Définir la source des événements
    options.get_events_method = 'erpnext.buying.page.calendar_purchase2.calendar_purchase2.get_events';

    // Ajouter des filtres
    page.add_field({
        fieldtype: 'Select',
        fieldname: 'document_type',
        label: __('Document Type'),
        options: [
            'All',
            'Material Request',
            'Request for Quotation',
            'Supplier Quotation',
            'Purchase Order',
            'Purchase Invoice',
            'Payment Entry',
            'Purchase Receipt'
        ],
        default: 'All',
        change: function() {
            calendar.refresh();
        }
    });

    // Initialiser le calendrier
    let calendar = new frappe.views.Calendar(options);

    // Ajouter un bouton de rafraîchissement
    page.add_menu_item(__('Refresh Calendar'), function() {
        calendar.refresh();
    });
}; 