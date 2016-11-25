(function() {
  self.JSREPLEngine = (function() {
    function JSREPLEngine(input, output, result1, error, sandbox, ready) {
      this.result = result1;
      this.error = error;
      this.sandbox = sandbox;
      this.inspect = this.sandbox.console.inspect;
      this.functionClass = this.sandbox.Function;
      this.sandbox.__eval = this.sandbox["eval"];
      ready();
    }

    JSREPLEngine.prototype.Eval = function(command) {
      var e, result;
      try {
        result = this.sandbox.__eval(command);
        return this.result(result === void 0 ? '' : this.inspect(result));
      } catch (_error) {
        e = _error;
        return this.error(e);
      }
    };

    JSREPLEngine.prototype.RawEval = function(command) {
      var e, result;
      try {
        result = this.sandbox.__eval(command);
        return this.result(result);
      } catch (_error) {
        e = _error;
        return this.error(e);
      }
    };

    JSREPLEngine.prototype.GetNextLineIndent = function(command) {
      var e;
      try {
        new this.functionClass(command);
        return false;
      } catch (_error) {
        e = _error;
        if (/[\[\{\(]$/.test(command)) {
          return 1;
        } else {
          return 0;
        }
      }
    };

    return JSREPLEngine;

  })();

}).call(this);
