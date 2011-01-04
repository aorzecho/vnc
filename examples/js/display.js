dojo.require("dojo.string");
var display = {

  start: function(args){
    args = dojo.mixin({localport:"",display_type:"", screen_size:"",lang:"",window_title:""}, args);
    var t = [
'<object type="application/x-java-applet" id="display" width="1" height="1">',
  '<param name="mayscript" value="true">',
  '<param name="archive" value="/applet/vnc.jar?v=' + new Date().getTime() + '">',
  '<param name="code" value="com.tigervnc.vncviewer.VncApplet" >',
  '<param name="port" value="${port}" >',
  '<param name="host" value="${host}" >',
  '<param name="window_title" value="VNC Viewer" >',
  '<param name="show_controls" value="no" >',
  '<param name="new_window" value="yes" >',
'</object>'].join('');
    dojo.place(dojo.string.substitute(t, args), args.appendto);
  },

  destroy: function(){
    dojo.destroy('display');
  }
};