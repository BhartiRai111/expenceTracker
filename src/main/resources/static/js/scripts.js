/*!
* Start Bootstrap - Freelancer v7.0.7
*/

// ================= GLOBAL FUNCTIONS =================

// ✅ Filter function (bahar hona chahiye)
function applyFilter() {
    let from = document.getElementById("fromDate").value;
    let to = document.getElementById("toDate").value;

    fetch(`/dashboard/filter?from=${from}&to=${to}`)
        .then(res => res.json())
        .then(data => {

            categoryData = data.categoryData;
            monthlyData = data.monthlyData;

            // Update total
            let total = Object.values(categoryData)
                .reduce((sum, val) => sum + val, 0);

            document.getElementById("totalExpense").innerText =
                total.toLocaleString();

            // Re-render charts
            renderCharts();
        });
}

// ✅ Chart render function (clean version)
function renderCharts() {

    // destroy old charts
    if (window.pieChart && typeof window.pieChart.destroy === "function") {
        window.pieChart.destroy();
    }

    if (window.barChart && typeof window.barChart.destroy === "function") {
        window.barChart.destroy();
    }

    let categoryLabels = Object.keys(categoryData);
    let categoryValues = Object.values(categoryData);

    window.pieChart = new Chart(document.getElementById('pieChart'), {
        type: 'pie',
        data: {
            labels: categoryLabels,
            datasets: [{
                data: categoryValues,
                backgroundColor: [
                    '#4e73df',
                    '#1cc88a',
                    '#36b9cc',
                    '#f6c23e',
                    '#e74a3b'
                ]
            }]
        }
    });

    let monthLabels = Object.keys(monthlyData);
    let monthValues = Object.values(monthlyData);

    window.barChart = new Chart(document.getElementById('barChart'), {
        type: 'bar',
        data: {
            labels: monthLabels,
            datasets: [{
                label: 'Monthly Expenses',
                data: monthValues,
                backgroundColor: '#36b9cc'
            }]
        },
        options: {
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 500
                    }
                }
            }
        }
    });

    const ctxLine = document.getElementById("lineChart");

    if (ctxLine) {
        new Chart(ctxLine, {
            type: 'line',
            data: {
                labels: Object.keys(monthlyData),
                datasets: [
                    {
                        label: 'Expense',
                        data: Object.values(monthlyData),
                        borderWidth: 2,
                        fill: false
                    },
                    {
                        label: 'Income',
                        data: Object.values(incomeData),
                        borderWidth: 2,
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true
            }
        });
    }
}

function sendMessage() {
    let input = document.getElementById("chatInput");
    let message = input.value;

    if (!message) return;

    let chatBox = document.getElementById("chatMessages");

    // user message
    chatBox.innerHTML += `<div><b>You:</b> ${message}</div>`;

    fetch("/api/chat", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ message: message })
    })
    .then(res => res.json())
    .then(data => {
        chatBox.innerHTML += `<div style="color:green;"><b>Bot:</b> ${data.reply}</div>`;
        chatBox.scrollTop = chatBox.scrollHeight;
    });

    input.value = "";
}

function exportExcel() {
    let from = document.getElementById("fromDate").value;
    let to = document.getElementById("toDate").value;
    // ✅ from mandatory
    if (!from) {
        alert("Please select FROM date");
        return;
    }
    // ✅ to optional → current date set
    if (!to) {
        let today = new Date();
        to = today.toISOString().split("T")[0];
    }
    window.location.href =
        `/dashboard/export?from=${from}&to=${to}`;
}


// ================= MAIN =================

window.addEventListener('DOMContentLoaded', event => {

    // ================= NAVBAR =================
    var navbarShrink = function () {
        const navbarCollapsible = document.body.querySelector('#mainNav');
        if (!navbarCollapsible) return;

        if (window.scrollY === 0) {
            navbarCollapsible.classList.remove('navbar-shrink')
        } else {
            navbarCollapsible.classList.add('navbar-shrink')
        }
    };

    navbarShrink();
    document.addEventListener('scroll', navbarShrink);

    const mainNav = document.body.querySelector('#mainNav');
    if (mainNav) {
        new bootstrap.ScrollSpy(document.body, {
            target: '#mainNav',
            rootMargin: '0px 0px -40%',
        });
    }

    const navbarToggler = document.body.querySelector('.navbar-toggler');
    const responsiveNavItems = [].slice.call(
        document.querySelectorAll('#navbarResponsive .nav-link')
    );

    responsiveNavItems.map(function (item) {
        item.addEventListener('click', () => {
            if (window.getComputedStyle(navbarToggler).display !== 'none') {
                navbarToggler.click();
            }
        });
    });

    // ================= DASHBOARD INIT =================

    if (typeof categoryData !== 'undefined' && typeof monthlyData !== 'undefined') {
        console.log("DATA RECEIVED:", categoryData, monthlyData);
        renderCharts();   // ✅ MUST
        let total = Object.values(categoryData)
            .reduce((sum, val) => sum + val, 0);
        document.getElementById("totalExpense").innerText =
            total.toLocaleString();
    }

});