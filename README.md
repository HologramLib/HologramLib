<div align="center">
  <img width="650px" src="assets/banner.png" alt="HologramLib Banner">
  
  [![Discord](https://img.shields.io/badge/Discord_Server-7289DA?style=flat&logo=discord&logoColor=white)](https://discord.gg/2UTkYj26B4)
  [![Wiki](https://img.shields.io/badge/Documentation-Wiki-2dad10)](https://github.com/HologramLib/HologramLib/wiki)
  [![JitPack](https://jitpack.io/v/HologramLib/HologramLib.svg)](https://jitpack.io/#HologramLib/HologramLib)
  [![JavaDocs](https://img.shields.io/badge/API-Docs-2ECC71)](https://HologramLib.github.io/HologramLib/)
  [![GitHub Downloads](https://img.shields.io/github/downloads/HologramLib/HologramLib/total?color=2ECC71)](https://github.com/HologramLib/HologramLib/releases)


  <p>Leave a :star: if you like this library :octocat:</p>
  <h3>Display Entity Based Hologram Library for Modern Minecraft Servers</h3>
  <p>Packet-based • Feature-rich • Developer-friendly</p>
</div>

---

1. [Installation](https://github.com/HologramLib/HologramLib/wiki/1.-Installation)  
2. [Getting Started](https://github.com/HologramLib/HologramLib/wiki/2.-Getting-Started)  
   - [Creating Holograms](https://github.com/HologramLib/HologramLib/wiki/3.-Creating-Holograms)  
   - [Hologram Management](https://github.com/HologramLib/HologramLib/wiki/4.-Hologram-Management)  
   - [Leaderboards](https://github.com/HologramLib/HologramLib/wiki/5.-Leaderboards)  
   - [Animations](https://github.com/HologramLib/HologramLib/wiki/6.-Animations)  

## 🫨 Features
- **Multi-Type Holograms**    
Text • Blocks • Items • Leaderboards  

- **Dynamic Content**  
Live animations • MiniMessage formatting • ItemsAdder emojis

- **Advanced Mechanics**  
Entity attachment • Per-player visibility • View distance control    

---

## ⚙️ Technical Specifications

**Compatibility**  
| Server Software | Minecraft Versions       | 
|-----------------|--------------------------|
| **Paper**       | 1.19.4 → 1.21.5 ✔️       |
| **Purpur**      | 1.19.4 → 1.21.5 ✔️       | 
| **Folia**       | 1.19.4 → 1.21.5 ✔️       | 
| **Spigot**      | 1.19.4 → 1.21.5 ✔️       | 
| **Bedrock**     | ❌ Not supported         | 
| **Legacy**      | ❌ (1.8 - 1.19.3)        | 

**Dependencies**  
- [PacketEvents](https://www.spigotmc.org/resources/80279/) (Required)

If you want to learn how to use HologramLib in your plugin, check out the detailed guide here:  
👉 [HologramLib Wiki](https://github.com/HologramLib/HologramLib/wiki)

---

## ✈️ Quick Integration

**Step 1: Add Dependency**
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.maximjsx:HologramLib:1.7.4'
}
```

When using maven you also have to add the following repo:
```xml
<repository>
    <id>evoke-snapshots</id>
    <url>https://maven.evokegames.gg/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

**Step 2: Basic Implementation**
```java
HologramManager manager = HologramAPI.getManager().get();

TextHologram hologram = new TextHologram("unique_id")
    .setMiniMessageText("<aqua>Hello world!")
    .setSeeThroughBlocks(false)
    .setShadow(true)
    .setScale(1.5F, 1.5F, 1.5F)
    .setTextOpacity((byte) 200)
    .setBackgroundColor(Color.fromARGB(60, 255, 236, 222).asARGB())
    .setMaxLineWidth(200);

manager.spawn(hologram);
```

---

## 📕 Learning Resources

<img width="536px" src="https://github.com/user-attachments/assets/e4d108d3-e6cb-4d33-b91b-aa989e5e4475" alt="HologramLib Banner">

| Resource | Description | 
|----------|-------------|
| [📖 Complete Wiki](https://github.com/HologramLib/HologramLib/wiki) | Setup guides • Detailed examples • Best practices |
| [💡 Example Plugin](https://github.com/HologramLib/ExamplePlugin) | Production-ready implementations |
| [🎥 Tutorial Series](https://github.com/HologramLib/HologramLib) | Video walkthroughs (Coming Soon) |

---

## 😎 Featured Implementations
- **TypingInChat** ([Modrinth](https://modrinth.com/plugin/typinginchat-plugin)) - Real-time typing visualization

*[Your Project Here 🫵]* - Submit via PR or <a href="https://discord.gg/2UTkYj26B4">Discord</a>!

---

## 👁️ Roadmap & Vision
**2025**  
- Particle-effect holograms
- Interactive holograms
- Improved animation system
- ~~Persistant holograms~~
- PlaceholderAPI

> [!WARNING]
> Persistant holograms & the addon system are still experimental features

## Contributors
Contributions to this repo or the example plugin are welcome!

<!-- CONTRIBUTORS:START -->

| Avatar | Username |
|--------|----------|
| [![](https://avatars.githubusercontent.com/u/114857048?v=4&s=50)](https://github.com/maximjsx) | [maximjsx]( https://github.com/maximjsx ) |
| <img src="https://avatars.githubusercontent.com/u/153451816?v=4" width="50" /> | [misieur]( https://github.com/misieur ) |
| [![](https://avatars.githubusercontent.com/in/15368?v=4&s=50)](https://github.com/apps/github-actions) | [github-actions[bot]]( https://github.com/apps/github-actions ) |
| [![](https://avatars.githubusercontent.com/u/116300577?v=4&s=50)](https://github.com/WhyZerVellasskx) | [WhyZerVellasskx]( https://github.com/WhyZerVellasskx ) |
| [![](https://avatars.githubusercontent.com/u/46348263?v=4&s=50)](https://github.com/RootException) | [RootException]( https://github.com/RootException ) |
| [![](https://avatars.githubusercontent.com/u/13736324?v=4&s=50)](https://github.com/matt11matthew) | [matt11matthew]( https://github.com/matt11matthew ) |

<!-- CONTRIBUTORS:END -->

<div align="center"><sup>Live Statistics</sup></div>

[![img](https://bstats.org/signatures/bukkit/HologramAPI.svg)](https://bstats.org/plugin/bukkit/HologramAPI/19375)

---

<div align="center">
  <sub>Used by 60+ servers | 4,000+ downloads across platforms</sub><br>
  <a href="https://www.spigotmc.org/resources/111746/">SpigotMC</a> •
  <a href="https://hangar.papermc.io/maximjsx/HologramLib">Hangar</a> •
  <a href="https://modrinth.com/plugin/hologramlib">Modrinth</a> •
  <a href="https://github.com/HologramLib/HologramLib/releases/latest">Latest Release</a> •
  <a href="https://discord.gg/2UTkYj26B4">Support</a><br>
  <sub>License: GPL-3.0 | © 2025 <a href="https://github.com/maximjsx/">Maxim</a></sub>
</div>

