package org.xapik.ploy.java.client.handlers.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xapik.ploy.java.client.handlers.JobExecutor;
import org.xapik.ploy.java.client.handlers.JobHandler;

@Slf4j
@Component
@JobHandler
public class TestExecutor {

    @JobExecutor(inputType = TestInputDto.class, name = "testJob")
    public TestOutputDto testJob(TestInputDto input) {
        String newMessage = input.message() + " - testJob";

        log.info("New message {}", newMessage);
        return new TestOutputDto(newMessage);
    }
}
