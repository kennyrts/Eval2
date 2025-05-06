frappe.pages['database'].on_page_load = function(wrapper) {
	var page = frappe.ui.make_app_page({
		parent: wrapper,
		title: 'Database',
		single_column: true
	});

	// Add sections
	page.main.html(`
		<div class="database-page">
			<div class="section-import card">
				<div class="section-title">
					<i class="fa fa-upload"></i>
					Import Data
				</div>
				<div class="section-content">
					<form id="csv-import-form">
						<div class="form-group file-upload">
							<label for="material-request-file">
								<i class="fa fa-file-text-o"></i>
								Material Request CSV File
							</label>
							<div class="file-input-wrapper">
								<input type="file" 
									class="form-control" 
									id="material-request-file" 
									name="material_request"
									accept=".csv"
									required>
								<div class="file-name">No file selected</div>
							</div>
						</div>
						<div class="form-group file-upload">
							<label for="supplier-file">
								<i class="fa fa-building-o"></i>
								Supplier CSV File
							</label>
							<div class="file-input-wrapper">
								<input type="file" 
									class="form-control" 
									id="supplier-file" 
									name="supplier"
									accept=".csv"
									required>
								<div class="file-name">No file selected</div>
							</div>
						</div>
						<div class="form-group file-upload">
							<label for="quotation-file">
								<i class="fa fa-file-o"></i>
								Request for Quotation CSV File
							</label>
							<div class="file-input-wrapper">
								<input type="file" 
									class="form-control" 
									id="quotation-file" 
									name="quotation"
									accept=".csv"
									required>
								<div class="file-name">No file selected</div>
							</div>
						</div>
						<button type="submit" class="btn btn-primary btn-import-csv">
							<i class="fa fa-upload"></i> Import CSV Files
						</button>
					</form>
				</div>
			</div>
			<div class="section-reset card">
				<div class="section-title">
					<i class="fa fa-database"></i>
					Database Management
				</div>
				<div class="section-content">
					<button class="btn btn-danger btn-reset-db">
						<i class="fa fa-refresh"></i> Reset Database
					</button>
				</div>
			</div>
		</div>
	`);

	// Add styles
	frappe.dom.set_style(`
		.database-page {
			padding: 20px;
			max-width: 800px;
			margin: 0 auto;
		}
		.card {
			background-color: var(--card-bg);
			border-radius: 12px;
			box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
			margin-bottom: 30px;
			transition: all 0.3s ease;
		}
		.card:hover {
			transform: translateY(-2px);
			box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
		}
		.section-title {
			font-size: 18px;
			font-weight: 600;
			color: var(--text-color);
			padding: 20px 25px;
			border-bottom: 1px solid var(--border-color);
			display: flex;
			align-items: center;
			gap: 10px;
		}
		.section-content {
			padding: 25px;
		}
		.form-group {
			margin-bottom: 20px;
		}
		.file-upload label {
			display: flex;
			align-items: center;
			gap: 8px;
			font-weight: 500;
			color: var(--text-color);
			margin-bottom: 8px;
		}
		.file-input-wrapper {
			position: relative;
			border: 2px dashed var(--border-color);
			border-radius: 8px;
			padding: 15px;
			background: var(--bg-color);
			transition: all 0.3s ease;
		}
		.file-input-wrapper:hover {
			border-color: var(--primary);
			background: var(--bg-light);
		}
		.file-input-wrapper input[type="file"] {
			opacity: 0;
			position: absolute;
			top: 0;
			left: 0;
			width: 100%;
			height: 100%;
			cursor: pointer;
		}
		.file-name {
			color: var(--text-muted);
			font-size: 13px;
		}
		.btn {
			padding: 10px 20px;
			border-radius: 6px;
			font-weight: 500;
			transition: all 0.3s ease;
			display: inline-flex;
			align-items: center;
			gap: 8px;
		}
		.btn-primary {
			background-color: var(--primary);
			color: white;
			border: none;
		}
		.btn-primary:hover {
			background-color: var(--primary-dark);
			transform: translateY(-1px);
		}
		.btn-danger {
			background-color: var(--red);
			color: white;
			border: none;
		}
		.btn-danger:hover {
			background-color: var(--red-dark);
			transform: translateY(-1px);
		}
		.btn i {
			font-size: 14px;
		}
	`);

	// Update file name display
	function updateFileName(input) {
		const fileName = input.files[0] ? input.files[0].name : 'No file selected';
		$(input).closest('.file-input-wrapper').find('.file-name').text(fileName);
	}

	page.main.find('input[type="file"]').on('change', function() {
		updateFileName(this);
	});

	// Handle Import CSV
	page.main.find('#csv-import-form').on('submit', function(e) {
		e.preventDefault();
		
		let materialRequestFile = page.main.find('#material-request-file').get(0).files[0];
		let supplierFile = page.main.find('#supplier-file').get(0).files[0];
		let quotationFile = page.main.find('#quotation-file').get(0).files[0];
		
		if (!materialRequestFile || !supplierFile || !quotationFile) {
			frappe.throw(__('Please select all three CSV files'));
			return;
		}

		let formData = new FormData();
		formData.append('material_request_file', materialRequestFile);
		formData.append('supplier_file', supplierFile);
		formData.append('quotation_file', quotationFile);

		frappe.call({
			method: 'erpnext.database.page.database.database.import_csv',
			args: {
				files: formData
			},
			freeze: true,
			freeze_message: __('Importing CSV files...'),
			callback: function(r) {
				if (!r.exc) {
					frappe.show_alert({
						message: __('CSV import successful'),
						indicator: 'green'
					});
					// Reset all file inputs and their display
					page.main.find('input[type="file"]').val('').each(function() {
						updateFileName(this);
					});
				}
			}
		});
	});

	// Handle Reset Database
	page.main.find('.btn-reset-db').on('click', function() {
		frappe.confirm(
			'Are you sure you want to reset the database? This action cannot be undone.',
			function() {
				frappe.call({
					method: 'erpnext.database.page.database.database.reset_database',
					freeze: true,
					freeze_message: __('Resetting Database...'),
					callback: function(r) {
						if (!r.exc) {
							frappe.show_alert({
								message: __('Database reset successful'),
								indicator: 'green'
							});
							frappe.ui.toolbar.clear_cache();
						}
					}
				});
			}
		);
	});
}