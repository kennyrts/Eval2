frappe.pages['database'].on_page_load = function(wrapper) {
	var page = frappe.ui.make_app_page({
		parent: wrapper,
		title: 'Database',
		single_column: true
	});

	// Add sections
	page.main.html(`
		<div class="database-page">
			<div class="section-test card">
				<div class="section-title">
					<i class="fa fa-check-circle"></i>
					Formulaire de Test
				</div>
				<div class="section-content">
					<div class="test-form">
						<div class="form-group">
							<label for="test_input">Champ de test</label>
							<input type="text" class="form-control" id="test_input" placeholder="Entrez une valeur de test">
						</div>
						<div class="form-group">
							<label for="test_file">Fichier de test</label>
							<div class="file-input-wrapper">
								<input type="file" class="form-control" id="test_file" name="test_file">
								<div class="file-name">Aucun fichier sélectionné</div>
							</div>
						</div>
						<button class="btn btn-primary" id="test_button">
							<i class="fa fa-play"></i> Tester la fonction
						</button>
						<div id="test_result" class="mt-3"></div>
					</div>
				</div>
			</div>
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
									name="material_request_file"
									accept=".csv"
									>
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
									name="supplier_file"
									accept=".csv"
									>
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
									name="quotation_file"
									accept=".csv"
									>
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

	// Update file name display for all file inputs
	page.main.find('input[type="file"]').on('change', function() {
		const fileName = this.files[0] ? this.files[0].name : 'Aucun fichier sélectionné';
		$(this).closest('.file-input-wrapper').find('.file-name').text(fileName);
	});

	// Handle Import CSV
	page.main.find('#csv-import-form').on('submit', function(e) {
		e.preventDefault();
		
		// Get files
		let materialRequestFile = page.main.find('#material-request-file').get(0).files[0];
		let supplierFile = page.main.find('#supplier-file').get(0).files[0];
		let quotationFile = page.main.find('#quotation-file').get(0).files[0];
		
		if (!materialRequestFile && !supplierFile && !quotationFile) {
			frappe.msgprint(__('Veuillez sélectionner au moins un fichier CSV à importer'));
			return;
		}

		// Désactiver le bouton pendant l'import
		let $importButton = page.main.find('.btn-import-csv');
		$importButton.prop('disabled', true);
		$importButton.html('<i class="fa fa-spinner fa-spin"></i> Import en cours...');

		// Upload each file individually first
		Promise.all([
			uploadFile(materialRequestFile),
			uploadFile(supplierFile),
			uploadFile(quotationFile)
		]).then(([file1_url, file2_url, file3_url]) => {
			// Call import_three_csv with the file URLs
			frappe.call({
				method: 'erpnext.database.page.database.database.import_three_csv',
				args: {
					file1_url: file1_url,
					file2_url: file2_url,
					file3_url: file3_url
				},
				callback: function(r) {
					$importButton.prop('disabled', false);
					$importButton.html('<i class="fa fa-upload"></i> Import CSV Files');

					if (r.message && r.message.success) {
						frappe.msgprint({
							title: __('Success'),
							indicator: 'green',
							message: r.message.message + '<br><br>' + r.message.details.join('<br>')
						});
					} else {
						frappe.msgprint({
							title: __('Error'),
							indicator: 'red',
							message: r.message ? r.message.message : __('Import failed')
						});
					}
				}
			});
		}).catch(error => {
			$importButton.prop('disabled', false);
			$importButton.html('<i class="fa fa-upload"></i> Import CSV Files');
			frappe.msgprint({
				title: __('Error'),
				indicator: 'red',
				message: __('Failed to upload files: ') + error
			});
		});
	});

	// Function to upload a file and return its URL
	function uploadFile(file) {
		return new Promise((resolve, reject) => {
			if (!file) {
				reject('No file provided');
				return;
			}

			let form_data = new FormData();
			form_data.append('file', file);
			form_data.append('is_private', 1);
			form_data.append('csrf_token', frappe.csrf_token);

			$.ajax({
				url: '/api/method/upload_file',
				type: 'POST',
				data: form_data,
				processData: false,
				contentType: false,
				success: function(r) {
					if (r.message) {
						resolve(r.message.file_url);
					} else {
						reject('File upload failed');
					}
				},
				error: function() {
					reject('File upload failed');
				}
			});
		});
	}

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

	// Handle Test Button
	page.main.find('#test_button').on('click', function() {
		let test_value = page.main.find('#test_input').val();
		let test_file = page.main.find('#test_file').get(0).files[0];
		
		let form_data = new FormData();
		if (test_value) {
			form_data.append('test_value', test_value);
		}
		if (test_file) {
			form_data.append('test_file', test_file);
		}
		
		// Ajouter le csrf_token pour la sécurité
		form_data.append('csrf_token', frappe.csrf_token);
		
		$.ajax({
			url: '/api/method/erpnext.database.page.database.database.test_function',
			type: 'POST',
			data: form_data,
			processData: false,
			contentType: false,
			headers: {
				'X-Frappe-CSRF-Token': frappe.csrf_token
			},
			success: function(r) {
				if (!r.exc && r.message) {
					frappe.show_alert({
						message: r.message.message,
						indicator: r.message.status
					}, 5);
					
					let resultHtml = `<div class="alert alert-success">`;
					resultHtml += `<p>${r.message.message}</p>`;
					if (r.message.file_content) {
						resultHtml += `<hr><pre>${r.message.file_content}</pre>`;
					}
					resultHtml += `</div>`;
					
					page.main.find('#test_result').html(resultHtml);
				} else {
					frappe.show_alert({
						message: "Erreur : Réponse invalide du serveur",
						indicator: 'red'
					}, 5);
				}
			},
			error: function(xhr, status, error) {
				console.error("Erreur détaillée:", xhr.responseText);
				frappe.show_alert({
					message: "Erreur lors de l'envoi : " + (xhr.responseJSON ? xhr.responseJSON._server_messages : error),
					indicator: 'red'
				}, 5);
			}
		});
	});
}