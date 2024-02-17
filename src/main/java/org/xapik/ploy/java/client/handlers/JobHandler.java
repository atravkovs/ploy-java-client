package org.xapik.ploy.java.client.handlers;

public interface JobHandler<T> {

    void execute(T input);

}
