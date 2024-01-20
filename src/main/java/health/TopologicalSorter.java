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
        int[] inDegrees = new int[parsed.size()];

        for (ParsedToken response : parsed) {
            int nodeIdx = response.index();
            adjList.computeIfAbsent(nodeIdx, k -> new ArrayList<>());
            for (Barrier barrier : response.barriers()) {
                if (barrier.on() != null) {
                    for (int dep : barrier.on()) {
                        adjList.get(nodeIdx).add(dep);
                        inDegrees[dep]++;
                    }
                }
            }
        }

        Map<Integer, List<ParsedToken>> res = groupByLevels(adjList, inDegrees, parsed);

        log.info("Levels {}, nodes: {} - {} ms", res.size(), parsed.size(), System.currentTimeMillis() - l);

        return state.grouped(res);
    }

    private static Map<Integer, List<ParsedToken>> groupByLevels(
            Map<Integer, List<Integer>> graph,
            int[] inDegrees,
            List<ParsedToken> initResponses
    ) {
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < inDegrees.length; i++) {
            if (inDegrees[i] == 0) {
                queue.add(i);
            }
        }

        Map<Integer, List<ParsedToken>> levelGroups = new LinkedHashMap<>();
        List<List<ParsedToken>> allLevels = new ArrayList<>();
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<ParsedToken> currentLevel = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                Integer node = queue.poll();
                currentLevel.add(initResponses.get(node));

                for (Integer neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    inDegrees[neighbor]--;
                    if (inDegrees[neighbor] == 0) {
                        queue.add(neighbor);
                    }
                }
            }
            allLevels.add(currentLevel);
        }


        int level = 0;
        for (int i = allLevels.size() - 1; i >= 0; i--) {
            levelGroups.put(level++, allLevels.get(i));
        }

        return levelGroups;
    }



}

