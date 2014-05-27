$(document).ready(function(){
  var pathname = window.location.pathname;
  var matches = pathname.match(/\/([^/]+\/[^/]+)/);
  var pair = matches[1]
  var underpair = pair.replace(/\//,'_');
  var maxchars = 256;
  var firstmessage = 0;
  var messagelim = 32;

  function sendMessage(message) {
    $.post('/pub/' + pair, message);
  }

  function updateCharsLeft(len) {
    $('.charleft').text('chars left: ' + (maxchars - len));
  }

  function escapeHtml(m) {
    return $('<div/>').text(m).html();
  }

  function padzero(v) {
    return (v < 10) ? '0' + v : v;
  }

  function makeDate(dt) {
    return dt.getFullYear() + '-' + padzero(dt.getMonth()+1) + '-' + padzero(dt.getDate())
  }

  function makeTime(time) {
    var date = new Date(time);
    var today = makeDate(new Date());
    var someday = makeDate(date);
    var datetime = padzero(date.getHours()) + ':' + padzero(date.getMinutes()) + ':' + padzero(date.getSeconds());
    if (today != someday) {
      datetime = someday + ' ' + datetime;
    }
    var tz = date.getTimezoneOffset();
    var offset = ((tz<0? '+':'-') + padzero(parseInt(Math.abs(tz/60))) + padzero(Math.abs(tz%60)));
    return datetime + ' GMT' + offset;
  }
    
  function ordinalClick() {
    var input = $('textarea');
    var cursorPos = input.prop('selectionStart');
    var v = input.val();
    var textBefore = v.substring(0,  cursorPos);
    var textAfter  = v.substring(cursorPos);
    var ref = ((textBefore.length > 0 && !textBefore.match(/\s+$/)) ? ' ' : '') + $(this).text() + ' ';
     input.val(textBefore + ref + textAfter);
     input.prop('selectionStart', cursorPos + ref.length);
     input.focus();
  }

  function makeMessage(msgdata, pseudo, isHistory) {
    var abc = $('<div/>');
    var msg = $('<div class="message' + ((isHistory) ? ' history' : '') + '"/>');
    if (!msgdata) {
      msg.append('<div class="error">Error #404</div>');
      return msg;
    }
    var aordinal = $('<a class="ordinal">&gt;&gt;' + msgdata.id + '</a>');
    var postTime = makeTime(msgdata.posted);
    if (!pseudo) {
      msg.append('<a name="' + msgdata.id + '"></a>');
      aordinal.click(ordinalClick);
    }
    msg
      .append(aordinal)
      .append('<div class="clearfix"/>')
      .append('<div class="datetime"><span>' + postTime + '</span><img width="16" height="16" src="http://www.gravatar.com/avatar/' + msgdata.remote + '?d=identicon&s=16" class="hash"></img></div>')
      .append('<div class="clearfix"/>')
      .append('<img width="32" height="32" src="http://www.gravatar.com/avatar/' + msgdata.author + '?d=identicon&s=32" class="hash"></img>')
      .append('<div class="msgtext">' + msgdata.message + '</div>')
      .append('<div class="clearfix"/>');

    function processTooltip(m, parenttip) {
      m.find('a[href^="#"]').filter(function(){
        return $(this).attr('href').match(/^#\d+$/);
      }).mouseenter(function() {
        var id = $(this).attr('href').substring(1);
        ex = $('a[name=' + id + ']');
        if (ex.length > 0) {
          var parent = ex.parent().clone();
          processTooltip(parent, this);
          var that = this;
          if (parenttip) {
            parenttip.childtip = this;
          }
          parent.mouseleave(function(){
            $(that).tooltip('close');
          });
          $(this).tooltip({
            content: function() {
              return parent;
            },
            items: '*',
            position: {
              my: 'left top',
              at: 'left top',
              collision: 'fit fit'
            },
            open: function() {
              $(this).attr('tooltiped', true);
            },
            close: function() {
              $(this).attr('tooltiped', false);
              if(this.childtip) {
                $(this.childtip).tooltip().tooltip('close');
              }
            }
          }).on('mouseout focusout', function(event) {
            event.stopImmediatePropagation();
          });
          $(this).tooltip('open');
        }
      });
    }

    processTooltip(msg);

    return msg;
  }

  function addMessage(msgdata, pseudo, isHistory, isPrevHistory) {
    var chatarea = $('.chat-area')[0];
    var toScroll = ((chatarea.scrollHeight - chatarea.scrollTop) - $(chatarea).height()) < 50;
    var msg = makeMessage(msgdata, pseudo, isHistory);
    if (isPrevHistory) {
      $('.histbutt').after(msg);
      chatarea.scrollTop += msg.outerHeight(true);
    } else {
      $(chatarea).append(msg);
      if (toScroll || isHistory) {
        chatarea.scrollTop = chatarea.scrollHeight;
      }
    }
  }

  updateCharsLeft(0);

  $('textarea').keyup(function(e) {
    $(this).val($(this).val().substr(0,maxchars));
    var len = $(this).val().length
    updateCharsLeft(len);
  }).keydown(function(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
      var message = $(this).val();
      if (message.replace(/\s/g,'').length > 0) {
        sendMessage(message);
      }
      $(this).val('');
      e.preventDefault();
      return false;
    }
    if (e.keyCode >= 48) {
      var len = $(this).val().length;
        if(len >= 256) {
          e.preventDefault();
          return false;
        }
      }
    return true;
  });

  $.post('/history/' + pair, function(data) {
    if (data.length > 0) {
      firstmessage = data[0].id;
      if (data.length >= messagelim) {
        var histbutt = $('<a class="histbutt" href="#">...</a>').click(function(){
          $.post('/history/' + pair + '/' + firstmessage + '/before', function(pdata) {

            firstmessage = pdata[0].id;
            for (var i = pdata.length-1; i >= 0; i--) {
              addMessage(pdata[i], false, true, true);
            }

            if (pdata.length < messagelim) {
              $('.histbutt').remove();
            }
          });
        });
        $('.chat-area').append(histbutt);
      }
    } 
    for (var i = 0; i < data.length; i++) {
      addMessage(data[i], false, true, false);
    }
  });

  (function poll() {
    $.ajax({url: '/sub/' + underpair, success: function(data) {
      if (data.type == 'message') {
        addMessage(data.data, false, false, false);
      }
    }, dataType: 'json', complete: poll, timeout: 30000});
  })();

  function statupdate(){
    $.ajax({url: '/stat/' + underpair, success: function(data) {
      console.log(data);
      $('.counter').text('online: ' + data.subscribers);
      setTimeout(statupdate, 5000);
    }, dataType: 'json'});
  };

    $('.chat-area').mouseenter(function() {
      $('a[tooltiped=true]').tooltip('close');   
    });


  statupdate();
});
