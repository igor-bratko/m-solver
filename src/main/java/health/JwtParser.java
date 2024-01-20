package health;

import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import health.ParsedToken.RequestState;
import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
public class JwtParser {
    private final ObjectMapper om;

    public RequestState parse(List<String> jwts) {
        List<ParsedToken> parsed = jwts.stream().map(jwt -> {
            try {
                String payload = JWT.decode(jwt).getPayload();
                String decoded = new String(Base64.getDecoder().decode(payload.getBytes()));

                return om.readValue(decoded, ParsedToken.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        return new RequestState()
                .initRaw(jwts)
                .initParsed(parsed)
                .proofOfWork(new String[jwts.size()]);
    }
}
