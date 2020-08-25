## 指令

以下举些常用指令的例子 (Powershell、CMD、Bash 通用), 更多信息请输入

```sh
java -jar Minesweeper.jar help
```

或针对 AI 测试的指令

```sh
java -jar Minesweeper.jar test --help
```

以查看.

```sh
# 以 GUI 运行
java -jar Minesweeper.jar
java -jar Minesweeper.jar gui

# 以 CLI 运行
java -jar Minesweeper.jar cli

# 测试 AI
java -jar Minesweeper.jar test
java -jar Minesweeper.jar test --times 1000 --difficulty 3 --rule xp
java -jar Minesweeper.jar test --times 250000 --difficulty exp --rule 7
java -jar Minesweeper.jar test --times 100 --rule XP
```

## 不同算法的胜率

| 策略 | 胜率 | 平均每局耗时 |
| ---- | ---- | ---- |
| 仅靠逻辑 (单格判断 + 双格判断 + 枚举判断)   | 5% 左右    | 4 ms 以下  |
| 逻辑 + 瞎猜                               | 23% 左右   | 4 ms 以下  |
| 逻辑 + 当前局面爆雷的概率 (雷率)           | 36% ~ 37%  | 6 ms       |
| 逻辑 + 雷率 + 一些小规律小策略             | 38.5%      | 6 ms       |
| 逻辑 + 雷率 + 小规律 + 下一局面预判        | 39.3%      | 8 ms 以上  |
| 逻辑 + 雷率 + 小规律 + 下局预判 + 残局胜率  | 39.6% 以上 | 16 ms 以上 |

## 当前版本胜率

```
private static final int MAX_NEXT_SITUATION_NUM = 15;
private static final int MAX_WIN_RATE_NUM = 12;
运行局数: 500000   胜率: 39.68%   运行总耗时: 21275秒   平均胜局耗时: 57毫秒   平均每局耗时: 42毫秒
探索程度统计:
A 占比
|
|
|
|
|
|  M                                       M
|  M                                   M   M
|  M                                   M   M
|  M   M   M   M   M   M   M   M   M   M   M
+---------------------------------------------> 探索程度
   0% 10% 20% 30% 40% 50% 60% 70% 80% 90% 100%
```
