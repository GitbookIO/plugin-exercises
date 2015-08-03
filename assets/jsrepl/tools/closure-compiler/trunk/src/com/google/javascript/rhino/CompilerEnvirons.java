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
 *   Igor Bukanov, igor@fastmail.fm
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

import java.util.Hashtable;

public class CompilerEnvirons
{
    public CompilerEnvirons()
    {
        languageVersion = Context.VERSION_DEFAULT;
        generateDebugInfo = true;
        useDynamicScope = false;
        reservedKeywordAsIdentifier = false;
        allowMemberExprAsFunctionName = false;
        xmlAvailable = true;
        optimizationLevel = 0;
        generatingSource = true;
        strictMode = false;
        warningAsError = false;
        parseJSDocDocumentation = false;
    }

    public void initFromContext(Context cx)
    {
        setErrorReporter(cx.getErrorReporter());
        this.languageVersion = cx.getLanguageVersion();
        useDynamicScope = cx.compileFunctionsWithDynamicScopeFlag;
        generateDebugInfo = (!cx.isGeneratingDebugChanged()
                             || cx.isGeneratingDebug());
        reservedKeywordAsIdentifier
            = cx.hasFeature(Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER);
        allowMemberExprAsFunctionName
            = cx.hasFeature(Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME);
        strictMode
            = cx.hasFeature(Context.FEATURE_STRICT_MODE);
        warningAsError = cx.hasFeature(Context.FEATURE_WARNING_AS_ERROR);
        xmlAvailable
            = cx.hasFeature(Context.FEATURE_E4X);

        optimizationLevel = cx.getOptimizationLevel();

        generatingSource = cx.isGeneratingSource();
        activationNames = cx.activationNames;
    }

    public final ErrorReporter getErrorReporter()
    {
        return errorReporter;
    }

    public void setErrorReporter(ErrorReporter errorReporter)
    {
        if (errorReporter == null) throw new IllegalArgumentException();
        this.errorReporter = errorReporter;
    }

    public final int getLanguageVersion()
    {
        return languageVersion;
    }

    public void setLanguageVersion(int languageVersion)
    {
        Context.checkLanguageVersion(languageVersion);
        this.languageVersion = languageVersion;
    }

    public final boolean isGenerateDebugInfo()
    {
        return generateDebugInfo;
    }

    public void setGenerateDebugInfo(boolean flag)
    {
        this.generateDebugInfo = flag;
    }

    public final boolean isUseDynamicScope()
    {
        return useDynamicScope;
    }

    public final boolean isReservedKeywordAsIdentifier()
    {
        return reservedKeywordAsIdentifier;
    }

    public void setReservedKeywordAsIdentifier(boolean flag)
    {
        reservedKeywordAsIdentifier = flag;
    }

    public final boolean isAllowMemberExprAsFunctionName()
    {
        return allowMemberExprAsFunctionName;
    }

    public void setAllowMemberExprAsFunctionName(boolean flag)
    {
        allowMemberExprAsFunctionName = flag;
    }

    public final boolean isXmlAvailable()
    {
        return xmlAvailable;
    }

    public void setXmlAvailable(boolean flag)
    {
        xmlAvailable = flag;
    }

    public final int getOptimizationLevel()
    {
        return optimizationLevel;
    }

    public void setOptimizationLevel(int level)
    {
        Context.checkOptimizationLevel(level);
        this.optimizationLevel = level;
    }

    public boolean getAnnotateTypes()
    {
        return annotateTypes;
    }

    public void setAnnotateTypes(boolean flag)
    {
        annotateTypes = flag;
    }

    public final boolean getParseJSDocDocumentation() {
        return parseJSDocDocumentation;
    }

    public void setParseJSDocDocumentation(boolean flag) {
        parseJSDocDocumentation = flag;
    }

    public boolean getParseJSDoc()
    {
        return parseJSDoc;
    }

    public void setParseJSDoc(boolean flag)
    {
        parseJSDoc = flag;
    }

    public final boolean isGeneratingSource()
    {
        return generatingSource;
    }

    public final boolean isStrictMode()
    {
        return strictMode;
    }

    public final boolean reportWarningAsError()
    {
        return warningAsError;
    }
    /**
     * Specify whether or not source information should be generated.
     * <p>
     * Without source information, evaluating the "toString" method
     * on JavaScript functions produces only "[native code]" for
     * the body of the function.
     * Note that code generated without source is not fully ECMA
     * conformant.
     */
    public void setGeneratingSource(boolean generatingSource)
    {
        this.generatingSource = generatingSource;
    }

    private ErrorReporter errorReporter;

    private int languageVersion;
    private boolean generateDebugInfo;
    private boolean useDynamicScope;
    private boolean reservedKeywordAsIdentifier;
    private boolean allowMemberExprAsFunctionName;
    private boolean xmlAvailable;
    private int optimizationLevel;
    private boolean generatingSource;
    private boolean strictMode;
    private boolean warningAsError;
    private boolean annotateTypes;
    private boolean parseJSDoc;
    private boolean parseJSDocDocumentation;
    Hashtable<Object, Object> activationNames;
}
