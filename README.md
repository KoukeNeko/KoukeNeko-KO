# KoukeNeko-KO Plugin

## 🌟 Introduction

KoukeNeko-KO is a Minecraft plugin that introduces a bleeding/knockout system, preventing instant death and giving teammates a chance to rescue fallen players. When fatally damaged, players enter a bleeding state instead of dying immediately.

## 🔧 Features

- ✅ Players enter bleeding state instead of dying immediately
- ✅ Teammates can rescue bleeding players by right-clicking them
- ✅ Visual effects for bleeding state (particles, potion effects)
- ✅ Configurable rescue clicks and bleeding duration
- ✅ Customizable messages and effects

## 📂 Installation

1. Place the plugin (`KoukeNeko-KO.jar`) into your server's `plugins` folder.
2. Restart your server to activate the plugin.
3. The plugin will generate a default configuration file.

## ⚙️ Configuration

Example configuration (`config.yml`):

```yaml
# KoukeNeko-KO 插件設定檔案

# 瀕死相關設定
bleeding:
  # 瀕死時間（秒）
  duration: 60

  # 救援點選次數
  revival-clicks: 5

  # 瀕死狀態特效
  effects:
    # 是否在瀕死玩家頭上顯示標題
    title: true
    title-text: "&c你正在流血！"
    subtitle-text: "&e需要隊友救援！"

    # 是否在瀕死玩家頭上顯示粒子效果
    particles: true

    # 是否在瀕死時發出音效
    sound: true

  # 瀕死狀態下玩家能否移動
  can-move: false

  # 救援提示資訊
  messages:
    bleeding-start: "&c你進入了瀕死狀態！你有 {time} 秒等待救援，否則將會死亡。"
    bleeding-rescue: "&a玩家 {player} 正在救援你！還需 {clicks} 次點選。"
    bleeding-rescued: "&a你已被 {player} 成功救援！"
    bleeding-death: "&c你因失血過多而死亡！"

    rescuer-start: "&a你正在救援 {player}！還需 {clicks} 次點選。"
    rescuer-success: "&a你成功救援了 {player}！"

    cannot-rescue-self: "&c你不能救援自己！"
```

## 🚀 Commands

- `/koukenekokoreload` (Permission: `koukeneko.ko.admin`)
  - Aliases: `knkoreload`
  - Reloads the plugin's configuration.

## 🔑 Permissions

| Permission              | Description                   |
|-------------------------|-------------------------------|
| `koukeneko.ko.admin`    | Allows use of the reload command |

## 🎮 How It Works

1. When a player receives fatal damage, they enter a bleeding state instead of dying
2. The player appears laying on the ground with visual effects (particles, screen effects)
3. Other players can rescue them by right-clicking multiple times
4. If not rescued within the configured time limit, the player dies
5. If successfully rescued, the player regains half their health

---

## 🌟 簡介

KoukeNeko-KO 是一款 Minecraft 插件，引入了流血/擊倒系統，防止玩家立即死亡並給予隊友救援的機會。當玩家受到致命傷害時，會進入瀕死狀態而非立即死亡。

## 🔧 功能特色

- ✅ 玩家受到致命傷害時進入瀕死狀態，而非立即死亡
- ✅ 隊友可以透過右鍵點選來救援瀕死玩家
- ✅ 瀕死狀態有視覺效果（粒子、藥水效果）
- ✅ 可自訂救援點選次數和瀕死持續時間
- ✅ 可自訂訊息和效果

## 📂 安裝方法

1. 將插件 (`KoukeNeko-KO.jar`) 放入伺服器的 `plugins` 資料夾。
2. 重新啟動伺服器以啟用插件。
3. 插件將會產生預設設定檔。

## 🚀 指令

- `/koukenekokoreload` (權限: `koukeneko.ko.admin`)
  - 別名: `knkoreload`
  - 重新載入插件的設定檔。

## 🔑 許可權

| 權限                   | 說明                 |
|----------------------|---------------------|
| `koukeneko.ko.admin` | 允許使用重新載入指令   |

## 🎮 運作方式

1. 當玩家受到致命傷害時，不會立即死亡，而是進入瀕死狀態
2. 玩家會呈現倒地狀態，並有視覺效果（粒子、螢幕效果）
3. 其他玩家可以透過多次右鍵點選來救援瀕死玩家
4. 如果在設定的時間內沒有被救援，玩家將會死亡
5. 如果成功被救援，玩家將恢復一半的生命值

---

🚀 Enjoy your game! 祝你遊戲愉快！
