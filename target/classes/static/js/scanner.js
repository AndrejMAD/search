$(function() {

    $('.scan-sites').on('click', function() {
        $.get('/scan', {}, function(response) {
            alert('Run scanning sites.');
        });

        setInterval(updateScanCount, 1000);
    })

    let updateScanCount = function() {
        $.get('/scan/count', {}, function(response) {
            $('.scan-count-list').html($('<div class="scan-count">'+ response + '</div>'));
        })
    }
})