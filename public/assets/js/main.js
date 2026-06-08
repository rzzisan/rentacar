/* Basic JavaScript utilities */

// Confirm delete action
function confirmDelete(message = 'আপনি নিশ্চিত?') {
    return confirm(message);
}

// Show alert message
function showAlert(message, type = 'success') {
    alert(message);
}

// Redirect to page
function redirectTo(url) {
    window.location.href = url;
}

// Format currency
function formatCurrency(amount, currency = 'BDT') {
    return new Intl.NumberFormat('bn-BD', {
        style: 'currency',
        currency: 'BDT',
    }).format(amount);
}

// Format date
function formatDate(dateString) {
    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return new Date(dateString).toLocaleDateString('bn-BD', options);
}

// Submit form
function submitForm(formId) {
    document.getElementById(formId).submit();
}
