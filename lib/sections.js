var _ = require('lodash');

// Split a page up into sections (lesson, exercises, ...)
function split(nodes) {
    var section = [];

    return _(nodes)
    .reduce(, function(sections, el) {
        if(el.type === 'hr') {
            sections.push(section);
            section = [];
            section.links = nodes.links || {};
        } else {
            section.push(el);
        }

        return sections;
    }, [])
     // Add remaining nodes
    .concat([section])
    // Exclude empty sections
    .filter(_.negate(_.isEmpty))
    // Spit out JS array
    .value();
}

function join(sections) {
    var nodes = _.reduce(sections, function(nodes, section) {
        return nodes.concat(section);
    }, []);
    nodes.links = (sections[0] && (sections[0].links) || {};
    return nodes;
}

module.exports = {
    split: split,
    join: split,
};
