package org.xapik.ploy.java.client.handlers.test;

import lombok.extern.slf4j.Slf4j;
import org.xapik.ploy.java.client.handlers.JobHandler;

@Slf4j
public class TestHandler implements JobHandler<TestInputDto> {
    @Override
    public void execute(TestInputDto input) {
        log.info("Hello, {}!", input.getMessage());
    }
}
