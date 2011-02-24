dojo.require("dojo.string");

(function($, d){
   
  $.applet = {

    _paramify: function(key, value){
      return '  <param name="' + key + '" value="' + value + '" />';      
    },
  
    inject: function(node, args){
      node = d.byId(node);
      var params = [];
      for(var k in args){
         if (args.hasOwnProperty(k)) {
           params.push(this._paramify(k, args[k]));
         }
      }
      var width = args.width || "1";
      var heigth = args.heigth || "1";

      var t = [
        '<object type="application/x-java-applet" width="' + width + '" height="' + heigth + '">',
        '  <param name="mayscript" value="true" />',
        params.join('\n'),
        '</object>'].join('\n');
      d.place(t, node);
    }
  };
})(window, dojo);
