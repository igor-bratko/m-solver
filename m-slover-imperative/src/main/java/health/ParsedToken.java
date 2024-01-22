package health;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ParsedToken(int index, int total, long epoch, List<Barrier> barriers) {

    public record Barrier(BarrierType type, int[] on, int from, int until) {
    }

    public enum BarrierType {
        DEPENDENCY,
        TIME
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class RequestState {
        private List<String> initRaw;
        private List<ParsedToken> initParsed;
        private Map<Integer, List<ParsedToken>> grouped;
        private String[] proofOfWork;
        private Map<Integer, Set<Integer>> dependencies;
        private Map<Integer, Set<Integer>> reverseDependencies;
    }

    public record SvgPart(boolean success, String payload) { }

    public record ErrorDto(List<Issue> issues) { }

    public record Issue(String code, String message, Params params) { }

    public record Params(Expected expected, int actual) { }

    public record Expected(int after, int before) { }

}
