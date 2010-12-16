dojo.require("dojo.string");

var tunnel = {

  // name of this object
  _name: "tunnel",

  publish: function(/*String*/event, /*String*/message){
    switch(event){
      case "Init":
        this.onInit();
        break;
      case "Error":
        this.onError(message);
        break;
      default:
        this.onError(message);
        break;
    }
  },

  onInit: function(){},

  onError: function(message){},
  
  create: function(args){
    var t = [
'<object type="application/x-java-applet" id="tunnel" width="1" height="1" >',
  '<param name="mayscript" value="true">',
  '<param name="archive" value="/applet/evd.jar">',
  '<param name="code" value="Tunnel">',      
  '<param name="host" value="${host}" >',
  '<param name="remoteip" value="${remoteip}" >',
  '<param name="localport" value="${localport}" >',
  '<param name="remoteport" value="${remoteport}" >',
  '<param name="username" value="${username}" >',
  '<param name="callback" value="tunnel.publish" >',      
'</object>'].join('\n');
    args.name = this._name;
    dojo.place(dojo.string.substitute(t, args), args.appendto);
  }
};