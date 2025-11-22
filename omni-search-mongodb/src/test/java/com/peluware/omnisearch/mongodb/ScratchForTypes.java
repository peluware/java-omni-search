package com.peluware.omnisearch.mongodb;

import java.util.List;
import java.util.Map;

import static com.peluware.omnisearch.mongodb.ReflectionUtils.resolveFieldType;
import static com.peluware.omnisearch.mongodb.ReflectionUtils.resolveComponentFieldType;

public class ScratchForTypes {

    abstract static class Base<T> {
        public T value;
        public List<T> list;
    }


    static class Derived extends Base<String> {
        public Map<String, Integer> map;
    }


    static class AnotherBase<T> extends Base<T> {
        public T anotherValue;
    }


    static class AnotherDerived extends AnotherBase<Integer> {
        public List<Integer> anotherList;
    }

    // Mini test en main
    public static void main(String[] args) throws NoSuchFieldException {
        derived();
        System.out.println("-----");
        anotherDerived();
    }

    private static void derived() throws NoSuchFieldException {
        var field1 = Base.class.getDeclaredField("value");
        var field2 = Base.class.getDeclaredField("list");
        var field3 = Derived.class.getDeclaredField("map");


        System.out.println("Field 'value' resolved type: " + resolveFieldType(field1, Derived.class).getName());
        System.out.println("Field 'list' resolved type: " + resolveFieldType(field2, Derived.class).getName());
        System.out.println("Field 'list<T>' component type: " + resolveComponentFieldType(field2, Derived.class).getName());
        System.out.println("Field 'map' resolved type: " + resolveFieldType(field3, Derived.class).getName());
    }

    private static void anotherDerived() throws NoSuchFieldException {
        var field1 = Base.class.getDeclaredField("value");
        var field2 = Base.class.getDeclaredField("list");
        var field3 = AnotherBase.class.getDeclaredField("anotherValue");
        var field4 = AnotherDerived.class.getDeclaredField("anotherList");

        System.out.println("Field 'value' resolved type: " + resolveFieldType(field1, AnotherDerived.class).getName());
        System.out.println("Field 'list' resolved type: " + resolveFieldType(field2, AnotherDerived.class).getName());
        System.out.println("Field 'list<T>' component type: " + resolveComponentFieldType(field2, AnotherDerived.class).getName());
        System.out.println("Field 'anotherValue' resolved type: " + resolveFieldType(field3, AnotherDerived.class).getName());
        System.out.println("Field 'anotherList' resolved type: " + resolveFieldType(field4, AnotherDerived.class).getName());
        System.out.println("Field 'anotherList<T>' component type: " + resolveComponentFieldType(field4, AnotherDerived.class).getName());
    }
}
