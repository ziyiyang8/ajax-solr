(function ($) {

AjaxSolr.ResultWidget = AjaxSolr.AbstractWidget.extend({

	afterRequest: function () {
		  $(this.target).empty();
		  for (var i = 0, l = this.manager.response.response.docs.length; i < l; i++) {
		    var doc = this.manager.response.response.docs[i];
		    $(this.target).append(this.template(doc));
		  }
		},

		template: function (doc) {	
		  // limit snippet to first 150 characters
		  var snippet;
		  if (doc.description === undefined)
			  snippet = "";
		  else
			  snippet = doc.description.toString().substring(0,150);
			
		  var output = '<div><h2><a href=' + doc.url + ' target="_blank"' + '\>' + doc.title + '</a></h2>';
		  //output += '<p id="links_' + doc.id + '" class="links"></p>';
		  output += '<a href=' + doc.url + ' target="_blank"' + '\>' + doc.url + '</a>';
		  output += '<p>' + snippet + '</p></div>';
		  return output;
		}
	
});

})(jQuery);