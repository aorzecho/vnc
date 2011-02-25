dojo.require("dojo.string");

(function($, d){

  $.vnc = {
  
    start: function(node, args){

      $.applet.inject(node, {
        archive: '/applet/vnc.jar?v=' + new Date().getTime(),
        code:"com.tigervnc.VncApplet",
        port: args.port,
        host: args.host,
        title: args.title,
        new_window: "Yes",
        log_level: "error"
      });
    }
  };   
 })(window, dojo);
