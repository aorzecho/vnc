dojo.require("dojo.string");

(function($, d){

  $.vnc = {
  
    start: function(node, args){

      $.applet.inject(node, {
        archive: '/applet/vnc.jar',
        code:"com.tigervnc.vncviewer.VncApplet",
        port: args.port,
        host: args.host,
        window_title: "VNC Viewer",
        new_window: "Yes",
        debug_level: "debug"
      });
    }
  };   
 })(window, dojo);
