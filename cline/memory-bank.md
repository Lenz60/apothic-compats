# MEMORY BANK — Apothic Compats 1.20.1 Backport (Part 1 of 3)
(Continued in memory-bank-2.md and memory-bank-3.md)

## 1. TASK
Backport `apothic-compats` v0.2.6 from **MC 1.21.1 / NeoForge 21.1.249** to **MC 1.20.1 / Forge 47.4.10**.
- Branch `1.21.1` = original NeoForge mod (untouched reference).
- Branch `1.20.1` = the backport (stripped build for the user's modlist lives here, uncommitted).
- Branch `1.20.1-full-compat` = backup of the full-compat backport BEFORE stripping absent-mod content.
- The mod is an Apotheosis compatibility addon: ~95% datapack content (affixes, gems, gear sets, affix loot entries, salvaging recipes, tags), plus runtime code (custom affix types, loot categories, attributes, curio items, mixins, events).

## 2. BUILD ENVIRONMENT
- **Toolchain**: ForgeGradle 6 (`net.minecraftforge.gradle [6.0,6.2)`) + Parchment Librarian + Gradle **8.5** wrapper + **JDK 17** (via `org.gradle.java.home=C:/Users/Rafly/Documents/Project/_ref/jdk17/jdk-17.0.21+9` in gradle.properties). System java is JDK 24 (too new for Gradle 8.5 daemon).
- **ForgeGradle cache seeding** (bypasses hanging CDN downloads — user downloaded artifacts manually):
  - `%USERPROFILE%\.gradle\caches\forge_gradle\maven_downloader\net\minecraftforge\forge\1.20.1-47.4.10\forge-1.20.1-47.4.10-userdev.jar` + `.md5` (32-char hex, no newline)
  - `...\maven_downloader\de\oceanlabs\mcp\mcp_config\1.20.1-20230612.114412\mcp_config-1.20.1-20230612.114412.zip` + `.md5`
  - MCP config version comes from the userdev jar's `config.json` (`"mcp": "de.oceanlabs.mcp:mcp_config:1.20.1-20230612.114412@zip"`). NOTE: `...114421` (wrong) vs `...114412` (correct).
- **Parchment**: `mappings channel: 'parchment', version: "<date>-<mcver>"` = `2023.07.16-1.20.1` — DATE FIRST. Librarian throws "Parchment version ... is invalid" if reversed.
- **Reference sources**: `_ref\apotheosis-1.20` and `_ref\placebo-1.20` (GitHub clones, branch 1.20). Jars, converter scripts, build logs also in `_ref\`. `javap` at `C:\Program Files\Java\jdk-24\bin\javap.exe`.
- **Repos in build.gradle**: mavenCentral, cursemaven, api.modrinth.com/maven, maven.blamejared.com, maven.shadowsoffire.dev, maven.theillusivec4.top, maven.architectury.dev. All mod deps wrapped in `fg.deobf(...)`.
- **Builds**: `gradlew build` (includes compileJava). Jar: `build\libs\apothic_compats-0.2.6.jar`.

## 3. DEPENDENCY VERSIONS (gradle.properties)
- apotheosis 7.4.8, placebo 8.6.3, attributeslib(ApothicAttributes) 1.3.5, curios 5.2.0-beta.3+1.20.1
- compile compat: aether 6134920 (cursemaven 255308), malum 6646111 (484064), create 7178761 (328085), irons-artifice 1.20.1-1.1.1 (modrinth), lodestone **1.20.1-[IP_ADDRESS]** (modrinth lodestonelib — Malum 1.6.7 requires `[1.20.1-1.6.4,)`; 1.8.x renamed ModCombatItem→LodestoneCombatItem)
- runtime: supplementaries 9140345, amendments 8657397 (only needed for block mixins — REMOVED in strip)
- forge 47.4.10 (matches user's server), MC 1.20.1, parchment 2023.07.16