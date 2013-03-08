dojo.provide('vnc.Viewer');

dojo.require('java.applet');

dojo.declare('vnc.Viewer', null, {
            
    started: false,

    title: null,
    host: null,
    port: null,
    node: null,
    archive: dojo.moduleUrl("vnc","resources/vnc.jar"),

    constructor: function(args){
        dojo.safeMixin(this, args);
        
        this.id = 'vnc-viewer-' + vnc.Viewer.id;
        vnc.Viewer.id = vnc.Viewer.id + 1;
        if(typeof vnc.Viewer.viewers[this.id] !== 'undefined'){
            console.log('vnc.Viewer already present');
            dojo.destroy(this.id);
        }
        vnc.Viewer.viewers[this.id] = this;
    },

    start: function(){
        if(this.started){
            console.log('error already started');
            return;
        }

        var args = {
            port: this.port,
            host: this.host,
            title: this.title,
            id: this.id,
            log_level:'info'
        };
    
        args.archive = this.archive;
        args.code = 'com.tigervnc.VncApplet';
        args.new_window = "yes";
        args.show_controls = 'no';
        args.callback = 'vnc.Viewer.javatrigger';

        // NOTE: 
        // Static variables are shared accross applets
        // tigervnc uses static variables so it fucks up.
        // Disabling the classloader cache
        args.classloader_cache = 'false';
        applet.inject(this.node, args);
    },

    onDestroy: function(){
        var self = this;
        function doIt(){
            dojo.destroy(this.id);
        }
        setTimeout(doIt, 0);
    }
});

vnc.Viewer.viewers = {};
vnc.Viewer.INIT = 'display:init';
vnc.Viewer.DESTROY = 'display:destroy';

vnc.Viewer.id = 0;
vnc.Viewer.javatrigger = function(evtName, id, msg){
    console.log('vnc::Viewer::javatrigger', arguments);
    var self = this;
    var display = self.viewers[id];

    function f(){
        switch('display:' + evtName){
        case self.INIT:
            break;
        case self.DESTROY:
            display && display.onDestroy();
            break;
        default:
            console.log('WTF? event:' + evtName + " shouldn't happen");
        }
        // dojo.publish('vnc:' + eventname, [{id:id,msg:msg}]);
    }
    setTimeout(f, 0);
};
