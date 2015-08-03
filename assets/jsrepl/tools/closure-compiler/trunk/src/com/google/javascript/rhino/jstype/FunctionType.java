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

import static com.google.javascript.rhino.jstype.JSTypeNative.OBJECT_TYPE;
import static com.google.javascript.rhino.jstype.JSTypeNative.U2U_CONSTRUCTOR_TYPE;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.rhino.ErrorReporter;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This derived type provides extended information about a function, including
 * its return type and argument types.<p>
 *
 * Note: the parameters list is the LP node that is the parent of the
 * actual NAME node containing the parsed argument list (annotated with
 * JSDOC_TYPE_PROP's for the compile-time type of each argument.
 */
public class FunctionType extends PrototypeObjectType {
  private static final long serialVersionUID = 1L;

  private enum Kind {
    ORDINARY,
    CONSTRUCTOR,
    INTERFACE
  }

  /**
   * {@code [[Call]]} property.
   */
  private ArrowType call;

  /**
   * The {@code prototype} property. This field is lazily initialized by
   * {@code #getPrototype()}. The most important reason for lazily
   * initializing this field is that there are cycles in the native types
   * graph, so some prototypes must temporarily be {@code null} during
   * the construction of the graph.
   *
   * If non-null, the type must be a PrototypeObjectType.
   */
  private Property prototypeSlot;

  /**
   * Whether a function is a constructor, an interface, or just an ordinary
   * function.
   */
  private final Kind kind;

  /**
   * The type of {@code this} in the scope of this function.
   */
  private ObjectType typeOfThis;

  /**
   * The function node which this type represents. It may be {@code null}.
   */
  private Node source;

  /**
   * The interfaces directly implemented by this function (for constructors)
   * It is only relevant for constructors. May not be {@code null}.
   */
  private List<ObjectType> implementedInterfaces = ImmutableList.of();

  /**
   * The interfaces directly extendeded by this function (for interfaces)
   * It is only relevant for constructors. May not be {@code null}.
   */
  private List<ObjectType> extendedInterfaces = ImmutableList.of();

  /**
   * The types which are subtypes of this function. It is only relevant for
   * constructors and may be {@code null}.
   */
  private List<FunctionType> subTypes;

  /**
   * The template type name. May be {@code null}.
   */
  private String templateTypeName;

  /** Creates an instance for a function that might be a constructor. */
  FunctionType(JSTypeRegistry registry, String name, Node source,
      ArrowType arrowType, ObjectType typeOfThis,
      String templateTypeName,  boolean isConstructor, boolean nativeType) {
    super(registry, name,
        registry.getNativeObjectType(JSTypeNative.FUNCTION_INSTANCE_TYPE),
        nativeType);
    Preconditions.checkArgument(source == null ||
        Token.FUNCTION == source.getType());
    Preconditions.checkNotNull(arrowType);
    this.source = source;
    this.kind = isConstructor ? Kind.CONSTRUCTOR : Kind.ORDINARY;
    if (isConstructor) {
      this.typeOfThis = typeOfThis != null ?
          typeOfThis : new InstanceObjectType(registry, this, nativeType);
    } else {
      this.typeOfThis = typeOfThis != null ?
          typeOfThis :
          registry.getNativeObjectType(JSTypeNative.UNKNOWN_TYPE);
    }
    this.call = arrowType;
    this.templateTypeName = templateTypeName;
  }

  /** Creates an instance for a function that is an interface. */
  private FunctionType(JSTypeRegistry registry, String name, Node source) {
    super(registry, name,
        registry.getNativeObjectType(JSTypeNative.FUNCTION_INSTANCE_TYPE));
    Preconditions.checkArgument(source == null ||
        Token.FUNCTION == source.getType());
    Preconditions.checkArgument(name != null);
    this.source = source;
    this.call = new ArrowType(registry, new Node(Token.LP), null);
    this.kind = Kind.INTERFACE;
    this.typeOfThis = new InstanceObjectType(registry, this);
  }

  /** Creates an instance for a function that is an interface. */
  static FunctionType forInterface(
      JSTypeRegistry registry, String name, Node source) {
    return new FunctionType(registry, name, source);
  }

  @Override
  public boolean isInstanceType() {
    // The universal constructor is its own instance, bizarrely.
    return isEquivalentTo(registry.getNativeType(U2U_CONSTRUCTOR_TYPE));
  }

  @Override
  public boolean isConstructor() {
    return kind == Kind.CONSTRUCTOR;
  }

  @Override
  public boolean isInterface() {
    return kind == Kind.INTERFACE;
  }

  @Override
  public boolean isOrdinaryFunction() {
    return kind == Kind.ORDINARY;
  }

  @Override
  public FunctionType toMaybeFunctionType() {
    return this;
  }

  @Override
  public boolean canBeCalled() {
    return true;
  }

  public boolean hasImplementedInterfaces() {
    if (!implementedInterfaces.isEmpty()){
      return true;
    }
    FunctionType superCtor = isConstructor() ?
        getSuperClassConstructor() : null;
    if (superCtor != null) {
      return superCtor.hasImplementedInterfaces();
    }
    return false;
  }

  public Iterable<Node> getParameters() {
    Node n = getParametersNode();
    if (n != null) {
      return n.children();
    } else {
      return Collections.emptySet();
    }
  }

  /** Gets an LP node that contains all params. May be null. */
  public Node getParametersNode() {
    return call.parameters;
  }

  /** Gets the minimum number of arguments that this function requires. */
  public int getMinArguments() {
    // NOTE(nicksantos): There are some native functions that have optional
    // parameters before required parameters. This algorithm finds the position
    // of the last required parameter.
    int i = 0;
    int min = 0;
    for (Node n : getParameters()) {
      i++;
      if (!n.isOptionalArg() && !n.isVarArgs()) {
        min = i;
      }
    }
    return min;
  }

  /**
   * Gets the maximum number of arguments that this function requires,
   * or Integer.MAX_VALUE if this is a variable argument function.
   */
  public int getMaxArguments() {
    Node params = getParametersNode();
    if (params != null) {
      Node lastParam = params.getLastChild();
      if (lastParam == null || !lastParam.isVarArgs()) {
        return params.getChildCount();
      }
    }

    return Integer.MAX_VALUE;
  }

  public JSType getReturnType() {
    return call.returnType;
  }

  public boolean isReturnTypeInferred() {
    return call.returnTypeInferred;
  }

  /** Gets the internal arrow type. For use by subclasses only. */
  ArrowType getInternalArrowType() {
    return call;
  }

  @Override
  public StaticSlot<JSType> getSlot(String name) {
    if ("prototype".equals(name)) {
      // Lazy initialization of the prototype field.
      getPrototype();
      return prototypeSlot;
    } else {
      return super.getSlot(name);
    }
  }

  /**
   * Includes the prototype iff someone has created it. We do not want
   * to expose the prototype for ordinary functions.
   */
  public Set<String> getOwnPropertyNames() {
    if (prototypeSlot == null) {
      return super.getOwnPropertyNames();
    } else {
      Set<String> names = Sets.newHashSet("prototype");
      names.addAll(super.getOwnPropertyNames());
      return names;
    }
  }

  /**
   * Gets the {@code prototype} property of this function type. This is
   * equivalent to {@code (ObjectType) getPropertyType("prototype")}.
   */
  public ObjectType getPrototype() {
    // lazy initialization of the prototype field
    if (prototypeSlot == null) {
      setPrototype(
          new PrototypeObjectType(
              registry,
              this.getReferenceName() + ".prototype",
              registry.getNativeObjectType(OBJECT_TYPE),
              isNativeObjectType()),
          null);
    }
    return (ObjectType) prototypeSlot.getType();
  }

  /**
   * Sets the prototype, creating the prototype object from the given
   * base type.
   * @param baseType The base type.
   */
  public void setPrototypeBasedOn(ObjectType baseType) {
    setPrototypeBasedOn(baseType, null);
  }

  void setPrototypeBasedOn(ObjectType baseType, Node propertyNode) {
    // This is a bit weird. We need to successfully handle these
    // two cases:
    // Foo.prototype = new Bar();
    // and
    // Foo.prototype = {baz: 3};
    // In the first case, we do not want new properties to get
    // added to Bar. In the second case, we do want new properties
    // to get added to the type of the anonymous object.
    //
    // We handle this by breaking it into two cases:
    //
    // In the first case, we create a new PrototypeObjectType and set
    // its implicit prototype to the type being assigned. This ensures
    // that Bar will not get any properties of Foo.prototype, but properties
    // later assigned to Bar will get inherited properly.
    //
    // In the second case, we just use the anonymous object as the prototype.
    if (baseType.hasReferenceName() ||
        isNativeObjectType() ||
        baseType.isFunctionPrototypeType() ||
        !(baseType instanceof PrototypeObjectType)) {

      baseType = new PrototypeObjectType(
          registry, this.getReferenceName() + ".prototype", baseType);
    }
    setPrototype((PrototypeObjectType) baseType, propertyNode);
  }

  /**
   * Sets the prototype.
   * @param prototype the prototype. If this value is {@code null} it will
   *        silently be discarded.
   */
  boolean setPrototype(PrototypeObjectType prototype, Node propertyNode) {
    if (prototype == null) {
      return false;
    }
    // getInstanceType fails if the function is not a constructor
    if (isConstructor() && prototype == getInstanceType()) {
      return false;
    }

    PrototypeObjectType oldPrototype = prototypeSlot == null
        ? null : (PrototypeObjectType) prototypeSlot.getType();
    boolean replacedPrototype = oldPrototype != null;

    this.prototypeSlot = new Property("prototype", prototype, true,
        propertyNode == null ? source : propertyNode);
    prototype.setOwnerFunction(this);

    if (oldPrototype != null) {
      // Disassociating the old prototype makes this easier to debug--
      // we don't have to worry about two prototypes running around.
      oldPrototype.setOwnerFunction(null);
    }

    if (isConstructor() || isInterface()) {
      FunctionType superClass = getSuperClassConstructor();
      if (superClass != null) {
        superClass.addSubType(this);
      }

      if (isInterface()) {
        for (ObjectType interfaceType : getExtendedInterfaces()) {
          if (interfaceType.getConstructor() != null) {
            interfaceType.getConstructor().addSubType(this);
          }
        }
      }
    }

    if (replacedPrototype) {
      clearCachedValues();
    }

    return true;
  }

  /**
   * Returns all interfaces implemented by a class or its superclass and any
   * superclasses for any of those interfaces. If this is called before all
   * types are resolved, it may return an incomplete set.
   */
  public Iterable<ObjectType> getAllImplementedInterfaces() {
    // Store them in a linked hash set, so that the compile job is
    // deterministic.
    Set<ObjectType> interfaces = Sets.newLinkedHashSet();

    for (ObjectType type : getImplementedInterfaces()) {
      addRelatedInterfaces(type, interfaces);
    }
    return interfaces;
  }

  private void addRelatedInterfaces(ObjectType instance, Set<ObjectType> set) {
    FunctionType constructor = instance.getConstructor();
    if (constructor != null) {
      if (!constructor.isInterface()) {
        return;
      }

      set.add(instance);

      for (ObjectType interfaceType : instance.getCtorExtendedInterfaces()) {
        addRelatedInterfaces(interfaceType, set);
      }
    }
  }

  /** Returns interfaces implemented directly by a class or its superclass. */
  public Iterable<ObjectType> getImplementedInterfaces() {
    FunctionType superCtor = isConstructor() ?
        getSuperClassConstructor() : null;
    if (superCtor == null) {
      return implementedInterfaces;
    } else {
      return Iterables.concat(
          implementedInterfaces, superCtor.getImplementedInterfaces());
    }
  }

  public void setImplementedInterfaces(List<ObjectType> implementedInterfaces) {
    // Records this type for each implemented interface.
    for (ObjectType type : implementedInterfaces) {
      registry.registerTypeImplementingInterface(this, type);
    }
    this.implementedInterfaces = ImmutableList.copyOf(implementedInterfaces);
  }

  /**
   * Returns all extended interfaces declared by an interfaces or its super-
   * interfaces. If this is called before all types are resolved, it may return
   * an incomplete set.
   */
  public Iterable<ObjectType> getAllExtendedInterfaces() {
    // Store them in a linked hash set, so that the compile job is
    // deterministic.
    Set<ObjectType> extendedInterfaces = Sets.newLinkedHashSet();

    for (ObjectType interfaceType : getExtendedInterfaces()) {
      addRelatedExtendedInterfaces(interfaceType, extendedInterfaces);
    }
    return extendedInterfaces;
  }

  private void addRelatedExtendedInterfaces(ObjectType instance,
      Set<ObjectType> set) {
    FunctionType constructor = instance.getConstructor();
    if (constructor != null) {
      set.add(instance);

      for (ObjectType interfaceType : constructor.getExtendedInterfaces()) {
        addRelatedExtendedInterfaces(interfaceType, set);
      }
    }
  }

  /** Returns interfaces directly extended by an interface */
  public Iterable<ObjectType> getExtendedInterfaces() {
    return extendedInterfaces;
  }

  /** Returns the number of interfaces directly extended by an interface */
  public int getExtendedInterfacesCount() {
    return extendedInterfaces.size();
  }

  public void setExtendedInterfaces(List<ObjectType> extendedInterfaces)
    throws UnsupportedOperationException {
    if (isInterface()) {
      this.extendedInterfaces = ImmutableList.copyOf(extendedInterfaces);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public JSType getPropertyType(String name) {
    if (!hasOwnProperty(name)) {
      if ("call".equals(name)) {
        // Define the "call" function lazily.
        Node params = getParametersNode();
        if (params == null) {
          // If there's no params array, don't do any type-checking
          // in this CALL function.
          defineDeclaredProperty(name,
              new FunctionBuilder(registry)
              .withReturnType(getReturnType())
              .build(),
              source);
        } else {
          params = params.cloneTree();
          Node thisTypeNode = Node.newString(Token.NAME, "thisType");
          thisTypeNode.setJSType(
              registry.createOptionalNullableType(getTypeOfThis()));
          params.addChildToFront(thisTypeNode);
          thisTypeNode.setOptionalArg(true);

          defineDeclaredProperty(name,
              new FunctionBuilder(registry)
              .withParamsNode(params)
              .withReturnType(getReturnType())
              .build(),
              source);
        }
      } else if ("apply".equals(name)) {
        // Define the "apply" function lazily.
        FunctionParamBuilder builder = new FunctionParamBuilder(registry);

        // Ecma-262 says that apply's second argument must be an Array
        // or an arguments object. We don't model the arguments object,
        // so let's just be forgiving for now.
        // TODO(nicksantos): Model the Arguments object.
        builder.addOptionalParams(
            registry.createNullableType(getTypeOfThis()),
            registry.createNullableType(
                registry.getNativeType(JSTypeNative.OBJECT_TYPE)));

        defineDeclaredProperty(name,
            new FunctionBuilder(registry)
            .withParams(builder)
            .withReturnType(getReturnType())
            .build(),
            source);
      }
    }

    return super.getPropertyType(name);
  }

  @Override
  boolean defineProperty(String name, JSType type,
      boolean inferred, Node propertyNode) {
    if ("prototype".equals(name)) {
      ObjectType objType = type.toObjectType();
      if (objType != null) {
        if (prototypeSlot != null &&
            objType.isEquivalentTo(prototypeSlot.getType())) {
          return true;
        }
        this.setPrototypeBasedOn(objType, propertyNode);
        return true;
      } else {
        return false;
      }
    }
    return super.defineProperty(name, type, inferred, propertyNode);
  }

  @Override
  public JSType getLeastSupertype(JSType that) {
    return supAndInfHelper(that, true);
  }

  @Override
  public JSType getGreatestSubtype(JSType that) {
    return supAndInfHelper(that, false);
  }

  /**
   * Computes the supremum or infimum of functions with other types.
   * Because sup() and inf() share a lot of logic for functions, we use
   * a single helper.
   * @param leastSuper If true, compute the supremum of {@code this} with
   *     {@code that}. Otherwise compute the infimum.
   * @return The least supertype or greatest subtype.
   */
  private JSType supAndInfHelper(JSType that, boolean leastSuper) {
    // NOTE(nicksantos): When we remove the unknown type, the function types
    // form a lattice with the universal constructor at the top of the lattice,
    // and the LEAST_FUNCTION_TYPE type at the bottom of the lattice.
    //
    // When we introduce the unknown type, it's much more difficult to make
    // heads or tails of the partial ordering of types, because there's no
    // clear hierarchy between the different components (parameter types and
    // return types) in the ArrowType.
    //
    // Rather than make the situation more complicated by introducing new
    // types (like unions of functions), we just fallback on the simpler
    // approach of getting things right at the top and the bottom of the
    // lattice.
    if (isFunctionType() && that.isFunctionType()) {
      if (isEquivalentTo(that)) {
        return this;
      }

      FunctionType other = that.toMaybeFunctionType();

      // If these are ordinary functions, then merge them.
      // Don't do this if any of the params/return
      // values are unknown, because then there will be cycles in
      // their local lattice and they will merge in weird ways.
      if (other != null &&
          isOrdinaryFunction() && that.isOrdinaryFunction() &&
          !this.call.hasUnknownParamsOrReturn() &&
          !other.call.hasUnknownParamsOrReturn()) {

        // Check for the degenerate case, but double check
        // that there's not a cycle.
        boolean isSubtypeOfThat = this.isSubtype(that);
        boolean isSubtypeOfThis = that.isSubtype(this);
        if (isSubtypeOfThat && !isSubtypeOfThis) {
          return leastSuper ? that : this;
        } else if (isSubtypeOfThis && !isSubtypeOfThat) {
          return leastSuper ? this : that;
        }

        // Merge the two functions component-wise.
        FunctionType merged = tryMergeFunctionPiecewise(other, leastSuper);
        if (merged != null) {
          return merged;
        }
      }

      // The function instance type is a special case
      // that lives above the rest of the lattice.
      JSType functionInstance = registry.getNativeType(
          JSTypeNative.FUNCTION_INSTANCE_TYPE);
      if (functionInstance.isEquivalentTo(that)) {
        return leastSuper ? that : this;
      } else if (functionInstance.isEquivalentTo(this)) {
        return leastSuper ? this : that;
      }

      // In theory, we should be using the GREATEST_FUNCTION_TYPE as the
      // greatest function. In practice, we don't because it's way too
      // broad. The greatest function takes var_args None parameters, which
      // means that all parameters register a type warning.
      //
      // Instead, we use the U2U ctor type, which has unknown type args.
      FunctionType greatestFn =
          registry.getNativeFunctionType(JSTypeNative.U2U_CONSTRUCTOR_TYPE);
      FunctionType leastFn =
          registry.getNativeFunctionType(JSTypeNative.LEAST_FUNCTION_TYPE);
      return leastSuper ? greatestFn : leastFn;
    }

    return leastSuper ?
        super.getLeastSupertype(that) :
        super.getGreatestSubtype(that);
  }

  /**
   * Try to get the sup/inf of two functions by looking at the
   * piecewise components.
   */
  private FunctionType tryMergeFunctionPiecewise(
      FunctionType other, boolean leastSuper) {
    Node newParamsNode = null;
    if (call.hasEqualParameters(other.call)) {
      newParamsNode = call.parameters;
    } else {
      // If the parameters are not equal, don't try to merge them.
      // Someday, we should try to merge the individual params.
      return null;
    }

    JSType newReturnType = leastSuper ?
        call.returnType.getLeastSupertype(other.call.returnType) :
        call.returnType.getGreatestSubtype(other.call.returnType);

    ObjectType newTypeOfThis = null;
    if (isEquivalent(typeOfThis, other.typeOfThis)) {
      newTypeOfThis = typeOfThis;
    } else {
      JSType maybeNewTypeOfThis = leastSuper ?
          typeOfThis.getLeastSupertype(other.typeOfThis) :
          typeOfThis.getGreatestSubtype(other.typeOfThis);
      if (maybeNewTypeOfThis instanceof ObjectType) {
        newTypeOfThis = (ObjectType) maybeNewTypeOfThis;
      } else {
        newTypeOfThis = leastSuper ?
            registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE) :
            registry.getNativeObjectType(JSTypeNative.NO_OBJECT_TYPE);
      }
    }

    boolean newReturnTypeInferred =
        call.returnTypeInferred || other.call.returnTypeInferred;

    return new FunctionType(
        registry, null, null,
        new ArrowType(
            registry, newParamsNode, newReturnType, newReturnTypeInferred),
        newTypeOfThis, null, false, false);
  }

  /**
   * Given a constructor or an interface type, get its superclass constructor
   * or {@code null} if none exists.
   */
  public FunctionType getSuperClassConstructor() {
    Preconditions.checkArgument(isConstructor() || isInterface());
    ObjectType maybeSuperInstanceType = getPrototype().getImplicitPrototype();
    if (maybeSuperInstanceType == null) {
      return null;
    }
    return maybeSuperInstanceType.getConstructor();
  }

  /**
   * Given an interface and a property, finds the top-most super interface
   * that has the property defined (including this interface).
   */
  public static ObjectType getTopDefiningInterface(ObjectType type,
      String propertyName) {
    ObjectType foundType = null;
    if (type.hasProperty(propertyName)) {
      foundType = type;
    }
    for (ObjectType interfaceType : type.getCtorExtendedInterfaces()) {
      if (interfaceType.hasProperty(propertyName)) {
        foundType = getTopDefiningInterface(interfaceType, propertyName);
      }
    }
    return foundType;
  }

  /**
   * Given a constructor or an interface type and a property, finds the
   * top-most superclass that has the property defined (including this
   * constructor).
   */
  public ObjectType getTopMostDefiningType(String propertyName) {
    Preconditions.checkState(isConstructor() || isInterface());
    Preconditions.checkArgument(getPrototype().hasProperty(propertyName));
    FunctionType ctor = this;

    if (isInterface()) {
      return getTopDefiningInterface(this.getInstanceType(), propertyName);
    }

    ObjectType topInstanceType = ctor.getInstanceType();
    while (true) {
      topInstanceType = ctor.getInstanceType();
      ctor = ctor.getSuperClassConstructor();
      if (ctor == null || !ctor.getPrototype().hasProperty(propertyName)) {
        break;
      }
    }
    return topInstanceType;
  }

  /**
   * Two function types are equal if their signatures match. Since they don't
   * have signatures, two interfaces are equal if their names match.
   */
  @Override
  public boolean isEquivalentTo(JSType otherType) {
    FunctionType that =
        JSType.toMaybeFunctionType(otherType);
    if (that == null) {
      return false;
    }
    if (this.isConstructor()) {
      if (that.isConstructor()) {
        return this == that;
      }
      return false;
    }
    if (this.isInterface()) {
      if (that.isInterface()) {
        return this.getReferenceName().equals(that.getReferenceName());
      }
      return false;
    }
    if (that.isInterface()) {
      return false;
    }
    return this.typeOfThis.isEquivalentTo(that.typeOfThis) &&
        this.call.isEquivalentTo(that.call);
  }

  @Override
  public int hashCode() {
    return isInterface() ? getReferenceName().hashCode() : call.hashCode();
  }

  public boolean hasEqualCallType(FunctionType otherType) {
    return this.call.isEquivalentTo(otherType.call);
  }

  /**
   * Informally, a function is represented by
   * {@code function (params): returnType} where the {@code params} is a comma
   * separated list of types, the first one being a special
   * {@code this:T} if the function expects a known type for {@code this}.
   */
  @Override
  public String toString() {
    if (this == registry.getNativeType(JSTypeNative.FUNCTION_INSTANCE_TYPE)) {
      return "Function";
    }

    StringBuilder b = new StringBuilder(32);
    b.append("function (");
    int paramNum = call.parameters.getChildCount();
    boolean hasKnownTypeOfThis = !typeOfThis.isUnknownType();
    if (hasKnownTypeOfThis) {
      if (isConstructor()) {
        b.append("new:");
      } else {
        b.append("this:");
      }
      b.append(typeOfThis.toString());
    }
    if (paramNum > 0) {
      if (hasKnownTypeOfThis) {
        b.append(", ");
      }
      Node p = call.parameters.getFirstChild();
      if (p.isVarArgs()) {
        appendVarArgsString(b, p.getJSType());
      } else {
        b.append(p.getJSType().toString());
      }
      p = p.getNext();
      while (p != null) {
        b.append(", ");
        if (p.isVarArgs()) {
          appendVarArgsString(b, p.getJSType());
        } else {
          b.append(p.getJSType().toString());
        }
        p = p.getNext();
      }
    }
    b.append("): ");
    b.append(call.returnType);
    return b.toString();
  }

  /** Gets the string representation of a var args param. */
  private void appendVarArgsString(StringBuilder builder, JSType paramType) {
    if (paramType.isUnionType()) {
      // Remove the optionalness from the var arg.
      paramType = paramType.toMaybeUnionType().getRestrictedUnion(
          registry.getNativeType(JSTypeNative.VOID_TYPE));
    }
    builder.append("...[").append(paramType.toString()).append("]");
  }

  /**
   * A function is a subtype of another if their call methods are related via
   * subtyping and {@code this} is a subtype of {@code that} with regard to
   * the prototype chain.
   */
  @Override
  public boolean isSubtype(JSType that) {
    if (JSType.isSubtype(this, that)) {
      return true;
    }

    if (that.isFunctionType()) {
      FunctionType other = that.toMaybeFunctionType();
      if (other.isInterface()) {
        // Any function can be assigned to an interface function.
        return true;
      }
      if (this.isInterface()) {
        // An interface function cannot be assigned to anything.
        return false;
      }
      // If functionA is a subtype of functionB, then their "this" types
      // should be contravariant. However, this causes problems because
      // of the way we enforce overrides. Because function(this:SubFoo)
      // is not a subtype of function(this:Foo), our override check treats
      // this as an error. It also screws up out standard method
      // for aliasing constructors. Let's punt on all this for now.
      // TODO(nicksantos): fix this.
      boolean treatThisTypesAsCovariant =
        // If either one of these is a ctor, skip 'this' checking.
        this.isConstructor() || other.isConstructor() ||

        // An interface 'this'-type is non-restrictive.
        // In practical terms, if C implements I, and I has a method m,
        // then any m doesn't necessarily have to C#m's 'this'
        // type doesn't need to match I.
        (other.typeOfThis.getConstructor() != null &&
             other.typeOfThis.getConstructor().isInterface()) ||

        // If one of the 'this' types is covariant of the other,
        // then we'll treat them as covariant (see comment above).
        other.typeOfThis.isSubtype(this.typeOfThis) ||
        this.typeOfThis.isSubtype(other.typeOfThis);
      return treatThisTypesAsCovariant && this.call.isSubtype(other.call);
    }

    return getNativeType(JSTypeNative.FUNCTION_PROTOTYPE).isSubtype(that);
  }

  @Override
  public <T> T visit(Visitor<T> visitor) {
    return visitor.caseFunctionType(this);
  }

  /**
   * Gets the type of instance of this function.
   * @throws IllegalStateException if this function is not a constructor
   *         (see {@link #isConstructor()}).
   */
  public ObjectType getInstanceType() {
    Preconditions.checkState(hasInstanceType());
    return typeOfThis;
  }

  /**
   * Sets the instance type. This should only be used for special
   * native types.
   */
  void setInstanceType(ObjectType instanceType) {
    typeOfThis = instanceType;
  }

  /**
   * Returns whether this function type has an instance type.
   */
  public boolean hasInstanceType() {
    return isConstructor() || isInterface();
  }

  /**
   * Gets the type of {@code this} in this function.
   */
  @Override
  public ObjectType getTypeOfThis() {
    return typeOfThis.isNoObjectType() ?
        registry.getNativeObjectType(JSTypeNative.OBJECT_TYPE) : typeOfThis;
  }

  /**
   * Gets the source node or null if this is an unknown function.
   */
  public Node getSource() {
    return source;
  }

  /**
   * Sets the source node.
   */
  public void setSource(Node source) {
    if (null == source) {
      prototypeSlot = null;
    }
    this.source = source;
  }

  /** Adds a type to the list of subtypes for this type. */
  private void addSubType(FunctionType subType) {
    if (subTypes == null) {
      subTypes = Lists.newArrayList();
    }
    subTypes.add(subType);
  }

  @Override
  public void clearCachedValues() {
    super.clearCachedValues();

    if (subTypes != null) {
      for (FunctionType subType : subTypes) {
        subType.clearCachedValues();
      }
    }

    if (!isNativeObjectType()) {
      if (hasInstanceType()) {
        getInstanceType().clearCachedValues();
      }

      if (prototypeSlot != null) {
        ((PrototypeObjectType) prototypeSlot.getType()).clearCachedValues();
      }
    }
  }

  /**
   * Returns a list of types that are subtypes of this type. This is only valid
   * for constructor functions, and may be null. This allows a downward
   * traversal of the subtype graph.
   */
  public List<FunctionType> getSubTypes() {
    return subTypes;
  }

  @Override
  public boolean hasCachedValues() {
    return prototypeSlot != null || super.hasCachedValues();
  }

  /**
   * Gets the template type name.
   */
  public String getTemplateTypeName() {
    return templateTypeName;
  }

  @Override
  JSType resolveInternal(ErrorReporter t, StaticScope<JSType> scope) {
    setResolvedTypeInternal(this);

    call = (ArrowType) safeResolve(call, t, scope);
    if (prototypeSlot != null) {
      prototypeSlot.setType(
          safeResolve(prototypeSlot.getType(), t, scope));
    }

    // Warning about typeOfThis if it doesn't resolve to an ObjectType
    // is handled further upstream.
    //
    // TODO(nicksantos): Handle this correctly if we have a UnionType.
    //
    // TODO(nicksantos): In ES3, the runtime coerces "null" to the global
    // activation object. In ES5, it leaves it as null. Just punt on this
    // issue for now by coercing out null. This is complicated by the
    // fact that when most people write @this {Foo}, they really don't
    // mean "nullable Foo". For certain tags (like @extends) we de-nullify
    // the name for them.
    JSType maybeTypeOfThis = safeResolve(typeOfThis, t, scope);
    if (maybeTypeOfThis != null) {
      maybeTypeOfThis = maybeTypeOfThis.restrictByNotNullOrUndefined();
    }
    if (maybeTypeOfThis instanceof ObjectType) {
      typeOfThis = (ObjectType) maybeTypeOfThis;
    }

    boolean changed = false;
    ImmutableList.Builder<ObjectType> resolvedInterfaces =
        ImmutableList.builder();
    for (ObjectType iface : implementedInterfaces) {
      ObjectType resolvedIface = (ObjectType) iface.resolve(t, scope);
      resolvedInterfaces.add(resolvedIface);
      changed |= (resolvedIface != iface);
    }
    if (changed) {
      implementedInterfaces = resolvedInterfaces.build();
    }

    if (subTypes != null) {
      for (int i = 0; i < subTypes.size(); i++) {
        subTypes.set(
            i, JSType.toMaybeFunctionType(subTypes.get(i).resolve(t, scope)));
      }
    }

    return super.resolveInternal(t, scope);
  }

  @Override
  public String toDebugHashCodeString() {
    if (this == registry.getNativeType(JSTypeNative.FUNCTION_INSTANCE_TYPE)) {
      return super.toDebugHashCodeString();
    }

    StringBuilder b = new StringBuilder(32);
    b.append("function (");
    int paramNum = call.parameters.getChildCount();
    boolean hasKnownTypeOfThis = !typeOfThis.isUnknownType();
    if (hasKnownTypeOfThis) {
      b.append("this:");
      b.append(getDebugHashCodeStringOf(typeOfThis));
    }
    if (paramNum > 0) {
      if (hasKnownTypeOfThis) {
        b.append(", ");
      }
      Node p = call.parameters.getFirstChild();
      b.append(getDebugHashCodeStringOf(p.getJSType()));
      p = p.getNext();
      while (p != null) {
        b.append(", ");
        b.append(getDebugHashCodeStringOf(p.getJSType()));
        p = p.getNext();
      }
    }
    b.append(")");
    b.append(": ");
    b.append(getDebugHashCodeStringOf(call.returnType));
    return b.toString();
  }

  private String getDebugHashCodeStringOf(JSType type) {
    if (type == this) {
      return "me";
    } else {
      return type.toDebugHashCodeString();
    }
  }
}
