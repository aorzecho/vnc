dojo.require("dojo.string");

(function($, d){
   
  $.applet = {

    _paramify: function(key, value){
      return '  <param name="' + key + '" value="' + value + '" />';      
    },
  
    inject: function(node, args){
      node = d.byId(node);
      var id = args.id,
          width = args.width || "1",
          heigth = args.heigth || "1",
          params = [];

      for(var k in args){
        params.push(this._paramify(k, args[k]));
      }

      var t = [
        '<object id="' + id + '" type="application/x-java-applet" width="' + width + '" height="' + heigth + '">',
        '  <param name="mayscript" value="true" />',
        params.join('\n'),
        '</object>'].join('\n');
      d.place(t, node);
    }
  };
})(window, dojo);
