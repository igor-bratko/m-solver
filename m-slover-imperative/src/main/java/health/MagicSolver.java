package health;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import health.ParsedToken.RequestState;
import health.ParsedToken.SvgPart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

import static health.ParsedToken.BarrierType.DEPENDENCY;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
public class MagicSolver {
    private final ApicoClient client;
    private final ObjectMapper om;

    public String[] solve(String magic) {
        List<String> jwts = client.initRequest(magic);
        RequestState state = parse(jwts);
        buildDependencies(state);

        ConcurrentLinkedQueue<Integer> readyNodes = new ConcurrentLinkedQueue<>();
        Map<Integer, Set<Integer>> dependencies = state.dependencies();
        dependencies.forEach((node, deps) -> {
            if (deps.isEmpty()) {
                readyNodes.add(node);
            }
        });

        Set<Integer> processed = ConcurrentHashMap.newKeySet();
        while (!readyNodes.isEmpty() || !processed.containsAll(dependencies.keySet())) {
            Integer node = readyNodes.poll();
            if (node != null) {
                var future = CompletableFuture.runAsync(() -> {
                    getSvgPart(magic, node, state);
                    processed.add(node);
                    state.reverseDependencies().get(node).forEach(dependentNode -> {
                        if (processed.containsAll(dependencies.get(dependentNode))) {
                            readyNodes.add(dependentNode);
                        }
                    });
                });
                if (future.isCompletedExceptionally()) {
                    System.out.println(future.exceptionNow().getMessage());
                    break;
                }
            }
        }

        return state.proofOfWork();
    }

    private void getSvgPart(String magic, Integer idx, RequestState state) {
        ParsedToken node = state.initParsed().get(idx);

        Map<Integer, String> pow = node.barriers().stream()
                .filter(b -> b.type() == DEPENDENCY)
                .flatMap(b -> IntStream.of(b.on()).boxed())
                .collect(toMap(identity(), depId -> state.proofOfWork()[depId]));

        SvgPart svgPart = client.postMagic(idx, magic, state.initRaw().get(idx), pow);
        state.proofOfWork()[idx] = svgPart.payload();
    }

    private void buildDependencies(RequestState state) {
        Map<Integer, Set<Integer>> dependencies = new HashMap<>();
        Map<Integer, Set<Integer>> reverseDependencies = new HashMap<>();

        List<ParsedToken> nodes = state.initParsed();
        nodes.forEach(n -> {
            dependencies.put(n.index(), new HashSet<>());
            reverseDependencies.put(n.index(), new HashSet<>());
        });

        nodes.forEach(n -> n.barriers().stream()
                .filter(b -> nonNull(b.on()))
                .flatMap(b -> IntStream.of(b.on()).boxed())
                .forEach(d -> {
                    dependencies.get(n.index()).add(d);
                    reverseDependencies.get(d).add(n.index());
                }));

        state.dependencies(dependencies).reverseDependencies(reverseDependencies);
    }

    private RequestState parse(List<String> jwts) {
        List<ParsedToken> parsed = jwts.stream().map(jwt -> {
            try {
                String payload = JWT.decode(jwt).getPayload();
                String decoded = new String(Base64.getDecoder().decode(payload.getBytes()));

                return om.readValue(decoded, ParsedToken.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return new RequestState().initRaw(jwts).initParsed(parsed).proofOfWork(new String[jwts.size()]);
    }

}
