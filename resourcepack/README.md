# Paczka zasobów z nowymi modelami broni

`QualityArmory-21-axon.zip` to domyślna paczka QualityArmory dla 1.21.4 (`QualityArmory-21.zip`) z podmienionymi modelami AK47 (model 4), Remingtona (7) i M4A1-S (25). Reszta broni, dźwięki i numery modeli są bez zmian.

Wgranie (`plugins/QualityArmory/config.yml`):

```yaml
DefaultResourcepackOverride: true
DefaultResourcepack:
  '0': https://github.com/ZombieStriker/QualityArmory-Resourcepack/releases/download/latest/QualityArmory.zip
  1-21-4: https://raw.githubusercontent.com/toMiXgaming/AXON-Structure/refs/heads/claude/struktura-agro-pol-9egwdv/resourcepack/QualityArmory-21-axon.zip
```

Po scaleniu PR zamień `refs/heads/claude/struktura-agro-pol-9egwdv` na `refs/heads/main`.

`zrodla/` zawiera skrypty, które generują modele i tekstury (`python3 gen.py && python3 preview.py && python3 build_pack.py`); potrzebują rozpakowanej oryginalnej paczki QualityArmory.
