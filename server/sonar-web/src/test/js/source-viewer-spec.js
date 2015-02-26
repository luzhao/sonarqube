/* global casper:false */


var lib = require('../lib'),
    testName = lib.testName('Source Viewer');

lib.initMessages();
lib.changeWorkingDirectory('source-viewer-spec');
lib.configureCasper();


casper.test.begin(testName('Base'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line', function () {
          // Check header elements
          test.assertExists('.source-viewer-header');
          test.assertSelectorContains('.source-viewer-header', 'SonarQube');
          test.assertSelectorContains('.source-viewer-header', 'SonarQube :: Batch');
          test.assertSelectorContains('.source-viewer-header', 'src/main/java/org/sonar/batch/index/Cache.java');
          test.assertExists('.source-viewer-header .js-favorite');
          test.assertExists('.source-viewer-header-actions');

          // Check main measures
          // FIXME enable lines check
          //test.assertSelectorContains('.source-viewer-header-measure', '379');
          test.assertSelectorContains('.source-viewer-header-measure', 'A');
          test.assertSelectorContains('.source-viewer-header-measure', '2h 10min');
          test.assertSelectorContains('.source-viewer-header-measure', '6');
          test.assertSelectorContains('.source-viewer-header-measure', '74.3%');
          test.assertSelectorContains('.source-viewer-header-measure', '5.8%');

          // Check source
          // FIXME enable source lines count check
          //test.assertElementCount('.source-line', 518);
          test.assertSelectorContains('.source-viewer', 'public class Cache');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Decoration'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        // Check issues decoration
        test.assertElementCount('.has-issues', 6);
      })

      .then(function () {
        // Check coverage decoration
        test.assertElementCount('.source-line-covered', 142);
        test.assertElementCount('.source-line-uncovered', 50);
        test.assertElementCount('.source-line-partially-covered', 2);
      })

      .then(function () {
        // Check duplications decoration
        test.assertElementCount('.source-line-duplicated', 30);
      })

      .then(function () {
        // Check scm decoration
        test.assertElementCount('.source-line-scm-inner', 186);
        test.assertExists('.source-line-scm-inner[data-author="simon.brandhof@gmail.com"]');
        test.assertExists('.source-line-scm-inner[data-author="julien.henry@sonarsource.com"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Test File'), function (test) {
  casper
      .start(lib.buildUrl('source-viewer'), function () {
        lib.setDefaultViewport();

        lib.mockRequest('/api/l10n/index', '{}');
        lib.mockRequestFromFile('/api/components/app', 'tests/app.json');
        lib.mockRequestFromFile('/api/sources/lines', 'tests/lines.json');
        lib.mockRequestFromFile('/api/issues/search', 'issues.json');
      })

      .then(function () {
        casper.waitForSelector('.source-line');
      })

      .then(function () {
        test.assertSelectorContains('.source-viewer-header-measure', '6');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
