package health;

import health.ParsedToken.RequestState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static health.ParsedToken.BarrierType.DEPENDENCY;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class MagicSolver {
    private final ApicoClient client;
    private final JwtParser parser;

    public Mono<String[]> solve(String magic) {
        return client.initRequest(magic)
                .map(parser::parse)
                .map(TopologicalSorter::sort)
                .flatMap(state -> Flux.fromIterable(state.grouped().entrySet())
                        .concatMap(entry -> processByLevel(magic, state, entry))
                        .collectList()
                        .thenReturn(state))
                .map(RequestState::proofOfWork);
    }

    private Mono<RequestState> processByLevel(
            String magic,
            RequestState state,
            Map.Entry<Integer, List<ParsedToken>> levels
    ) {
        return Flux.fromIterable(levels.getValue())
                .parallel()
                .flatMap(parsedToken -> {
                    int idx = parsedToken.index();

                    Map<Integer, String> pow = parsedToken.barriers().stream()
                            .filter(b -> b.type() == DEPENDENCY)
                            .flatMap(b -> IntStream.of(b.on()).boxed())
                            .collect(toMap(identity(), depId -> state.proofOfWork()[depId]));

                    return client.postMagic(idx, magic, state.initRaw().get(idx), pow)
                            .doOnNext(res -> state.proofOfWork()[idx] = res.payload());
                })
                .sequential()
                .then(Mono.just(state));
    }

}
