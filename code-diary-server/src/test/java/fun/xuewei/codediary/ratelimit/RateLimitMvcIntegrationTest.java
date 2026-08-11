package fun.xuewei.codediary.ratelimit;

import fun.xuewei.codediary.component.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RateLimitMvcIntegrationTest {

    @Test
    void shouldReturnExistingServerBusyResponseWhenLimitIsExceeded() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequestsPerMinute(1);
        IpRateLimitInterceptor interceptor = new IpRateLimitInterceptor(
                properties,
                new IpRateLimiter(properties)
        );
        MockMvc mockMvc = standaloneSetup(new TestController())
                .setControllerAdvice(new RestExceptionHandler())
                .addInterceptors(interceptor)
                .build();

        mockMvc.perform(get("/limited").with(request -> {
                    request.setRemoteAddr("192.168.1.10");
                    return request;
                }))
                .andExpect(status().isOk());

        mockMvc.perform(get("/limited").with(request -> {
                    request.setRemoteAddr("192.168.1.10");
                    return request;
                }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("B0004"))
                .andExpect(jsonPath("$.message").value("系统繁忙"));
    }

    @RestController
    private static class TestController {

        @GetMapping("/limited")
        public String limited() {
            return "ok";
        }
    }
}
