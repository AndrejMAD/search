$(function() {

    $('.send').on('click', function() {
        let query = $('.query').val();

        $.post('/search', {query: query}, function(response) {
            $('.result').html('');

            for (i in response) {
                $('.result').append($('<div class="result-item"><a href="' + response[i].uri +'">' + response[i].title + '</a></div>'));
            }
        });
    })
})