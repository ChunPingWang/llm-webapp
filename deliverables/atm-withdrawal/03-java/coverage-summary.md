# 測試涵蓋率（JaCoCo）

需求:≥ 80%。實際結果:全項 **100%**。

| 指標 | 涵蓋 | 百分比 |
|------|------|--------|
| INSTRUCTION | 380/380 | **100%** |
| BRANCH | 26/26 | **100%** |
| LINE | 84/84 | **100%** |
| COMPLEXITY | 49/49 | **100%** |
| METHOD | 36/36 | **100%** |
| CLASS | 12/12 | **100%** |

- 測試總數:43(含 7 個 Cucumber BDD 場景,正向/反向)
- Cucumber 使用英文 annotation(@Given/@When/@Then/@And),feature 檔為繁體中文
- 重現:`cd java-project && mvn clean test`(JDK 21)
