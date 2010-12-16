dojo.require("dojo.string");
var display = {

  start: function(args){
    args = dojo.mixin({port:"",host:"", controls:"no", windowTitle: null}, args);
    args.windowTitle = "Vnc Viewer " + args.host + ":" + args.port;
    var t = [
'<object type="application/x-java-applet" id="display" width="1" height="1">',
  '<param name="code" value="com.tigervnc.vncviewer.VncViewer">',
//  '<param name="archive" value="/applet/vnc.jar">',
  '<param name="archive" value="/applet/vnc.jar?v=' + new Date().getTime() + '">',
  '<param name="host" value="${host}" >',
  '<param name="port" value="${port}" >',
  '<param name="windowTitle" value="${windowTitle}" >',
  '<param name="show controls" value="${controls}" >',
  '<param name="Open New Window" value="yes" >',
'</object>'].join('');
    dojo.place(dojo.string.substitute(t, args), args.appendto);
  }
};