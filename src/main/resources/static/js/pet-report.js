<script th:inline="javascript">
        document.addEventListener("DOMContentLoaded", function () {
            var stats = [[${statsBySpecies}]];

            console.log("Dữ liệu biểu đồ:", stats);

            var ctx = document.getElementById('petChart');
            if (!ctx) return;

            var labels = [];
            var data = [];
            var backgroundColors = ['#f97316', '#3b82f6', '#10b981', '#ef4444', '#8b5cf6']; // Cam, Xanh, Lục, Đỏ, Tím

            if (stats && stats.length > 0) {
                labels = stats.map(function(item) { return item[0]; });
                data = stats.map(function(item) { return item[1]; });
            } else {
                labels = ['No Data'];
                data = [1];
                backgroundColors = ['#e5e7eb'];
            }

            new Chart(ctx.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: backgroundColors,
                        borderWidth: 0,
                        hoverOffset: 10
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '70%',
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: {
                                usePointStyle: true,
                                padding: 20,
                                font: { size: 12 }
                            }
                        }
                    }
                }
            });
        });

        function toggleModal(modalID) {
            const modal = document.getElementById(modalID);
            if (modal) {
                modal.classList.toggle('opacity-0');
                modal.classList.toggle('pointer-events-none');
                document.body.classList.toggle('modal-active');
            }
        }

        document.addEventListener("DOMContentLoaded", function () {
            const stats = window.petStatsData;
            const ctx = document.getElementById('petChart');

            if (!ctx) return;

            let labels = ['No Data'];
            let data = [1];
            let colors = ['#e5e7eb'];

            if (stats && stats.length > 0) {
                labels = stats.map(item => item[0]);
                data = stats.map(item => item[1]);
                colors = ['#f97316', '#3b82f6', '#10b981', '#a855f7', '#ef4444']; // Cam, Xanh, Lục, Tím, Đỏ
            }

            new Chart(ctx.getContext('2d'), {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: colors,
                        borderWidth: 0,
                        hoverOffset: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    cutout: '70%',
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    let label = context.label || '';
                                    if (label) { label += ': '; }
                                    let value = context.raw;
                                    return label + value;
                                }
                            }
                        }
                    }
                }
            });
        });
    </script>