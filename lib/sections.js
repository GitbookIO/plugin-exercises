var _ = require('lodash');

// Split a page up into sections (lesson, exercises, ...)
function split(nodes) {
    var section = [];

    var sections = _.reduce(nodes, function(sections, el) {
        if(el.type === 'hr') {
            sections.push(section);
            section = [];
        } else {
            section.push(el);
        }

        return sections;
    }, [])
     // Add remaining nodes
    .concat([section])
    // Exclude empty sections
    .filter(_.negate(_.isEmpty));

    // Copy over links (kramed needs them)
    sections.links = nodes.links || {};

    return sections;
}

function join(sections) {
    var nodes = _.reduce(sections, function(nodes, section) {
        return nodes.concat(section);
    }, []);

    // Restore links (needed by kramed)
    nodes.links = sections.links || {};

    return nodes;
}

module.exports = {
    split: split,
    join: join,
};
