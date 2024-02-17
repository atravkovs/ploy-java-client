package org.xapik.ploy.java.client.handlers;

import lombok.Data;

import java.lang.reflect.Method;

@Data
public class JobExecutorEntry {

    String name;
    Method method;
    Object handler;
    Class<?> inputClass;

}
