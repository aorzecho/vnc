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
         node = dojo.byId(node);
         console.log('vnc::start', node, args);
         var id = uid();
         dojo.publish(vnc.INJECT);

         $.applet.inject(node, {
                archive: (args.archive || 'vnc.jar') + '?v=' + new Date().getTime(),
                id: id,
                code:'com.tigervnc.VncApplet',
                port: args.port,
                host: args.host,
                title: args.title,
                show_controls: args.show_controls,             
                new_window: 'Yes',
                log_level: 'debug',
                callback: 'vnc.javatrigger'
            });

         return id;
     }

     // You can subscribe to events like this:
     // dojo.subscribe(vnc.INIT, function(){});
     // dojo.subscribe(vnc.DESTROY, function(){});
     // dojo.subscribe(vnc.INJECT, function(){});

     $.vnc = {
         start:start,
         javatrigger: javatrigger,

         // EVENTS
         CONNECTION_ERROR: 'vnc:connection_error',
         INIT: 'vnc:init',
         DESTROY: 'vnc:destroy',
         INJECT: 'vnc:inject'
     };

    dojo.subscribe(vnc.DESTROY, function(obj){
        console.log('vnc:destroy');
        dojo.destroy(obj.id);
    });

 })(window, dojo);
