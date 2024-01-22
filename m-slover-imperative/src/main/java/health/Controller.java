package health;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

@RequiredArgsConstructor
@RestController
public class Controller {
    private final MagicSolver solver;

    @GetMapping(value = "/magic/{magic}", produces = "image/svg")
    private String solve(@PathVariable String magic) {
        String[] parts = solver.solve(magic);

        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (nonNull(part)) {
                sb.append(part);
            }
        }

        return new String(Base64.getDecoder().decode(sb.toString().getBytes()), UTF_8);
    }
}