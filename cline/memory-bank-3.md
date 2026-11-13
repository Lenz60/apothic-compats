# MEMORY BANK — Part 3 of 3 (Drift, Fix Log, Strip Plan, Gotchas)

## 6. MALUM VERSION DRIFT (1.21.1 2.x vs 1.20.1 1.6.7)
- Missing in 1.6.7: item `soulwoven_silk` (recipe output — CRASH-causing for malum users; remap -> `soul_stained_steel_plating` planned), attributes `geas_limit`, `charge_capacity`, `charge_duration`, `soul_ward_recovery_rate` (2.x-only), mob effect `fiery_embrace` (1.6.7 calls it `ifrits_embrace`).
- Exists in 1.6.7 (lang format `attribute.name.malum.<id>`): soul_ward_capacity/integrity, scythe_proficiency, malignant_conversion, arcane_resonance, spirit_spoils (attribute in 1.6.7).
- NOTE: full-compat branch may still need these remaps for malum-1.6.7 users.

## 7. RUNTIME FIX LOG (chronological)
1. mods.toml: neoforge.mods.toml -> mods.toml, `mandatory=true`, MixinConfigs manifest attr -> fixed InvalidModFileException.
2. Mod class: no-arg ctor + FMLJavaModLoadingContext -> fixed NoSuchMethodException `<init>()`.
3. DeferredRegister lazy-init -> forceLoad pattern -> fixed IllegalStateException after RegisterEvent.
4. LootCategory validators: ModList-guarded (`s -> ModList.isLoaded(...) && instanceof`) -> fixed NoClassDefFoundError MalumScytheItem (packs without the mod).
5. Salvaging recipe crash: conditions key + missing item ids (see parts 1-2 and 6).
6. General principle: every mod touch is behind `ModList.get().isLoaded(...)` — compat mods are never "required"; only forge/minecraft/placebo/apotheosis are mandatory in mods.toml.

## 8. STRIP FOR USER'S MODLIST (planned & approved)
- User's pack ABSENT modids: malum, create, ae2, allthemodium, ars_nouveau, eternal_starlight, undergarden, mekanism(+tools), bumblezone, born_in_chaos_v1, alexscaves, alexsmobs, mowziesmobs, supplementaries, amendments, ancientreforging, lodestone.
- KEEP: aether, cataclysm, deep_aether, deeperdarker, twilightforest, curios, irons_artifice, farmersdelight, friendsandfoes, apothic_compats own content, vanilla.
- Strip = delete data JSONs whose conditions reference absent mods + alexscaves biome tags; delete CandleHolderBlockMixin/DoubleSkullBlockMixin/SkullCandleBlockMixin + mixins.json entries; drop moonlight/supplementaries/amendments deps from build.gradle + gradle.properties.

## 9. ENVIRONMENT GOTCHAS (Windows / Cline shell)
- PowerShell array elements = separate sessions: variables do NOT carry between commands -> put multi-step logic in a .ps1 file written via editor, execute with `powershell -ExecutionPolicy Bypass -File`.
- Multi-line here-strings in Cline command arrays: newline between statements sometimes eaten -> one Set-Content per array element, Set-Content LAST, no trailing Write-Output.
- Commas inside inline commands get shredded (parsed as array separators) -> never inline complex Replace() calls containing commas.
- `Set-Content -Encoding utf8` writes BOM -> strip EF BB BF from all sources (javac/Groovy choke). Editor-written files have no BOM.
- Editor tool fails matching CRLF files written by shell -> prefer script-based edits for shell-written files, editor for editor-written files.
- Inline PS string escaping of quotes/backticks is error-prone -> prefer [char]96-style or script files.
- Regex replacement strings: backtick-dollar escaping leaked a literal backtick once -> verify edited lines after replacements.
- Gradle build runs fine direct (fast) but background Start-Process + log polling survives tool timeouts.
- Modrinth API for version lookup; cfwidget for curse file IDs; user downloads stuck artifacts manually into the project folder when asked.
- Verify edited files after every scripted edit (Select-String / Get-Content).

## 10. USER RUNTIME
- FreesmLauncher instance "1.20.1v6 Cutemascraft [TEST]", Forge 47.4.10, Java 17, Sinytra Connector present (fabric mods), GTX 1660 Ti.
- Modlist includes: Apotheosis 7.4.8, Placebo 8.6.3, ApothicAttributes 1.3.7, ApothicCurios, ApothicCombat, Aether 1.5.2, Deep Aether 1.1.7, DeeperDarker 1.3.3, Cataclysm 3.31, Twilight Forest 4.3.2508, Curios 5.14.1, Iron's Artifice 1.1.1, Moonlight 2.16.36, Patchouli 85, Farmer's Delight, Friends&Foes, Iron's Spellbooks.
- Does NOT include: malum, create, ae2, mekanism, ars_nouveau, undergarden, allthemodium, eternal_starlight, supplementaries, amendments, lodestone.
- User prefers: manual gradle runs when builds stall; plan->act toggling; jar at build\libs\apothic_compats-0.2.6.jar.