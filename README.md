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

## 当前版本胜率

```
运行局数: 250000   胜率: 39.44%   运行总耗时: 3600秒   平均胜局耗时: 17毫秒   平均每局耗时: 14毫秒
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