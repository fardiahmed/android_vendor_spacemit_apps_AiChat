# A16_RISCV/vendor/spacemit/apps/AiChat: android16-riscv

Changes made for the Android 16 (AOSP, riscv64) bring-up of the BananaPi BPI-F3 (SpacemiT K1) and the BananaPi BPI-SM10 (SpacemiT K3), on branch `android16-riscv`.

AI Chat app: front end for the on-device llama-server.

## Changes

- **AiChat: add the AI Chat app**: Shows the local llama-server web UI, or how to add a model.

## Notes

- Enabled by `SPACEMIT_LLM` in device/spacemit/common/spacemit-features.mk. The server is vendor/spacemit/ai/llama.

## Build

```
source build/envsetup.sh
lunch aosp_bananapi_f3_tablet trunk_staging userdebug   # BPI-F3 (K1)
lunch aosp_bananapi_sm10_tablet trunk_staging userdebug # BPI-SM10 (K3), not booted yet
m AiChat
```
