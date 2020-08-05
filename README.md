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