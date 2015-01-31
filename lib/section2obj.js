var _ = require('lodash');

var langs = require('./langs');

function section2obj(section) {
    var nonCodeNodes = _.reject(section, {
        'type': 'code'
    });

    var codeNodes = _.filter(section, {
        'type': 'code'
    });

    // Languages in code blocks
    var _langs = _.pluck(codeNodes, 'lang').map(langs.normalize);

    // Check that they are all the same
    var validLangs = _.all(_.map(_langs, function(lang) {
        return lang && lang === langs[0];
    }));

    // Main language
    var lang = validLangs ? _langs[0] : null;

    return {
        content: nonCodeNodes,
        lang: lang,
        code: {
            initial: codeNodes[0].text,
            solution: codeNodes[1].text,
            validation: codeNodes[2].text,
            // Context is optional
            context: codeNodes[3] ? codeNodes[3].text : null,
        }
    };
}

module.exports = section2obj;
