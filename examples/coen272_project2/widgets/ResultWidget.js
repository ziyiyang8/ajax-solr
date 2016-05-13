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
		  var output = '<div><h2><a href=' + doc.url + '\>' + doc.title + '</a></h2>';
		  //output += '<p id="links_' + doc.id + '" class="links"></p>';
		  output += '<a href=' + doc.url + '\>' + doc.url + '</a>';
		  output += '<p>' + doc.description + '</p></div>';
		  return output;
		}
	
});

})(jQuery);