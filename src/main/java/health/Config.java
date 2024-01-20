package health;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

@Slf4j
@Configuration
public class Config {

    @Bean
    public RouterFunction<ServerResponse> route(MagicSolver solver) {
        return RouterFunctions.route()
                .GET("/{magic}", req -> handle(solver, req))
                .build();
    }

    private static Mono<ServerResponse> handle(MagicSolver solver, ServerRequest req) {
        return solver.solve(req.pathVariable("magic"))
                .map(Config::toSvg)
                .flatMap(bytes -> ServerResponse.ok()
                        .contentType(MediaType.valueOf("image/svg"))
                        .bodyValue(bytes));
    }

    private static String toSvg(String[] result) {
        StringBuilder sb = new StringBuilder();

        for (String part : result) {
            if (nonNull(part)) {
                sb.append(part);
            }
        }
        return new String(Base64.getDecoder().decode(sb.toString().getBytes()), UTF_8);
    }

    @Bean
    public ApicoClient apicoClient(@Value("${apico.url}") String url,
                                   WebClient.Builder webClientBuilder,
                                   ObjectMapper om
    ) {
        WebClient webClient = webClientBuilder.baseUrl(url)
                .filter((req, next) -> Mono.fromSupplier(System::currentTimeMillis)
                        .flatMap(start -> next.exchange(req).doOnSuccess(resp -> log.debug(
                                "END  {}  {}", resp.statusCode(), System.currentTimeMillis() - start))
                        ))
                .build();
        return new ApicoClient(webClient, om);
    }

    @Bean
    public JwtParser parser(ObjectMapper om) {
        return new JwtParser(om);
    }
}
