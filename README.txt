YHCC WALLET 3.0 — Android patch

This patch upgrades the existing YHCC_WALLET_Android project in place. It adds:
- Android launcher icon (adaptive icon + legacy density assets) using the supplied YHCC 3.0 artwork.
- A 3-second animated Compose launch screen with rotating gold rings, pulsing logo, and fade-in.
- App label/theme updates for YHCC WALLET 3.0.

Apply from the project root (~/YHCC/YHCC_WALLET_Android):

    unzip -o ~/storage/downloads/YHCC_WALLET_3_0_branding_patch.zip
    git add .
    git commit -m "Add YHCC WALLET 3.0 launcher icon and animated splash"
    git push

The patch does not include a prebuilt APK. GitHub Actions must complete successfully to produce the APK artifact.

Wallet notice: the current project is an interface/demo wallet. Editable balances are local display records; address QR generation and transaction drafts do not broadcast blockchain transactions or move funds.
