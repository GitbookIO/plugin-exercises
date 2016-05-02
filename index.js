var _ = require("lodash");
var fs = require("fs");
var path = require("path");

var retro = require("./lib/retro");

var WEBSITE_TPL = _.template(fs.readFileSync(path.resolve(__dirname, "./assets/website.html")));
var EBOOK_TPL = _.template(fs.readFileSync(path.resolve(__dirname, "./assets/ebook.html")));

module.exports = {
    website: {
        assets: "./assets",
        js: [
            "ace/ace.js",
            "ace/theme-tomorrow.js",
            "ace/mode-javascript.js",
            "exercises.js"
        ],
        css: [
            "exercises.css"
        ]
    },
    ebook: {
        assets: "./assets",
        css: [
            "ebook.css"
        ]
    },
    blocks: {
        exercise: {
            parse: false,
            blocks: ["initial", "solution", "validation", "context"],
            process: function(blk) {
                var codes = {};

                _.each(blk.blocks, function(_blk) {
                    codes[_blk.name] = _blk.body.trim();
                });

                // Select appropriate template
                var tpl = (this.generator === 'website' ? WEBSITE_TPL : EBOOK_TPL);

                return tpl({
                    message: blk.body,
                    codes: codes
                });
            }
        }
    },
    hooks: {
        "page:before": function(page) {
            // Skip all non markdown pages
            if(page.type != "markdown") {
                return page;
            }

            // Rewrite content (modernizing old exercises)
            page.content = retro(page.content);

            // Return modified page
            return page;
        }
    }
};
