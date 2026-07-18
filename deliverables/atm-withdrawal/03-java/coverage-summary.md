# 測試涵蓋率（JaCoCo）

需求:≥ 80%。實際結果如下(全部達標)。

| 指標 | 涵蓋 | 百分比 |
|------|------|--------|
| INSTRUCTION | 517/522 | **99.0%** |
| BRANCH | 24/26 | **92.3%** |
| LINE | 115/117 | **98.3%** |
| COMPLEXITY | 57/60 | **95.0%** |
| METHOD | 46/47 | **97.9%** |
| CLASS | 13/14 | **92.9%** |

- 測試總數:41(含 7 個 Cucumber BDD 場景,正向/反向/場景大綱)
- Cucumber 使用英文 annotation(@Given/@When/@Then/@And);feature 檔為繁體中文
- 重現:`cd java-project && mvn clean test`(JDK 21)

> 備註:本次產生的 step definition 在「插卡後才設定餘額前置條件」的場景有載入時機問題,
> 已於  加一行重新載入卡片修正(見 git 記錄),使場景大綱與餘額不足情境正確通過。
