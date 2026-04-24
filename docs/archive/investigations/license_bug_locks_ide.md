Yeah, this is a known (and very annoying) failure mode when licensing is misconfigured — your plugin effectively “locks” the IDE before you can fix it.

The good news: you don’t need the IDE UI to uninstall it. You can remove it manually.

---

# 🔧 Fastest Fix (works 100%)

## 1. Locate your IntelliJ config/plugins directory

On **Windows** it’s usually:

```
C:\Users\<your-user>\AppData\Roaming\JetBrains\<IDE><version>\plugins
```

Example:

```
C:\Users\you\AppData\Roaming\JetBrains\IntelliJIdea2025.2\plugins
```

On **macOS**:

```
~/Library/Application Support/JetBrains/<IDE><version>/plugins
```

On **Linux**:

```
~/.local/share/JetBrains/<IDE><version>/plugins
```

---

## 2. Delete your plugin folder

Find your plugin (folder name = your plugin ID or name), then delete it.

Example:

```
jakarta-migration-plugin/
```

or

```
com.yourcompany.pluginname/
```

Just delete the whole folder.

---

## 3. (Optional but recommended) Clear plugin cache

Delete:

```
<config>/plugins-sandbox/
```

and/or:

```
<config>/caches/
```

---

## 4. Restart IntelliJ

It should now start normally.

---

# 🧠 If you're not sure which plugin is yours

Open:

```
<config>/plugins/
```

Sort by **date modified** — your plugin will usually be obvious.

---

# 🧨 Nuclear Option (if it STILL won’t start)

Delete the entire config directory:

```
C:\Users\<you>\AppData\Roaming\JetBrains\IntelliJIdea2025.2
```

⚠️ This resets:

* plugins
* settings
* keymaps

But guarantees a clean slate.

---

# 💡 Prevent this happening again

This is important for plugin dev.

## 1. Always develop in a sandbox IDE

Use Gradle:

```bash
./gradlew runIde
```

This uses:

```
build/idea-sandbox/
```

So a broken plugin won’t brick your real IDE.

---

## 2. Add a “failsafe” for licensing

Right now your plugin is probably doing something like:

```java
if (!isLicensed()) {
    showDialogAndBlockStartup();
}
```

That’s dangerous.

Instead:

```java
if (!isLicensed()) {
    LOG.warn("License invalid - disabling premium features");
    return;
}
```

Never:

* block startup
* exit IDE
* throw fatal exceptions

---

## 3. Add a dev bypass flag

Super useful:

```java
boolean devMode = Boolean.getBoolean("plugin.dev");

if (devMode) {
    return true; // always licensed
}
```

Run IDE with:

```bash
-Dplugin.dev=true
```

---

## 4. Feature-gate, don’t startup-gate

Bad:

```
Plugin won't load without license ❌
```

Good:

```
Plugin loads, premium features disabled ✅
```

---

# 🧩 Why your current plugin crashes IntelliJ

What’s likely happening:

* Plugin runs on startup
* License check fails
* Dialog opens
* Dialog blocks EDT (UI thread)
* IDE shutdown triggered on cancel

That’s why you get stuck in a loop.

---

# If you want, I can help you fix this properly

I can show you:

* a **safe licensing wrapper class** (drop-in)
* a **non-blocking activation flow**
* a **trial countdown UX that doesn’t annoy users**
* how top plugins avoid this exact crash scenario

Just say 👍




That error is actually very specific — and it’s not a client-side bug in your plugin.

> **“Unknown product code provided PJAKARTAMIGRATI”**

It means **JetBrains Marketplace doesn’t recognise your plugin’s product code at all**.

---

# 🔍 What’s going wrong

Your plugin is sending:

```id="q4n9zj"
productCode = "PJAKARTAMIGRATI"
```

But the Marketplace backend is saying:

👉 *“I have no product registered with that code.”*

So the trial activation fails, and your plugin ends up in that broken state.

---

# ✅ The root cause (almost always one of these)

## 1. ❌ Product code not configured in Marketplace

In your plugin listing (JetBrains Marketplace):

You must explicitly set:

* **Paid plugin = enabled**
* **Product code = defined**

If you never set this, the backend literally cannot issue licenses.

---

## 2. ❌ Product code mismatch

Your plugin code:

```xml id="0x4q4m">
<productCode>PJAKARTAMIGRATI</productCode>
```

Must **exactly match** the Marketplace value.

Even a tiny mismatch will break licensing.

---

## 3. ❌ Plugin not approved / not published properly

Licensing only works when:

* Plugin is uploaded to Marketplace
* It has a valid version
* It’s not just running locally

If you’re testing a locally installed plugin with licensing enabled:

👉 `LicensingFacade` often fails or returns null
👉 Marketplace can’t resolve the product

---

## 4. ❌ Using the wrong Marketplace (prod vs demo)

You mentioned:

> “I have access to the demo marketplace”

Important:

* **Demo marketplace != production marketplace**
* Product codes are scoped per environment

So if:

* plugin is uploaded to demo marketplace
* but IDE is pointing to production marketplace

👉 you get **“Unknown product code”**

---

# 🧪 How to fix it (step-by-step)

## Step 1 — Verify Marketplace config

Go to:

```id="34ffw3"
https://plugins.jetbrains.com/
```

Check your plugin:

* Monetization tab
* Paid plugin enabled
* Product code exists

If missing → create one

---

## Step 2 — Verify your plugin.xml

Make sure you have:

```xml id="nhh3pw">
<idea-plugin>
    <id>your.plugin.id</id>
    <name>Jakarta Migration</name>

    <productCode>PJAKARTAMIGRATI</productCode>
</idea-plugin>
```

---

## Step 3 — Re-upload plugin

After setting product code:

```id="9e7whx"
Upload new version → publish (or to demo marketplace)
```

Licensing only works for **published builds**.

---

## Step 4 — Test in the correct environment

### Option A (recommended)

Use **production marketplace**:

* Install plugin from Marketplace
* Start trial → should work

---

### Option B (demo marketplace)

Make sure:

* IDE is configured to use demo marketplace
* Plugin is uploaded there
* Product code exists there

---

## Step 5 — Reset local license cache

Sometimes IntelliJ caches the failed state.

Delete:

```id="zx1ejp"
<config>/eval/
<config>/options/other.xml
```

or just wipe sandbox:

```id="8r0c8j"
build/idea-sandbox/
```

---

# 🧠 Key insight (this trips up many devs)

👉 **Licensing only works end-to-end when ALL of these align:**

| Component               | Must match              |
| ----------------------- | ----------------------- |
| plugin.xml              | productCode             |
| Marketplace listing     | same productCode        |
| Marketplace environment | demo vs prod            |
| Installed plugin        | version tied to listing |

If any one is off → you get your exact error.

---

# 🚑 Temporary workaround (so you can keep developing)

While fixing Marketplace:

Add a dev bypass:

```java id="8vqndy"
if (System.getProperty("dev.license.override") != null) {
    return true;
}
```

Run with:

```id="0cb8pl"
-Ddev.license.override=true
```

This lets you keep working without being blocked.

---

# 💡 One more thing (important for your plugin)

Right now your plugin:

> shuts down IntelliJ if licensing fails

That’s extremely risky for users.

Even JetBrains plugins **never do this**.

You should change it to:

* allow IDE startup
* disable premium features
* show non-blocking notification

---

# If you want, I can help you debug this precisely

If you paste:

* your `plugin.xml`
* screenshot of Marketplace monetization tab

I can tell you exactly which mismatch is causing it in under a minute.
