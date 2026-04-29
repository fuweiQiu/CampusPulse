# Python for FHIR 範例

這個資料夾提供決賽規則要求的三個最小 Python 範例，直接對公有雲端 FHIR server `https://hapi.fhir.org/baseR4` 操作：

- `create_patient.py`
  建立 `Patient`，範例資料為蔣小名 / 男 / 1967-01-01
- `create_temperature_observation.py`
  針對指定病人建立一筆體溫 `Observation`
- `fetch_temperature_observations.py`
  調閱並呈現指定病人的體溫紀錄

## 執行方式

使用 Python 3.10+，不需要額外安裝套件。

```bash
cd python-fhir
python3 create_patient.py
python3 create_temperature_observation.py 52960712 --temperature 38
python3 fetch_temperature_observations.py 52960712
```

若本機 Python 憑證鏈設定不完整，也可暫時用 `--insecure` 做公開測試站驗證：

```bash
python3 create_patient.py --insecure
```

## 注意事項

- `hapi.fhir.org` 是公開測試伺服器，不適合存放真實個資或病歷資料。
- 公開測試資料可能會被定期清除，因此 patient id / observation id 不保證永久存在。
- 體溫 Observation 採用較標準的 FHIR vital signs 結構：
  - code: `LOINC 8310-5 Body temperature`
  - valueQuantity.unit: `C`
  - valueQuantity.code: `Cel`
