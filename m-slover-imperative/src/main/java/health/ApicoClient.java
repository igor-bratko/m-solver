package health;


import com.fasterxml.jackson.databind.ObjectMapper;
import health.ParsedToken.ErrorDto;
import health.ParsedToken.Params;
import health.ParsedToken.SvgPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.Map;

import static java.time.Duration.ofMillis;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@RequiredArgsConstructor
public class ApicoClient {
    private final WebClient client;
    private final ObjectMapper om;

    public List<String> initRequest(String magic) {
        return client.get().uri("/" + magic)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                })
                .block();
    }

    public SvgPart postMagic(int node, String magic, String payload, Map<Integer, String> pow) {
        return client.post().uri("/" + magic)
                .contentType(APPLICATION_JSON)
                .bodyValue(Map.of("payload", payload, "responses", pow))
                .<SvgPart>exchangeToMono(resp -> {
                    if (resp.statusCode() == OK) {
                        return resp.bodyToMono(new ParameterizedTypeReference<>() {
                        });
                    } else {
                        return resp.bodyToMono(String.class)
                                .doOnNext(res -> log.warn("Error: {}", res))
                                .flatMap(error -> Mono.error(new RuntimeException(error)));
                    }
                })
                .retryWhen(dynamicRetry(node))
                .block();
    }

    private Retry dynamicRetry(int node) {
        return Retry.from(companion -> companion
                .flatMap(rs -> {
                            String msg = rs.failure().getLocalizedMessage();
                            log.warn("Retry {} node", node);
                            if (msg.contains("too early or too late")) {
                                try {
                                    Params params = om.readValue(msg, ErrorDto.class).issues().get(0).params();

                                    int before = params.expected().before();
                                    int after = params.expected().after();
                                    if (params.actual() < after) {
                                        int delay = after - params.actual();
                                        log.warn("Added delay {}", delay);
                                        return Mono.delay(ofMillis(delay));
                                    } else if (params.actual() > before) {
                                        return Mono.error(() -> new RuntimeException("Too late"));
                                    } else {
                                        //should not occur
                                        return Mono.error(() -> new RuntimeException("Unexpected"));
                                    }
                                } catch (Exception exc) {
                                    return Mono.error(() -> new RuntimeException(exc));
                                }
                            } else if (msg.contains("currently unavailable")) {
                                return Mono.delay(ofMillis(50));
                            } else {
                                return Mono.error(rs.failure());
                            }
                        }
                ));
    }
}
