# Labels

Plain-text label dictionaries from `TITANICBHAI/AI-ASSISTANT-INCOMPLETE`.

## Why no `.tflite` model files are here

The original AII repo shipped several `.tflite` files (e.g. `ui_elements_detector.tflite`, `combat_effects_detector.tflite`, `environment_detector.tflite`, `item_detector.tflite`). The repo owner confirmed these were **fake / stub models**: file size correct, weights either random or absent. Loading them would not produce useful detections.

Only the **JSON metadata** describing what each model *was supposed to be* (`ml_models_metadata/`) is preserved, so a future port can train or source a real model with the same I/O contract.

## Directory layout

- `labels/` — extended class lists (UI elements, environments, items, combat effects, game-specific for COD / PUBG / Free Fire). Use these to define the output classes of your real custom-trained detector.
- `models/` — original COCO labels + per-game label maps + `labelmap.txt` index.
- `ml_models_metadata/` — JSON metadata for each AII detector (input shape, output shape, anchors, class count). Treat as a **specification** for what to train.

## How the spine uses labels

Once you have a real custom model, place it under `android/app/src/main/assets/models/` and update `core/perception/ObjectDetectorEngine.kt` to load it. Read the matching label file from `assets/labels/` to translate class indices to names. The spine already does this for the COCO MediaPipe model; the pattern is identical for a custom one.
