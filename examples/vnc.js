dojo.require("dojo.string");

(function($, d){

   function uid(){
			var uid = new Date().getTime().toString(32), i;
      for (i = 0; i < 5; i++) {
				uid += Math.floor(Math.random() * 65535).toString(32);
			}
      return uid;
    }
    
   function javatrigger(eventname, id, msg){
     console.log('javatrigger', eventname, msg);
     function f(){
       dojo.publish('vnc:' + eventname, [{id:id,msg:msg}]);
     }
     setTimeout(f, 0);
   }

   function start(node, args){
     var id = uid();

     dojo.publish('vnc:inject');

     $.applet.inject(node, {
        archive: (args.archive || 'vnc.jar') + '?v=' + new Date().getTime(),
        id: id,
        code:"com.tigervnc.VncLiveConnectApplet",
        port: args.port,
        host: args.host,
        title: args.title,
        new_window: "Yes",
        log_level: "error",
        callback: 'vnc.javatrigger'
      });
     return id;
   }

   dojo.subscribe('vnc:init', function(){});
   dojo.subscribe('vnc:destroy', function(){});

   $.vnc = {
     start:start,
     javatrigger: javatrigger,

     // EVENTS
     CONNECTION_ERROR: 'vnc:connection_error',
     INIT: 'vnc:init',
     DESTROY: 'vnc:destroy'
   };

 })(window, dojo);
