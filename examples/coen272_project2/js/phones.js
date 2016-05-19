var Manager;

(function ($) {
	
  $(function () {
    Manager = new AjaxSolr.Manager({
      solrUrl: 'http://localhost:8983/solr/myexample/'
    });
    Manager.addWidget(new AjaxSolr.ResultWidget({
        id: 'result',
        target: '#docs'
      }));
    Manager.addWidget(new AjaxSolr.PagerWidget({
    	  id: 'pager',
    	  target: '#pager',
    	  prevLabel: '&lt;',
    	  nextLabel: '&gt;',
    	  innerWindow: 1,
    	  renderHeader: function (perPage, offset, total) {
    	    $('#pager-header').html($('<span></span>').text('displaying ' + Math.min(total, offset + 1) + ' to ' + Math.min(total, offset + perPage) + ' of ' + total));
    	  }
    	}));
    Manager.addWidget(new AjaxSolr.CurrentSearchWidget({
    	  id: 'currentsearch',
    	  target: '#selection',
    	}));
    Manager.addWidget(new AjaxSolr.TextWidget({
    	  id: 'text',
    	  target: '#search'
    	}));
    // remember values when going back on page
    Manager.setStore(new AjaxSolr.ParameterHashStore());
    Manager.store.exposed = [ 'fq', 'q', 'start' ];
    Manager.init();
    Manager.store.addByValue('q', '');
    Manager.doRequest();

    var fields = [ 'brands', 'cameras' ];
    for (var i = 0, l = fields.length; i < l; i++) {
        Manager.addWidget(new AjaxSolr.MultiSelectWidget({ //MultiSelectWidget instead of Tagcloudwidget
            id: fields[i],
            target: '#' + fields[i],
    	field: fields[i],
    	max_show: 10,
    	max_facets: 20,
    	sort_type: 'count' //possible values: 'range', 'lex', 'count'
          }));
        }
    
    var params = {
    		  facet: true,
    		  'facet.field': [ 'brands', 'cameras' ],
    		  'facet.limit': 20,
    		  'facet.mincount': 1,
    		  'f.topics.facet.limit': 50,
    		  'json.nl': 'map'
    };
    for (var name in params) {
    	Manager.store.addByValue(name, params[name]);
    }
	
   
  });
  
})(jQuery);