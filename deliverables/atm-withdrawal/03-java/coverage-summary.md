# 測試涵蓋率（JaCoCo）

需求:≥ 80%。實際結果:全項 **100%**。

| 指標 | 涵蓋 | 百分比 |
|------|------|--------|
| INSTRUCTION | 357/357 | **100%** |
| BRANCH | 16/16 | **100%** |
| LINE | 85/85 | **100%** |
| COMPLEXITY | 43/43 | **100%** |
| METHOD | 35/35 | **100%** |
| CLASS | 10/10 | **100%** |

- 測試總數:27(含 7 個 Cucumber BDD 場景)
- Cucumber 英文 annotation;feature 為繁體中文
- 重現:`cd java-project && mvn clean test`(JDK 21)

> 備註:本次生成的 step definition 有一組重複定義(@When 與 @And 同一 Cucumber 表達式),
> 已移除重複者使 Cucumber 不再拋 DuplicateStepDefinitionException(見 git 記錄)。
