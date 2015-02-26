/* global casper:false */

var lib = require('../lib');

lib.initMessages();
lib.changeWorkingDirectory('coding-rules-page-should-show-custom-rules');
lib.configureCasper();


casper.test.begin('coding-rules-page-should-show-custom-rules', 3, function (test) {
  casper
      .start(lib.buildUrl('coding-rules'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/rules/app', 'app.json');
        lib.mockRequestFromFile('/api/rules/search', 'search-custom-rules.json',
            { data: { template_key: 'squid:ArchitecturalConstraint' } });
        lib.mockRequestFromFile('/api/rules/search', 'search.json');
        lib.mockRequestFromFile('/api/rules/show', 'show.json');
        lib.mockRequest('/api/issues/search', '{}');
      })

      .then(function () {
        casper.waitForSelector('.coding-rule.selected', function () {
          casper.click('.coding-rule.selected .js-rule');
        });
      })

      .then(function () {
        casper.waitForSelector('#coding-rules-detail-custom-rules .coding-rules-detail-list-name');
      })

      .then(function () {
        test.assertExists('#coding-rules-detail-custom-rules');
        test.assertElementCount('#coding-rules-detail-custom-rules .coding-rules-detail-list-name', 2);
        test.assertSelectorContains('#coding-rules-detail-custom-rules .coding-rules-detail-list-name',
            'Do not use org.h2.util.StringUtils');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
