var _ = require("lodash");
var fs = require("fs");
var path = require("path");

var EXERCISE_TPL = _.template(fs.readFileSync(path.resolve(__dirname, "./assets/exercise.html")));

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
        ],
        html: {
            "body:end": function(options) {
                return '<script src="'+options.staticBase+'/plugins/gitbook-plugin-exercises/jsrepl/jsrepl.js" id="jsrepl-script"></script>';
            }
        }
    },
    blocks: {
        exercise: {
            blocks: ["initial", "solution", "validation", "context"],
            process: function(blk) {
                var codes = {};

                _.each(blk.blocks, function(_blk) {
                    codes[_blk.name] = _blk.body.trim();
                });

                return EXERCISE_TPL({
                    message: blk.body,
                    codes: codes
                });
            }
        }
    }
};