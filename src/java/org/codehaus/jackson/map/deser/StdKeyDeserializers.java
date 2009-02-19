package org.codehaus.jackson.map.deser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used to contain simple/well-known key deserializers.
 * Following kinds of Objects can be handled currently:
 *<ul>
 * <li>Primitive wrappers</li>
 * <li>Enums (usually not needed, since EnumMap doesn't call us)</li>
 * <li>Anything with constructor that takes a single String arg
 *   (if not explicitly @JsonIgnore'd)</li>
 * <li>Anything with 'static T valueOf(String)' factory method
 *   (if not explicitly @JsonIgnore'd)</li>
 *</ul>
 */
class StdKeyDeserializers
{
    final HashMap<JavaType, KeyDeserializer> _keyDeserializers = new HashMap<JavaType, KeyDeserializer>();

    private StdKeyDeserializers()
    {
        add(new StdKeyDeserializer.BoolKD());
        add(new StdKeyDeserializer.ByteKD());
        add(new StdKeyDeserializer.CharKD());
        add(new StdKeyDeserializer.ShortKD());
        add(new StdKeyDeserializer.IntKD());
        add(new StdKeyDeserializer.LongKD());
        add(new StdKeyDeserializer.FloatKD());
        add(new StdKeyDeserializer.DoubleKD());
    }

    private void add(StdKeyDeserializer kdeser)
    {
        Class<?> keyClass = kdeser.getKeyClass();
        _keyDeserializers.put(TypeFactory.instance.fromClass(keyClass), kdeser);
    }

    public static HashMap<JavaType, KeyDeserializer> constructAll()
    {
        return new StdKeyDeserializers()._keyDeserializers;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Dynamic factory methods
    ////////////////////////////////////////////////////////////
     */

    public static KeyDeserializer constructEnumKeyDeserializer(JavaType type)
    {
        EnumResolver er = EnumResolver.constructFor(type.getRawClass());
        return new StdKeyDeserializer.EnumKD(er);
    }

    public static KeyDeserializer findStringBasedKeyDeserializer(JavaType type)
    {
        ClassIntrospector intr = new ClassIntrospector(type.getRawClass());
        // Ok, so: can we find T(String) constructor?
        Constructor<?> ctor = intr.findSingleArgConstructor(String.class);
        if (ctor != null) {
            return new StdKeyDeserializer.StringCtorKeyDeserializer(ctor);
        }
        /* or if not, "static T valueOf(String)" (or equivalent marked
         * with @JsonCreator annotation?)
         */
        Method m = intr.findFactoryMethod(String.class);
        if (m != null){
            return new StdKeyDeserializer.StringFactoryKeyDeserializer(m);
        }
        // nope, no such luck...
        return null;
    }
}
