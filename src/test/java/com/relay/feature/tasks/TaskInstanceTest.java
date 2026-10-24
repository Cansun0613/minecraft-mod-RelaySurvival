package com.relay.feature.tasks;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskInstanceTest {

    private static TaskDefinition def(String id) {
        return new TaskDefinition(id, "测试", "标题" + id, "描述", TaskType.DISTANCE, "", 10, 60, 30, 30, "C");
    }

    private static double flat(TaskDefinition definition) {
        return 1.0;
    }

    @Test
    void progressIsCapped() {
        TaskInstance instance = new TaskInstance(def("A"));
        instance.addProgress(100);
        assertEquals(10, instance.progress());
        assertTrue(instance.done());
    }

    @Test
    void expiresAfterLimit() {
        TaskInstance instance = new TaskInstance(new TaskDefinition("B", "测试", "t", "d",
                TaskType.DISTANCE, "", 1, 2, 10, 10, "C"));
        instance.tickSecond();
        instance.tickSecond();
        assertTrue(instance.expired());
    }

    @Test
    void pickReturnsDistinctTasks() {
        List<TaskDefinition> pool = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            pool.add(def("T" + i));
        }
        List<TaskInstance> picked = TaskInstance.pick(pool, 4, new Random(42), List.of(), TaskInstanceTest::flat);
        assertEquals(4, picked.size());
        Set<String> ids = new HashSet<>();
        for (TaskInstance instance : picked) {
            ids.add(instance.definition().id());
        }
        assertEquals(4, ids.size());
    }

    @Test
    void pickAvoidsExcludedTasks() {
        List<TaskDefinition> pool = List.of(def("X"), def("Y"));
        List<TaskInstance> picked = TaskInstance.pick(pool, 2, new Random(1),
                List.of(new TaskInstance(def("X"))), TaskInstanceTest::flat);
        assertEquals(1, picked.size());
        assertEquals("Y", picked.get(0).definition().id());
    }

    @Test
    void lowWeightTiersAppearLessOften() {
        List<TaskDefinition> pool = List.of(
                new TaskDefinition("S1", "测试", "s", "d", TaskType.DISTANCE, "", 1, 60, 10, 10, "S"),
                new TaskDefinition("D1", "测试", "d", "d", TaskType.DISTANCE, "", 1, 60, 10, 10, "D"));
        Random random = new Random(7);
        int s = 0;
        int d = 0;
        for (int i = 0; i < 400; i++) {
            TaskInstance instance = TaskInstance.pick(pool, 1, random, List.of(),
                    def2 -> def2.tier().equals("S") ? 1.0 : 10.0).get(0);
            if (instance.definition().tier().equals("S")) {
                s++;
            } else {
                d++;
            }
        }
        assertTrue(d > s * 3, "D 级应明显比 S 级常见，实际 S=" + s + " D=" + d);
    }
}
