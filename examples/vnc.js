dojo.require("dojo.string");

(function($, d){

   function uid(){
			var uid = new Date().getTime().toString(32), i;
      for (i = 0; i < 5; i++) {
				uid += Math.floor(Math.random() * 65535).toString(32);
			}
      return uid;
    }
    
   function javatrigger(eventname){
     function f(){
       dojo.publish('vnc:' + eventname);
     }
     setTimeout(f, 0);
   }

   function start(node, args){
     var id = this.uid();

     dojo.publish('vnc:inject');

     $.applet.inject(node, {
        archive: args.archive || 'vnc.jar?v=' + new Date().getTime(),
        id: id,
        code:"com.tigervnc.VncLiveConnectApplet",
        port: args.port,
        host: args.host,
        title: args.title,
        show_controls: args.show_controls,
        new_window: "Yes",
        log_level: "error"
      });
    }

   dojo.subscribe('vnc:init', function(){
     
   });

   dojo.subscribe('vnc:destroy', function(){
     
   });

 })(window, dojo);
