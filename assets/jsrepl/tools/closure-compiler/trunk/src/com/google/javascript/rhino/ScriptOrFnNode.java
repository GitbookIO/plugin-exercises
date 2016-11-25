/*
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Igor Bukanov
 *   Bob Jervis
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package com.google.javascript.rhino;

public class ScriptOrFnNode extends Node {

    private static final long serialVersionUID = 1L;

    public ScriptOrFnNode(int nodeType) {
        super(nodeType);
    }

    public ScriptOrFnNode(int nodeType, int lineno, int charno) {
        super(nodeType, lineno, charno);
    }

    public final String getSourceName() { return sourceName; }

    public final void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public final int getEncodedSourceStart() { return encodedSourceStart; }

    public final int getEncodedSourceEnd() { return encodedSourceEnd; }

    public final void setEncodedSourceBounds(int start, int end) {
        this.encodedSourceStart = start;
        this.encodedSourceEnd = end;
    }

    public final int getBaseLineno() { return baseLineno; }

    public final void setBaseLineno(int lineno) {
        // One time action
        if (lineno < 0 || baseLineno >= 0) Kit.codeBug();
        baseLineno = lineno;
    }

    public final int getEndLineno() { return endLineno; }

    public final void setEndLineno(int lineno) {
        // One time action
        if (lineno < 0 || endLineno >= 0) Kit.codeBug();
        endLineno = lineno;
    }

    public final int getFunctionCount() {
        if (functions == null) { return 0; }
        return functions.size();
    }

    public final FunctionNode getFunctionNode(int i) {
        return (FunctionNode)functions.get(i);
    }

    public final int addFunction(FunctionNode fnNode) {
        if (fnNode == null) Kit.codeBug();
        if (functions == null) { functions = new ObjArray(); }
        functions.add(fnNode);
        return functions.size() - 1;
    }

    public final int getRegexpCount() {
        if (regexps == null) { return 0; }
        return regexps.size() / 2;
    }

    public final String getRegexpString(int index) {
        return (String)regexps.get(index * 2);
    }

    public final String getRegexpFlags(int index) {
        return (String)regexps.get(index * 2 + 1);
    }

    public final int addRegexp(String string, String flags) {
        if (string == null) Kit.codeBug();
        if (regexps == null) { regexps = new ObjArray(); }
        regexps.add(string);
        regexps.add(flags);
        return regexps.size() / 2 - 1;
    }

    public final boolean hasParamOrVar(String name) {
        return itsVariableNames.has(name);
    }

    public final int getParamOrVarIndex(String name) {
        return itsVariableNames.get(name, -1);
    }

    public final String getParamOrVarName(int index) {
        return (String)itsVariables.get(index);
    }

    public final int getParamCount() {
        return varStart;
    }

    public final int getParamAndVarCount() {
        return itsVariables.size();
    }

    public final String[] getParamAndVarNames() {
        int N = itsVariables.size();
        if (N == 0) {
            return ScriptRuntime.emptyStrings;
        }
        String[] array = new String[N];
        itsVariables.toArray(array);
        return array;
    }

    public final boolean[] getParamAndVarConst() {
        int N = itsVariables.size();
        boolean[] array = new boolean[N];
        for (int i = 0; i < N; i++)
            if (itsConst.get(i) != null)
                array[i] = true;
        return array;
    }

    public final void addParam(String name) {
        // Check addparam is not called after addLocal
        if (varStart != itsVariables.size()) Kit.codeBug();
        // Allow non-unique parameter names: use the last occurrence (parser
        // will warn about dups)
        int index = varStart++;
        itsVariables.add(name);
        itsConst.add(null);
        itsVariableNames.put(name, index);
    }

    public static final int NO_DUPLICATE = 1;
    public static final int DUPLICATE_VAR = 0;
    public static final int DUPLICATE_PARAMETER = -1;
    public static final int DUPLICATE_CONST = -2;

    /**
     * This function adds a variable to the set of var declarations for a
     * function (or script).  This returns an indicator of a duplicate that
     * overrides a formal parameter (false if this dups a parameter).
     * @param name variable name
     * @return 1 if the name is not any form of duplicate, 0 if it duplicates a
     * non-parameter, -1 if it duplicates a parameter and -2 if it duplicates a
     * const.
     */
    public final int addVar(String name) {
        int vIndex = itsVariableNames.get(name, -1);
        if (vIndex != -1) {
            // There's already a variable or parameter with this name.
            if (vIndex >= varStart) {
                Object v = itsConst.get(vIndex);
                if (v != null)
                    return DUPLICATE_CONST;
                else
                    return DUPLICATE_VAR;
            } else
                return DUPLICATE_PARAMETER;
        }
        int index = itsVariables.size();
        itsVariables.add(name);
        itsConst.add(null);
        itsVariableNames.put(name, index);
        return NO_DUPLICATE;
    }

    public final boolean addConst(String name) {
        int vIndex = itsVariableNames.get(name, -1);
        if (vIndex != -1) {
            // There's already a variable or parameter with this name.
            return false;
        }
        int index = itsVariables.size();
        itsVariables.add(name);
        itsConst.add(name);
        itsVariableNames.put(name, index);
        return true;
    }

    public final void removeParamOrVar(String name) {
        int i = itsVariableNames.get(name, -1);
        if (i != -1) {
            itsVariables.remove(i);
            itsVariableNames.remove(name);
            ObjToIntMap.Iterator iter = itsVariableNames.newIterator();
            for (iter.start(); !iter.done(); iter.next()) {
                int v = iter.getValue();
                if (v > i) {
                    iter.setValue(v - 1);
                }
            }
        }
    }

    public final Object getCompilerData()
    {
        return compilerData;
    }

    public final void setCompilerData(Object data)
    {
        if (data == null) throw new IllegalArgumentException();
        // Can only call once
        if (compilerData != null) throw new IllegalStateException();
        compilerData = data;
    }

    private int encodedSourceStart;
    private int encodedSourceEnd;
    private String sourceName;
    private int baseLineno = -1;
    private int endLineno = -1;

    private ObjArray functions;

    private ObjArray regexps;

    // a list of the formal parameters and local variables
    private ObjArray itsVariables = new ObjArray();
    private ObjArray itsConst = new ObjArray();

    // mapping from name to index in list
    private ObjToIntMap itsVariableNames = new ObjToIntMap(11);

    private int varStart;               // index in list of first variable

    private Object compilerData;
}
