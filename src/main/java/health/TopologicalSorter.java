package health;

import health.ParsedToken.Barrier;
import health.ParsedToken.RequestState;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class TopologicalSorter {

    public static RequestState sort(RequestState state) {
        long l = System.currentTimeMillis();

        List<ParsedToken> parsed = state.initParsed();

        Map<Integer, List<Integer>> adjList = new HashMap<>();

        for (ParsedToken response : parsed) {
            adjList.computeIfAbsent(response.index(), k -> new ArrayList<>());
            for (Barrier barrier : response.barriers()) {
                if (barrier.on() != null) {
                    for (int dep : barrier.on()) {
                        adjList.get(response.index()).add(dep);
                    }
                }
            }
        }

        Map<Integer, Integer> inDegrees = new HashMap<>();
        for (Integer node : adjList.keySet()) {
            inDegrees.put(node, 0);
        }
        for (List<Integer> neighbors : adjList.values()) {
            for (Integer neighbor : neighbors) {
                inDegrees.put(neighbor, inDegrees.get(neighbor) + 1);
            }
        }

        Map<Integer, List<ParsedToken>> res = groupByLevels(adjList, inDegrees, parsed);

        log.info("Levels {}, nodes: {} - {} ms", res.size(), parsed.size(), System.currentTimeMillis() - l);

        return state.grouped(res);
    }

    private static Map<Integer, List<ParsedToken>> groupByLevels(Map<Integer, List<Integer>> graph,
                                                                 Map<Integer, Integer> inDegrees,
                                                                 List<ParsedToken> initResponses) {
        Queue<Integer> queue = new LinkedList<>();
        inDegrees.forEach((node, degree) -> {
            if (degree == 0) {
                queue.add(node);
            }
        });

        Map<Integer, List<ParsedToken>> levelGroups = new LinkedHashMap<>();
        int level = 0;
        List<List<ParsedToken>> allLevels = new ArrayList<>();
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<ParsedToken> currentLevel = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                Integer node = queue.poll();
                currentLevel.add(initResponses.get(node));

                for (Integer neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    inDegrees.put(neighbor, inDegrees.get(neighbor) - 1);
                    if (inDegrees.get(neighbor) == 0) {
                        queue.add(neighbor);
                    }
                }
            }
            allLevels.add(currentLevel);
        }

        Collections.reverse(allLevels);
        for (List<ParsedToken> levelGroup : allLevels) {
            levelGroups.put(level++, levelGroup);
        }

        return levelGroups;
    }


}

