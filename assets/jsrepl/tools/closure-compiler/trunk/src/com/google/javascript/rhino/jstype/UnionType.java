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
 *   Bob Jervis
 *   Google Inc.
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

package com.google.javascript.rhino.jstype;

import static com.google.javascript.rhino.jstype.TernaryValue.UNKNOWN;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.javascript.rhino.ErrorReporter;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The {@code UnionType} implements a common JavaScript idiom in which the
 * code is specifically designed to work with multiple input types.  Because
 * JavaScript always knows the runtime type of an object value, this is safer
 * than a C union.<p>
 *
 * For instance, values of the union type {@code (String,boolean)} can be of
 * type {@code String} or of type {@code boolean}. The commutativity of the
 * statement is captured by making {@code (String,boolean)} and
 * {@code (boolean,String)} equal.<p>
 *
 * The implementation of this class prevents the creation of nested
 * unions.<p>
 */
public class UnionType extends JSType {
  private static final long serialVersionUID = 1L;

  Collection<JSType> alternates;
  private final int hashcode;

  /**
   * Creates a union type.
   *
   * @param alternates the alternates of the union
   */
  UnionType(JSTypeRegistry registry, Collection<JSType> alternates) {
    super(registry);
    this.alternates = alternates;
    this.hashcode = this.alternates.hashCode();
  }

  /**
   * Gets the alternate types of this union type.
   * @return The alternate types of this union type. The returned set is
   *     immutable.
   */
  public Iterable<JSType> getAlternates() {
    return alternates;
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * numeric context, such as an operand of a multiply operator.
   *
   * @return true if the type can appear in a numeric context.
   */
  @Override
  public boolean matchesNumberContext() {
    // TODO(user): Reverse this logic to make it correct instead of generous.
    for (JSType t : alternates) {
      if (t.matchesNumberContext()) {
        return true;
      }
    }
    return false;
  }

  /**
   * This predicate is used to test whether a given type can appear in a
   * {@code String} context, such as an operand of a string concat ({@code +})
   * operator.<p>
   *
   * All types have at least the potential for converting to {@code String}.
   * When we add externally defined types, such as a browser OM, we may choose
   * to add types that do not automatically convert to {@code String}.
   *
   * @return {@code true} if not {@link VoidType}
   */
  @Override
  public boolean matchesStringContext() {
    // TODO(user): Reverse this logic to make it correct instead of generous.
    for (JSType t : alternates) {
      if (t.matchesStringContext()) {
        return true;
      }
    }
    return false;
  }

  /**
   * This predicate is used to test whether a given type can appear in an
   * {@code Object} context, such as the expression in a {@code with}
   * statement.<p>
   *
   * Most types we will encounter, except notably {@code null}, have at least
   * the potential for converting to {@code Object}.  Host defined objects can
   * get peculiar.<p>
   *
   * VOID type is included here because while it is not part of the JavaScript
   * language, functions returning 'void' type can't be used as operands of
   * any operator or statement.<p>
   *
   * @return {@code true} if the type is not {@link NullType} or
   *         {@link VoidType}
   */
  @Override
  public boolean matchesObjectContext() {
    // TODO(user): Reverse this logic to make it correct instead of generous.
    for (JSType t : alternates) {
      if (t.matchesObjectContext()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public JSType findPropertyType(String propertyName) {
    JSType propertyType = null;

    for (JSType alternate : getAlternates()) {
      // Filter out the null/undefined type.
      if (alternate.isNullType() || alternate.isVoidType()) {
        continue;
      }

      JSType altPropertyType = alternate.findPropertyType(propertyName);
      if (altPropertyType == null) {
        continue;
      }

      if (propertyType == null) {
        propertyType = altPropertyType;
      } else {
        propertyType = propertyType.getLeastSupertype(altPropertyType);
      }
    }

    return propertyType;
  }

  @Override
  public boolean canAssignTo(JSType that) {
    boolean canAssign = true;
    for (JSType t : alternates) {
      if (t.isUnknownType()) {
        return true;
      }
      canAssign &= t.canAssignTo(that);
    }
    return canAssign;
  }

  @Override
  public boolean canBeCalled() {
    for (JSType t : alternates) {
      if (!t.canBeCalled()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public JSType autobox() {
    UnionTypeBuilder restricted = new UnionTypeBuilder(registry);
    for (JSType t : alternates) {
      restricted.addAlternate(t.autobox());
    }
    return restricted.build();
  }

  @Override
  public JSType restrictByNotNullOrUndefined() {
    UnionTypeBuilder restricted = new UnionTypeBuilder(registry);
    for (JSType t : alternates) {
      restricted.addAlternate(t.restrictByNotNullOrUndefined());
    }
    return restricted.build();
  }

  @Override
  public TernaryValue testForEquality(JSType that) {
    TernaryValue result = null;
    for (JSType t : alternates) {
      TernaryValue test = t.testForEquality(that);
      if (result == null) {
        result = test;
      } else if (!result.equals(test)) {
        return UNKNOWN;
      }
    }
    return result;
  }

  /**
   * This predicate determines whether objects of this type can have the
   * {@code null} value, and therefore can appear in contexts where
   * {@code null} is expected.
   *
   * @return {@code true} for everything but {@code Number} and
   *         {@code Boolean} types.
   */
  @Override
  public boolean isNullable() {
    for (JSType t : alternates) {
      if (t.isNullable()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isUnknownType() {
    for (JSType t : alternates) {
      if (t.isUnknownType()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public JSType getLeastSupertype(JSType that) {
    if (!that.isUnknownType() && !that.isUnionType()) {
      for (JSType alternate : alternates) {
        if (!alternate.isUnknownType() && that.isSubtype(alternate)) {
          return this;
        }
      }
    }

    return getLeastSupertype(this, that);
  }

  JSType meet(JSType that) {
    UnionTypeBuilder builder = new UnionTypeBuilder(registry);
    for (JSType alternate : alternates) {
      if (alternate.isSubtype(that)) {
        builder.addAlternate(alternate);
      }
    }

    if (that.isUnionType()) {
      for (JSType otherAlternate : that.toMaybeUnionType().alternates) {
        if (otherAlternate.isSubtype(this)) {
          builder.addAlternate(otherAlternate);
        }
      }
    } else if (that.isSubtype(this)) {
      builder.addAlternate(that);
    }
    JSType result = builder.build();
    if (!result.isNoType()) {
      return result;
    } else if (this.isObject() && that.isObject()) {
      return getNativeType(JSTypeNative.NO_OBJECT_TYPE);
    } else {
      return getNativeType(JSTypeNative.NO_TYPE);
    }
  }

  /**
   * Two union types are equal if they have the same number of alternates
   * and all alternates are equal.
   */
  @Override
  public boolean isEquivalentTo(JSType object) {
    if (object == null) {
      return false;
    }
    if (object.isUnionType()) {
      UnionType that = object.toMaybeUnionType();
      if (alternates.size() != that.alternates.size()) {
        return false;
      }
      for (JSType alternate : that.alternates) {
        if (!hasAlternate(alternate)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  private boolean hasAlternate(JSType type) {
    for (JSType alternate : alternates) {
      if (alternate.isEquivalentTo(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.hashcode;
  }

  @Override
  public UnionType toMaybeUnionType() {
    return this;
  }

  @Override
  public boolean isObject() {
    for (JSType alternate : alternates) {
      if (!alternate.isObject()) {
        return false;
      }
    }
    return true;
  }

  /**
   * A {@link UnionType} contains a given type (alternate) iff the member
   * vector contains it.
   *
   * @param type The alternate which might be in this union.
   *
   * @return {@code true} if the alternate is in the union
   */
  public boolean contains(JSType type) {
    for (JSType alt : alternates) {
      if (alt.isEquivalentTo(type)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a more restricted union type than {@code this} one, in which all
   * subtypes of {@code type} have been removed.<p>
   *
   * Examples:
   * <ul>
   * <li>{@code (number,string)} restricted by {@code number} is
   *     {@code string}</li>
   * <li>{@code (null, EvalError, URIError)} restricted by
   *     {@code Error} is {@code null}</li>
   * </ul>
   *
   * @param type the supertype of the types to remove from this union type
   */
  public JSType getRestrictedUnion(JSType type) {
    UnionTypeBuilder restricted = new UnionTypeBuilder(registry);
    for (JSType t : alternates) {
      if (t.isUnknownType() || !t.isSubtype(type)) {
        restricted.addAlternate(t);
      }
    }
    return restricted.build();
  }

  @Override public String toString() {
    StringBuilder result = new StringBuilder();
    boolean firstAlternate = true;

    result.append("(");
    SortedSet<JSType> sorted = new TreeSet<JSType>(ALPHA);
    sorted.addAll(alternates);
    for (JSType t : sorted) {
      if (!firstAlternate) {
        result.append("|");
      }
      result.append(t.toString());
      firstAlternate = false;
    }
    result.append(")");
    return result.toString();
  }

  @Override
  public boolean isSubtype(JSType that) {
    // unknown
    if (that.isUnknownType()) {
      return true;
    }
    // all type
    if (that.isAllType()) {
      return true;
    }
    for (JSType element : alternates) {
      if (!element.isSubtype(that)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public JSType getRestrictedTypeGivenToBooleanOutcome(boolean outcome) {
    // gather elements after restriction
    UnionTypeBuilder restricted = new UnionTypeBuilder(registry);
    for (JSType element : alternates) {
      restricted.addAlternate(
          element.getRestrictedTypeGivenToBooleanOutcome(outcome));
    }
    return restricted.build();
  }

  @Override
  public BooleanLiteralSet getPossibleToBooleanOutcomes() {
    BooleanLiteralSet literals = BooleanLiteralSet.EMPTY;
    for (JSType element : alternates) {
      literals = literals.union(element.getPossibleToBooleanOutcomes());
      if (literals == BooleanLiteralSet.BOTH) {
        break;
      }
    }
    return literals;
  }

  @Override
  public TypePair getTypesUnderEquality(JSType that) {
    UnionTypeBuilder thisRestricted = new UnionTypeBuilder(registry);
    UnionTypeBuilder thatRestricted = new UnionTypeBuilder(registry);
    for (JSType element : alternates) {
      TypePair p = element.getTypesUnderEquality(that);
      if (p.typeA != null) {
        thisRestricted.addAlternate(p.typeA);
      }
      if (p.typeB != null) {
        thatRestricted.addAlternate(p.typeB);
      }
    }
    return new TypePair(
        thisRestricted.build(),
        thatRestricted.build());
  }

  @Override
  public TypePair getTypesUnderInequality(JSType that) {
    UnionTypeBuilder thisRestricted = new UnionTypeBuilder(registry);
    UnionTypeBuilder thatRestricted = new UnionTypeBuilder(registry);
    for (JSType element : alternates) {
      TypePair p = element.getTypesUnderInequality(that);
      if (p.typeA != null) {
        thisRestricted.addAlternate(p.typeA);
      }
      if (p.typeB != null) {
        thatRestricted.addAlternate(p.typeB);
      }
    }
    return new TypePair(
        thisRestricted.build(),
        thatRestricted.build());
  }

  @Override
  public TypePair getTypesUnderShallowInequality(JSType that) {
    UnionTypeBuilder thisRestricted = new UnionTypeBuilder(registry);
    UnionTypeBuilder thatRestricted = new UnionTypeBuilder(registry);
    for (JSType element : alternates) {
      TypePair p = element.getTypesUnderShallowInequality(that);
      if (p.typeA != null) {
        thisRestricted.addAlternate(p.typeA);
      }
      if (p.typeB != null) {
        thatRestricted.addAlternate(p.typeB);
      }
    }
    return new TypePair(
        thisRestricted.build(),
        thatRestricted.build());
  }

  @Override
  public <T> T visit(Visitor<T> visitor) {
    return visitor.caseUnionType(this);
  }

  @Override
  JSType resolveInternal(ErrorReporter t, StaticScope<JSType> scope) {
    setResolvedTypeInternal(this); // for circularly defined types.

    boolean changed = false;
    ImmutableList.Builder<JSType> resolvedTypes = ImmutableList.builder();
    for (JSType alternate : alternates) {
      JSType newAlternate = alternate.resolve(t, scope);
      changed |= (alternate != newAlternate);
      resolvedTypes.add(alternate);
    }
    if (changed) {
      Collection<JSType> newAlternates = resolvedTypes.build();
      Preconditions.checkState(
          newAlternates.hashCode() == this.hashcode);
      alternates = newAlternates;
    }
    return this;
  }

  @Override
  public String toDebugHashCodeString() {
    List<String> hashCodes = Lists.newArrayList();
    for (JSType a : alternates) {
      hashCodes.add(a.toDebugHashCodeString());
    }
    return "{(" + Joiner.on(",").join(hashCodes) + ")}";
  }

  @Override
  public boolean setValidator(Predicate<JSType> validator) {
    for (JSType a : alternates) {
      a.setValidator(validator);
    }
    return true;
  }
}
