package com.relay.feature.tasks;

import com.google.gson.Gson;
import com.relay.RelayMod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 任务池：默认任务写在代码里，首次运行会导出到 config/relay-tasks.json 方便改文案。
 * 任务灵感部分来自 Yet Another Minecraft Bingo（物品收集/成套/任意一件）。
 */
public final class TaskPool {

    private final List<TaskDefinition> tasks = new ArrayList<>();

    public List<TaskDefinition> all() {
        return List.copyOf(tasks);
    }

    public Optional<TaskDefinition> byId(String id) {
        return tasks.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    public int size() {
        return tasks.size();
    }

    public static TaskPool load(Path file) {
        TaskPool pool = new TaskPool();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_JSON, StandardCharsets.UTF_8);
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            PoolDto dto = new Gson().fromJson(json, PoolDto.class);
            if (dto != null && dto.tasks != null) {
                for (TaskDto item : dto.tasks) {
                    try {
                        pool.tasks.add(new TaskDefinition(
                                item.id, item.category, item.title, item.description,
                                TaskType.valueOf(item.type), item.param,
                                item.target, item.timeLimit, item.successSeconds, item.failSeconds, item.tier));
                    } catch (Exception e) {
                        RelayMod.LOGGER.warn("任务 {} 配置有误，已跳过", item.id);
                    }
                }
            }
        } catch (Exception e) {
            RelayMod.LOGGER.error("读取任务池失败，使用内置默认值", e);
            pool.tasks.clear();
            pool.tasks.addAll(defaults());
        }
        if (pool.tasks.isEmpty()) {
            pool.tasks.addAll(defaults());
        }
        return pool;
    }

    public static List<TaskDefinition> defaults() {
        PoolDto dto = new Gson().fromJson(DEFAULT_JSON, PoolDto.class);
        List<TaskDefinition> list = new ArrayList<>();
        for (TaskDto item : dto.tasks) {
            list.add(new TaskDefinition(item.id, item.category, item.title, item.description,
                    TaskType.valueOf(item.type), item.param,
                    item.target, item.timeLimit, item.successSeconds, item.failSeconds, item.tier));
        }
        return list;
    }

    private static final class PoolDto {
        List<TaskDto> tasks;
    }

    private static final class TaskDto {
        String id;
        String category;
        String title;
        String description;
        String type;
        String param = "";
        int target = 1;
        int timeLimit = 240;
        int successSeconds = 60;
        int failSeconds = 60;
        String tier = "C";
    }

    private static final String DEFAULT_JSON = """
            {
              "tasks": [
                            {"id":"EXP-001","category":"探索","title":"离开舒适区","description":"距离任务开始点至少 300 格","type":"DISTANCE","param":"","target":300,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"EXP-002","category":"探索","title":"长途跋涉","description":"距离任务开始点至少 600 格","type":"DISTANCE","param":"","target":600,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"EXP-003","category":"探索","title":"下界一游","description":"进入下界","type":"DIMENSION","param":"minecraft:the_nether","target":1,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"EXP-004","category":"探索","title":"末地一游","description":"进入末地","type":"DIMENSION","param":"minecraft:the_end","target":1,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"RES-001","category":"资源","title":"伐木工","description":"破坏 16 个原木（所有种类）","type":"BLOCK_BREAK","param":"#minecraft:logs","target":16,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-002","category":"资源","title":"采石场","description":"破坏 20 个石头","type":"BLOCK_BREAK","param":"minecraft:stone","target":20,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-003","category":"资源","title":"第一桶铁","description":"背包里拥有 3 个铁锭","type":"ITEM_COUNT","param":"minecraft:iron_ingot","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-004","category":"资源","title":"钻石！","description":"背包里拥有 1 个钻石","type":"ITEM_COUNT","param":"minecraft:diamond","target":1,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"RES-005","category":"资源","title":"羊毛工厂","description":"背包里拥有 8 个白色羊毛","type":"ITEM_COUNT","param":"minecraft:white_wool","target":8,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-006","category":"资源","title":"煤老板","description":"破坏 12 个煤矿石（含深板岩煤矿）","type":"BLOCK_BREAK","param":"#minecraft:coal_ores","target":12,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-007","category":"资源","title":"面包自由","description":"背包里拥有 6 个面包","type":"ITEM_COUNT","param":"minecraft:bread","target":6,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-008","category":"资源","title":"钓鱼佬","description":"背包里拥有 3 条鳕鱼","type":"ITEM_COUNT","param":"minecraft:cod","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-009","category":"资源","title":"沙雕","description":"破坏 24 个沙子","type":"BLOCK_BREAK","param":"minecraft:sand,minecraft:red_sand","target":24,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-010","category":"资源","title":"甘蔗田","description":"背包里拥有 12 个甘蔗","type":"ITEM_COUNT","param":"minecraft:sugar_cane","target":12,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-011","category":"资源","title":"皮革贩子","description":"背包里拥有 8 个皮革","type":"ITEM_COUNT","param":"minecraft:leather","target":8,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-012","category":"资源","title":"火药桶","description":"背包里拥有 8 个火药","type":"ITEM_COUNT","param":"minecraft:gunpowder","target":8,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-013","category":"资源","title":"捡骨头","description":"背包里拥有 8 根骨头","type":"ITEM_COUNT","param":"minecraft:bone","target":8,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-014","category":"资源","title":"蜘蛛丝","description":"背包里拥有 8 根线","type":"ITEM_COUNT","param":"minecraft:string","target":8,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-015","category":"资源","title":"黏土工","description":"背包里拥有 12 个黏土球","type":"ITEM_COUNT","param":"minecraft:clay_ball","target":12,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"SET-001","category":"套装","title":"一整套木质工具","description":"同时拥有木斧、木锄、木镐、木锹、木剑","type":"ITEM_SET","param":"minecraft:wooden_axe,minecraft:wooden_hoe,minecraft:wooden_pickaxe,minecraft:wooden_shovel,minecraft:wooden_sword","target":5,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"SET-002","category":"套装","title":"一整套石质工具","description":"同时拥有石斧、石锄、石镐、石锹、石剑","type":"ITEM_SET","param":"minecraft:stone_axe,minecraft:stone_hoe,minecraft:stone_pickaxe,minecraft:stone_shovel,minecraft:stone_sword","target":5,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"SET-003","category":"套装","title":"一整套皮革甲","description":"皮革帽子、上衣、裤子、靴子各一件","type":"ITEM_SET","param":"minecraft:leather_helmet,minecraft:leather_chestplate,minecraft:leather_leggings,minecraft:leather_boots","target":4,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"SET-004","category":"套装","title":"一整套铁质工具","description":"同时拥有铁斧、铁锄、铁镐、铁锹、铁剑","type":"ITEM_SET","param":"minecraft:iron_axe,minecraft:iron_hoe,minecraft:iron_pickaxe,minecraft:iron_shovel,minecraft:iron_sword","target":5,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"SET-005","category":"套装","title":"所有种类的鸡蛋","description":"普通蛋、棕色蛋、蓝色蛋各一个","type":"ITEM_SET","param":"minecraft:egg,minecraft:brown_egg,minecraft:blue_egg","target":3,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ANY-001","category":"任意","title":"任意羊毛","description":"拿到任意颜色的羊毛","type":"ITEM_ANY","param":"minecraft:white_wool,minecraft:orange_wool,minecraft:magenta_wool,minecraft:light_blue_wool,minecraft:yellow_wool,minecraft:lime_wool,minecraft:pink_wool,minecraft:gray_wool,minecraft:light_gray_wool,minecraft:cyan_wool,minecraft:purple_wool,minecraft:blue_wool,minecraft:brown_wool,minecraft:green_wool,minecraft:red_wool,minecraft:black_wool","target":1,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-002","category":"任意","title":"任意花","description":"采到任意一种花","type":"ITEM_ANY","param":"minecraft:poppy,minecraft:dandelion,minecraft:cornflower,minecraft:azure_bluet,minecraft:oxeye_daisy,minecraft:allium,minecraft:blue_orchid,minecraft:lily_of_the_valley,minecraft:red_tulip,minecraft:orange_tulip,minecraft:white_tulip,minecraft:pink_tulip,minecraft:sunflower,minecraft:lilac,minecraft:rose_bush,minecraft:peony","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-003","category":"任意","title":"任意床","description":"做出任意颜色的床","type":"ITEM_ANY","param":"minecraft:white_bed,minecraft:orange_bed,minecraft:magenta_bed,minecraft:light_blue_bed,minecraft:yellow_bed,minecraft:lime_bed,minecraft:pink_bed,minecraft:gray_bed,minecraft:light_gray_bed,minecraft:cyan_bed,minecraft:purple_bed,minecraft:blue_bed,minecraft:brown_bed,minecraft:green_bed,minecraft:red_bed,minecraft:black_bed","target":1,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-004","category":"任意","title":"任意染料","description":"拿到任意一种染料","type":"ITEM_ANY","param":"minecraft:white_dye,minecraft:orange_dye,minecraft:magenta_dye,minecraft:light_blue_dye,minecraft:yellow_dye,minecraft:lime_dye,minecraft:pink_dye,minecraft:gray_dye,minecraft:light_gray_dye,minecraft:cyan_dye,minecraft:purple_dye,minecraft:blue_dye,minecraft:brown_dye,minecraft:green_dye,minecraft:red_dye,minecraft:black_dye","target":1,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-005","category":"任意","title":"任意珊瑚","description":"拿到任意一种珊瑚","type":"ITEM_ANY","param":"minecraft:brain_coral,minecraft:bubble_coral,minecraft:fire_coral,minecraft:horn_coral,minecraft:tube_coral","target":1,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ANY-006","category":"任意","title":"任意蛙明灯","description":"拿到任意一种蛙明灯","type":"ITEM_ANY","param":"minecraft:ochre_froglight,minecraft:pearlescent_froglight,minecraft:verdant_froglight","target":1,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-001","category":"战斗","title":"清场","description":"击杀 8 只僵尸","type":"MOB_KILL","param":"minecraft:zombie","target":8,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-002","category":"战斗","title":"苦力怕克星","description":"击杀 3 只苦力怕","type":"MOB_KILL","param":"minecraft:creeper","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-003","category":"战斗","title":"白骨精","description":"击杀 6 只骷髅","type":"MOB_KILL","param":"minecraft:skeleton","target":6,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-004","category":"战斗","title":"蜘蛛恐惧症","description":"击杀 5 只蜘蛛","type":"MOB_KILL","param":"minecraft:spider","target":5,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"FGT-005","category":"战斗","title":"清理溺尸","description":"击杀 3 只溺尸","type":"MOB_KILL","param":"minecraft:drowned","target":3,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-006","category":"战斗","title":"下界巡逻","description":"击杀 3 只猪灵","type":"MOB_KILL","param":"minecraft:piglin","target":3,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-007","category":"战斗","title":"猎猪","description":"击杀 2 只疣猪兽","type":"MOB_KILL","param":"minecraft:hoglin","target":2,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-008","category":"战斗","title":"海底守卫","description":"击杀 1 只守卫者","type":"MOB_KILL","param":"minecraft:guardian","target":1,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-009","category":"战斗","title":"凋灵骷髅","description":"击杀 2 只凋灵骷髅","type":"MOB_KILL","param":"minecraft:wither_skeleton","target":2,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ADV-001","category":"进度","title":"石器时代","description":"完成进度「石器时代」","type":"ADVANCEMENT","param":"minecraft:story/mine_stone","target":1,"timeLimit":300,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ADV-002","category":"进度","title":"来硬的","description":"完成进度「来硬的」","type":"ADVANCEMENT","param":"minecraft:story/smelt_iron","target":1,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ADV-003","category":"进度","title":"我们需要再深入些","description":"完成进度「我们需要再深入些」","type":"ADVANCEMENT","param":"minecraft:story/enter_the_nether","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ADV-004","category":"进度","title":"钻石时代","description":"完成进度「钻石！」","type":"ADVANCEMENT","param":"minecraft:story/mine_diamond","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ADV-005","category":"进度","title":"与火共舞","description":"完成进度「与火共舞」","type":"ADVANCEMENT","param":"minecraft:story/obtain_blaze_rod","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ADV-006","category":"进度","title":"附魔师","description":"完成进度「附魔师」","type":"ADVANCEMENT","param":"minecraft:story/enchant_item","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ADV-007","category":"进度","title":"隔墙有眼","description":"完成进度「隔墙有眼」","type":"ADVANCEMENT","param":"minecraft:story/follow_ender_eye","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"STA-001","category":"统计","title":"钓鱼佬进阶","description":"钓到 4 条鱼（统计）","type":"STAT","param":"minecraft:fish_caught","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-002","category":"统计","title":"水手","description":"坐船航行 300 格","type":"STAT","param":"minecraft:boat_one_cm","target":30000,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-003","category":"统计","title":"骑马旅行","description":"骑马行进 300 格","type":"STAT","param":"minecraft:horse_one_cm","target":30000,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"STA-004","category":"统计","title":"敲钟人","description":"敲钟 5 次","type":"STAT","param":"minecraft:bell_ring","target":5,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"STA-005","category":"统计","title":"以物易物","description":"和村民交易 3 次","type":"STAT","param":"minecraft:traded_with_villager","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-006","category":"统计","title":"神射手","description":"射中靶子 5 次","type":"STAT","param":"minecraft:target_hit","target":5,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-016","category":"资源","title":"白桦伐木","description":"破坏 16 个白桦原木","type":"BLOCK_BREAK","param":"minecraft:birch_log","target":16,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-017","category":"资源","title":"深板岩","description":"破坏 32 个深板岩","type":"BLOCK_BREAK","param":"minecraft:deepslate","target":32,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-018","category":"资源","title":"下界岩","description":"破坏 32 个下界岩","type":"BLOCK_BREAK","param":"minecraft:netherrack","target":32,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-019","category":"资源","title":"铜矿工","description":"破坏 12 个铜矿石（含深板岩铜矿）","type":"BLOCK_BREAK","param":"#minecraft:copper_ores","target":12,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-020","category":"资源","title":"铁矿工","description":"破坏 12 个铁矿石（含深板岩铁矿）","type":"BLOCK_BREAK","param":"#minecraft:iron_ores","target":12,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-021","category":"资源","title":"金锭","description":"背包里拥有 6 个金锭","type":"ITEM_COUNT","param":"minecraft:gold_ingot","target":6,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"RES-022","category":"资源","title":"红石","description":"背包里拥有 16 个红石","type":"ITEM_COUNT","param":"minecraft:redstone","target":16,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-023","category":"资源","title":"青金石","description":"背包里拥有 9 个青金石","type":"ITEM_COUNT","param":"minecraft:lapis_lazuli","target":9,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-024","category":"资源","title":"末影珍珠","description":"背包里拥有 2 个末影珍珠","type":"ITEM_COUNT","param":"minecraft:ender_pearl","target":2,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"RES-025","category":"资源","title":"烈焰棒","description":"背包里拥有 2 根烈焰棒","type":"ITEM_COUNT","param":"minecraft:blaze_rod","target":2,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"RES-026","category":"资源","title":"黑曜石","description":"背包里拥有 4 个黑曜石","type":"ITEM_COUNT","param":"minecraft:obsidian","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-027","category":"资源","title":"蘑菇","description":"背包里拥有 8 个红蘑菇","type":"ITEM_COUNT","param":"minecraft:red_mushroom","target":8,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"FGT-010","category":"战斗","title":"末影人","description":"击杀 2 只末影人","type":"MOB_KILL","param":"minecraft:enderman","target":2,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-011","category":"战斗","title":"女巫","description":"击杀 1 只女巫","type":"MOB_KILL","param":"minecraft:witch","target":1,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-012","category":"战斗","title":"恶魂","description":"击杀 1 只恶魂","type":"MOB_KILL","param":"minecraft:ghast","target":1,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-013","category":"战斗","title":"岩浆怪","description":"击杀 4 只岩浆怪","type":"MOB_KILL","param":"minecraft:magma_cube","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-014","category":"战斗","title":"掠夺者","description":"击杀 3 只掠夺者","type":"MOB_KILL","param":"minecraft:pillager","target":3,"timeLimit":300,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"EXP-005","category":"探索","title":"远征","description":"距离任务开始点至少 1000 格","type":"DISTANCE","param":"","target":1000,"timeLimit":360,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ANY-007","category":"任意","title":"任意树苗","description":"拿到任意一种树苗","type":"ITEM_ANY","param":"minecraft:oak_sapling,minecraft:spruce_sapling,minecraft:birch_sapling,minecraft:jungle_sapling,minecraft:acacia_sapling,minecraft:dark_oak_sapling,minecraft:cherry_sapling,minecraft:pale_oak_sapling,minecraft:mangrove_propagule","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-008","category":"任意","title":"任意鱼","description":"拿到任意一种鱼","type":"ITEM_ANY","param":"minecraft:cod,minecraft:salmon,minecraft:tropical_fish,minecraft:pufferfish","target":1,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-009","category":"任意","title":"任意蘑菇","description":"拿到任意一种蘑菇","type":"ITEM_ANY","param":"minecraft:red_mushroom,minecraft:brown_mushroom","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-010","category":"任意","title":"任意带釉陶瓦","description":"拿到任意一种带釉陶瓦","type":"ITEM_ANY","param":"minecraft:white_glazed_terracotta,minecraft:orange_glazed_terracotta,minecraft:magenta_glazed_terracotta,minecraft:light_blue_glazed_terracotta,minecraft:yellow_glazed_terracotta,minecraft:lime_glazed_terracotta,minecraft:pink_glazed_terracotta,minecraft:gray_glazed_terracotta,minecraft:light_gray_glazed_terracotta,minecraft:cyan_glazed_terracotta,minecraft:purple_glazed_terracotta,minecraft:blue_glazed_terracotta,minecraft:brown_glazed_terracotta,minecraft:green_glazed_terracotta,minecraft:red_glazed_terracotta,minecraft:black_glazed_terracotta","target":1,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"SET-006","category":"套装","title":"一整套金质工具","description":"同时拥有金斧、金锄、金镐、金锹、金剑","type":"ITEM_SET","param":"minecraft:golden_axe,minecraft:golden_hoe,minecraft:golden_pickaxe,minecraft:golden_shovel,minecraft:golden_sword","target":5,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"SET-007","category":"套装","title":"所有种子","description":"小麦种子、西瓜种子、南瓜种子、甜菜种子各一个","type":"ITEM_SET","param":"minecraft:wheat_seeds,minecraft:melon_seeds,minecraft:pumpkin_seeds,minecraft:beetroot_seeds","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"SET-008","category":"套装","title":"所有鱼类","description":"鳕鱼、鲑鱼、热带鱼、河豚各一条","type":"ITEM_SET","param":"minecraft:cod,minecraft:salmon,minecraft:tropical_fish,minecraft:pufferfish","target":4,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-007","category":"统计","title":"钓鱼大师","description":"钓到 8 条鱼（统计）","type":"STAT","param":"minecraft:fish_caught","target":8,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"EXP-006","category":"探索","title":"出发","description":"距离任务开始点至少 100 格","type":"DISTANCE","param":"","target":100,"timeLimit":180,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-030","category":"资源","title":"沙砾","description":"破坏 32 个沙砾","type":"BLOCK_BREAK","param":"minecraft:gravel","target":32,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-031","category":"资源","title":"黏土","description":"破坏 16 个黏土块","type":"BLOCK_BREAK","param":"minecraft:clay","target":16,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-032","category":"资源","title":"雪块","description":"破坏 16 个雪块","type":"BLOCK_BREAK","param":"minecraft:snow_block","target":16,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-033","category":"资源","title":"南瓜","description":"背包里拥有 6 个南瓜","type":"ITEM_COUNT","param":"minecraft:pumpkin","target":6,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-034","category":"资源","title":"苹果","description":"背包里拥有 6 个苹果","type":"ITEM_COUNT","param":"minecraft:apple","target":6,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-035","category":"资源","title":"木棍","description":"背包里拥有 32 根木棍","type":"ITEM_COUNT","param":"minecraft:stick","target":32,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-036","category":"资源","title":"煤炭","description":"背包里拥有 16 个煤炭","type":"ITEM_COUNT","param":"minecraft:coal","target":16,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"FGT-015","category":"战斗","title":"史莱姆","description":"击杀 6 只史莱姆","type":"MOB_KILL","param":"minecraft:slime","target":6,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"FGT-016","category":"战斗","title":"蠹虫","description":"击杀 5 只蠹虫","type":"MOB_KILL","param":"minecraft:silverfish","target":5,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"FGT-017","category":"战斗","title":"洞穴蜘蛛","description":"击杀 4 只洞穴蜘蛛","type":"MOB_KILL","param":"minecraft:cave_spider","target":4,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-011","category":"任意","title":"任意船","description":"拿到任意一种船","type":"ITEM_ANY","param":"minecraft:oak_boat,minecraft:spruce_boat,minecraft:birch_boat,minecraft:jungle_boat,minecraft:acacia_boat,minecraft:dark_oak_boat,minecraft:mangrove_boat,minecraft:cherry_boat,minecraft:pale_oak_boat","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-012","category":"任意","title":"任意告示牌","description":"拿到任意一种告示牌","type":"ITEM_ANY","param":"minecraft:oak_sign,minecraft:spruce_sign,minecraft:birch_sign,minecraft:jungle_sign,minecraft:acacia_sign,minecraft:dark_oak_sign,minecraft:mangrove_sign,minecraft:cherry_sign,minecraft:pale_oak_sign","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ANY-013","category":"任意","title":"任意按钮","description":"拿到任意一种按钮","type":"ITEM_ANY","param":"minecraft:oak_button,minecraft:spruce_button,minecraft:birch_button,minecraft:jungle_button,minecraft:acacia_button,minecraft:dark_oak_button,minecraft:mangrove_button,minecraft:cherry_button,minecraft:pale_oak_button,minecraft:stone_button,minecraft:polished_blackstone_button","target":1,"timeLimit":240,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"STA-008","category":"统计","title":"散步","description":"步行 500 格（统计）","type":"STAT","param":"minecraft:walk_one_cm","target":50000,"timeLimit":360,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"ADV-008","category":"进度","title":"甜蜜的梦","description":"完成进度「甜蜜的梦」","type":"ADVANCEMENT","param":"minecraft:adventure/sleep_in_bed","target":1,"timeLimit":300,"successSeconds":60,"failSeconds":60,"tier":"D"},
                            {"id":"RES-037","category":"资源","title":"鸡蛋","description":"背包里拥有 8 个鸡蛋","type":"ITEM_COUNT","param":"minecraft:egg","target":8,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-038","category":"资源","title":"荧石","description":"背包里拥有 8 个荧石","type":"ITEM_COUNT","param":"minecraft:glowstone","target":8,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-039","category":"资源","title":"黑石","description":"破坏 24 个黑石","type":"BLOCK_BREAK","param":"minecraft:blackstone","target":24,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-040","category":"资源","title":"玄武岩","description":"破坏 24 个玄武岩","type":"BLOCK_BREAK","param":"minecraft:basalt","target":24,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-041","category":"资源","title":"灵魂沙","description":"破坏 16 个灵魂沙","type":"BLOCK_BREAK","param":"minecraft:soul_sand","target":16,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-042","category":"资源","title":"海晶碎片","description":"背包里拥有 8 个海晶碎片","type":"ITEM_COUNT","param":"minecraft:prismarine_shard","target":8,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-043","category":"资源","title":"海晶灯","description":"背包里拥有 4 个海晶灯","type":"ITEM_COUNT","param":"minecraft:sea_lantern","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-044","category":"资源","title":"下界石英","description":"背包里拥有 12 个下界石英","type":"ITEM_COUNT","param":"minecraft:quartz","target":12,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-045","category":"资源","title":"紫水晶","description":"背包里拥有 4 个紫水晶碎片","type":"ITEM_COUNT","param":"minecraft:amethyst_shard","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-046","category":"资源","title":"铜锭","description":"背包里拥有 12 个铜锭","type":"ITEM_COUNT","param":"minecraft:copper_ingot","target":12,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-047","category":"资源","title":"造纸","description":"背包里拥有 16 张纸","type":"ITEM_COUNT","param":"minecraft:paper","target":16,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-048","category":"资源","title":"书本","description":"背包里拥有 3 本书","type":"ITEM_COUNT","param":"minecraft:book","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-049","category":"资源","title":"蜂蜜","description":"背包里拥有 2 个蜂蜜瓶","type":"ITEM_COUNT","param":"minecraft:honey_bottle","target":2,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-050","category":"资源","title":"甜浆果","description":"背包里拥有 8 个甜浆果","type":"ITEM_COUNT","param":"minecraft:sweet_berries","target":8,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-051","category":"资源","title":"南瓜派","description":"背包里拥有 2 个南瓜派","type":"ITEM_COUNT","param":"minecraft:pumpkin_pie","target":2,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-052","category":"资源","title":"西瓜","description":"背包里拥有 8 片西瓜","type":"ITEM_COUNT","param":"minecraft:melon_slice","target":8,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-018","category":"战斗","title":"尸壳","description":"击杀 4 只尸壳","type":"MOB_KILL","param":"minecraft:husk","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-019","category":"战斗","title":"流浪者","description":"击杀 4 只流浪者","type":"MOB_KILL","param":"minecraft:stray","target":4,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-020","category":"战斗","title":"幻翼","description":"击杀 2 只幻翼","type":"MOB_KILL","param":"minecraft:phantom","target":2,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-021","category":"战斗","title":"僵尸村民","description":"击杀 3 只僵尸村民","type":"MOB_KILL","param":"minecraft:zombie_villager","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-022","category":"战斗","title":"沼骸","description":"击杀 3 只沼骸","type":"MOB_KILL","param":"minecraft:bogged","target":3,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"FGT-023","category":"战斗","title":"旋风人","description":"击杀 2 只旋风人","type":"MOB_KILL","param":"minecraft:breeze","target":2,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-014","category":"任意","title":"任意地毯","description":"拿到任意一种地毯","type":"ITEM_ANY","param":"minecraft:white_carpet,minecraft:orange_carpet,minecraft:magenta_carpet,minecraft:light_blue_carpet,minecraft:yellow_carpet,minecraft:lime_carpet,minecraft:pink_carpet,minecraft:gray_carpet,minecraft:light_gray_carpet,minecraft:cyan_carpet,minecraft:purple_carpet,minecraft:blue_carpet,minecraft:brown_carpet,minecraft:green_carpet,minecraft:red_carpet,minecraft:black_carpet","target":1,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-015","category":"任意","title":"任意染色玻璃","description":"拿到任意一种染色玻璃","type":"ITEM_ANY","param":"minecraft:white_stained_glass,minecraft:orange_stained_glass,minecraft:magenta_stained_glass,minecraft:light_blue_stained_glass,minecraft:yellow_stained_glass,minecraft:lime_stained_glass,minecraft:pink_stained_glass,minecraft:gray_stained_glass,minecraft:light_gray_stained_glass,minecraft:cyan_stained_glass,minecraft:purple_stained_glass,minecraft:blue_stained_glass,minecraft:brown_stained_glass,minecraft:green_stained_glass,minecraft:red_stained_glass,minecraft:black_stained_glass","target":1,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-016","category":"任意","title":"任意陶瓦","description":"拿到任意一种陶瓦","type":"ITEM_ANY","param":"minecraft:terracotta,minecraft:white_terracotta,minecraft:orange_terracotta,minecraft:magenta_terracotta,minecraft:light_blue_terracotta,minecraft:yellow_terracotta,minecraft:lime_terracotta,minecraft:pink_terracotta,minecraft:gray_terracotta,minecraft:light_gray_terracotta,minecraft:cyan_terracotta,minecraft:purple_terracotta,minecraft:blue_terracotta,minecraft:brown_terracotta,minecraft:green_terracotta,minecraft:red_terracotta,minecraft:black_terracotta","target":1,"timeLimit":240,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ANY-017","category":"任意","title":"任意旗帜","description":"拿到任意一种旗帜","type":"ITEM_ANY","param":"minecraft:white_banner,minecraft:orange_banner,minecraft:magenta_banner,minecraft:light_blue_banner,minecraft:yellow_banner,minecraft:lime_banner,minecraft:pink_banner,minecraft:gray_banner,minecraft:light_gray_banner,minecraft:cyan_banner,minecraft:purple_banner,minecraft:blue_banner,minecraft:brown_banner,minecraft:green_banner,minecraft:red_banner,minecraft:black_banner","target":1,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ADV-009","category":"进度","title":"怪物猎人","description":"完成进度「怪物猎人」","type":"ADVANCEMENT","param":"minecraft:adventure/kill_a_mob","target":1,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ADV-010","category":"进度","title":"升级","description":"完成进度「升级」","type":"ADVANCEMENT","param":"minecraft:story/upgrade_tools","target":1,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ADV-011","category":"进度","title":"西装革履","description":"完成进度「西装革履」","type":"ADVANCEMENT","param":"minecraft:story/obtain_armor","target":1,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"ADV-012","category":"进度","title":"第一次交易","description":"完成进度「以物易物」","type":"ADVANCEMENT","param":"minecraft:adventure/trade","target":1,"timeLimit":360,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-009","category":"统计","title":"蹦蹦跳跳","description":"跳跃 100 次（统计）","type":"STAT","param":"minecraft:jump","target":100,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"STA-010","category":"统计","title":"翻箱倒柜","description":"打开箱子 10 次（统计）","type":"STAT","param":"minecraft:open_chest","target":10,"timeLimit":300,"successSeconds":90,"failSeconds":60,"tier":"C"},
                            {"id":"RES-053","category":"资源","title":"远古残骸","description":"破坏 1 个远古残骸","type":"BLOCK_BREAK","param":"minecraft:ancient_debris","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"RES-054","category":"资源","title":"下界合金碎片","description":"背包里拥有 1 个下界合金碎片","type":"ITEM_COUNT","param":"minecraft:netherite_scrap","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"RES-055","category":"资源","title":"恶魂之泪","description":"背包里拥有 2 个恶魂之泪","type":"ITEM_COUNT","param":"minecraft:ghast_tear","target":2,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-024","category":"战斗","title":"烈焰人","description":"击杀 3 只烈焰人","type":"MOB_KILL","param":"minecraft:blaze","target":3,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"FGT-025","category":"战斗","title":"卫道士","description":"击杀 2 只卫道士","type":"MOB_KILL","param":"minecraft:vindicator","target":2,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"SET-010","category":"套装","title":"一整套铜质工具","description":"同时拥有铜斧、铜锄、铜镐、铜锹、铜剑","type":"ITEM_SET","param":"minecraft:copper_axe,minecraft:copper_hoe,minecraft:copper_pickaxe,minecraft:copper_shovel,minecraft:copper_sword","target":5,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"SET-011","category":"套装","title":"一整套锁链甲","description":"锁链头盔、胸甲、护腿、靴子各一件","type":"ITEM_SET","param":"minecraft:chainmail_helmet,minecraft:chainmail_chestplate,minecraft:chainmail_leggings,minecraft:chainmail_boots","target":4,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"SET-012","category":"套装","title":"所有原木种类","description":"主世界九种原木各一个（不含菌柄）","type":"ITEM_SET","param":"minecraft:oak_log,minecraft:spruce_log,minecraft:birch_log,minecraft:jungle_log,minecraft:acacia_log,minecraft:dark_oak_log,minecraft:mangrove_log,minecraft:cherry_log,minecraft:pale_oak_log","target":9,"timeLimit":480,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ANY-018","category":"任意","title":"任意音乐唱片","description":"拿到任意一张音乐唱片","type":"ITEM_ANY","param":"minecraft:music_disc_13,minecraft:music_disc_cat,minecraft:music_disc_blocks,minecraft:music_disc_chirp,minecraft:music_disc_far,minecraft:music_disc_mall,minecraft:music_disc_mellohi,minecraft:music_disc_stal,minecraft:music_disc_strad,minecraft:music_disc_ward,minecraft:music_disc_11,minecraft:music_disc_wait,minecraft:music_disc_otherside,minecraft:music_disc_pigstep,minecraft:music_disc_relic","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ANY-019","category":"任意","title":"任意陶片","description":"拿到任意一种陶片","type":"ITEM_ANY","param":"minecraft:angler_pottery_sherd,minecraft:archer_pottery_sherd,minecraft:arms_up_pottery_sherd,minecraft:blade_pottery_sherd,minecraft:brewer_pottery_sherd,minecraft:burn_pottery_sherd,minecraft:danger_pottery_sherd,minecraft:explorer_pottery_sherd,minecraft:friend_pottery_sherd,minecraft:heart_pottery_sherd,minecraft:heartbreak_pottery_sherd,minecraft:howl_pottery_sherd,minecraft:miner_pottery_sherd,minecraft:mourn_pottery_sherd,minecraft:plenty_pottery_sherd,minecraft:prize_pottery_sherd,minecraft:sheaf_pottery_sherd,minecraft:shelter_pottery_sherd,minecraft:skull_pottery_sherd,minecraft:snort_pottery_sherd,minecraft:flow_pottery_sherd,minecraft:guster_pottery_sherd,minecraft:scrape_pottery_sherd","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"EXP-007","category":"探索","title":"长征","description":"距离任务开始点至少 800 格","type":"DISTANCE","param":"","target":800,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"STA-011","category":"统计","title":"矿车旅行","description":"坐矿车行进 300 格（统计）","type":"STAT","param":"minecraft:minecart_one_cm","target":30000,"timeLimit":360,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"STA-012","category":"统计","title":"长途骑马","description":"骑马行进 600 格（统计）","type":"STAT","param":"minecraft:horse_one_cm","target":60000,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ADV-013","category":"进度","title":"本地酿造","description":"完成进度「本地酿造」","type":"ADVANCEMENT","param":"minecraft:nether/brew_potion","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ADV-014","category":"进度","title":"隐藏的深处","description":"完成进度「隐藏的深处」","type":"ADVANCEMENT","param":"minecraft:nether/obtain_ancient_debris","target":1,"timeLimit":480,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"ADV-015","category":"进度","title":"结束了？","description":"完成进度「结束了？」（进入末地）","type":"ADVANCEMENT","param":"minecraft:story/enter_the_end","target":1,"timeLimit":420,"successSeconds":120,"failSeconds":60,"tier":"B"},
                            {"id":"RES-056","category":"资源","title":"下界合金锭","description":"背包里拥有 1 个下界合金锭","type":"ITEM_COUNT","param":"minecraft:netherite_ingot","target":1,"timeLimit":600,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-026","category":"战斗","title":"远古守卫者","description":"击杀 1 只远古守卫者","type":"MOB_KILL","param":"minecraft:elder_guardian","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-027","category":"战斗","title":"潜影贝","description":"击杀 2 只潜影贝","type":"MOB_KILL","param":"minecraft:shulker","target":2,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-028","category":"战斗","title":"猪灵蛮兵","description":"击杀 1 只猪灵蛮兵","type":"MOB_KILL","param":"minecraft:piglin_brute","target":1,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-029","category":"战斗","title":"劫掠兽","description":"击杀 1 只劫掠兽","type":"MOB_KILL","param":"minecraft:ravager","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"FGT-030","category":"战斗","title":"唤魔者","description":"击杀 1 只唤魔者","type":"MOB_KILL","param":"minecraft:evoker","target":1,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"SET-013","category":"套装","title":"一整套钻石工具","description":"同时拥有钻石斧、锄、镐、锹、剑","type":"ITEM_SET","param":"minecraft:diamond_axe,minecraft:diamond_hoe,minecraft:diamond_pickaxe,minecraft:diamond_shovel,minecraft:diamond_sword","target":5,"timeLimit":600,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"SET-014","category":"套装","title":"所有颜色的羊毛","description":"十六种颜色的羊毛各一个","type":"ITEM_SET","param":"minecraft:white_wool,minecraft:orange_wool,minecraft:magenta_wool,minecraft:light_blue_wool,minecraft:yellow_wool,minecraft:lime_wool,minecraft:pink_wool,minecraft:gray_wool,minecraft:light_gray_wool,minecraft:cyan_wool,minecraft:purple_wool,minecraft:blue_wool,minecraft:brown_wool,minecraft:green_wool,minecraft:red_wool,minecraft:black_wool","target":16,"timeLimit":600,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"SET-015","category":"套装","title":"所有染料","description":"十六种染料各一个","type":"ITEM_SET","param":"minecraft:white_dye,minecraft:orange_dye,minecraft:magenta_dye,minecraft:light_blue_dye,minecraft:yellow_dye,minecraft:lime_dye,minecraft:pink_dye,minecraft:gray_dye,minecraft:light_gray_dye,minecraft:cyan_dye,minecraft:purple_dye,minecraft:blue_dye,minecraft:brown_dye,minecraft:green_dye,minecraft:red_dye,minecraft:black_dye","target":16,"timeLimit":600,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ANY-020","category":"任意","title":"任意下界合金工具","description":"拿到任意一件下界合金工具","type":"ITEM_ANY","param":"minecraft:netherite_axe,minecraft:netherite_hoe,minecraft:netherite_pickaxe,minecraft:netherite_shovel,minecraft:netherite_sword","target":1,"timeLimit":600,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"EXP-008","category":"探索","title":"万里长征","description":"距离任务开始点至少 1500 格","type":"DISTANCE","param":"","target":1500,"timeLimit":420,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"STA-013","category":"统计","title":"钓鱼宗师","description":"钓到 12 条鱼（统计）","type":"STAT","param":"minecraft:fish_caught","target":12,"timeLimit":480,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ADV-016","category":"进度","title":"解放末地","description":"完成进度「解放末地」","type":"ADVANCEMENT","param":"minecraft:end/kill_dragon","target":1,"timeLimit":900,"successSeconds":180,"failSeconds":60,"tier":"A"},
                            {"id":"ADV-017","category":"进度","title":"天空极限","description":"完成进度「天空极限」（找到鞘翅）","type":"ADVANCEMENT","param":"minecraft:end/elytra","target":1,"timeLimit":900,"successSeconds":180,"failSeconds":60,"tier":"A"}
              ]
            }
            """;
}
