/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.services.datamodel.backend.server.builder.projects;

import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.workbench.models.datamodel.oracle.FieldAccessorsAndMutators;
import org.drools.workbench.models.datamodel.oracle.ModelField;
import org.kie.workbench.common.services.datamodel.backend.server.builder.util.BlackLists;

/**
 * Find information for all "fields" on a class. A "field" is either a public property or a non-public property for which there is a "getter" and/or a "setter"
 */
public class ClassFieldInspector {

    private final Map<String, FieldInfo> fieldTypesFieldInfo = new HashMap<String, FieldInfo>();

    public ClassFieldInspector( final Class<?> clazz ) throws IOException {
        //Handle fields
        final List<Field> fields = new ArrayList<Field>( getAllFields( clazz ).values() );
        final List<Field> declaredFields = Arrays.asList( clazz.getDeclaredFields() );
        for ( Field field : fields ) {
            if ( BlackLists.isClassMethodBlackListed( clazz,
                                                      field.getName() ) ) {
                continue;
            }
            if ( BlackLists.isTypeBlackListed( field.getType() ) ) {
                continue;
            }

            if ( Modifier.isPublic( field.getModifiers() ) && !Modifier.isStatic( field.getModifiers() ) ) {
                this.fieldTypesFieldInfo.put( field.getName(),
                                              new FieldInfo( FieldAccessorsAndMutators.BOTH,
                                                             field.getGenericType(),
                                                             field.getType(),
                                                             declaredFields.contains( field ) ? ModelField.FIELD_ORIGIN.DECLARED : ModelField.FIELD_ORIGIN.INHERITED ) );
            }
        }

        //Handle methods
        final List<Method> methods = new ArrayList<Method>( getAllMethods( clazz ).values() );
        for ( Method method : methods ) {
            final int modifiers = method.getModifiers();
            if ( Modifier.isPublic( modifiers ) && !Modifier.isStatic( method.getModifiers() ) ) {
                String methodName = null;
                FieldAccessorsAndMutators accessorsAndMutators = null;
                if ( isGetter( method ) ) {
                    methodName = method.getName().substring( 3 );
                    methodName = Introspector.decapitalize( methodName );
                    accessorsAndMutators = FieldAccessorsAndMutators.ACCESSOR;

                } else if ( isBooleanGetter( method ) ) {
                    methodName = method.getName().substring( 2 );
                    methodName = Introspector.decapitalize( methodName );
                    accessorsAndMutators = FieldAccessorsAndMutators.ACCESSOR;

                } else if ( isSetter( method ) ) {
                    methodName = method.getName().substring( 3 );
                    methodName = Introspector.decapitalize( methodName );
                    accessorsAndMutators = FieldAccessorsAndMutators.MUTATOR;
                }

                if ( methodName != null ) {
                    if ( BlackLists.isClassMethodBlackListed( clazz,
                                                              methodName ) ) {
                        continue;
                    }
                    if ( BlackLists.isTypeBlackListed( method.getReturnType() ) ) {
                        continue;
                    }

                    //Correct accessor information, if a Field has already been discovered
                    if ( this.fieldTypesFieldInfo.containsKey( methodName ) ) {
                        final FieldInfo info = this.fieldTypesFieldInfo.get( methodName );
                        if ( accessorsAndMutators == FieldAccessorsAndMutators.ACCESSOR ) {
                            if ( info.accessorAndMutator == FieldAccessorsAndMutators.MUTATOR ) {
                                info.accessorAndMutator = FieldAccessorsAndMutators.BOTH;
                            }
                            info.genericType = method.getGenericReturnType();
                            info.returnType = method.getReturnType();

                        } else if ( accessorsAndMutators == FieldAccessorsAndMutators.MUTATOR ) {
                            if ( info.accessorAndMutator == FieldAccessorsAndMutators.ACCESSOR ) {
                                info.accessorAndMutator = FieldAccessorsAndMutators.BOTH;
                            }
                        }

                    } else {
                        final ModelField.FIELD_ORIGIN origin = getOrigin( methodName,
                                                                          getNames( fields ),
                                                                          getNames( declaredFields ) );
                        this.fieldTypesFieldInfo.put( methodName,
                                                      new FieldInfo( accessorsAndMutators,
                                                                     method.getGenericReturnType(),
                                                                     method.getReturnType(),
                                                                     origin ) );
                    }
                }
            }
        }
    }

    private boolean isGetter( final Method m ) {
        String name = m.getName();
        int parameterCount = m.getParameterTypes().length;
        if ( parameterCount != 0 ) {
            return false;
        }
        return ( name.length() > 3 && name.startsWith( "get" ) );
    }

    private boolean isBooleanGetter( final Method m ) {
        String name = m.getName();
        int parameterCount = m.getParameterTypes().length;
        if ( parameterCount != 0 ) {
            return false;
        }
        return ( name.length() > 2 && name.startsWith( "is" ) && ( Boolean.class.isAssignableFrom( m.getReturnType() ) || Boolean.TYPE == m.getReturnType() ) );
    }

    private boolean isSetter( final Method m ) {
        String name = m.getName();
        int parameterCount = m.getParameterTypes().length;
        if ( parameterCount != 1 ) {
            return false;
        }
        return ( name.length() > 3 && name.startsWith( "set" ) );
    }

    private List<String> getNames( final List<Field> fields ) {
        final List<String> names = new ArrayList<String>();
        for ( Field field : fields ) {
            names.add( field.getName() );
        }
        return names;
    }

    private ModelField.FIELD_ORIGIN getOrigin( final String methodName,
                                               final List<String> fields,
                                               final List<String> declaredFields ) {
        if ( declaredFields.contains( methodName ) ) {
            return ModelField.FIELD_ORIGIN.DECLARED;
        }
        if ( fields.contains( methodName ) ) {
            return ModelField.FIELD_ORIGIN.INHERITED;
        }
        return ModelField.FIELD_ORIGIN.DELEGATED;
    }

    public Set<String> getFieldNames() {
        return this.fieldTypesFieldInfo.keySet();
    }

    public Map<String, FieldInfo> getFieldTypesFieldInfo() {
        return this.fieldTypesFieldInfo;
    }

    //class.getDeclaredField(String) doesn't walk the inheritance tree; this does
    private Map<String, Field> getAllFields( Class<?> type ) {
        Map<String, Field> fields = new HashMap<String, Field>();
        for ( Class<?> c = type; c != null; c = c.getSuperclass() ) {
            for ( Field f : c.getDeclaredFields() ) {
                fields.put( f.getName(), f );
            }
        }
        return fields;
    }

    //class.getDeclaredMethods() doesn't walk the inheritance tree; this does
    private Map<String, Method> getAllMethods( Class<?> type ) {
        Map<String, Method> methods = new HashMap<String, Method>();
        for ( Class<?> c = type; c != null; c = c.getSuperclass() ) {
            for ( Method m : c.getDeclaredMethods() ) {
                methods.put( m.getName(), m );
            }
        }
        return methods;
    }

    public static class FieldInfo {

        private FieldAccessorsAndMutators accessorAndMutator;
        private Type genericType;
        private Class<?> returnType;
        private ModelField.FIELD_ORIGIN origin;

        private FieldInfo( final FieldAccessorsAndMutators accessorAndMutator,
                           final Type genericType,
                           final Class<?> returnType,
                           final ModelField.FIELD_ORIGIN origin ) {
            this.accessorAndMutator = accessorAndMutator;
            this.genericType = genericType;
            this.returnType = returnType;
            this.origin = origin;
        }

        public FieldAccessorsAndMutators getAccessorAndMutator() {
            return accessorAndMutator;
        }

        public Type getGenericType() {
            return genericType;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public ModelField.FIELD_ORIGIN getOrigin() {
            return origin;
        }

    }

}
