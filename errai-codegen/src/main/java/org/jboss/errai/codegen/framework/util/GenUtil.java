/*
 * Copyright 2011 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.codegen.framework.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;
import org.jboss.errai.codegen.framework.*;
import org.jboss.errai.codegen.framework.builder.impl.Scope;
import org.jboss.errai.codegen.framework.exception.InvalidTypeException;
import org.jboss.errai.codegen.framework.exception.OutOfScopeException;
import org.jboss.errai.codegen.framework.exception.TypeNotIterableException;
import org.jboss.errai.codegen.framework.exception.UndefinedMethodException;
import org.jboss.errai.codegen.framework.literal.LiteralFactory;
import org.jboss.errai.codegen.framework.literal.LiteralValue;
import org.jboss.errai.codegen.framework.meta.*;
import org.mvel2.DataConversion;

/**
 * @author Mike Brock <cbrock@redhat.com>
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class GenUtil {
  public static Statement[] generateCallParameters(Context context, Object... parameters) {
    Statement[] statements = new Statement[parameters.length];

    int i = 0;
    for (Object parameter : parameters) {
      statements[i++] = generate(context, parameter);
    }
    return statements;
  }

  public static Statement[] generateCallParameters(MetaMethod method, Context context, Object... parameters) {
    if (parameters.length != method.getParameters().length) {
      throw new UndefinedMethodException("Wrong number of parameters");
    }

    Statement[] statements = new Statement[parameters.length];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof Statement) {
        if (((Statement) parameter).getType() == null) {
          parameter = generate(context, parameter);
        }
      }
      statements[i] = convert(context, parameter, method.getParameters()[i++].getType());
    }
    return statements;
  }

  public static Statement generate(Context context, Object o) {
    if (o instanceof VariableReference) {
      return context.getVariable(((VariableReference) o).getName());
    }
    else if (o instanceof Variable) {
      Variable v = (Variable) o;
      if (context.isScoped(v)) {
        return v.getReference();
      }
      else {
        throw new OutOfScopeException("variable cannot be referenced from this scope: " + v.getName());
      }
    }
    else if (o instanceof Statement) {
      ((Statement) o).generate(context);
      return (Statement) o;
    }
    else {
      return LiteralFactory.getLiteral(o);
    }
  }

  public static void assertIsIterable(Statement statement) {
    Class<?> cls = statement.getType().asClass();

    if (!cls.isArray() && !Iterable.class.isAssignableFrom(cls))
      throw new TypeNotIterableException(statement.generate(Context.create()));
  }

  public static void assertAssignableTypes(MetaClass from, MetaClass to) {
    if (!to.asBoxed().isAssignableFrom(from.asBoxed())) {
      throw new InvalidTypeException(to.getFullyQualifiedName() + " is not assignable from "
              + from.getFullyQualifiedName());
    }
  }

  public static Statement convert(Context context, Object input, MetaClass targetType) {
    try {
      if (input instanceof Statement) {
        if (input instanceof LiteralValue<?>) {
          input = ((LiteralValue<?>) input).getValue();
        }
        else {
          ((Statement) input).generate(context);
          assertAssignableTypes(((Statement) input).getType(), targetType);
          return (Statement) input;
        }
      }

      Class<?> inputClass = input == null ? Object.class : input.getClass();
      Class<?> targetClass = targetType.asBoxed().asClass();
      if (DataConversion.canConvert(targetClass, inputClass)) {
        return generate(context, DataConversion.convert(input, targetClass));
      }
      else {
        return generate(context, input);
      }
    }
    catch (Throwable t) {
      throw new InvalidTypeException(t);
    }
  }

  public static String classesAsStrings(MetaClass... stmt) {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < stmt.length; i++) {
      buf.append(stmt[i].getFullyQualifiedName());
      if (i + 1 < stmt.length) {
        buf.append(", ");
      }
    }
    return buf.toString();
  }

  public static MetaClass[] fromParameters(MetaParameter... parms) {
    List<MetaClass> parameters = new ArrayList<MetaClass>();
    for (MetaParameter metaParameter : parms) {
      parameters.add(metaParameter.getType());
    }
    return parameters.toArray(new MetaClass[parameters.size()]);
  }

  public static MetaClass[] classToMeta(Class<?>[] types) {
    MetaClass[] metaClasses = new MetaClass[types.length];
    for (int i = 0; i < types.length; i++) {
      metaClasses[i] = MetaClassFactory.get(types[i]);
    }
    return metaClasses;
  }

  public static Class<?>[] jParmToClass(JParameter[] parms) throws ClassNotFoundException {
    Class<?>[] classes = new Class<?>[parms.length];
    for (int i = 0; i < parms.length; i++) {
      classes[i] = getPrimitiveOrClass(parms[i]);
    }
    return classes;
  }

  public static Class<?> getPrimitiveOrClass(JParameter parm) throws ClassNotFoundException {
    JType type = parm.getType();
    String name = type.isArray() != null ? type.getJNISignature().replace("/", ".") : type.getQualifiedSourceName();

    if (parm.getType().isPrimitive() != null) {
      char sig = parm.getType().isPrimitive().getJNISignature().charAt(0);

      switch (sig) {
        case 'Z':
          return boolean.class;
        case 'B':
          return byte.class;
        case 'C':
          return char.class;
        case 'D':
          return double.class;
        case 'F':
          return float.class;
        case 'I':
          return int.class;
        case 'J':
          return long.class;
        case 'S':
          return short.class;
        case 'V':
          return void.class;
        default:
          return null;
      }
    }
    else {
      return Class.forName(name, false, Thread.currentThread().getContextClassLoader());
    }
  }


  public static Scope scopeOf(MetaClass clazz) {
    if (clazz.isPublic()) {
      return Scope.Public;
    }
    else if (clazz.isPrivate()) {
      return Scope.Private;
    }
    else if (clazz.isProtected()) {
      return Scope.Protected;
    }
    else {
      return Scope.Package;
    }
  }

  public static Scope scopeOf(MetaClassMember member) {
    if (member.isPublic()) {
      return Scope.Public;
    }
    else if (member.isPrivate()) {
      return Scope.Private;
    }
    else if (member.isProtected()) {
      return Scope.Protected;
    }
    else {
      return Scope.Package;
    }
  }

  public static DefModifiers modifiersOf(MetaClassMember member) {
    DefModifiers defModifiers = new DefModifiers();
    if (member.isAbstract()) {
      defModifiers.addModifiers(Modifier.Abstract);
    }
    else if (member.isFinal()) {
      defModifiers.addModifiers(Modifier.Final);
    }
    else if (member.isStatic()) {
      defModifiers.addModifiers(Modifier.Static);
    }
    else if (member.isSynchronized()) {
      defModifiers.addModifiers(Modifier.Synchronized);
    }
    else if (member.isVolatile()) {
      defModifiers.addModifiers(Modifier.Volatile);
    }
    else if (member.isTransient()) {
      defModifiers.addModifiers(Modifier.Transient);
    }
    return defModifiers;
  }

  public static boolean equals(MetaConstructor a, MetaConstructor b) {
    if (!a.getName().equals(b.getName())) return false;
    if (a.getParameters().length != b.getParameters().length) return false;

    for (int i = 0; i < a.getParameters().length; i++) {
      if (!equals(a.getParameters()[i], b.getParameters()[i])) return false;
    }
    return true;
  }


  public static boolean equals(MetaMethod a, MetaMethod b) {
    if (!a.getName().equals(b.getName())) return false;
    if (a.getParameters().length != b.getParameters().length) return false;

    for (int i = 0; i < a.getParameters().length; i++) {
      if (!equals(a.getParameters()[i], b.getParameters()[i])) return false;
    }
    return true;
  }

  public static boolean equals(MetaParameter a, MetaParameter b) {
    return a.getType().isAssignableFrom(b.getType()) || b.getType().isAssignableFrom(a.getType());
  }
}